package com.linkwisehub.common.exception;

import com.linkwisehub.common.ErrorCode;
import lombok.Getter;

/**
 * 业务异常类
 * 用于处理业务逻辑相关的异常
 */
@Getter
public class BusinessException extends RuntimeException {
    
    /**
     * 错误码
     */
    private final String errorCode;
    
    /**
     * 详细错误信息
     */
    private final String detail;
    
    /**
     * HTTP状态码
     */
    private final int httpStatus;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode.getCode();
        this.detail = null;
        this.httpStatus = 400;
    }
    
    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getDescription() + ": " + detail);
        this.errorCode = errorCode.getCode();
        this.detail = detail;
        this.httpStatus = 400;
    }
    
    public BusinessException(ErrorCode errorCode, String detail, int httpStatus) {
        super(errorCode.getDescription() + ": " + detail);
        this.errorCode = errorCode.getCode();
        this.detail = detail;
        this.httpStatus = httpStatus;
    }
    
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.detail = null;
        this.httpStatus = 400;
    }
    
    public BusinessException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.detail = null;
        this.httpStatus = httpStatus;
    }
    
    /**
     * 获取用户友好的错误消息
     */
    public String getFriendlyMessage() {
        ErrorCode code = ErrorCode.fromCode(this.errorCode);
        return code.getFriendlyMessage();
    }
    
    // ==================== 便捷工厂方法 ====================
    
    /**
     * 会话不存在
     */
    public static BusinessException conversationNotFound(Long id) {
        return new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND, 
            "会话ID: " + id);
    }
    
    /**
     * 会话访问被拒绝
     */
    public static BusinessException conversationAccessDenied(Long id) {
        return new BusinessException(ErrorCode.CONVERSATION_ACCESS_DENIED, 
            "无权访问会话: " + id);
    }
    
    /**
     * 消息为空
     */
    public static BusinessException messageEmpty() {
        return new BusinessException(ErrorCode.MESSAGE_EMPTY);
    }
    
    /**
     * 消息过长
     */
    public static BusinessException messageTooLong(int maxLength) {
        return new BusinessException(ErrorCode.MESSAGE_TOO_LONG, 
            "消息长度不能超过 " + maxLength + " 字符");
    }
    
    /**
     * 参数无效
     */
    public static BusinessException paramInvalid(String paramName) {
        return new BusinessException(ErrorCode.PARAM_INVALID, paramName);
    }
    
    /**
     * 缺少必要参数
     */
    public static BusinessException paramMissing(String paramName) {
        return new BusinessException(ErrorCode.PARAM_MISSING, paramName);
    }
}
