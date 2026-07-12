package com.linkwisehub.modules.ai.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Conversation {
    private Long id;
    private String title;
    private Long userId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
