package com.animelog.backend.controller;

import com.animelog.backend.dto.AjaxResult;
import com.animelog.backend.dto.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，将业务异常统一映射为结构化响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务参数异常（如账号或密码错误、用户已存在等），返回 400。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public AjaxResult handleIllegalArgument(IllegalArgumentException e) {
        return new AjaxResult(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * 处理权限不足异常，返回 403。
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
    public AjaxResult handleAccessDenied(AccessDeniedException e) {
        return new AjaxResult(HttpStatus.FORBIDDEN, "权限不足");
    }

    /**
     * 兜底处理未预期的异常，返回 500。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
    public AjaxResult handleException(Exception e) {
        return new AjaxResult(HttpStatus.ERROR, "服务器内部错误：" + e.getMessage());
    }
}
