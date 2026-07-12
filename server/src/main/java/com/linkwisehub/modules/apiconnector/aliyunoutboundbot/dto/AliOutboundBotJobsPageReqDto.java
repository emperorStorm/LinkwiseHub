package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 分页查询外呼任务请求对象。
 */
@Data
public class AliOutboundBotJobsPageReqDto {
    /** 外呼实例 ID，不传时使用配置默认值。 */
    private String instanceId;
    /** 任务组 ID。 */
    @NotBlank(message = "任务组ID不能为空")
    private String jobGroupId;
    /** 任务状态。 */
    private String jobStatus;
    /** 任务失败原因。 */
    private String jobFailureReason;
    /** 页码，默认 1。 */
    private Integer pageNumber;
    /** 每页条数，默认 10。 */
    private Integer pageSize;
}
