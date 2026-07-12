package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.service;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.sdk.gateway.pop.models.Response;
import com.aliyun.sdk.service.outboundbot20191226.models.AssignJobsRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.CancelJobsRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.CreateJobGroupRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.DescribeJobGroupRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.DescribeJobRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.ListJobsByGroupRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.ResumeJobsRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.StartJobRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.SuspendJobsRequest;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.config.AliOutboundBotProperties;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotAssignJobsReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotCreateJobGroupReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotJobControlReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotJobGroupQueryReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotJobQueryReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotJobsPageReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotRespDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotStartJobReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.enums.AliOutboundBotActionEnum;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.enums.AliOutboundBotCallbackEventEnum;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.enums.AliOutboundBotCallbackProcessStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 阿里智能外呼服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aliyun.outbound-bot", name = "enabled", havingValue = "true")
public class AliOutboundBotServiceImpl implements AliOutboundBotService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AliOutboundBotProperties properties;
    private final AliOutboundBotClientGateway clientGateway;
    private final List<AliOutboundBotCallbackHandler> callbackHandlers;
    private final AliOutboundBotCallbackValidator callbackValidator;
    private final AliOutboundBotJsonSupport jsonSupport;
    private final AliOutboundBotCallLogger callLogger;

    /**
     * 创建外呼任务组，并记录主动调用日志。
     */
    @Override
    public AliOutboundBotRespDto createJobGroup(AliOutboundBotCreateJobGroupReqDto reqDto) {
        CreateJobGroupRequest request = CreateJobGroupRequest.builder()
                .instanceId(defaultString(reqDto.getInstanceId(), properties.getInstanceId()))
                .jobGroupName(reqDto.getJobGroupName())
                .jobGroupDescription(reqDto.getJobGroupDescription())
                .scenarioId(defaultString(reqDto.getScenarioId(), properties.getScenarioId()))
                .scriptId(defaultString(reqDto.getScriptId(), properties.getScriptId()))
                .callingNumber(defaultList(reqDto.getCallingNumber(), properties.getCallingNumbers()))
                .recallCallingNumber(defaultList(reqDto.getRecallCallingNumber(), properties.getRecallCallingNumbers()))
                .strategyJson(reqDto.getStrategyJson())
                .recallStrategyJson(reqDto.getRecallStrategyJson())
                .priority(reqDto.getPriority())
                .minConcurrency(reqDto.getMinConcurrency())
                .ringingDuration(reqDto.getRingingDuration())
                .flashSmsExtras(reqDto.getFlashSmsExtras())
                .build();
        return executeAliCall(AliOutboundBotActionEnum.CREATE_JOB_GROUP, reqDto, jsonSupport.toJson(request),
                clientGateway.createJobGroup(request));
    }

    /**
     * 导入任务组名单，路径任务组 ID 优先补入请求体。
     */
    @Override
    public AliOutboundBotRespDto assignJobs(String jobGroupId, AliOutboundBotAssignJobsReqDto reqDto) {
        if (isBlank(reqDto.getJobGroupId())) {
            reqDto.setJobGroupId(jobGroupId);
        }
        AssignJobsRequest request = AssignJobsRequest.builder()
                .instanceId(defaultString(reqDto.getInstanceId(), properties.getInstanceId()))
                .jobGroupId(defaultString(reqDto.getJobGroupId(), jobGroupId))
                .jobsJson(reqDto.getJobsJson())
                .callingNumber(defaultList(reqDto.getCallingNumber(), properties.getCallingNumbers()))
                .isAsynchrony(reqDto.getIsAsynchrony())
                .jobDataParsingTaskId(reqDto.getJobDataParsingTaskId())
                .rosterType(reqDto.getRosterType())
                .strategyJson(reqDto.getStrategyJson())
                .build();
        return executeAliCall(AliOutboundBotActionEnum.ASSIGN_JOBS, reqDto, jsonSupport.toJson(request),
                clientGateway.assignJobs(request));
    }

    /**
     * 发起单个号码外呼；jobJson 为空时由请求对象自动组装。
     */
    @Override
    public AliOutboundBotRespDto startJob(AliOutboundBotStartJobReqDto reqDto) {
        StartJobRequest request = StartJobRequest.builder()
                .instanceId(defaultString(reqDto.getInstanceId(), properties.getInstanceId()))
                .jobGroupId(reqDto.getJobGroupId())
                .scenarioId(defaultString(reqDto.getScenarioId(), properties.getScenarioId()))
                .scriptId(defaultString(reqDto.getScriptId(), properties.getScriptId()))
                .callingNumber(defaultList(reqDto.getCallingNumber(), properties.getCallingNumbers()))
                .jobJson(resolveSingleJobJson(reqDto))
                .build();
        return executeAliCall(AliOutboundBotActionEnum.START_JOB, reqDto, jsonSupport.toJson(request),
                clientGateway.startJob(request));
    }

    /**
     * 启动或恢复任务组下的外呼任务。
     */
    @Override
    public AliOutboundBotRespDto resumeJobs(AliOutboundBotJobControlReqDto reqDto) {
        ResumeJobsRequest request = ResumeJobsRequest.builder()
                .instanceId(defaultString(reqDto.getInstanceId(), properties.getInstanceId()))
                .jobGroupId(reqDto.getJobGroupId())
                .scenarioId(defaultString(reqDto.getScenarioId(), properties.getScenarioId()))
                .all(defaultBoolean(reqDto.getAll()))
                .jobId(reqDto.getJobId())
                .jobReferenceId(reqDto.getJobReferenceId())
                .build();
        return executeAliCall(AliOutboundBotActionEnum.RESUME_JOBS, reqDto, jsonSupport.toJson(request),
                clientGateway.resumeJobs(request));
    }

    /**
     * 暂停任务组下的外呼任务。
     */
    @Override
    public AliOutboundBotRespDto suspendJobs(AliOutboundBotJobControlReqDto reqDto) {
        SuspendJobsRequest request = SuspendJobsRequest.builder()
                .instanceId(defaultString(reqDto.getInstanceId(), properties.getInstanceId()))
                .jobGroupId(reqDto.getJobGroupId())
                .scenarioId(defaultString(reqDto.getScenarioId(), properties.getScenarioId()))
                .all(defaultBoolean(reqDto.getAll()))
                .jobId(reqDto.getJobId())
                .jobReferenceId(reqDto.getJobReferenceId())
                .build();
        return executeAliCall(AliOutboundBotActionEnum.SUSPEND_JOBS, reqDto, jsonSupport.toJson(request),
                clientGateway.suspendJobs(request));
    }

    /**
     * 取消任务组下的外呼任务。
     */
    @Override
    public AliOutboundBotRespDto cancelJobs(AliOutboundBotJobControlReqDto reqDto) {
        CancelJobsRequest request = CancelJobsRequest.builder()
                .instanceId(defaultString(reqDto.getInstanceId(), properties.getInstanceId()))
                .jobGroupId(reqDto.getJobGroupId())
                .scenarioId(defaultString(reqDto.getScenarioId(), properties.getScenarioId()))
                .all(defaultBoolean(reqDto.getAll()))
                .jobId(reqDto.getJobId())
                .jobReferenceId(reqDto.getJobReferenceId())
                .build();
        return executeAliCall(AliOutboundBotActionEnum.CANCEL_JOBS, reqDto, jsonSupport.toJson(request),
                clientGateway.cancelJobs(request));
    }

    /**
     * 查询外呼任务组详情。
     */
    @Override
    public AliOutboundBotRespDto queryJobGroup(AliOutboundBotJobGroupQueryReqDto reqDto) {
        DescribeJobGroupRequest request = DescribeJobGroupRequest.builder()
                .instanceId(defaultString(reqDto.getInstanceId(), properties.getInstanceId()))
                .jobGroupId(reqDto.getJobGroupId())
                .briefTypes(reqDto.getBriefTypes())
                .build();
        return executeAliCall(AliOutboundBotActionEnum.DESCRIBE_JOB_GROUP, reqDto, jsonSupport.toJson(request),
                clientGateway.describeJobGroup(request));
    }

    /**
     * 分页查询任务组下的外呼任务。
     */
    @Override
    public AliOutboundBotRespDto queryJobs(AliOutboundBotJobsPageReqDto reqDto) {
        ListJobsByGroupRequest request = ListJobsByGroupRequest.builder()
                .instanceId(defaultString(reqDto.getInstanceId(), properties.getInstanceId()))
                .jobGroupId(reqDto.getJobGroupId())
                .jobStatus(reqDto.getJobStatus())
                .jobFailureReason(reqDto.getJobFailureReason())
                .pageNumber(defaultInteger(reqDto.getPageNumber(), 1))
                .pageSize(defaultInteger(reqDto.getPageSize(), 10))
                .build();
        return executeAliCall(AliOutboundBotActionEnum.LIST_JOBS_BY_GROUP, reqDto, jsonSupport.toJson(request),
                clientGateway.listJobsByGroup(request));
    }

    /**
     * 查询单个外呼任务详情。
     */
    @Override
    public AliOutboundBotRespDto queryJobDetail(AliOutboundBotJobQueryReqDto reqDto) {
        DescribeJobRequest request = DescribeJobRequest.builder()
                .instanceId(defaultString(reqDto.getInstanceId(), properties.getInstanceId()))
                .jobId(reqDto.getJobId())
                .withScript(reqDto.getWithScript())
                .build();
        return executeAliCall(AliOutboundBotActionEnum.DESCRIBE_JOB, reqDto, jsonSupport.toJson(request),
                clientGateway.describeJob(request));
    }

    /**
     * 处理阿里智能外呼回调，失败时也返回 JSON 摘要并写应用日志。
     */
    @Override
    public JSONObject callbackResult(String rawBody, Map<String, String> headers, String sourceIp) {
        LocalDateTime startTime = LocalDateTime.now();
        JSONObject impData = new JSONObject();
        JSONObject resultData = new JSONObject();
        JSONObject body = null;
        AliOutboundBotCallbackEventEnum eventEnum = AliOutboundBotCallbackEventEnum.UNKNOWN;
        boolean success = false;
        impData.put("headers", headers == null ? Collections.emptyMap() : headers);
        impData.put("rawBody", rawBody);
        impData.put("sourceIp", sourceIp);
        impData.put("receiveTime", format(startTime));
        try {
            body = jsonSupport.parseCallbackBody(rawBody);
            eventEnum = AliOutboundBotCallbackEventEnum.of(resolveEventType(body));
            impData.put("body", body);
            impData.put("eventType", eventEnum.getCode());
            callbackValidator.validate(body, rawBody, headers, sourceIp);
            JSONObject handlerResult = dispatchCallback(eventEnum.getCode(), body);
            success = true;
            resultData.put("processStatus", AliOutboundBotCallbackProcessStatusEnum.SUCCESS.getCode());
            resultData.put("signatureValid", Boolean.TRUE);
            resultData.put("dispatchResult", handlerResult);
            resultData.put("responseTime", format(LocalDateTime.now()));
        } catch (Exception e) {
            resultData.put("processStatus", AliOutboundBotCallbackProcessStatusEnum.FAILED.getCode());
            resultData.put("signatureValid", Boolean.FALSE);
            resultData.put("errorMessage", e.getMessage());
            resultData.put("responseTime", format(LocalDateTime.now()));
            impData.put("body", body);
            impData.put("eventType", eventEnum.getCode());
            log.error("阿里智能外呼回调处理异常", e);
        } finally {
            callLogger.logCallback(impData, resultData, success);
        }
        return resultData;
    }

    /**
     * 等待异步 SDK 响应，并统一转换响应、记录日志。
     */
    private <T extends Response> AliOutboundBotRespDto executeAliCall(AliOutboundBotActionEnum actionEnum,
                                                                      Object bizReq,
                                                                      JSONObject sdkReq,
                                                                      CompletableFuture<T> future) {
        LocalDateTime requestTime = LocalDateTime.now();
        JSONObject impData = new JSONObject();
        impData.put("action", actionEnum.getAction());
        impData.put("bizReq", bizReq);
        impData.put("sdkReq", sdkReq);
        impData.put("requestTime", format(requestTime));

        JSONObject resultData = new JSONObject();
        boolean success = false;
        try {
            T response = await(future);
            AliOutboundBotRespDto respDto = jsonSupport.toRespDto(actionEnum, response);
            success = Boolean.TRUE.equals(respDto.getSuccess());
            resultData.put("aliResponse", jsonSupport.toJson(response));
            resultData.put("bizResp", respDto);
            resultData.put("responseTime", format(LocalDateTime.now()));
            return respDto;
        } catch (Exception e) {
            resultData.put("errorMessage", e.getMessage());
            resultData.put("responseTime", format(LocalDateTime.now()));
            throw new RuntimeException(actionEnum.getDesc() + "失败", e);
        } finally {
            callLogger.logApiCall(actionEnum, impData, resultData, success);
        }
    }

    /**
     * 在配置的超时时间内等待阿里异步 SDK 返回。
     */
    private <T> T await(CompletableFuture<T> future) throws ExecutionException, InterruptedException, TimeoutException {
        return future.get(defaultInteger(properties.getAsyncWaitTimeoutSeconds(), 10), TimeUnit.SECONDS);
    }

    /**
     * 分发回调到匹配的业务处理器。
     */
    private JSONObject dispatchCallback(String eventType, JSONObject body) {
        if (CollectionUtils.isEmpty(callbackHandlers)) {
            JSONObject result = new JSONObject();
            result.put("handled", Boolean.FALSE);
            result.put("message", "未配置回调业务处理器");
            return result;
        }
        for (AliOutboundBotCallbackHandler handler : callbackHandlers) {
            if (handler.support(eventType, body)) {
                JSONObject result = handler.handle(body);
                if (result == null) {
                    result = new JSONObject();
                }
                result.put("handler", handler.getClass().getName());
                return result;
            }
        }
        JSONObject result = new JSONObject();
        result.put("handled", Boolean.FALSE);
        result.put("message", "未匹配到回调业务处理器");
        return result;
    }

    /**
     * 解析回调事件类型，兼容不同字段命名。
     */
    private String resolveEventType(JSONObject body) {
        if (body == null) {
            return null;
        }
        String eventType = body.getString("eventType");
        if (isBlank(eventType)) {
            eventType = body.getString("event_type");
        }
        if (isBlank(eventType)) {
            eventType = body.getString("type");
        }
        return eventType;
    }

    /**
     * 组装单呼任务 JSON，调用方传 jobJson 时保持原样透传。
     */
    private String resolveSingleJobJson(AliOutboundBotStartJobReqDto reqDto) {
        if (!isBlank(reqDto.getJobJson())) {
            return reqDto.getJobJson();
        }
        if (isBlank(reqDto.getPhoneNumber())) {
            throw new IllegalArgumentException("单个号码外呼时，jobJson 和 phoneNumber 不能同时为空");
        }
        JSONObject jobJson = new JSONObject();
        jobJson.put("phoneNumber", reqDto.getPhoneNumber());
        jobJson.put("name", reqDto.getName());
        jobJson.put("referenceId", reqDto.getReferenceId());
        if (reqDto.getExtras() != null && !reqDto.getExtras().isEmpty()) {
            jobJson.put("extras", reqDto.getExtras());
        }
        return jobJson.toJSONString();
    }

    /** 为空时使用默认字符串。 */
    private String defaultString(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    /** 为空列表时使用默认列表，并过滤环境变量占位产生的空字符串。 */
    private List<String> defaultList(List<String> value, List<String> defaultValue) {
        List<String> selected = CollectionUtils.isEmpty(value) ? defaultValue : value;
        if (CollectionUtils.isEmpty(selected)) {
            return null;
        }
        List<String> result = new ArrayList<>(selected.size());
        for (String item : selected) {
            if (!isBlank(item)) {
                result.add(item.trim());
            }
        }
        return result.isEmpty() ? null : result;
    }

    /** 为空时默认控制全部任务。 */
    private Boolean defaultBoolean(Boolean value) {
        return value == null ? Boolean.TRUE : value;
    }

    /** 为空时使用默认整数。 */
    private Integer defaultInteger(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    /** 格式化日志时间。 */
    private String format(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_FORMATTER.format(dateTime);
    }

    /** 判断字符串是否为空白。 */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
