package com.linkwisehub.modules.ai.knowledge.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库分类实体，使用 parentId 维护文件夹树。
 */
@Data
public class AiKnowledgeCategory {
    private Long id;
    private Long parentId;
    private String name;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
