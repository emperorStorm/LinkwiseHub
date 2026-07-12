package com.linkwisehub.modules.ai.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private Long conversationId;
    private String message;
    /**
     * 选择的模型
     * 可选值: deepseek-v4-flash, qwen3.5-plus, MiniMax-M2.7-highspeed, qwen3.6:latest
     * 为空时使用默认模型
     */
    private String model;
    /**
     * 问答范围：internet / local_knowledge
     * 为空时默认使用互联网逻辑
     */
    private String scope;
}
