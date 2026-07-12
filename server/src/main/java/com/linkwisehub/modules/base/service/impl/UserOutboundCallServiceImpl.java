package com.linkwisehub.modules.base.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.linkwisehub.common.exception.BusinessException;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotRespDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotStartJobReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.service.AliOutboundBotService;
import com.linkwisehub.modules.base.entity.User;
import com.linkwisehub.modules.base.service.UserOutboundCallService;
import com.linkwisehub.modules.base.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 用户外呼编排服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserOutboundCallServiceImpl implements UserOutboundCallService {
    private static final Pattern MOBILE_PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final UserService userService;
    private final ObjectProvider<AliOutboundBotService> aliOutboundBotServiceProvider;

    /**
     * 查询用户、校验手机号，并组装单呼请求调用阿里智能外呼。
     */
    @Override
    public AliOutboundBotRespDto startOutboundCall(Long userId) {
        if (userId == null) {
            throw new BusinessException("OUTBOUND_CALL_PARAM_MISSING", "用户ID不能为空");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("OUTBOUND_CALL_USER_NOT_FOUND", "用户不存在");
        }
        String phoneNumber = normalizePhone(user.getPhone());
        if (phoneNumber == null) {
            throw new BusinessException("OUTBOUND_CALL_PHONE_INVALID", "用户手机号为空或格式不正确");
        }
        AliOutboundBotService aliOutboundBotService = aliOutboundBotServiceProvider.getIfAvailable();
        if (aliOutboundBotService == null) {
            throw new BusinessException("OUTBOUND_CALL_DISABLED", "阿里智能外呼未启用，请先配置 aliyun.outbound-bot.enabled=true");
        }
        return aliOutboundBotService.startJob(buildStartJobReq(user, phoneNumber));
    }

    /**
     * 组装阿里单呼请求，referenceId 固定用用户 ID 保证可追踪。
     */
    private AliOutboundBotStartJobReqDto buildStartJobReq(User user, String phoneNumber) {
        AliOutboundBotStartJobReqDto reqDto = new AliOutboundBotStartJobReqDto();
        reqDto.setPhoneNumber(phoneNumber);
        reqDto.setName(defaultString(user.getRealName(), user.getUsername()));
        reqDto.setReferenceId("USER-" + user.getId());
        reqDto.setExtras(buildUserExtras(user));
        return reqDto;
    }

    /**
     * 生成回调可识别的用户扩展字段。
     */
    private JSONObject buildUserExtras(User user) {
        JSONObject extras = new JSONObject();
        extras.put("bizType", "OA_USER_OUTBOUND_CALL");
        extras.put("userId", user.getId());
        extras.put("username", user.getUsername());
        extras.put("realName", user.getRealName());
        extras.put("organizationId", user.getOrganizationId());
        extras.put("organizationName", user.getOrganizationName());
        return extras;
    }

    /**
     * 清理手机号中的空白字符和连字符后做中国大陆手机号校验。
     */
    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.replaceAll("[\\s-]", "");
        if (!MOBILE_PHONE_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized;
    }

    /**
     * 字符串为空时返回默认值。
     */
    private String defaultString(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }
}
