package com.linkwisehub.modules.ai.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiKnowledgePublishStatusReqDto {
    @NotBlank(message = "发布状态不能为空")
    private String publishStatus;
}
