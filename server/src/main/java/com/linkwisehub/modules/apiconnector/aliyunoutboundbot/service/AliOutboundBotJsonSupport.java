package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.sdk.gateway.pop.models.Response;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotRespDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.enums.AliOutboundBotActionEnum;
import darabonba.core.TeaModel;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 阿里智能外呼 JSON 转换与响应封装工具。
 */
@Component
public class AliOutboundBotJsonSupport {

    /**
     * 将 SDK 响应转换为项目统一返回对象。
     */
    public AliOutboundBotRespDto toRespDto(AliOutboundBotActionEnum actionEnum, Response response) {
        JSONObject bodyJson = bodyToJson(response);
        AliOutboundBotRespDto respDto = new AliOutboundBotRespDto();
        respDto.setAction(actionEnum.getAction());
        Integer statusCode = responseStatusCode(response);
        respDto.setStatusCode(statusCode);
        respDto.setRequestId(bodyJson.getString("requestId"));
        respDto.setSuccess(resolveSuccess(response, bodyJson));
        respDto.setData(bodyJson);
        return respDto;
    }

    /**
     * 将普通对象或 TeaModel 转成 JSONObject，方便日志记录。
     */
    public JSONObject toJson(Object value) {
        if (value == null) {
            return new JSONObject();
        }
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        }
        if (value instanceof TeaModel) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putAll(((TeaModel) value).toMap());
            return jsonObject;
        }
        return JSON.parseObject(JSON.toJSONString(value));
    }

    /**
     * 解析回调原始 body，非法 JSON 会返回明确业务异常。
     */
    public JSONObject parseCallbackBody(String rawBody) {
        if (rawBody == null || rawBody.trim().isEmpty()) {
            throw new IllegalArgumentException("回调报文不能为空");
        }
        try {
            return JSON.parseObject(rawBody);
        } catch (Exception e) {
            throw new IllegalArgumentException("回调报文不是合法JSON", e);
        }
    }

    /**
     * 优先从 SDK response.body 获取业务响应体。
     */
    private JSONObject bodyToJson(Response response) {
        if (response == null) {
            return new JSONObject();
        }
        JSONObject responseJson = toJson(response);
        Object body = responseJson.get("body");
        if (body == null) {
            return new JSONObject();
        }
        if (body instanceof JSONObject) {
            return (JSONObject) body;
        }
        if (body instanceof Map) {
            JSONObject bodyJson = new JSONObject();
            bodyJson.putAll((Map<? extends String, ?>) body);
            return bodyJson;
        }
        return JSON.parseObject(JSON.toJSONString(body));
    }

    /**
     * 根据 body.success 或 HTTP 状态码推断是否成功。
     */
    private Boolean resolveSuccess(Response response, JSONObject bodyJson) {
        Boolean bodySuccess = bodyJson.getBoolean("success");
        if (bodySuccess != null) {
            return bodySuccess;
        }
        Integer statusCode = responseStatusCode(response);
        return statusCode != null && statusCode >= 200 && statusCode < 300;
    }

    /**
     * 从 SDK 响应对象里读取 HTTP 状态码。
     */
    private Integer responseStatusCode(Response response) {
        JSONObject responseJson = toJson(response);
        return responseJson.getInteger("statusCode");
    }
}
