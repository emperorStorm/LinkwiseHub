package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 阿里智能外呼回调处理状态枚举。
 */
@Getter
@AllArgsConstructor
public enum AliOutboundBotCallbackProcessStatusEnum {
    SUCCESS("SUCCESS", "处理成功"),
    FAILED("FAILED", "处理失败"),
    IGNORED("IGNORED", "已忽略");

    private final String code;
    private final String desc;
}
