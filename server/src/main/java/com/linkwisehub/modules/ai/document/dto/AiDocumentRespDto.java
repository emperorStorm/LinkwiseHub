package com.linkwisehub.modules.ai.document.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 文档列表和详情响应对象。
 */
@Data
public class AiDocumentRespDto {
    private Long id;
    private String title;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String parseStatus;
    private Integer chunkCount;
    private String errorMessage;
    private String sourceType;
    private String publishStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
