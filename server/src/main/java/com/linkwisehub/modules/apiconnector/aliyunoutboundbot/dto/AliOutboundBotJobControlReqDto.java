package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 启动、暂停、取消外呼任务请求对象。
 */
@Data
public class AliOutboundBotJobControlReqDto {
    /** 外呼实例 ID，不传时使用配置默认值。 */
    private String instanceId;
    /** 任务组 ID。 */
    @NotBlank(message = "任务组ID不能为空")
    private String jobGroupId;
    /** 场景 ID，不传时使用配置默认值。 */
    private String scenarioId;
    /** 是否控制任务组下全部任务。 */
    private Boolean all;
    /** 阿里任务 ID 列表。 */
    private List<String> jobId;
    /** 业务任务引用 ID 列表。 */
    private List<String> jobReferenceId;
}
