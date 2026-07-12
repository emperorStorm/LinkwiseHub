package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.controller;

import com.alibaba.fastjson.JSONObject;
import com.linkwisehub.common.ApiResponse;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotAssignJobsReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotCreateJobGroupReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotJobControlReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotJobGroupQueryReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotJobQueryReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotJobsPageReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotRespDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotStartJobReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.service.AliOutboundBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * 阿里智能外呼机器人控制器。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/aliyun-outbound-bot/mob")
@ConditionalOnProperty(prefix = "aliyun.outbound-bot", name = "enabled", havingValue = "true")
public class AliOutboundBotController {
    private final AliOutboundBotService service;

    /** 创建外呼任务组。 */
    @PostMapping("/v1/job-groups")
    public ApiResponse<AliOutboundBotRespDto> createJobGroup(
            @Valid @RequestBody AliOutboundBotCreateJobGroupReqDto reqDto) {
        return ApiResponse.success(service.createJobGroup(reqDto));
    }

    /** 导入外呼名单。 */
    @PostMapping("/v1/job-groups/{jobGroupId}/jobs")
    public ApiResponse<AliOutboundBotRespDto> assignJobs(@PathVariable("jobGroupId") String jobGroupId,
                                                          @Valid @RequestBody AliOutboundBotAssignJobsReqDto reqDto) {
        return ApiResponse.success(service.assignJobs(jobGroupId, reqDto));
    }

    /** 单个号码外呼。 */
    @PostMapping("/v1/jobs/start")
    public ApiResponse<AliOutboundBotRespDto> startJob(@RequestBody AliOutboundBotStartJobReqDto reqDto) {
        return ApiResponse.success(service.startJob(reqDto));
    }

    /** 启动或恢复外呼任务。 */
    @PostMapping("/v1/jobs/resume")
    public ApiResponse<AliOutboundBotRespDto> resumeJobs(@Valid @RequestBody AliOutboundBotJobControlReqDto reqDto) {
        return ApiResponse.success(service.resumeJobs(reqDto));
    }

    /** 暂停外呼任务。 */
    @PostMapping("/v1/jobs/suspend")
    public ApiResponse<AliOutboundBotRespDto> suspendJobs(@Valid @RequestBody AliOutboundBotJobControlReqDto reqDto) {
        return ApiResponse.success(service.suspendJobs(reqDto));
    }

    /** 取消外呼任务。 */
    @PostMapping("/v1/jobs/cancel")
    public ApiResponse<AliOutboundBotRespDto> cancelJobs(@Valid @RequestBody AliOutboundBotJobControlReqDto reqDto) {
        return ApiResponse.success(service.cancelJobs(reqDto));
    }

    /** 查询外呼任务组。 */
    @PostMapping("/v1/job-groups/query")
    public ApiResponse<AliOutboundBotRespDto> queryJobGroup(
            @Valid @RequestBody AliOutboundBotJobGroupQueryReqDto reqDto) {
        return ApiResponse.success(service.queryJobGroup(reqDto));
    }

    /** 分页查询外呼任务。 */
    @PostMapping("/v1/jobs/query")
    public ApiResponse<AliOutboundBotRespDto> queryJobs(@Valid @RequestBody AliOutboundBotJobsPageReqDto reqDto) {
        return ApiResponse.success(service.queryJobs(reqDto));
    }

    /** 查询外呼任务详情。 */
    @PostMapping("/v1/jobs/detail")
    public ApiResponse<AliOutboundBotRespDto> queryJobDetail(@Valid @RequestBody AliOutboundBotJobQueryReqDto reqDto) {
        return ApiResponse.success(service.queryJobDetail(reqDto));
    }

    /** 接收阿里外呼结果回调，保留 raw body 供签名校验。 */
    @PostMapping("/v1/callback/result")
    public ApiResponse<JSONObject> callbackResult(@RequestBody(required = false) String rawBody,
                                                  @RequestHeader Map<String, String> headers) {
        return ApiResponse.success(service.callbackResult(rawBody, headers, resolveSourceIp(headers)));
    }

    /** 从网关转发头中解析真实来源 IP。 */
    private String resolveSourceIp(Map<String, String> headers) {
        String forwardedFor = getIgnoreCase(headers, "X-Forwarded-For");
        if (forwardedFor != null && forwardedFor.trim().length() > 0) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = getIgnoreCase(headers, "X-Real-IP");
        if (realIp != null && realIp.trim().length() > 0) {
            return realIp.trim();
        }
        return "";
    }

    /** 忽略大小写读取请求头。 */
    private String getIgnoreCase(Map<String, String> headers, String key) {
        if (headers == null || key == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
