package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 查询外呼任务组请求对象。
 */
@Data
public class AliOutboundBotJobGroupQueryReqDto {
    /** 外呼实例 ID，不传时使用配置默认值。 */
    private String instanceId;
    /** 任务组 ID。 */
    @NotBlank(message = "任务组ID不能为空")
    private String jobGroupId;
    /** 阿里任务组摘要类型。 */
    private List<String> briefTypes;
}
