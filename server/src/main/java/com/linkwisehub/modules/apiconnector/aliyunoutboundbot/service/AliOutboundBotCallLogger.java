package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.service;

import com.alibaba.fastjson.JSONObject;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.enums.AliOutboundBotActionEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 阿里智能外呼应用日志记录器。
 */
@Slf4j
@Component
public class AliOutboundBotCallLogger {

    /**
     * 记录主动调用日志，使用 JSON 字符串便于后续检索和复制复现。
     */
    public void logApiCall(AliOutboundBotActionEnum actionEnum, JSONObject impData,
                           JSONObject resultData, boolean success) {
        if (success) {
            log.info("阿里智能外呼主动调用成功 action={}, impData={}, resultData={}",
                    actionEnum.getAction(), toSafeJson(impData), toSafeJson(resultData));
        } else {
            log.error("阿里智能外呼主动调用失败 action={}, impData={}, resultData={}",
                    actionEnum.getAction(), toSafeJson(impData), toSafeJson(resultData));
        }
    }

    /**
     * 记录回调处理日志，demo 阶段不落表但保留排障信息。
     */
    public void logCallback(JSONObject impData, JSONObject resultData, boolean success) {
        if (success) {
            log.info("阿里智能外呼回调处理成功 impData={}, resultData={}", toSafeJson(impData), toSafeJson(resultData));
        } else {
            log.error("阿里智能外呼回调处理失败 impData={}, resultData={}", toSafeJson(impData), toSafeJson(resultData));
        }
    }

    /**
     * 统一处理空 JSON，避免日志里出现 null 难以检索。
     */
    private String toSafeJson(JSONObject value) {
        return value == null ? "{}" : value.toJSONString();
    }
}
