package com.pm.ai.assistan.auth.exception;

public class PmAuthorizationMissingException extends RuntimeException {

    /**
     * 当前本地登录会话还没有绑定 PM token 时抛出。
     */
    public PmAuthorizationMissingException() {
        super("请先绑定 PM 系统授权");
    }
}
