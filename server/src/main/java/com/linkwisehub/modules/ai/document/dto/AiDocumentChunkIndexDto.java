package com.linkwisehub.modules.ai.document.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Chunk 检索索引数据，包含 ES 写入和混合检索过滤所需的文档状态。
 */
@Data
public class AiDocumentChunkIndexDto {
    private Long chunkId;
    private Long documentId;
    private Long categoryId;
    private Integer chunkIndex;
    private String title;
    private String fileName;
    private String content;
    private String sourceTitle;
    private String sourceType;
    private String vectorId;
    private String publishStatus;
    private Integer documentStatus;
    private Integer chunkStatus;
    private LocalDateTime updateTime;
}
