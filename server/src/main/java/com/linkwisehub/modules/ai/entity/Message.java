package com.linkwisehub.modules.ai.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Message {
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private String model;
    private String modelName;
    private Long elapsedMs;
    private Integer status;
    private LocalDateTime createTime;
}
