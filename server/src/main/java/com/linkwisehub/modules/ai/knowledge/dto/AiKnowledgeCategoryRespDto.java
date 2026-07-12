package com.linkwisehub.modules.ai.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiKnowledgeCategoryRespDto {
    private Long id;
    private Long parentId;
    private String name;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<AiKnowledgeCategoryRespDto> children = new ArrayList<>();
}
