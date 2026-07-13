package com.linkwisehub.modules.ai.document.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ParsedDocumentBlock {
    private Integer index;
    private String blockType;
    private String content;
    private Integer page;
    private String title;
    private Map<String, Object> metadata = new HashMap<>();
}
