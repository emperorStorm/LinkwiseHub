package com.linkwisehub.modules.ai.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.AiServiceException;
import com.linkwisehub.config.AiConfig;
import com.linkwisehub.modules.ai.service.AiProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * DeepSeek大模型服务实现。
 * 通过OpenAI兼容接口接入，保持与现有聊天模块一致的请求和响应格式。
 */
@Slf4j
@Service("deepSeekProvider")
public class DeepSeekProvider implements AiProvider {

    @Autowired
    private AiConfig aiConfig;

    @Override
    public String chat(List<ChatMessage> messages) {
        Instant startTime = Instant.now();

        try {
            validateConfig();
            JSONObject requestBody = buildRequestBody(messages);

            log.info("调用DeepSeek API - 模型: {}, 消息数: {}",
                aiConfig.getDeepseek().getModel(), messages.size());

            String response = sendRequest(requestBody);
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            log.info("DeepSeek请求成功 - 耗时: {}ms", duration);

            return parseResponse(response);
        } catch (AiServiceException e) {
            throw e;
        } catch (WebClientResponseException e) {
            handleHttpError(e);
            throw AiServiceException.serviceUnavailable();
        } catch (Exception e) {
            log.error("DeepSeek服务调用异常", e);
            if (e.getCause() instanceof TimeoutException) {
                throw new AiServiceException(ErrorCode.AI_SERVICE_TIMEOUT, "AI服务响应超时");
            }
            throw new AiServiceException(ErrorCode.AI_NETWORK_ERROR, "网络连接失败: " + e.getMessage());
        }
    }

    @Override
    public boolean chatStream(List<ChatMessage> messages, Consumer<String> contentConsumer) {
        try {
            validateConfig();
            JSONObject requestBody = buildRequestBody(messages);
            // DeepSeek兼容OpenAI流式协议，开启stream后返回SSE事件。
            requestBody.put("stream", true);

            log.info("调用DeepSeek API（流式） - 模型: {}, 消息数: {}",
                aiConfig.getDeepseek().getModel(), messages.size());

            return sendStreamRequest(requestBody, contentConsumer);
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek服务流式调用异常", e);
            if (e.getCause() instanceof TimeoutException) {
                throw new AiServiceException(ErrorCode.AI_SERVICE_TIMEOUT, "AI服务响应超时");
            }
            throw new AiServiceException(ErrorCode.AI_NETWORK_ERROR, "网络连接失败: " + e.getMessage());
        }
    }

    /**
     * 校验DeepSeek调用所需配置，避免运行时返回不清晰的第三方错误。
     */
    private void validateConfig() {
        AiConfig.DeepSeekConfig deepseek = aiConfig.getDeepseek();
        if (deepseek == null) {
            log.error("AI配置错误: DeepSeek配置缺失");
            throw AiServiceException.configMissing("DeepSeek配置缺失");
        }
        if (deepseek.getApiKey() == null || deepseek.getApiKey().trim().isEmpty()) {
            log.error("AI配置错误: DeepSeek API Key未配置");
            throw AiServiceException.configMissing("DeepSeek API Key未配置");
        }
        if (deepseek.getModel() == null || deepseek.getModel().trim().isEmpty()) {
            log.error("AI配置错误: DeepSeek模型名称未配置");
            throw AiServiceException.configMissing("DeepSeek模型名称未配置");
        }
        if (deepseek.getBaseUrl() == null || deepseek.getBaseUrl().trim().isEmpty()) {
            log.error("AI配置错误: DeepSeek API地址未配置");
            throw AiServiceException.configMissing("DeepSeek API地址未配置");
        }
    }

    /**
     * 构建OpenAI兼容的聊天请求体。
     */
    private JSONObject buildRequestBody(List<ChatMessage> messages) {
        AiConfig.DeepSeekConfig deepseek = aiConfig.getDeepseek();
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", deepseek.getModel());

        JSONArray requestMessages = new JSONArray();
        for (ChatMessage msg : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", msg.getRole());
            msgObj.put("content", msg.getContent());
            requestMessages.add(msgObj);
        }
        requestBody.put("messages", requestMessages);
        requestBody.put("max_tokens", deepseek.getMaxTokens());
        requestBody.put("temperature", deepseek.getTemperature());
        return requestBody;
    }

    /**
     * 发送非流式请求并返回原始响应。
     */
    private String sendRequest(JSONObject requestBody) {
        AiConfig.DeepSeekConfig deepseek = aiConfig.getDeepseek();
        WebClient webClient = buildWebClient(deepseek);

        try {
            return webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody.toJSONString())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(deepseek.getTimeout()))
                .block();
        } catch (Exception e) {
            if (e.getCause() instanceof TimeoutException
                || e.getMessage() != null && e.getMessage().contains("timeout")) {
                throw new RuntimeException("AI服务响应超时", e);
            }
            throw e;
        }
    }

    /**
     * 发送流式请求，按累积文本回调给上层用于打字机效果。
     */
    private boolean sendStreamRequest(JSONObject requestBody, Consumer<String> contentConsumer) {
        AiConfig.DeepSeekConfig deepseek = aiConfig.getDeepseek();
        WebClient webClient = buildWebClient(deepseek);
        StringBuilder fullContent = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] error = new Throwable[1];

        try {
            webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody.toJSONString())
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMillis(deepseek.getTimeout()))
                .subscribe(
                    chunk -> appendStreamChunk(chunk, fullContent, contentConsumer),
                    e -> {
                        log.error("DeepSeek流式响应错误", e);
                        error[0] = e;
                        latch.countDown();
                    },
                    () -> {
                        log.info("DeepSeek流式响应完成");
                        latch.countDown();
                    }
                );

            boolean completed = latch.await(120, TimeUnit.SECONDS);
            if (!completed) {
                throw new RuntimeException("AI服务响应超时");
            }
            if (error[0] != null) {
                throw new RuntimeException(error[0]);
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("DeepSeek流式请求被中断");
            return false;
        } catch (Exception e) {
            log.error("发送DeepSeek流式请求失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析一段SSE内容并追加到完整回复中。
     */
    private void appendStreamChunk(String chunk, StringBuilder fullContent, Consumer<String> contentConsumer) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }

        String[] lines = chunk.split("\\r?\\n");
        for (String line : lines) {
            String content = parseSSEData(line);
            if (content != null && !content.isEmpty()) {
                fullContent.append(content);
                contentConsumer.accept(fullContent.toString());
            }
        }
    }

    /**
     * 从OpenAI兼容SSE行中提取增量文本。
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
        if ("[DONE]".equals(data)) {
            return null;
        }

        try {
            JSONObject json = JSON.parseObject(data);
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }

            JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
            if (delta != null && delta.containsKey("content")) {
                return delta.getString("content");
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            if (message != null && message.containsKey("content")) {
                return message.getString("content");
            }
        } catch (Exception e) {
            log.debug("解析DeepSeek SSE数据失败: {}", data);
        }
        return null;
    }

    /**
     * 创建带通用Header的WebClient，避免每次重复配置请求头。
     */
    private WebClient buildWebClient(AiConfig.DeepSeekConfig deepseek) {
        return WebClient.builder()
            .baseUrl(deepseek.getBaseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Authorization", "Bearer " + deepseek.getApiKey())
            .build();
    }

    /**
     * 按HTTP状态码转换为业务侧统一AI异常。
     */
    private void handleHttpError(WebClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        String responseBody = e.getResponseBodyAsString();

        log.error("DeepSeek HTTP请求失败 - 状态码: {}, 响应: {}", statusCode, responseBody);

        switch (statusCode) {
            case 400:
                throw AiServiceException.configMissing("请求参数错误");
            case 401:
            case 403:
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
     * 解析OpenAI兼容的非流式响应。
     */
    private String parseResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.error("DeepSeek返回空响应");
            throw AiServiceException.parseError("空响应");
        }

        try {
            JSONObject json = JSON.parseObject(response);
            if (json.containsKey("error")) {
                handleApiError(json);
            }

            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("DeepSeek响应缺少choices字段: {}", response);
                throw AiServiceException.parseError("响应格式异常");
            }

            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            if (message == null) {
                log.error("DeepSeek响应缺少message字段: {}", response);
                throw AiServiceException.parseError("响应内容异常");
            }

            String content = message.getString("content");
            if (content == null || content.trim().isEmpty()) {
                log.warn("DeepSeek返回空内容");
                return "抱歉，我现在无法生成回复，请稍后重试。";
            }
            return content;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析DeepSeek响应失败: {}", e.getMessage());
            log.debug("DeepSeek原始响应: {}", response);
            throw AiServiceException.parseError(e.getMessage());
        }
    }

    /**
     * 处理DeepSeek返回的错误对象或错误字符串。
     */
    private void handleApiError(JSONObject json) {
        Object error = json.get("error");
        String errorMsg = null;

        if (error instanceof JSONObject) {
            errorMsg = ((JSONObject) error).getString("message");
        } else if (error != null) {
            errorMsg = String.valueOf(error);
        }
        if (errorMsg == null) {
            errorMsg = json.getString("message");
        }

        log.error("DeepSeek API返回错误 - 消息: {}", errorMsg);

        if (errorMsg != null) {
            String lowerMsg = errorMsg.toLowerCase();
            if (lowerMsg.contains("invalid") && lowerMsg.contains("key")) {
                throw AiServiceException.apiKeyInvalid();
            }
            if (lowerMsg.contains("expired")) {
                throw AiServiceException.apiKeyExpired();
            }
            if (lowerMsg.contains("quota") || lowerMsg.contains("insufficient balance")
                || errorMsg.contains("额度")) {
                throw AiServiceException.quotaExceeded();
            }
            if (lowerMsg.contains("rate limit") || lowerMsg.contains("too many requests")
                || errorMsg.contains("限流")) {
                throw AiServiceException.rateLimited();
            }
            if (lowerMsg.contains("model") && (lowerMsg.contains("not found")
                || lowerMsg.contains("unavailable"))) {
                throw AiServiceException.modelUnavailable(aiConfig.getDeepseek().getModel());
            }
        }

        throw AiServiceException.serviceUnavailable();
    }

    @Override
    public String getProviderName() {
        return "DeepSeek";
    }
}
