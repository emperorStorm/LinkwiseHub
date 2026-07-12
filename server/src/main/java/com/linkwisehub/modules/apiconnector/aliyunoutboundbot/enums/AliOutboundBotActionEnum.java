package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 阿里智能外呼 OpenAPI Action 枚举。
 */
@Getter
@AllArgsConstructor
public enum AliOutboundBotActionEnum {
    CREATE_JOB_GROUP("CreateJobGroup", "创建外呼任务组"),
    ASSIGN_JOBS("AssignJobs", "导入外呼名单"),
    START_JOB("StartJob", "单个号码外呼"),
    RESUME_JOBS("ResumeJobs", "启动/恢复外呼任务"),
    SUSPEND_JOBS("SuspendJobs", "暂停外呼任务"),
    CANCEL_JOBS("CancelJobs", "取消外呼任务"),
    DESCRIBE_JOB_GROUP("DescribeJobGroup", "查询外呼任务组"),
    LIST_JOBS_BY_GROUP("ListJobsByGroup", "分页查询外呼任务"),
    DESCRIBE_JOB("DescribeJob", "查询外呼任务详情"),
    CALLBACK_RESULT("CallbackResult", "外呼结果回调");

    private final String action;
    private final String desc;
}
