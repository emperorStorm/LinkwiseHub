package com.linkwisehub.modules.ai.knowledge.dto;

import lombok.Data;

import java.util.Map;

@Data
public class OnlyOfficePreviewConfigRespDto {
    private String documentServerApiUrl;
    private Map<String, Object> config;
}
