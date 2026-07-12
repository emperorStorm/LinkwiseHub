package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 创建外呼任务组请求对象。
 */
@Data
public class AliOutboundBotCreateJobGroupReqDto {
    /** 外呼实例 ID，不传时使用配置默认值。 */
    private String instanceId;
    /** 任务组名称。 */
    @NotBlank(message = "任务组名称不能为空")
    private String jobGroupName;
    /** 任务组描述。 */
    private String jobGroupDescription;
    /** 场景 ID，不传时使用配置默认值。 */
    private String scenarioId;
    /** 话术 ID，不传时使用配置默认值。 */
    private String scriptId;
    /** 主叫号码，不传时使用配置默认值。 */
    private List<String> callingNumber;
    /** 重呼主叫号码，不传时使用配置默认值。 */
    private List<String> recallCallingNumber;
    /** 外呼策略 JSON 字符串。 */
    private String strategyJson;
    /** 重呼策略 JSON 字符串。 */
    private String recallStrategyJson;
    /** 任务优先级。 */
    private String priority;
    /** 最小并发数。 */
    private Long minConcurrency;
    /** 振铃时长，单位以阿里接口定义为准。 */
    private Long ringingDuration;
    /** 闪信扩展参数。 */
    private String flashSmsExtras;
}
