package com.linkwisehub.modules.ai.document.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentParseJobRespDto {
    private Long jobId;
    private Long documentId;
    private String parseEngine;
    private String status;
    private Integer progress;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createTime;
}
