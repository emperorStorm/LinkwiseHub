package com.linkwisehub.modules.ai.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiKnowledgeDocumentRespDto {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String parseStatus;
    private Integer chunkCount;
    private String errorMessage;
    private String contentHtml;
    private String publishStatus;
    private String sourceType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
