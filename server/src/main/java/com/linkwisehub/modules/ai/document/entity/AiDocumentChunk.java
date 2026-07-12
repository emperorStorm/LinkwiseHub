package com.linkwisehub.modules.ai.document.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 文档分片实体，保存可预览和后续可检索的文本块。
 */
@Data
public class AiDocumentChunk {
    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String content;
    private Integer contentLength;
    private String sourceTitle;
    private Integer sourcePage;
    private Integer sourceParagraph;
    private String metadataJson;
    private String vectorId;
    private String vectorStatus;
    private String vectorErrorMessage;
    private Integer status;
    private LocalDateTime createTime;
}
