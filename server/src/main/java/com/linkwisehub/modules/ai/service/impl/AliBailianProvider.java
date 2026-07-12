package com.linkwisehub.modules.ai.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.linkwisehub.config.AiConfig;
import com.linkwisehub.modules.ai.service.AiProvider;
import com.linkwisehub.common.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * 阿里云百炼大模型服务实现
 * 支持重试机制、详细的错误分类和友好的错误提示
 */
@Slf4j
@Service("aliBailianProvider")
public class AliBailianProvider implements AiProvider {

    @Autowired
    private AiConfig aiConfig;

    /**
     * 最大重试次数
     */
    @Value("${ai.retry.max-attempts:3}")
    private int maxRetryAttempts;

    /**
     * 重试间隔时间（毫秒）
     */
    @Value("${ai.retry.delay-ms:1000}")
    private long retryDelayMs;

    /**
     * 最大重试间隔时间（毫秒）
     */
    @Value("${ai.retry.max-delay-ms:5000}")
    private long maxRetryDelayMs;

    @Override
    public String chat(List<ChatMessage> messages) {
        Instant startTime = Instant.now();

        try {
            // 验证配置
            validateConfig();

            // 构建请求体
            JSONObject requestBody = buildRequestBody(messages);

            log.info("调用阿里云百炼API - 模型: {}, 消息数: {}, 最大重试: {}次",
                aiConfig.getAliyun().getModel(), messages.size(), maxRetryAttempts);

            // 发送请求（带重试机制）
            String response = sendRequestWithRetry(requestBody);

            long duration = Duration.between(startTime, Instant.now()).toMillis();
            log.info("AI请求成功 - 耗时: {}ms", duration);

            // 解析响应
            return parseResponse(response);

        } catch (AiServiceException e) {
            // 已经是处理过的AI异常，直接抛出
            throw e;
        } catch (WebClientResponseException e) {
            // 处理HTTP响应错误
            handleHttpError(e);
            throw AiServiceException.serviceUnavailable();
        } catch (Exception e) {
            log.error("AI服务调用异常", e);
            if (e.getCause() instanceof TimeoutException) {
                throw new AiServiceException(com.linkwisehub.common.ErrorCode.AI_SERVICE_TIMEOUT, "AI服务响应超时");
            }
            throw new AiServiceException(com.linkwisehub.common.ErrorCode.AI_NETWORK_ERROR, "网络连接失败: " + e.getMessage());
        }
    }

    @Override
    public boolean chatStream(List<ChatMessage> messages, java.util.function.Consumer<String> contentConsumer) {
        Instant startTime = Instant.now();

        try {
            // 验证配置
            validateConfig();

            // 构建请求体
            JSONObject requestBody = buildRequestBody(messages);
            // 启用流式响应
            requestBody.put("stream", true);

            log.info("调用阿里云百炼API（流式） - 模型: {}, 消息数: {}",
                aiConfig.getAliyun().getModel(), messages.size());

            // 发送流式请求
            return sendStreamRequest(requestBody, contentConsumer);

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI服务流式调用异常", e);
            if (e.getCause() instanceof TimeoutException) {
                throw new AiServiceException(com.linkwisehub.common.ErrorCode.AI_SERVICE_TIMEOUT, "AI服务响应超时");
            }
            throw new AiServiceException(com.linkwisehub.common.ErrorCode.AI_NETWORK_ERROR, "网络连接失败: " + e.getMessage());
        }
    }

    /**
     * 发送流式请求
     */
    private boolean sendStreamRequest(JSONObject requestBody, java.util.function.Consumer<String> contentConsumer) {
        AiConfig.AliyunConfig aliyun = aiConfig.getAliyun();

        WebClient webClient = WebClient.builder()
                .baseUrl(aliyun.getBaseUrl())
                .defaultHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + aliyun.getApiKey())
                .build();

        final StringBuilder fullContent = new StringBuilder();
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final Throwable[] error = new Throwable[1];

        try {
            webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody.toJSONString())
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(
                    chunk -> {
                        try {
                            String[] lines = chunk.split("\\r?\\n");
                            for (String line : lines) {
                                String content = parseSSEData(line);
                                if (content != null && !content.isEmpty()) {
                                    fullContent.append(content);
                                    contentConsumer.accept(fullContent.toString());
                                }
                            }
                        } catch (Exception e) {
                            log.error("处理SSE数据失败", e);
                        }
                    },
                    e -> {
                        log.error("流式响应错误", e);
                        error[0] = e;
                        latch.countDown();
                    },
                    () -> {
                        log.info("流式响应完成");
                        latch.countDown();
                    }
                );

            // 等待流完成，最多等待2分钟
            latch.await(120, java.util.concurrent.TimeUnit.SECONDS);

            if (error[0] != null) {
                throw new RuntimeException(error[0]);
            }

            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("流式请求被中断");
            return false;
        } catch (Exception e) {
            log.error("发送流式请求失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析SSE数据行
     */
    private String parseSSEData(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }

        String data = line.trim();
        if (data.isEmpty() || data.startsWith(":")) {
            return null;
        }
        if (data.startsWith("data:")) {
            data = data.substring(5).trim();
        }

        // 检查是否结束
        if ("[DONE]".equals(data)) {
            return null;
        }

        try {
            JSONObject json = JSON.parseObject(data);
            // 解析流式响应
            if (json.containsKey("choices")) {
                JSONArray choices = json.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                    if (delta != null && delta.containsKey("content")) {
                        return delta.getString("content");
                    }
                    JSONObject message = choices.getJSONObject(0).getJSONObject("message");
                    if (message != null && message.containsKey("content")) {
                        return message.getString("content");
                    }
                }
            }
        } catch (Exception e) {
            log.debug("解析SSE数据失败: {}", data);
        }

        return null;
    }

    /**
     * 验证配置
     */
    private void validateConfig() {
        AiConfig.AliyunConfig aliyun = aiConfig.getAliyun();
        if (aliyun == null) {
            log.error("AI配置错误: 阿里云配置缺失");
            throw AiServiceException.configMissing("阿里云配置缺失");
        }
        if (aliyun.getApiKey() == null || aliyun.getApiKey().trim().isEmpty()) {
            log.error("AI配置错误: API Key未配置");
            throw AiServiceException.configMissing("API Key未配置");
        }
        if (aliyun.getModel() == null || aliyun.getModel().trim().isEmpty()) {
            log.error("AI配置错误: 模型名称未配置");
            throw AiServiceException.configMissing("模型名称未配置");
        }
        if (aliyun.getBaseUrl() == null || aliyun.getBaseUrl().trim().isEmpty()) {
            log.error("AI配置错误: API地址未配置");
            throw AiServiceException.configMissing("API地址未配置");
        }
    }

    /**
     * 构建请求体
     */
    private JSONObject buildRequestBody(List<ChatMessage> messages) {
        AiConfig.AliyunConfig aliyun = aiConfig.getAliyun();
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", aliyun.getModel());

        JSONArray msgs = new JSONArray();
        for (ChatMessage msg : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", msg.getRole());
            msgObj.put("content", msg.getContent());
            msgs.add(msgObj);
        }
        requestBody.put("messages", msgs);

        // 添加参数
        requestBody.put("max_tokens", aliyun.getMaxTokens());
        requestBody.put("temperature", aliyun.getTemperature());

        return requestBody;
    }

    /**
     * 发送请求（带重试机制）
     */
    private String sendRequestWithRetry(JSONObject requestBody) {
        AiConfig.AliyunConfig aliyun = aiConfig.getAliyun();
        WebClient webClient = WebClient.builder()
                .baseUrl(aliyun.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + aliyun.getApiKey())
                .build();

        // 构建重试策略（指数退避）
        Retry retryStrategy = Retry.backoff(maxRetryAttempts, Duration.ofMillis(retryDelayMs))
                .maxBackoff(Duration.ofMillis(maxRetryDelayMs))
                .filter(this::isRetryableException)
                .doBeforeRetry(signal -> {
                    log.warn("AI请求失败，准备第{}次重试 - 错误: {}",
                        signal.totalRetries() + 1, signal.failure().getMessage());
                })
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    log.error("AI请求重试{}次后仍失败", maxRetryAttempts);
                    return retrySignal.failure();
                });

        try {
            return webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody.toJSONString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(aiConfig.getAliyun().getTimeout()))
                    .retryWhen(retryStrategy)
                    .block();
        } catch (Exception e) {
            if (e.getCause() instanceof TimeoutException ||
                e.getMessage() != null && e.getMessage().contains("timeout")) {
                throw new RuntimeException("AI服务响应超时", e);
            }
            throw e;
        }
    }

    /**
     * 判断异常是否可重试
     */
    private boolean isRetryableException(Throwable throwable) {
        // 网络错误、超时、服务不可用等可重试
        if (throwable instanceof TimeoutException) {
            return true;
        }
        if (throwable instanceof java.net.ConnectException) {
            return true;
        }
        if (throwable instanceof java.net.SocketTimeoutException) {
            return true;
        }
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) throwable;
            int statusCode = wcre.getStatusCode().value();
            // 502, 503, 504 网关错误可重试
            // 429 限流可重试
            // 401, 403 认证错误不可重试
            return statusCode == 502 || statusCode == 503 || statusCode == 504 || statusCode == 429;
        }
        // 解析响应时的一些临时错误也可重试
        if (throwable instanceof com.alibaba.fastjson.JSONException) {
            return true;
        }
        return false;
    }

    /**
     * 处理HTTP错误
     */
    private void handleHttpError(WebClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        String responseBody = e.getResponseBodyAsString();

        log.error("AI HTTP请求失败 - 状态码: {}, 响应: {}", statusCode, responseBody);

        switch (statusCode) {
            case 400:
                // 请求参数错误
                try {
                    JSONObject errorJson = JSON.parseObject(responseBody);
                    String errorMsg = errorJson.getString("error");
                    if (errorMsg != null && errorMsg.contains("invalid request")) {
                        throw AiServiceException.configMissing("请求参数无效");
                    }
                } catch (AiServiceException ae) {
                    throw ae;
                } catch (Exception ex) {
                    throw AiServiceException.configMissing("请求参数错误");
                }
                break;

            case 401:
            case 403:
                // 认证授权错误
                if (responseBody.contains("invalid api key") ||
                    responseBody.contains("Incorrect API key")) {
                    throw AiServiceException.apiKeyInvalid();
                }
                if (responseBody.contains("expired")) {
                    throw AiServiceException.apiKeyExpired();
                }
                throw AiServiceException.apiKeyInvalid();

            case 429:
                // 请求过于频繁
                throw AiServiceException.rateLimited();

            case 500:
            case 502:
            case 503:
            case 504:
                // 服务器错误，可重试
                throw AiServiceException.serviceUnavailable();

            default:
                throw AiServiceException.networkError("HTTP " + statusCode);
        }
    }

    /**
     * 解析API响应
     */
    private String parseResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.error("AI返回空响应");
            throw AiServiceException.parseError("空响应");
        }

        try {
            JSONObject json = JSON.parseObject(response);

            // 检查API错误
            if (json.containsKey("error")) {
                handleApiError(json);
            }

            // 解析正常响应
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("AI响应缺少choices字段: {}", response);
                throw AiServiceException.parseError("响应格式异常");
            }

            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            if (message == null) {
                log.error("AI响应缺少message字段: {}", response);
                throw AiServiceException.parseError("响应内容异常");
            }

            String content = message.getString("content");
            if (content == null || content.trim().isEmpty()) {
                log.warn("AI返回空内容");
                return "抱歉，我现在无法生成回复，请稍后重试。";
            }

            return content;

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析AI响应失败: {}", e.getMessage());
            log.debug("原始响应: {}", response);
            throw AiServiceException.parseError(e.getMessage());
        }
    }

    /**
     * 处理API返回的错误
     */
    private void handleApiError(JSONObject json) {
        String errorType = json.getString("error_type");
        String errorMsg = json.getString("error");

        if (errorMsg == null) {
            errorMsg = json.getString("message");
        }
        if (errorMsg == null && json.containsKey("error_message")) {
            errorMsg = json.getString("error_message");
        }

        log.error("AI API返回错误 - 类型: {}, 消息: {}", errorType, errorMsg);

        // 根据错误类型判断
        if (errorMsg != null) {
            if (errorMsg.contains("Invalid API Key") ||
                errorMsg.contains("incorrect api key") ||
                errorMsg.contains("invalid api key")) {
                throw AiServiceException.apiKeyInvalid();
            }
            if (errorMsg.contains("expired")) {
                throw AiServiceException.apiKeyExpired();
            }
            if (errorMsg.contains("quota") || errorMsg.contains("额度") ||
                errorMsg.contains("limit")) {
                throw AiServiceException.quotaExceeded();
            }
            if (errorMsg.contains("rate limit") || errorMsg.contains("限流")) {
                throw AiServiceException.rateLimited();
            }
            if (errorMsg.contains("model") && errorMsg.contains("not found")) {
                throw AiServiceException.modelUnavailable(aiConfig.getAliyun().getModel());
            }
        }

        throw AiServiceException.serviceUnavailable();
    }

    @Override
    public String getProviderName() {
        return "阿里云百炼";
    }
}
