package com.linkwisehub.modules.base.service;

import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotRespDto;

/**
 * 用户外呼编排服务。
 */
public interface UserOutboundCallService {
    /** 根据用户 ID 查询手机号并发起阿里智能外呼。 */
    AliOutboundBotRespDto startOutboundCall(Long userId);
}
