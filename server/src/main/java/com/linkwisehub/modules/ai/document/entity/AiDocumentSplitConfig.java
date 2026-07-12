package com.linkwisehub.modules.ai.document.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 文档全局分片配置实体，控制后续上传文档的 Chunk 生成规则。
 */
@Data
public class AiDocumentSplitConfig {
    private Long id;
    private Integer targetChunkLength;
    private Boolean splitByBlankLine;
    private Boolean preserveMarkdownTitle;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
