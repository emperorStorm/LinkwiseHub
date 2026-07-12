package com.linkwisehub.common.exception;

import com.linkwisehub.common.ErrorCode;
import lombok.Getter;

/**
 * AI服务异常类
 * 用于处理与AI服务相关的所有异常情况
 */
@Getter
public class AiServiceException extends RuntimeException {
    
    /**
     * 错误码
     */
    private final String errorCode;
    
    /**
     * 原始错误信息（用于日志记录）
     */
    private final String originalError;
    
    /**
     * 是否可以重试
     */
    private final boolean retryable;
    
    public AiServiceException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode.getCode();
        this.originalError = message;
        this.retryable = isRetryableError(errorCode);
    }
    
    public AiServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode.getCode();
        this.originalError = cause != null ? cause.getMessage() : message;
        this.retryable = isRetryableError(errorCode);
    }
    
    public AiServiceException(String errorCode, String message, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.originalError = message;
        this.retryable = retryable;
    }
    
    /**
     * 判断错误是否可重试
     */
    private boolean isRetryableError(ErrorCode errorCode) {
        return errorCode == ErrorCode.AI_SERVICE_TIMEOUT ||
               errorCode == ErrorCode.AI_SERVICE_UNAVAILABLE ||
               errorCode == ErrorCode.AI_NETWORK_ERROR ||
               errorCode == ErrorCode.NETWORK_TIMEOUT ||
               errorCode == ErrorCode.AI_RATE_LIMITED ||
               errorCode == ErrorCode.SYSTEM_BUSY;
    }
    
    /**
     * 获取用户友好的错误消息
     */
    public String getFriendlyMessage() {
        ErrorCode code = ErrorCode.fromCode(this.errorCode);
        return code.getFriendlyMessage();
    }
    
    /**
     * 创建配置缺失异常
     */
    public static AiServiceException configMissing(String detail) {
        return new AiServiceException(ErrorCode.AI_CONFIG_MISSING, 
            "AI服务配置不完整: " + detail);
    }
    
    /**
     * 创建API Key无效异常
     */
    public static AiServiceException apiKeyInvalid() {
        return new AiServiceException(ErrorCode.AI_API_KEY_INVALID, 
            "API Key无效");
    }
    
    /**
     * 创建API Key过期异常
     */
    public static AiServiceException apiKeyExpired() {
        return new AiServiceException(ErrorCode.AI_API_KEY_EXPIRED, 
            "API Key已过期");
    }
    
    /**
     * 创建配额超限异常
     */
    public static AiServiceException quotaExceeded() {
        return new AiServiceException(ErrorCode.AI_QUOTA_EXCEEDED, 
            "AI服务配额已用完");
    }
    
    /**
     * 创建限流异常
     */
    public static AiServiceException rateLimited() {
        return new AiServiceException(ErrorCode.AI_RATE_LIMITED, 
            "请求过于频繁");
    }
    
    /**
     * 创建超时异常
     */
    public static AiServiceException timeout() {
        return new AiServiceException(ErrorCode.AI_SERVICE_TIMEOUT, 
            "AI服务响应超时");
    }
    
    /**
     * 创建网络错误异常
     */
    public static AiServiceException networkError(String detail) {
        return new AiServiceException(ErrorCode.AI_NETWORK_ERROR, 
            "网络连接失败: " + detail);
    }
    
    /**
     * 创建解析错误异常
     */
    public static AiServiceException parseError(String detail) {
        return new AiServiceException(ErrorCode.AI_PARSE_ERROR, 
            "无法解析AI响应: " + detail);
    }
    
    /**
     * 创建模型不可用异常
     */
    public static AiServiceException modelUnavailable(String model) {
        return new AiServiceException(ErrorCode.AI_MODEL_UNAVAILABLE, 
            "AI模型 " + model + " 不可用");
    }
    
    /**
     * 创建服务不可用异常
     */
    public static AiServiceException serviceUnavailable() {
        return new AiServiceException(ErrorCode.AI_SERVICE_UNAVAILABLE, 
            "AI服务暂时不可用");
    }
    
    /**
     * 创建重试次数耗尽异常
     */
    public static AiServiceException retryExhausted(int maxRetries) {
        return new AiServiceException(ErrorCode.AI_RETRY_EXHAUSTED, 
            "AI服务重试 " + maxRetries + " 次后仍失败");
    }
}
