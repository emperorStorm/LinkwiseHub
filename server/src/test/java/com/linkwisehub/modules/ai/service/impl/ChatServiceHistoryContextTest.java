package com.linkwisehub.modules.ai.service.impl;

import com.linkwisehub.config.AiConfig;
import com.linkwisehub.modules.ai.entity.ConversationSummary;
import com.linkwisehub.modules.ai.entity.Message;
import com.linkwisehub.modules.ai.mapper.ConversationSummaryMapper;
import com.linkwisehub.modules.ai.service.AiProvider;
import com.linkwisehub.modules.ai.service.LocalKnowledgeSearchService;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceHistoryContextTest {

    private ChatServiceImpl service;
    private ConversationSummaryMapper summaryMapper;
    private LocalKnowledgeSearchService localKnowledgeSearchService;
    private AiProvider provider;

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl();
        summaryMapper = mock(ConversationSummaryMapper.class);
        localKnowledgeSearchService = mock(LocalKnowledgeSearchService.class);
        provider = mock(AiProvider.class);

        ReflectionTestUtils.setField(service, "aiConfig", buildAiConfig());
        ReflectionTestUtils.setField(service, "conversationSummaryMapper", summaryMapper);
        ReflectionTestUtils.setField(service, "localKnowledgeSearchService", localKnowledgeSearchService);
    }

    @Test
    void shortConversationKeepsOriginalMessagesAndDoesNotSummarize() throws Exception {
        List<Message> messages = List.of(
            message(1L, "user", "第一问"),
            message(2L, "assistant", "第一答"),
            message(3L, "user", "当前问题")
        );

        List<AiProvider.ChatMessage> aiMessages = buildAiMessages(messages, "当前问题", "internet");

        assertEquals(4, aiMessages.size());
        assertEquals(1, countContent(aiMessages, "当前问题"));
        verify(provider, never()).chat(any());
        verify(summaryMapper, never()).insert(any());
    }

    @Test
    void longConversationUsesSummaryAndRecentMessages() throws Exception {
        when(provider.chat(any())).thenReturn("旧对话摘要");
        List<Message> messages = buildMessages(15);

        List<AiProvider.ChatMessage> aiMessages = buildAiMessages(messages, "user-15", "internet");

        assertEquals(15, aiMessages.size());
        assertTrue(aiMessages.get(1).getContent().contains("旧对话摘要"));
        assertEquals("user-3", aiMessages.get(2).getContent());
        assertEquals("user-15", aiMessages.get(aiMessages.size() - 1).getContent());

        ArgumentCaptor<ConversationSummary> captor = ArgumentCaptor.forClass(ConversationSummary.class);
        verify(summaryMapper).insert(captor.capture());
        assertEquals(2L, captor.getValue().getCoveredMessageId());
    }

    @Test
    void localKnowledgeConversationKeepsOnlyLocalRecentRoundsAfterKnowledgeContext() throws Exception {
        when(provider.chat(any())).thenReturn("本地摘要");
        when(localKnowledgeSearchService.search("user-15")).thenReturn(List.of(
            new LocalKnowledgeSearchService.SearchResult("制度文档", "知识库内容")
        ));

        List<AiProvider.ChatMessage> aiMessages = buildAiMessages(buildMessages(15), "user-15", "local_knowledge");

        assertEquals(10, aiMessages.size());
        assertTrue(aiMessages.get(1).getContent().contains("知识库内容"));
        assertTrue(aiMessages.get(2).getContent().contains("本地摘要"));
        assertEquals("user-9", aiMessages.get(3).getContent());
        assertEquals("user-15", aiMessages.get(aiMessages.size() - 1).getContent());
    }

    @Test
    void summaryFailureFallsBackToRecentMessages() throws Exception {
        when(provider.chat(any())).thenThrow(new RuntimeException("summary failed"));

        List<AiProvider.ChatMessage> aiMessages = buildAiMessages(buildMessages(15), "user-15", "internet");

        assertEquals(14, aiMessages.size());
        assertEquals("user-3", aiMessages.get(1).getContent());
        verify(summaryMapper, never()).insert(any());
    }

    @SuppressWarnings("unchecked")
    private List<AiProvider.ChatMessage> buildAiMessages(List<Message> messages, String currentMessage, String scope)
            throws Exception {
        Method method = ChatServiceImpl.class.getDeclaredMethod(
            "buildAiMessages",
            Long.class,
            List.class,
            String.class,
            AiProvider.class,
            String.class
        );
        method.setAccessible(true);
        return (List<AiProvider.ChatMessage>) method.invoke(service, 1L, messages, currentMessage, provider, scope);
    }

    private AiConfig buildAiConfig() {
        AiConfig config = new AiConfig();
        config.getChat().getHistory().setRecentRounds(6);
        config.getChat().getHistory().setLocalRecentRounds(3);
        config.getChat().getHistory().setSummaryTriggerRounds(6);
        config.getChat().getHistory().setSummaryMaxChars(1200);
        config.getChat().getHistory().setHistoryMaxChars(8000);
        config.getChat().getHistory().setLocalHistoryMaxChars(3000);
        return config;
    }

    private List<Message> buildMessages(int count) {
        List<Message> messages = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            messages.add(message(i, i % 2 == 0 ? "assistant" : "user", "user-" + i));
        }
        return messages;
    }

    private Message message(Long id, String role, String content) {
        Message message = new Message();
        message.setId(id);
        message.setRole(role);
        message.setContent(content);
        message.setStatus(1);
        return message;
    }

    private long countContent(List<AiProvider.ChatMessage> aiMessages, String content) {
        return aiMessages.stream().filter(message -> content.equals(message.getContent())).count();
    }
}
