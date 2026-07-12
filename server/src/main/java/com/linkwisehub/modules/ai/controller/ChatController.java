package com.linkwisehub.modules.ai.controller;

import com.linkwisehub.modules.ai.dto.ChatRequest;
import com.linkwisehub.modules.ai.dto.ChatResponse;
import com.linkwisehub.modules.ai.entity.Conversation;
import com.linkwisehub.modules.ai.entity.Message;
import com.linkwisehub.modules.ai.service.ChatService;
import com.linkwisehub.common.ApiResponse;
import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI聊天控制器
 * 提供对话管理的RESTful API接口
 * 所有异常由全局异常处理器统一处理
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * SSE超时时间（毫秒）
     * AI回复可能较长，设置较长的超时时间
     */
    private static final long SSE_TIMEOUT = 600000L; // 10分钟

    /**
     * SSE线程池，用于异步发送SSE事件
     */
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<Conversation>>> getConversations() {
        log.info("获取会话列表请求");
        List<Conversation> conversations = chatService.getConversations();
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    /**
     * 创建新会话
     */
    @PostMapping("/conversations")
    public ResponseEntity<ApiResponse<Conversation>> createConversation(
            @RequestBody(required = false) Map<String, String> body) {
        String title = body != null ? body.get("title") : null;
        log.info("创建新会话请求 - 标题: {}", title);
        Conversation conversation = chatService.createConversation(title);
        return ResponseEntity.ok(ApiResponse.success("会话创建成功", conversation));
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(@PathVariable Long id) {
        log.info("删除会话请求 - ID: {}", id);
        chatService.deleteConversation(id);
        return ResponseEntity.ok(ApiResponse.success("会话删除成功"));
    }

    /**
     * 获取会话消息列表
     */
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<List<Message>>> getMessages(@PathVariable Long id) {
        log.info("获取消息列表请求 - 会话ID: {}", id);
        List<Message> messages = chatService.getMessages(id);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * 发送聊天消息
     * 核心接口，统一错误处理由全局异常处理器处理
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@RequestBody ChatRequest request) {
        log.info("发送聊天消息请求 - 会话ID: {}, 消息长度: {}, 模型: {}, 范围: {}",
            request.getConversationId(),
            request.getMessage() != null ? request.getMessage().length() : 0,
            request.getModel(),
            request.getScope());

        // 参数校验
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw BusinessException.messageEmpty();
        }

        ChatResponse response = chatService.chat(request.getConversationId(), request.getMessage(), request.getModel(), request.getScope());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * SSE流式发送聊天消息
     * 直接复用服务层流式能力，实时推送AI返回的累计文本
     *
     * @param request 聊天请求
     * @return SSEEmitter用于流式响应
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        log.info("SSE流式聊天请求 - 会话ID: {}, 消息长度: {}, 模型: {}, 范围: {}",
            request.getConversationId(),
            request.getMessage() != null ? request.getMessage().length() : 0,
            request.getModel(),
            request.getScope());

        // 参数校验
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            // 返回错误事件
            SseEmitter errorEmitter = new SseEmitter(SSE_TIMEOUT);
            try {
                errorEmitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"errorCode\":\"" + ErrorCode.MESSAGE_EMPTY.getCode() + "\",\"message\":\"消息不能为空\"}"));
                errorEmitter.send(SseEmitter.event()
                    .name("done")
                    .data("{}"));
                errorEmitter.complete();
            } catch (IOException e) {
                log.error("SSE发送错误失败", e);
            }
            return errorEmitter;
        }

        // 创建SSEEmitter
        final SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 保存参数用于异步处理
        final Long conversationId = request.getConversationId();
        final String message = request.getMessage();
        final String model = request.getModel();
        final String scope = request.getScope();

        // 异步处理AI回复
        sseExecutor.execute(() -> {
            try {
                // 发送开始事件
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data("{\"status\":\"started\"}"));

                boolean completed = chatService.chatStream(conversationId, message, model, scope, content -> {
                    if (content == null) {
                        return;
                    }
                    try {
                        emitter.send(SseEmitter.event()
                            .name("content")
                            .data("{\"content\":\"" + escapeJson(content) + "\"}"));
                    } catch (IOException e) {
                        throw new RuntimeException("SSE发送内容失败", e);
                    }
                });

                if (!completed) {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"errorCode\":\"500\",\"message\":\"AI流式回复未完整结束\"}"));
                    emitter.complete();
                    return;
                }

                emitter.send(SseEmitter.event()
                    .name("done")
                    .data("{\"status\":\"done\"}"));

                emitter.complete();
                log.info("SSE流式聊天完成 - 会话ID: {}", conversationId);

            } catch (Exception e) {
                log.error("SSE流式聊天异常 - 会话ID: {}", conversationId, e);
                try {
                    // 发送错误事件
                    String errorMessage = e.getMessage();
                    String errorCode = "500";
                    if (e instanceof BusinessException) {
                        errorCode = ((BusinessException) e).getErrorCode();
                    }
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"errorCode\":\"" + errorCode + "\",\"message\":\"" + escapeJson(errorMessage) + "\"}"));
                } catch (IOException ioException) {
                    log.error("SSE发送错误事件失败", ioException);
                }
                emitter.complete();
            }
        });

        // 配置超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时 - 会话ID: {}", conversationId);
        });

        emitter.onCompletion(() -> {
            log.debug("SSE连接完成 - 会话ID: {}", conversationId);
        });

        emitter.onError(e -> {
            log.error("SSE连接错误 - 会话ID: {}", conversationId, e);
        });

        return emitter;
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJson(String content) {
        if (content == null) {
            return "";
        }
        return content
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * 获取AI服务状态
     * 用于前端检测AI服务是否可用
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "AI Chat");
        status.put("status", "running");
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
