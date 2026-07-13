package com.linkwisehub.modules.ai.document.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiDocumentProcessingJob {
    private Long id;
    private Long documentId;
    private String taskType;
    private String parseEngine;
    private String providerTaskId;
    private String status;
    private Integer progress;
    private Integer retryCount;
    private String resultBucket;
    private String manifestPath;
    private String markdownPath;
    private String blocksPath;
    private String errorMessage;
    private LocalDateTime nextPollTime;
    private LocalDateTime lockedUntil;
    private String workerId;
    private Integer version;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
