package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 查询外呼任务详情请求对象。
 */
@Data
public class AliOutboundBotJobQueryReqDto {
    /** 外呼实例 ID，不传时使用配置默认值。 */
    private String instanceId;
    /** 阿里任务 ID。 */
    @NotBlank(message = "任务ID不能为空")
    private String jobId;
    /** 是否返回话术信息。 */
    private Boolean withScript;
}
