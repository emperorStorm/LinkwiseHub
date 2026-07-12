package com.linkwisehub.modules.ai.service;

import lombok.Data;
import java.util.function.Consumer;

/**
 * AI服务提供商接口
 * 定义AI对话的统一接口，支持多种AI服务提供商
 */
public interface AiProvider {

    /**
     * 发送对话请求
     * @param messages 历史消息列表
     * @return AI回复内容
     */
    String chat(java.util.List<ChatMessage> messages);

    /**
     * 流式发送对话请求，模拟打字机效果
     * @param messages 历史消息列表
     * @param contentConsumer 内容消费者，用于接收流式内容
     * @return 是否成功完成
     */
    default boolean chatStream(java.util.List<ChatMessage> messages, Consumer<String> contentConsumer) {
        // 默认实现：先获取完整回复，然后逐字符发送
        String fullResponse = chat(messages);
        return streamText(fullResponse, contentConsumer);
    }

    /**
     * 逐字符流式发送文本，模拟打字机效果
     * @param text 要发送的文本
     * @param contentConsumer 内容消费者
     * @return 是否成功完成
     */
    default boolean streamText(String text, Consumer<String> contentConsumer) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        // 按句子或词组分割，实现更自然的打字机效果
        String[] sentences = text.split("(?<=[。！？；\n])|(?<=[.!?;])");
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i];
            // 如果句子为空或只有空白字符，跳过
            if (sentence.trim().isEmpty()) {
                continue;
            }
            // 将句子添加到缓冲区
            buffer.append(sentence);
            // 发送完整句子
            contentConsumer.accept(buffer.toString());
            // 如果不是最后一个句子，添加分隔符
            if (i < sentences.length - 1) {
                // 等待一小段时间，模拟打字间隔
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 获取提供商名称
     */
    String getProviderName();

    /**
     * 聊天消息
     */
    @Data
    class ChatMessage {
        private String role;      // user / assistant / system
        private String content;

        public ChatMessage() {}

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
