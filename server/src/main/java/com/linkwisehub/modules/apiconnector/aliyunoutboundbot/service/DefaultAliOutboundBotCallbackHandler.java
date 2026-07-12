package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 默认回调处理器：demo 阶段只记录回调，不强制落业务表。
 */
@Order(Integer.MAX_VALUE)
@Component
public class DefaultAliOutboundBotCallbackHandler implements AliOutboundBotCallbackHandler {
    /** 默认兜底处理所有事件。 */
    @Override
    public boolean support(String eventType, JSONObject body) {
        return true;
    }

    /** 返回未匹配到具体业务处理器的说明。 */
    @Override
    public JSONObject handle(JSONObject body) {
        JSONObject result = new JSONObject();
        result.put("handled", Boolean.FALSE);
        result.put("message", "未匹配到具体业务处理器，已记录回调日志");
        return result;
    }
}
