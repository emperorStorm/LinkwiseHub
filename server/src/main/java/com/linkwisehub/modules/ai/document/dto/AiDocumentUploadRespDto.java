package com.linkwisehub.modules.ai.document.dto;

import lombok.Data;

/**
 * AI 文档上传解析结果响应对象。
 */
@Data
public class AiDocumentUploadRespDto {
    private Long documentId;
    private String fileName;
    private String parseStatus;
    private Integer chunkCount;

    public AiDocumentUploadRespDto(Long documentId, String fileName, String parseStatus, Integer chunkCount) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.parseStatus = parseStatus;
        this.chunkCount = chunkCount;
    }
}
