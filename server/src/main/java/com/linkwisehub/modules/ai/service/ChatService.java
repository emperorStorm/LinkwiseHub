package com.linkwisehub.modules.ai.service;

import com.linkwisehub.modules.ai.entity.Conversation;
import com.linkwisehub.modules.ai.entity.Message;
import com.linkwisehub.modules.ai.dto.ChatResponse;
import java.util.List;
import java.util.function.Consumer;

public interface ChatService {
    List<Conversation> getConversations();
    Conversation createConversation(String title);
    void deleteConversation(Long id);
    List<Message> getMessages(Long conversationId);

    /**
     * 发送消息并获取AI回复
     * @param conversationId 会话ID
     * @param message 用户消息
     * @param model 选择的模型，为空时使用默认模型
     * @return AI回复
     */
    ChatResponse chat(Long conversationId, String message, String model);

    ChatResponse chat(Long conversationId, String message, String model, String scope);

    /**
     * 发送消息并获取AI回复（兼容旧接口）
     * @param conversationId 会话ID
     * @param message 用户消息
     * @return AI回复
     */
    ChatResponse chatWithDefaultModel(Long conversationId, String message);

    /**
     * 流式聊天，返回完整的AI回复内容
     * @param conversationId 会话ID
     * @param message 用户消息
     * @param model 选择的模型，为空时使用默认模型
     * @return AI完整回复内容
     */
    String chatStream(Long conversationId, String message, String model);

    String chatStream(Long conversationId, String message, String model, String scope);

    /**
     * 流式聊天，返回完整的AI回复内容（兼容旧接口）
     * @param conversationId 会话ID
     * @param message 用户消息
     * @return AI完整回复内容
     */
    String chatStreamWithDefaultModel(Long conversationId, String message);

    /**
     * 流式聊天，通过回调函数逐段返回内容
     * @param conversationId 会话ID
     * @param message 用户消息
     * @param model 选择的模型，为空时使用默认模型
     * @param contentConsumer 内容消费者，用于接收流式内容
     * @return 是否成功完成
     */
    boolean chatStream(Long conversationId, String message, String model, Consumer<String> contentConsumer);

    boolean chatStream(Long conversationId, String message, String model, String scope, Consumer<String> contentConsumer);

    /**
     * 流式聊天，通过回调函数逐段返回内容（兼容旧接口）
     * @param conversationId 会话ID
     * @param message 用户消息
     * @param contentConsumer 内容消费者，用于接收流式内容
     * @return 是否成功完成
     */
    boolean chatStreamWithDefaultModel(Long conversationId, String message, Consumer<String> contentConsumer);
}
