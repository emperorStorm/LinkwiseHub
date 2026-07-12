package com.linkwisehub.modules.ai.controller;

import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.modules.ai.service.ChatService;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerStreamTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void streamChatUsesChatStreamAndSendsContentEvents() throws Exception {
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(4);
            consumer.accept("chunk-one");
            consumer.accept("chunk-one chunk-two");
            return true;
        }).when(chatService).chatStream(
            eq(1L),
            eq("hello"),
            eq("model-a"),
            eq("local_knowledge"),
            ArgumentMatchers.<Consumer<String>>any()
        );

        MvcResult mvcResult = mockMvc.perform(post("/api/ai/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"conversationId":1,"message":"hello","model":"model-a","scope":"local_knowledge"}
                    """))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("event:start")))
            .andExpect(content().string(containsString("{\"status\":\"started\"}")))
            .andExpect(content().string(containsString("event:content")))
            .andExpect(content().string(containsString("{\"content\":\"chunk-one\"}")))
            .andExpect(content().string(containsString("{\"content\":\"chunk-one chunk-two\"}")))
            .andExpect(content().string(containsString("event:done")))
            .andExpect(content().string(containsString("{\"status\":\"done\"}")));

        verify(chatService, never()).chat(any(), any(), any(), any());
    }

    @Test
    void streamChatReturnsErrorEventWhenMessageEmpty() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/ai/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"message":"   "}
                    """))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("event:error")))
            .andExpect(content().string(containsString("\"errorCode\":\"" + ErrorCode.MESSAGE_EMPTY.getCode() + "\"")))
            .andExpect(content().string(containsString("event:done")));
    }

    @Test
    void streamChatReturnsErrorEventWhenServiceThrowsBusinessException() throws Exception {
        doThrow(new BusinessException(ErrorCode.BUSINESS_ERROR, "stream failed"))
            .when(chatService).chatStream(
                eq(2L),
                eq("trigger error"),
                eq("model-a"),
                eq("local_knowledge"),
                ArgumentMatchers.<Consumer<String>>any()
            );

        MvcResult mvcResult = mockMvc.perform(post("/api/ai/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"conversationId":2,"message":"trigger error","model":"model-a","scope":"local_knowledge"}
                    """))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("event:start")))
            .andExpect(content().string(containsString("event:error")))
            .andExpect(content().string(containsString("\"errorCode\":\"" + ErrorCode.BUSINESS_ERROR.getCode() + "\"")))
            .andExpect(content().string(containsString("stream failed")));
    }
}
