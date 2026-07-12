package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 导入外呼名单请求对象。
 */
@Data
public class AliOutboundBotAssignJobsReqDto {
    /** 外呼实例 ID，不传时使用配置默认值。 */
    private String instanceId;
    /** 任务组 ID，不传时使用路径参数。 */
    private String jobGroupId;
    /** 外呼名单 JSON 字符串列表。 */
    @NotEmpty(message = "外呼名单不能为空")
    private List<String> jobsJson;
    /** 主叫号码，不传时使用配置默认值。 */
    private List<String> callingNumber;
    /** 是否异步导入名单。 */
    private Boolean isAsynchrony;
    /** 名单解析任务 ID。 */
    private String jobDataParsingTaskId;
    /** 名单类型。 */
    private String rosterType;
    /** 外呼策略 JSON 字符串。 */
    private String strategyJson;
}
