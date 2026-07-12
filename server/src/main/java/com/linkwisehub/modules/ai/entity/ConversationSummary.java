package com.linkwisehub.modules.ai.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversationSummary {
    private Long id;
    private Long conversationId;
    private String summaryContent;
    private Long coveredMessageId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
