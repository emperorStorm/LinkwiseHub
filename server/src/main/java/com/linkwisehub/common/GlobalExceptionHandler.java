package com.linkwisehub.common;

import com.linkwisehub.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

/**
 * 全局异常处理器，统一把异常转换成前端可识别的 ApiResponse。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常，保持 HTTP 200 返回体里的业务错误码，方便当前前端统一读取。
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getErrorCode(), e.getMessage());
        return ApiResponse.error(e.getHttpStatus(), e.getMessage(), e.getErrorCode());
    }

    /**
     * 处理 @Valid 请求体校验异常。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().isEmpty()
                ? "参数校验失败"
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ApiResponse.error(400, message, ErrorCode.PARAM_INVALID.getCode());
    }

    /**
     * 处理表单或路径参数绑定异常。
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().isEmpty()
                ? "参数绑定失败"
                : e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ApiResponse.error(400, message, ErrorCode.PARAM_INVALID.getCode());
    }

    /**
     * 处理单个参数约束异常。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException e) {
        return ApiResponse.error(400, e.getMessage(), ErrorCode.PARAM_INVALID.getCode());
    }

    /**
     * 处理 JSON 解析异常。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败", e);
        return ApiResponse.error(400, "请求体格式不正确", ErrorCode.PARAM_INVALID.getCode());
    }

    /**
     * 兜底处理系统异常，避免把堆栈直接暴露给前端。
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.systemError("系统异常，请稍后重试");
    }
}
