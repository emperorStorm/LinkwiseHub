package com.linkwisehub.modules.ai.document.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 文档分片配置响应对象。
 */
@Data
public class AiDocumentSplitConfigRespDto {
    private Long id;
    private Integer targetChunkLength;
    private Boolean splitByBlankLine;
    private Boolean preserveMarkdownTitle;
    private LocalDateTime updateTime;
}
