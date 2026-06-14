package com.pm.ai.assistan.auth.exception;

public class AuthException extends RuntimeException {

    /**
     * 用于业务认证失败的异常，例如账号不存在、密码错误、用户未启用。
     */
    public AuthException(String message) {
        super(message);
    }
}
