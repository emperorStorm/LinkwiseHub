package com.linkwisehub.modules.ai.document.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * AI 文档分片配置保存请求对象。
 */
@Data
public class AiDocumentSplitConfigSaveReqDto {
    @Min(value = 200, message = "目标 Chunk 长度不能小于 200")
    @Max(value = 2000, message = "目标 Chunk 长度不能大于 2000")
    private Integer targetChunkLength;
    private Boolean splitByBlankLine;
    private Boolean preserveMarkdownTitle;
}
