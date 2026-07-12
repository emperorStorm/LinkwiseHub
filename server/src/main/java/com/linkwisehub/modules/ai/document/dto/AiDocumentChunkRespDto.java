package com.linkwisehub.modules.ai.document.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 文档 Chunk 预览响应对象。
 */
@Data
public class AiDocumentChunkRespDto {
    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String documentName;
    private String content;
    private Integer contentLength;
    private String sourceTitle;
    private Integer sourcePage;
    private Integer sourceParagraph;
    private String metadataJson;
    private String vectorId;
    private String vectorStatus;
    private String vectorErrorMessage;
    private LocalDateTime createTime;
}
