package com.linkwisehub.modules.ai.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiKnowledgeDocumentReqDto {
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 120, message = "标题不能超过120个字符")
    private String title;

    private String contentHtml;
    private String publishStatus;
}
