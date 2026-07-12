package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.service;

import com.aliyun.sdk.service.outboundbot20191226.AsyncClient;
import com.aliyun.sdk.service.outboundbot20191226.models.AssignJobsRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.AssignJobsResponse;
import com.aliyun.sdk.service.outboundbot20191226.models.CancelJobsRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.CancelJobsResponse;
import com.aliyun.sdk.service.outboundbot20191226.models.CreateJobGroupRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.CreateJobGroupResponse;
import com.aliyun.sdk.service.outboundbot20191226.models.DescribeJobGroupRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.DescribeJobGroupResponse;
import com.aliyun.sdk.service.outboundbot20191226.models.DescribeJobRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.DescribeJobResponse;
import com.aliyun.sdk.service.outboundbot20191226.models.ListJobsByGroupRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.ListJobsByGroupResponse;
import com.aliyun.sdk.service.outboundbot20191226.models.ResumeJobsRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.ResumeJobsResponse;
import com.aliyun.sdk.service.outboundbot20191226.models.StartJobRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.StartJobResponse;
import com.aliyun.sdk.service.outboundbot20191226.models.SuspendJobsRequest;
import com.aliyun.sdk.service.outboundbot20191226.models.SuspendJobsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 阿里智能外呼异步 SDK 网关，避免业务层直接依赖 AsyncClient 细节。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aliyun.outbound-bot", name = "enabled", havingValue = "true")
public class AliOutboundBotClientGateway {
    private final AsyncClient asyncClient;

    /** 创建外呼任务组。 */
    public CompletableFuture<CreateJobGroupResponse> createJobGroup(CreateJobGroupRequest request) {
        return asyncClient.createJobGroup(request);
    }

    /** 导入外呼名单。 */
    public CompletableFuture<AssignJobsResponse> assignJobs(AssignJobsRequest request) {
        return asyncClient.assignJobs(request);
    }

    /** 发起单个号码外呼。 */
    public CompletableFuture<StartJobResponse> startJob(StartJobRequest request) {
        return asyncClient.startJob(request);
    }

    /** 启动或恢复外呼任务。 */
    public CompletableFuture<ResumeJobsResponse> resumeJobs(ResumeJobsRequest request) {
        return asyncClient.resumeJobs(request);
    }

    /** 暂停外呼任务。 */
    public CompletableFuture<SuspendJobsResponse> suspendJobs(SuspendJobsRequest request) {
        return asyncClient.suspendJobs(request);
    }

    /** 取消外呼任务。 */
    public CompletableFuture<CancelJobsResponse> cancelJobs(CancelJobsRequest request) {
        return asyncClient.cancelJobs(request);
    }

    /** 查询外呼任务组。 */
    public CompletableFuture<DescribeJobGroupResponse> describeJobGroup(DescribeJobGroupRequest request) {
        return asyncClient.describeJobGroup(request);
    }

    /** 分页查询外呼任务。 */
    public CompletableFuture<ListJobsByGroupResponse> listJobsByGroup(ListJobsByGroupRequest request) {
        return asyncClient.listJobsByGroup(request);
    }

    /** 查询外呼任务详情。 */
    public CompletableFuture<DescribeJobResponse> describeJob(DescribeJobRequest request) {
        return asyncClient.describeJob(request);
    }
}
