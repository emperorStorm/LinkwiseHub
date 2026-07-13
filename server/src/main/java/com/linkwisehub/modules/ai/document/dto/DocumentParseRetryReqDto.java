package com.linkwisehub.modules.ai.document.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DocumentParseRetryReqDto {
    @Pattern(regexp = "(?i)LEGACY|MINERU|AUTO", message = "解析策略仅支持 LEGACY、MINERU、AUTO")
    private String strategy;
}
