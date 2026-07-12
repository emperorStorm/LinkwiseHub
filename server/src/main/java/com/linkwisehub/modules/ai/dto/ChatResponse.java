package com.linkwisehub.modules.ai.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatResponse {
    private Long conversationId;
    private Long userMessageId;
    private Long assistantMessageId;
    private String userMessage;
    private String assistantMessage;
    private LocalDateTime createTime;

    public ChatResponse() {
        this.createTime = LocalDateTime.now();
    }
}
