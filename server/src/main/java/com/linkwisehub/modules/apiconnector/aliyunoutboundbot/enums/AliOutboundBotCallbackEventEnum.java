package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 阿里智能外呼回调事件枚举。
 */
@Getter
@AllArgsConstructor
public enum AliOutboundBotCallbackEventEnum {
    JOB_STARTED("JOB_STARTED", "任务开始"),
    CALL_RINGING("CALL_RINGING", "振铃"),
    CALL_ANSWERED("CALL_ANSWERED", "接通"),
    CALL_HANGUP("CALL_HANGUP", "挂断"),
    CALL_FAILED("CALL_FAILED", "呼叫失败"),
    INTENT_HIT("INTENT_HIT", "命中意图"),
    ASR_RESULT("ASR_RESULT", "识别文本"),
    RECORDING_READY("RECORDING_READY", "录音生成"),
    JOB_FINISHED("JOB_FINISHED", "任务完成"),
    UNKNOWN("UNKNOWN", "未知事件");

    private final String code;
    private final String desc;

    /**
     * 根据阿里回调里的事件编码解析枚举，无法识别时归类为 UNKNOWN。
     */
    public static AliOutboundBotCallbackEventEnum of(String code) {
        if (code == null || code.trim().isEmpty()) {
            return UNKNOWN;
        }
        for (AliOutboundBotCallbackEventEnum item : values()) {
            if (item.code.equalsIgnoreCase(code.trim())) {
                return item;
            }
        }
        return UNKNOWN;
    }
}
