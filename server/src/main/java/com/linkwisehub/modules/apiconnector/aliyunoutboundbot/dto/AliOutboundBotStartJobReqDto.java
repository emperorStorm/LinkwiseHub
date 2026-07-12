package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

/**
 * 单个号码外呼请求对象。
 */
@Data
public class AliOutboundBotStartJobReqDto {
    /** 外呼实例 ID，不传时使用配置默认值。 */
    private String instanceId;
    /** 任务组 ID，可选；传入时单呼任务归属到该任务组。 */
    private String jobGroupId;
    /** 场景 ID，不传时使用配置默认值。 */
    private String scenarioId;
    /** 话术 ID，不传时使用配置默认值。 */
    private String scriptId;
    /** 主叫号码，不传时使用配置默认值。 */
    private List<String> callingNumber;
    /** 单个外呼任务 JSON 字符串；为空时根据 phoneNumber/name/referenceId/extras 自动组装。 */
    private String jobJson;
    /** 被叫号码。 */
    private String phoneNumber;
    /** 联系人姓名。 */
    private String name;
    /** 业务引用 ID。 */
    private String referenceId;
    /** 业务扩展字段。 */
    private JSONObject extras;
}
