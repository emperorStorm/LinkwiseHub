package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.service;

import com.alibaba.fastjson.JSONObject;

/**
 * 阿里智能外呼回调业务处理器。
 */
public interface AliOutboundBotCallbackHandler {
    /** 判断当前处理器是否支持该回调事件。 */
    boolean support(String eventType, JSONObject body);

    /** 处理回调业务并返回摘要结果。 */
    JSONObject handle(JSONObject body);
}
