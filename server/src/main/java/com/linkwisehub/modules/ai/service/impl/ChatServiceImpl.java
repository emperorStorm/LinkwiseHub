package com.linkwisehub.modules.ai.service.impl;

import com.linkwisehub.config.AiConfig;
import com.linkwisehub.modules.ai.entity.Conversation;
import com.linkwisehub.modules.ai.entity.ConversationSummary;
import com.linkwisehub.modules.ai.entity.Message;
import com.linkwisehub.modules.ai.dto.ChatResponse;
import com.linkwisehub.modules.ai.mapper.ConversationMapper;
import com.linkwisehub.modules.ai.mapper.MessageMapper;
import com.linkwisehub.modules.ai.mapper.ConversationSummaryMapper;
import com.linkwisehub.modules.ai.service.AiProvider;
import com.linkwisehub.modules.ai.service.ChatService;
import com.linkwisehub.modules.ai.service.LocalKnowledgeSearchService;
import com.linkwisehub.common.exception.AiServiceException;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 聊天服务实现类
 * 负责对话管理和AI响应生成，支持多模型动态切换
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    /**
     * 最大消息长度限制
     */
    private static final int MAX_MESSAGE_LENGTH = 2000;

    /**
     * MiniMax模型标识
     */
    private static final String MODEL_MINIMAX = "MiniMax-M2.7-highspeed";

    /**
     * DeepSeek模型标识
     */
    private static final String MODEL_DEEPSEEK = "deepseek-v4-flash";

    /**
     * 通义千问模型标识
     */
    private static final String MODEL_QWEN = "qwen3.5-plus";

    /**
     * Ollama本地模型标识
     */
    private static final String MODEL_OLLAMA_QWEN = "qwen3.6:latest";

    private static final String MODEL_NAME_DEEPSEEK = "DeepSeek V4 Flash";

    private static final String MODEL_NAME_QWEN = "通义千问";

    private static final String MODEL_NAME_MINIMAX = "MiniMax";

    private static final String MODEL_NAME_OLLAMA_QWEN = "qwen3.6（ollama）";

    /**
     * 默认会话标题
     */
    private static final String DEFAULT_CONVERSATION_TITLE = "新对话";

    /**
     * 会话标题最大长度
     */
    private static final int MAX_TITLE_LENGTH = 24;

    private static final String SCOPE_INTERNET = "internet";

    private static final String SCOPE_LOCAL_KNOWLEDGE = "local_knowledge";

    private static final int MAX_LOCAL_CONTEXT_LENGTH = 6000;

    private static final String LOCAL_KNOWLEDGE_NO_HIT_REPLY =
        "本地知识库中暂未找到相关资料。你可以补充文档名称、关键词或更多背景，我再帮你继续查找；也可以切换到互联网范围提问。";

    private static final String AI_EMPTY_REPLY =
        "抱歉，我没有从模型获取到可用回答。你可以换个问法，或补充要查询的文档、关键词后再试。";

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ConversationSummaryMapper conversationSummaryMapper;

    @Autowired
    private AiConfig aiConfig;

    // 注入阿里云百炼AI服务提供者
    @Autowired
    @Qualifier("aliBailianProvider")
    private AiProvider aliYunProvider;

    // 注入MiniMax AI服务提供者
    @Autowired
    @Qualifier("miniMaxProvider")
    private AiProvider miniMaxProvider;

    // 注入DeepSeek AI服务提供者
    @Autowired
    @Qualifier("deepSeekProvider")
    private AiProvider deepSeekProvider;

    // 注入Ollama本地AI服务提供者
    @Autowired
    @Qualifier("ollamaProvider")
    private AiProvider ollamaProvider;

    @Autowired
    private LocalKnowledgeSearchService localKnowledgeSearchService;

    /**
     * 根据模型名称获取对应的Provider
     * @param model 模型名称
     * @return AI服务提供者
     */
    private AiProvider getProvider(String model) {
        String selectedModel = normalizeModel(model);

        if (MODEL_DEEPSEEK.equals(selectedModel)) {
            log.info("使用DeepSeek模型");
            return deepSeekProvider;
        } else if (MODEL_MINIMAX.equals(selectedModel)) {
            log.info("使用MiniMax模型");
            return miniMaxProvider;
        } else if (MODEL_QWEN.equals(selectedModel)) {
            log.info("使用阿里云百炼模型");
            return aliYunProvider;
        } else if (MODEL_OLLAMA_QWEN.equals(selectedModel)) {
            log.info("使用Ollama本地模型");
            return ollamaProvider;
        }

        // 未识别模型时回退到DeepSeek，避免错误配置造成递归或请求中断。
        log.warn("未识别模型: {}，回退到DeepSeek模型", model);
        return deepSeekProvider;
    }

    private String normalizeModel(String model) {
        return model != null && !model.trim().isEmpty()
            ? model.trim()
            : aiConfig.getDefaultModel();
    }

    private String resolveModel(String model) {
        String selectedModel = normalizeModel(model);
        if (MODEL_DEEPSEEK.equals(selectedModel) || MODEL_MINIMAX.equals(selectedModel)
            || MODEL_QWEN.equals(selectedModel) || MODEL_OLLAMA_QWEN.equals(selectedModel)) {
            return selectedModel;
        }
        return MODEL_DEEPSEEK;
    }

    private String getModelDisplayName(String model) {
        if (MODEL_DEEPSEEK.equals(model)) {
            return MODEL_NAME_DEEPSEEK;
        } else if (MODEL_MINIMAX.equals(model)) {
            return MODEL_NAME_MINIMAX;
        } else if (MODEL_QWEN.equals(model)) {
            return MODEL_NAME_QWEN;
        } else if (MODEL_OLLAMA_QWEN.equals(model)) {
            return MODEL_NAME_OLLAMA_QWEN;
        }
        return MODEL_NAME_DEEPSEEK;
    }

    @Override
    public List<Conversation> getConversations() {
        try {
            return conversationMapper.selectAll();
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "获取会话列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Conversation createConversation(String title) {
        try {
            Conversation conversation = new Conversation();
            conversation.setTitle(title != null && !title.isEmpty() ? title : "新对话");
            conversation.setStatus(1);
            conversationMapper.insert(conversation);
            log.info("创建新会话成功 - ID: {}, 标题: {}", conversation.getId(), conversation.getTitle());
            return conversation;
        } catch (Exception e) {
            log.error("创建会话失败", e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "创建会话失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteConversation(Long id) {
        try {
            // 检查会话是否存在
            Conversation conversation = conversationMapper.selectById(id);
            if (conversation == null) {
                throw BusinessException.conversationNotFound(id);
            }

            // 先删除关联的消息
            messageMapper.deleteByConversationId(id);
            conversationSummaryMapper.deleteByConversationId(id);
            // 再删除会话
            conversationMapper.delete(id);
            log.info("删除会话成功 - ID: {}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除会话失败 - ID: {}", id, e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "删除会话失败: " + e.getMessage());
        }
    }

    @Override
    public List<Message> getMessages(Long conversationId) {
        try {
            // 检查会话是否存在
            Conversation conversation = conversationMapper.selectById(conversationId);
            if (conversation == null) {
                throw BusinessException.conversationNotFound(conversationId);
            }

            List<Message> messages = messageMapper.selectByConversationId(conversationId);
            log.debug("获取消息列表成功 - 会话ID: {}, 消息数: {}", conversationId, messages.size());
            return messages;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取消息列表失败 - 会话ID: {}", conversationId, e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "获取消息列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ChatResponse chat(Long conversationId, String message, String model) {
        return chat(conversationId, message, model, SCOPE_INTERNET);
    }

    @Override
    @Transactional
    public ChatResponse chat(Long conversationId, String message, String model, String scope) {
        // 1. 参数校验
        validateMessage(message);

        Conversation conversation;
        String selectedModel = resolveModel(model);
        AiProvider provider = getProvider(selectedModel);
        String safeScope = normalizeScope(scope);

        try {
            // 2. 创建或获取会话
            conversation = getOrCreateConversation(conversationId);

            // 3. 保存用户消息
            Message userMessage = saveUserMessage(conversation.getId(), message);

            // 4. 调用AI服务获取回复
            long startTime = System.currentTimeMillis();
            String assistantReply = generateAIResponse(conversation.getId(), message, provider, safeScope);
            long elapsedMs = System.currentTimeMillis() - startTime;

            // 5. 保存AI回复
            Message assistantMessage = saveAssistantMessage(
                conversation.getId(), assistantReply, selectedModel, getModelDisplayName(selectedModel), elapsedMs);

            // 6. 自动生成对话标题（如果是新对话）
            updateConversationTitleIfNeeded(conversation, message);

            // 7. 构建响应
            ChatResponse response = buildChatResponse(conversation, userMessage, assistantMessage, message, assistantReply);

            log.info("聊天请求处理成功 - 会话ID: {}, 用户消息长度: {}",
                conversation.getId(), message.length());

            return response;

        } catch (BusinessException e) {
            log.warn("业务异常 - {}", e.getMessage());
            throw e;
        } catch (AiServiceException e) {
            log.error("AI服务异常 - 错误码: {}, 可重试: {}", e.getErrorCode(), e.isRetryable());
            // AI服务异常直接抛出，由全局异常处理器处理
            throw e;
        } catch (Exception e) {
            log.error("聊天处理异常 - ", e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                "处理消息失败: " + e.getMessage());
        }
    }

    @Override
    public ChatResponse chatWithDefaultModel(Long conversationId, String message) {
        return chat(conversationId, message, aiConfig.getDefaultModel(), SCOPE_INTERNET);
    }

    @Override
    public String chatStream(Long conversationId, String message, String model) {
        return chatStream(conversationId, message, model, SCOPE_INTERNET);
    }

    @Override
    public String chatStream(Long conversationId, String message, String model, String scope) {
        // 创建临时存储完整回复
        StringBuilder fullResponse = new StringBuilder();

        chatStream(conversationId, message, model, scope, content -> {
            fullResponse.setLength(0);
            fullResponse.append(content);
        });

        return fullResponse.toString();
    }

    @Override
    public String chatStreamWithDefaultModel(Long conversationId, String message) {
        return chatStream(conversationId, message, aiConfig.getDefaultModel(), SCOPE_INTERNET);
    }

    @Override
    public boolean chatStream(Long conversationId, String message, String model, Consumer<String> contentConsumer) {
        return chatStream(conversationId, message, model, SCOPE_INTERNET, contentConsumer);
    }

    @Override
    public boolean chatStream(Long conversationId, String message, String model, String scope, Consumer<String> contentConsumer) {
        // 1. 参数校验
        validateMessage(message);

        Conversation conversation;
        StringBuilder fullResponseBuilder = new StringBuilder();
        String selectedModel = resolveModel(model);
        AiProvider provider = getProvider(selectedModel);
        String safeScope = normalizeScope(scope);

        try {
            // 2. 创建或获取会话
            conversation = getOrCreateConversation(conversationId);

            // 3. 保存用户消息
            Message userMessage = saveUserMessage(conversation.getId(), message);

            // 4. 获取历史消息上下文
            List<Message> historyMessages = messageMapper.selectByConversationId(conversation.getId());
            List<AiProvider.ChatMessage> aiMessages = buildAiMessages(
                conversation.getId(), historyMessages, message, provider, safeScope);

            if (aiMessages.isEmpty()) {
                contentConsumer.accept(LOCAL_KNOWLEDGE_NO_HIT_REPLY);
                saveAssistantMessage(conversation.getId(), LOCAL_KNOWLEDGE_NO_HIT_REPLY,
                    selectedModel, getModelDisplayName(selectedModel), 0L);
                updateConversationTitleIfNeeded(conversation, message);
                return true;
            }

            log.info("流式聊天请求 - 会话ID: {}, 用户消息长度: {}, 模型: {}",
                conversation.getId(), message.length(), model != null ? model : "默认");

            // 5. 调用AI服务流式获取回复
            long startTime = System.currentTimeMillis();
            boolean completed = provider.chatStream(aiMessages, content -> {
                // 收集完整回复
                fullResponseBuilder.setLength(0);
                fullResponseBuilder.append(content);
                // 同时回调给消费者
                contentConsumer.accept(content);
            });

            // 6. 获取完整回复并保存到数据库
            String fullResponse = fullResponseBuilder.toString();
            if (fullResponse == null || fullResponse.trim().isEmpty()) {
                fullResponse = AI_EMPTY_REPLY;
                contentConsumer.accept(fullResponse);
            }
            long elapsedMs = System.currentTimeMillis() - startTime;
            saveAssistantMessage(conversation.getId(), fullResponse,
                selectedModel, getModelDisplayName(selectedModel), elapsedMs);

            // 7. 自动生成对话标题（如果是新对话）
            updateConversationTitleIfNeeded(conversation, message);

            log.info("流式聊天完成 - 会话ID: {}, 回复长度: {}", conversation.getId(), fullResponse.length());

            return completed;

        } catch (BusinessException e) {
            log.warn("业务异常 - {}", e.getMessage());
            throw e;
        } catch (AiServiceException e) {
            log.error("AI服务异常 - 错误码: {}, 可重试: {}", e.getErrorCode(), e.isRetryable());
            throw e;
        } catch (Exception e) {
            log.error("流式聊天处理异常 - ", e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                "处理消息失败: " + e.getMessage());
        }
    }

    @Override
    public boolean chatStreamWithDefaultModel(Long conversationId, String message, Consumer<String> contentConsumer) {
        return chatStream(conversationId, message, aiConfig.getDefaultModel(), SCOPE_INTERNET, contentConsumer);
    }

    /**
     * 校验消息内容
     */
    private void validateMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw BusinessException.messageEmpty();
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw BusinessException.messageTooLong(MAX_MESSAGE_LENGTH);
        }
    }

    /**
     * 获取或创建会话
     */
    private Conversation getOrCreateConversation(Long conversationId) {
        if (conversationId == null) {
            return createConversation(null);
        }

        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            log.info("会话不存在，创建新会话 - 传入ID: {}", conversationId);
            return createConversation(null);
        }
        return conversation;
    }

    /**
     * 保存用户消息
     */
    private Message saveUserMessage(Long conversationId, String content) {
        Message userMessage = new Message();
        userMessage.setConversationId(conversationId);
        userMessage.setRole("user");
        userMessage.setContent(content);
        userMessage.setStatus(1);
        messageMapper.insert(userMessage);
        log.debug("保存用户消息成功 - ID: {}", userMessage.getId());
        return userMessage;
    }

    /**
     * 保存AI回复
     */
    private Message saveAssistantMessage(Long conversationId, String content) {
        return saveAssistantMessage(conversationId, content, null, null, null);
    }

    private Message saveAssistantMessage(Long conversationId, String content, String model, String modelName, Long elapsedMs) {
        Message assistantMessage = new Message();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(content);
        assistantMessage.setModel(model);
        assistantMessage.setModelName(modelName);
        assistantMessage.setElapsedMs(elapsedMs);
        assistantMessage.setStatus(1);
        messageMapper.insert(assistantMessage);
        log.debug("保存AI回复成功 - ID: {}", assistantMessage.getId());
        return assistantMessage;
    }

    /**
     * 如果需要，使用首次问题生成会话标题。
     */
    private void updateConversationTitleIfNeeded(Conversation conversation, String userMessage) {
        if (conversation == null || userMessage == null) {
            return;
        }

        String currentTitle = conversation.getTitle();
        if (currentTitle == null || currentTitle.trim().isEmpty()
            || DEFAULT_CONVERSATION_TITLE.equals(currentTitle)) {
            String title = buildConversationTitle(userMessage);
            conversationMapper.updateTitle(conversation.getId(), title);
            conversation.setTitle(title);
            log.debug("自动更新会话标题 - ID: {}, 标题: {}", conversation.getId(), title);
        }
    }

    /**
     * 从用户首个问题中提炼短标题。
     */
    private String buildConversationTitle(String userMessage) {
        String title = userMessage.trim()
            .replaceAll("\\s+", " ")
            .replaceAll("^[，。！？、,.!?\\s]+", "");

        int endIndex = findTitleEndIndex(title);
        if (endIndex > 0) {
            title = title.substring(0, endIndex);
        }

        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH) + "...";
        }

        return title.isEmpty() ? DEFAULT_CONVERSATION_TITLE : title;
    }

    /**
     * 优先取首句作为标题，避免长问题直接占满左侧列表。
     */
    private int findTitleEndIndex(String title) {
        char[] stopChars = {'。', '！', '？', '.', '!', '?', '\n'};
        int endIndex = -1;
        for (char stopChar : stopChars) {
            int index = title.indexOf(stopChar);
            if (index >= 0 && (endIndex < 0 || index < endIndex)) {
                endIndex = index;
            }
        }
        return endIndex;
    }

    /**
     * 构建聊天响应
     */
    private ChatResponse buildChatResponse(Conversation conversation, Message userMessage,
            Message assistantMessage, String userMsg, String assistantReply) {
        ChatResponse response = new ChatResponse();
        response.setConversationId(conversation.getId());
        response.setUserMessageId(userMessage.getId());
        response.setAssistantMessageId(assistantMessage.getId());
        response.setUserMessage(userMsg);
        response.setAssistantMessage(assistantReply);
        response.setCreateTime(LocalDateTime.now());
        return response;
    }

    /**
     * 调用AI服务生成回复
     * 包含完整的错误处理和日志记录
     */
    private String generateAIResponse(Long conversationId, String userMessage, AiProvider provider, String scope) {
        try {
            // 1. 获取历史消息上下文
            List<Message> historyMessages = messageMapper.selectByConversationId(conversationId);
            log.debug("获取历史消息 - 会话ID: {}, 历史消息数: {}", conversationId, historyMessages.size());

            // 2. 构建AI消息列表
            List<AiProvider.ChatMessage> aiMessages = buildAiMessages(
                conversationId, historyMessages, userMessage, provider, scope);
            if (aiMessages.isEmpty()) {
                return LOCAL_KNOWLEDGE_NO_HIT_REPLY;
            }

            log.info("发送AI请求 - 提供商: {}, 消息总数: {}",
                provider.getProviderName(), aiMessages.size());

            // 3. 调用AI服务
            String assistantReply = provider.chat(aiMessages);

            if (assistantReply == null || assistantReply.trim().isEmpty()) {
                log.warn("AI返回空回复");
                return AI_EMPTY_REPLY;
            }

            return assistantReply;

        } catch (AiServiceException e) {
            // AI服务异常，记录详细日志后重新抛出
            log.error("AI服务调用失败 - 错误码: {}, 错误信息: {}, 可重试: {}",
                e.getErrorCode(), e.getMessage(), e.isRetryable());
            throw e;
        } catch (Exception e) {
            log.error("生成AI回复时发生未知错误", e);
            throw new AiServiceException(ErrorCode.AI_SERVICE_ERROR,
                "AI服务暂时不可用: " + e.getMessage(), e);
        }
    }

    /**
     * 构建AI消息列表
     */
    private List<AiProvider.ChatMessage> buildAiMessages(Long conversationId, List<Message> historyMessages,
            String currentMessage, AiProvider provider, String scope) {
        if (SCOPE_LOCAL_KNOWLEDGE.equals(scope)) {
            return buildLocalKnowledgeAiMessages(conversationId, historyMessages, currentMessage, provider);
        }
        return buildInternetAiMessages(conversationId, historyMessages, provider);
    }

    private List<AiProvider.ChatMessage> buildInternetAiMessages(Long conversationId, List<Message> historyMessages,
            AiProvider provider) {
        List<AiProvider.ChatMessage> aiMessages = new ArrayList<>();
        aiMessages.add(new AiProvider.ChatMessage("system",
            "你是智链中枢的智能助手，名叫小秘。你的职责是：\n" +
            "1. 回答用户关于智链中枢功能的问题\n" +
            "2. 提供菜谱和烹饪建议\n" +
            "3. 帮助用户处理办公事务\n" +
            "4. 回答各类办公相关问题\n" +
            "请用友好、专业的态度回复。如果无法回答，请诚实地说明。"));

        appendManagedHistory(aiMessages, conversationId, historyMessages, provider,
            getRecentRounds(), getHistoryMaxChars());
        return aiMessages;
    }

    private List<AiProvider.ChatMessage> buildLocalKnowledgeAiMessages(Long conversationId, List<Message> historyMessages,
            String currentMessage, AiProvider provider) {
        List<LocalKnowledgeSearchService.SearchResult> results = localKnowledgeSearchService.search(currentMessage);
        if (results.isEmpty()) {
            return List.of();
        }
        List<AiProvider.ChatMessage> aiMessages = new ArrayList<>();
        aiMessages.add(new AiProvider.ChatMessage("system",
            "你是智链中枢的本地知识库问答助手。请只依据下方【本地知识库资料】回答用户问题，" +
            "不要使用互联网常识补充资料中没有的信息。资料不足时请说明本地知识库资料不足。"));
        aiMessages.add(new AiProvider.ChatMessage("system", buildLocalKnowledgeContext(results)));

        appendManagedHistory(aiMessages, conversationId, historyMessages, provider,
            getLocalRecentRounds(), getLocalHistoryMaxChars());
        return aiMessages;
    }

    private void appendManagedHistory(List<AiProvider.ChatMessage> aiMessages, Long conversationId,
            List<Message> historyMessages, AiProvider provider, int recentRounds, int maxChars) {
        if (historyMessages == null || historyMessages.isEmpty()) {
            return;
        }

        HistoryContext context = buildHistoryContext(conversationId, historyMessages, provider, recentRounds);
        int remainingChars = maxChars;
        if (context.summaryContent != null && !context.summaryContent.trim().isEmpty()) {
            String summary = "【对话摘要】\n" + context.summaryContent.trim();
            aiMessages.add(new AiProvider.ChatMessage("system", summary));
            remainingChars -= summary.length();
        }

        for (Message message : selectRecentMessagesWithinBudget(context.recentMessages, remainingChars)) {
            if (message.getRole() != null && message.getContent() != null && !message.getContent().trim().isEmpty()) {
                aiMessages.add(new AiProvider.ChatMessage(message.getRole(), message.getContent()));
            }
        }
    }

    private HistoryContext buildHistoryContext(Long conversationId, List<Message> historyMessages,
            AiProvider provider, int recentRounds) {
        List<Message> safeMessages = filterValidMessages(historyMessages);
        int retainCount = Math.max(1, recentRounds) * 2 + 1;
        int recentStart = Math.max(0, safeMessages.size() - retainCount);
        List<Message> recentMessages = new ArrayList<>(safeMessages.subList(recentStart, safeMessages.size()));

        ConversationSummary summary = conversationSummaryMapper.selectByConversationId(conversationId);
        if (safeMessages.size() <= getSummaryTriggerRounds() * 2 + 1) {
            return new HistoryContext(summary != null ? summary.getSummaryContent() : null, recentMessages);
        }

        List<Message> summaryMessages = selectMessagesToSummarize(safeMessages, recentStart, summary);
        if (summaryMessages.isEmpty()) {
            return new HistoryContext(summary != null ? summary.getSummaryContent() : null, recentMessages);
        }

        ConversationSummary updatedSummary = updateConversationSummary(conversationId, summary, summaryMessages, provider);
        String summaryContent = updatedSummary != null ? updatedSummary.getSummaryContent()
            : summary != null ? summary.getSummaryContent() : null;
        return new HistoryContext(summaryContent, recentMessages);
    }

    private List<Message> filterValidMessages(List<Message> messages) {
        List<Message> validMessages = new ArrayList<>();
        for (Message message : messages) {
            if (message != null && message.getId() != null && message.getRole() != null
                && message.getContent() != null && !message.getContent().trim().isEmpty()) {
                validMessages.add(message);
            }
        }
        return validMessages;
    }

    private List<Message> selectMessagesToSummarize(List<Message> messages, int recentStart, ConversationSummary summary) {
        Long coveredMessageId = summary != null && summary.getCoveredMessageId() != null
            ? summary.getCoveredMessageId()
            : 0L;
        List<Message> summaryMessages = new ArrayList<>();
        for (int i = 0; i < recentStart; i++) {
            Message message = messages.get(i);
            if (message.getId() > coveredMessageId) {
                summaryMessages.add(message);
            }
        }
        return summaryMessages;
    }

    private ConversationSummary updateConversationSummary(Long conversationId, ConversationSummary summary,
            List<Message> summaryMessages, AiProvider provider) {
        try {
            String newSummary = generateConversationSummary(summary, summaryMessages, provider);
            if (newSummary == null || newSummary.trim().isEmpty()) {
                return summary;
            }

            Long coveredMessageId = summaryMessages.get(summaryMessages.size() - 1).getId();
            if (summary == null) {
                ConversationSummary createdSummary = new ConversationSummary();
                createdSummary.setConversationId(conversationId);
                createdSummary.setSummaryContent(newSummary);
                createdSummary.setCoveredMessageId(coveredMessageId);
                createdSummary.setStatus(1);
                conversationSummaryMapper.insert(createdSummary);
                return createdSummary;
            }

            summary.setSummaryContent(newSummary);
            summary.setCoveredMessageId(coveredMessageId);
            summary.setStatus(1);
            conversationSummaryMapper.update(summary);
            return summary;
        } catch (Exception e) {
            log.warn("更新会话摘要失败，降级为最近消息上下文 - 会话ID: {}", conversationId, e);
            return summary;
        }
    }

    private String generateConversationSummary(ConversationSummary summary, List<Message> summaryMessages,
            AiProvider provider) {
        List<AiProvider.ChatMessage> summaryPrompt = new ArrayList<>();
        summaryPrompt.add(new AiProvider.ChatMessage("system",
            "你是对话摘要助手。请把历史对话压缩成简洁摘要，只保留用户目标、关键事实、已确认结论、待办约束和未解决问题。"));
        summaryPrompt.add(new AiProvider.ChatMessage("user", buildSummaryPrompt(summary, summaryMessages)));
        String generatedSummary = provider.chat(summaryPrompt);
        if (generatedSummary == null) {
            return summary != null ? summary.getSummaryContent() : null;
        }
        return limitText(generatedSummary.trim(), getSummaryMaxChars());
    }

    private String buildSummaryPrompt(ConversationSummary summary, List<Message> summaryMessages) {
        StringBuilder builder = new StringBuilder();
        if (summary != null && summary.getSummaryContent() != null && !summary.getSummaryContent().trim().isEmpty()) {
            builder.append("【已有摘要】\n").append(summary.getSummaryContent().trim()).append("\n\n");
        }
        builder.append("【新增历史消息】\n");
        for (Message message : summaryMessages) {
            builder.append(message.getRole()).append(": ").append(message.getContent().trim()).append("\n");
        }
        builder.append("\n请输出更新后的摘要，控制在").append(getSummaryMaxChars()).append("字以内。");
        return builder.toString();
    }

    private List<Message> selectRecentMessagesWithinBudget(List<Message> recentMessages, int maxChars) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return List.of();
        }
        int safeMaxChars = Math.max(500, maxChars);
        int usedChars = 0;
        List<Message> selectedMessages = new ArrayList<>();
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            Message message = recentMessages.get(i);
            int messageLength = message.getContent() != null ? message.getContent().length() : 0;
            if (!selectedMessages.isEmpty() && usedChars + messageLength > safeMaxChars) {
                break;
            }
            selectedMessages.add(message);
            usedChars += messageLength;
        }
        Collections.reverse(selectedMessages);
        return selectedMessages;
    }

    private String limitText(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars);
    }

    private int getRecentRounds() {
        return Math.max(1, aiConfig.getChat().getHistory().getRecentRounds());
    }

    private int getLocalRecentRounds() {
        return Math.max(1, aiConfig.getChat().getHistory().getLocalRecentRounds());
    }

    private int getSummaryTriggerRounds() {
        return Math.max(1, aiConfig.getChat().getHistory().getSummaryTriggerRounds());
    }

    private int getSummaryMaxChars() {
        return Math.max(200, aiConfig.getChat().getHistory().getSummaryMaxChars());
    }

    private int getHistoryMaxChars() {
        return Math.max(1000, aiConfig.getChat().getHistory().getHistoryMaxChars());
    }

    private int getLocalHistoryMaxChars() {
        return Math.max(1000, aiConfig.getChat().getHistory().getLocalHistoryMaxChars());
    }

    private static class HistoryContext {
        private final String summaryContent;
        private final List<Message> recentMessages;

        private HistoryContext(String summaryContent, List<Message> recentMessages) {
            this.summaryContent = summaryContent;
            this.recentMessages = recentMessages;
        }
    }

    private String buildLocalKnowledgeContext(List<LocalKnowledgeSearchService.SearchResult> results) {
        StringBuilder builder = new StringBuilder("【本地知识库资料】\n");
        int index = 1;
        for (LocalKnowledgeSearchService.SearchResult result : results) {
            String content = result.getContent();
            if (content == null || content.trim().isEmpty()) {
                continue;
            }
            String block = "\n[" + index + "] " + result.getTitle() + "\n" + content.trim() + "\n";
            if (builder.length() + block.length() > MAX_LOCAL_CONTEXT_LENGTH) {
                break;
            }
            builder.append(block);
            index++;
        }
        return builder.toString();
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.trim().isEmpty()) {
            return SCOPE_INTERNET;
        }
        String safeScope = scope.trim();
        return SCOPE_LOCAL_KNOWLEDGE.equals(safeScope) ? SCOPE_LOCAL_KNOWLEDGE : SCOPE_INTERNET;
    }
}
