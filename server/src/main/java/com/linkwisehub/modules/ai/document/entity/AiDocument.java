package com.linkwisehub.modules.ai.document.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 文档主表实体，记录上传文件和解析结果状态。
 */
@Data
public class AiDocument {
    private Long id;
    private Long categoryId;
    private String title;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String storageBucket;
    private String storagePath;
    private String parseStatus;
    private Integer chunkCount;
    private String errorMessage;
    private String contentHtml;
    private String publishStatus;
    private String sourceType;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
