package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.service;

import com.alibaba.fastjson.JSONObject;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotAssignJobsReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotCreateJobGroupReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotJobControlReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotJobGroupQueryReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotJobQueryReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotJobsPageReqDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotRespDto;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.dto.AliOutboundBotStartJobReqDto;

import java.util.Map;

/**
 * 阿里智能外呼服务定义。
 */
public interface AliOutboundBotService {
    /** 创建外呼任务组。 */
    AliOutboundBotRespDto createJobGroup(AliOutboundBotCreateJobGroupReqDto reqDto);

    /** 导入任务组外呼名单。 */
    AliOutboundBotRespDto assignJobs(String jobGroupId, AliOutboundBotAssignJobsReqDto reqDto);

    /** 发起单个号码外呼。 */
    AliOutboundBotRespDto startJob(AliOutboundBotStartJobReqDto reqDto);

    /** 启动或恢复外呼任务。 */
    AliOutboundBotRespDto resumeJobs(AliOutboundBotJobControlReqDto reqDto);

    /** 暂停外呼任务。 */
    AliOutboundBotRespDto suspendJobs(AliOutboundBotJobControlReqDto reqDto);

    /** 取消外呼任务。 */
    AliOutboundBotRespDto cancelJobs(AliOutboundBotJobControlReqDto reqDto);

    /** 查询外呼任务组。 */
    AliOutboundBotRespDto queryJobGroup(AliOutboundBotJobGroupQueryReqDto reqDto);

    /** 分页查询外呼任务。 */
    AliOutboundBotRespDto queryJobs(AliOutboundBotJobsPageReqDto reqDto);

    /** 查询外呼任务详情。 */
    AliOutboundBotRespDto queryJobDetail(AliOutboundBotJobQueryReqDto reqDto);

    /** 处理阿里外呼回调。 */
    JSONObject callbackResult(String rawBody, Map<String, String> headers, String sourceIp);
}
