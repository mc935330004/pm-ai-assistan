package com.pm.ai.assistan.auth.controller;

import com.pm.ai.assistan.auth.dto.AuthResult;
import com.pm.ai.assistan.auth.exception.AuthException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 认证模块统一异常处理。
 * 让业务错误、参数错误都返回 AuthResult，前端可以统一读取 msg。
 */
@RestControllerAdvice(basePackages = "com.pm.ai.assistan.auth.controller")
public class AuthExceptionHandler {

    /**
     * 登录、注册、权限管理中的业务异常。
     */
    @ExceptionHandler(AuthException.class)
    public AuthResult<Void> handleAuthException(AuthException exception) {
        return AuthResult.fail(exception.getMessage());
    }

    /**
     * DTO 参数校验异常，优先返回第一个字段错误。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AuthResult<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("请求参数不正确");
        return AuthResult.fail(message);
    }

    /**
     * JSON 请求体为空或格式错误。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public AuthResult<Void> handleUnreadableBody() {
        return AuthResult.fail("请求体不能为空或格式不正确");
    }
}
