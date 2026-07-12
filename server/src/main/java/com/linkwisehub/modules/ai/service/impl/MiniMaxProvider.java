package com.linkwisehub.modules.ai.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.linkwisehub.config.AiConfig;
import com.linkwisehub.modules.ai.service.AiProvider;
import com.linkwisehub.common.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * MiniMax大模型服务实现
 * 支持流式响应、详细的错误分类和友好的错误提示
 */
@Slf4j
@Service("miniMaxProvider")
public class MiniMaxProvider implements AiProvider {

    @Autowired
    private AiConfig aiConfig;

    @Override
    public String chat(List<ChatMessage> messages) {
        Instant startTime = Instant.now();

        try {
            // 验证配置
            validateConfig();

            // 构建请求体
            JSONObject requestBody = buildRequestBody(messages);

            log.info("调用MiniMax API - 模型: {}, 消息数: {}",
                aiConfig.getMinimax().getModel(), messages.size());

            // 发送请求
            String response = sendRequest(requestBody);

            long duration = Duration.between(startTime, Instant.now()).toMillis();
            log.info("AI请求成功 - 耗时: {}ms", duration);

            // 解析响应
            return parseResponse(response);

        } catch (AiServiceException e) {
            throw e;
        } catch (WebClientResponseException e) {
            handleHttpError(e);
            throw AiServiceException.serviceUnavailable();
        } catch (Exception e) {
            log.error("MiniMax服务调用异常", e);
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

            log.info("调用MiniMax API（流式） - 模型: {}, 消息数: {}",
                aiConfig.getMinimax().getModel(), messages.size());

            // 发送流式请求
            return sendStreamRequest(requestBody, contentConsumer);

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("MiniMax服务流式调用异常", e);
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
        AiConfig.MiniMaxConfig minimax = aiConfig.getMinimax();

        WebClient webClient = WebClient.builder()
                .baseUrl(minimax.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + minimax.getApiKey())
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
            // MiniMax 流式响应格式与 OpenAI 兼容
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
        AiConfig.MiniMaxConfig minimax = aiConfig.getMinimax();
        if (minimax == null) {
            log.error("AI配置错误: MiniMax配置缺失");
            throw AiServiceException.configMissing("MiniMax配置缺失");
        }
        if (minimax.getApiKey() == null || minimax.getApiKey().trim().isEmpty()) {
            log.error("AI配置错误: API Key未配置");
            throw AiServiceException.configMissing("API Key未配置");
        }
        if (minimax.getModel() == null || minimax.getModel().trim().isEmpty()) {
            log.error("AI配置错误: 模型名称未配置");
            throw AiServiceException.configMissing("模型名称未配置");
        }
        if (minimax.getBaseUrl() == null || minimax.getBaseUrl().trim().isEmpty()) {
            log.error("AI配置错误: API地址未配置");
            throw AiServiceException.configMissing("API地址未配置");
        }
    }

    /**
     * 构建请求体
     */
    private JSONObject buildRequestBody(List<ChatMessage> messages) {
        AiConfig.MiniMaxConfig minimax = aiConfig.getMinimax();
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", minimax.getModel());

        JSONArray msgs = new JSONArray();
        for (ChatMessage msg : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", msg.getRole());
            msgObj.put("content", msg.getContent());
            msgs.add(msgObj);
        }
        requestBody.put("messages", msgs);

        // 添加参数
        requestBody.put("max_tokens", minimax.getMaxTokens());
        requestBody.put("temperature", minimax.getTemperature());

        return requestBody;
    }

    /**
     * 发送请求
     */
    private String sendRequest(JSONObject requestBody) {
        AiConfig.MiniMaxConfig minimax = aiConfig.getMinimax();
        WebClient webClient = WebClient.builder()
                .baseUrl(minimax.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + minimax.getApiKey())
                .build();

        try {
            return webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody.toJSONString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(minimax.getTimeout()))
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
     * 处理HTTP错误
     */
    private void handleHttpError(WebClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        String responseBody = e.getResponseBodyAsString();

        log.error("MiniMax HTTP请求失败 - 状态码: {}, 响应: {}", statusCode, responseBody);

        switch (statusCode) {
            case 400:
                throw AiServiceException.configMissing("请求参数错误");
            case 401:
            case 403:
                if (responseBody.contains("invalid") || responseBody.contains("Incorrect")) {
                    throw AiServiceException.apiKeyInvalid();
                }
                throw AiServiceException.apiKeyInvalid();
            case 429:
                throw AiServiceException.rateLimited();
            case 500:
            case 502:
            case 503:
            case 504:
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
        String errorMsg = json.getString("error");
        if (errorMsg == null) {
            errorMsg = json.getString("message");
        }

        log.error("MiniMax API返回错误 - 消息: {}", errorMsg);

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
        }

        throw AiServiceException.serviceUnavailable();
    }

    @Override
    public String getProviderName() {
        return "MiniMax";
    }
}
