package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * 阿里智能外呼统一返回对象。
 */
@Data
public class AliOutboundBotRespDto {
    /** 阿里 OpenAPI Action 名称。 */
    private String action;
    /** 本次 OpenAPI 调用是否成功。 */
    private Boolean success;
    /** HTTP 状态码。 */
    private Integer statusCode;
    /** 阿里云请求 ID，便于控制台或工单排查。 */
    private String requestId;
    /** 阿里云原始响应 body。 */
    private JSONObject data;
}
