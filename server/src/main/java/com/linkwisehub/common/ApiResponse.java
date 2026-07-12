package com.linkwisehub.common;

/**
 * 统一API响应结果封装类
 * 用于前后端交互的标准化响应格式
 */
public class ApiResponse<T> {
    
    /**
     * 响应状态码
     */
    private int code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 错误码（用于前端错误类型判断）
     */
    private String errorCode;
    
    /**
     * 请求追踪ID（用于日志排查）
     */
    private String traceId;
    
    /**
     * 时间戳
     */
    private long timestamp;
    
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }
    
    /**
     * 成功响应（带消息）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(200, message, null);
    }
    
    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
    
    /**
     * 失败响应（带错误码）
     */
    public static <T> ApiResponse<T> error(int code, String message, String errorCode) {
        ApiResponse<T> response = new ApiResponse<>(code, message, null);
        response.setErrorCode(errorCode);
        return response;
    }
    
    /**
     * 业务异常响应
     */
    public static <T> ApiResponse<T> businessError(String message) {
        return new ApiResponse<>(400, message, null);
    }
    
    /**
     * 系统异常响应
     */
    public static <T> ApiResponse<T> systemError(String message) {
        return new ApiResponse<>(500, message, null);
    }
    
    // Getter and Setter
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
