package com.linkwisehub.common;

/**
 * 错误码枚举类
 * 定义系统所有可能的错误类型，便于错误追踪和处理
 */
public enum ErrorCode {
    
    // ==================== 系统级错误 (1000-1999) ====================
    SYSTEM_ERROR("1000", "系统内部错误"),
    SYSTEM_BUSY("1001", "系统繁忙，请稍后重试"),
    SERVICE_UNAVAILABLE("1002", "服务暂不可用"),
    
    // ==================== 参数校验错误 (2000-2999) ====================
    PARAM_INVALID("2000", "参数无效"),
    PARAM_MISSING("2001", "缺少必要参数"),
    PARAM_TYPE_ERROR("2002", "参数类型错误"),
    MESSAGE_EMPTY("2003", "消息内容不能为空"),
    MESSAGE_TOO_LONG("2004", "消息内容过长"),
    
    // ==================== 业务逻辑错误 (3000-3999) ====================
    BUSINESS_ERROR("3000", "业务处理失败"),
    CONVERSATION_NOT_FOUND("3001", "会话不存在"),
    CONVERSATION_ACCESS_DENIED("3002", "无权访问该会话"),
    USER_NOT_FOUND("3003", "用户不存在"),
    
    // ==================== AI服务错误 (4000-4999) ====================
    AI_SERVICE_ERROR("4000", "AI服务调用失败"),
    AI_SERVICE_TIMEOUT("4001", "AI服务响应超时"),
    AI_SERVICE_UNAVAILABLE("4002", "AI服务暂时不可用，请稍后重试"),
    AI_CONFIG_MISSING("4003", "AI服务配置不完整"),
    AI_API_KEY_INVALID("4004", "AI服务API Key无效"),
    AI_API_KEY_EXPIRED("4005", "AI服务API Key已过期"),
    AI_QUOTA_EXCEEDED("4006", "AI服务配额已用完"),
    AI_RATE_LIMITED("4007", "AI服务请求过于频繁，请稍后重试"),
    AI_MODEL_UNAVAILABLE("4008", "指定的AI模型不可用"),
    AI_PARSE_ERROR("4009", "AI服务响应解析失败"),
    AI_NETWORK_ERROR("4010", "AI服务网络连接失败"),
    AI_RETRY_EXHAUSTED("4011", "AI服务重试次数已用完"),
    
    // ==================== 网络IO错误 (5000-5999) ====================
    NETWORK_ERROR("5000", "网络连接失败"),
    NETWORK_TIMEOUT("5001", "网络请求超时"),
    CONNECTION_REFUSED("5002", "无法连接到服务器"),
    
    // ==================== 认证授权错误 (6000-6999) ====================
    UNAUTHORIZED("6000", "未授权访问"),
    TOKEN_INVALID("6001", "认证令牌无效"),
    TOKEN_EXPIRED("6002", "认证令牌已过期"),
    PERMISSION_DENIED("6003", "权限不足");
    
    /**
     * 错误码
     */
    private final String code;
    
    /**
     * 错误描述
     */
    private final String description;
    
    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据错误码获取枚举
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return SYSTEM_ERROR;
    }
    
    /**
     * 获取友好的错误提示信息
     */
    public String getFriendlyMessage() {
        switch (this) {
            case AI_SERVICE_TIMEOUT:
                return "AI响应较慢，请检查网络后重试";
            case AI_SERVICE_UNAVAILABLE:
                return "AI服务暂时不可用，请稍后重试";
            case AI_API_KEY_INVALID:
                return "AI服务配置异常，请联系管理员";
            case AI_API_KEY_EXPIRED:
                return "AI服务授权已过期，请联系管理员续费";
            case AI_QUOTA_EXCEEDED:
                return "AI服务配额已用完，请明天再试或联系管理员";
            case AI_RATE_LIMITED:
                return "请求过于频繁，请稍后再试";
            case AI_NETWORK_ERROR:
                return "网络连接失败，请检查网络后重试";
            case NETWORK_TIMEOUT:
                return "网络请求超时，请重试";
            case PARAM_INVALID:
                return "输入内容不符合要求，请检查后重试";
            case MESSAGE_EMPTY:
                return "请输入您的问题";
            case MESSAGE_TOO_LONG:
                return "输入内容过长，请精简后重试";
            default:
                return this.description;
        }
    }
}
