package com.pm.ai.assistan.auth.dto;

import lombok.Data;

@Data
/**
 * 认证模块统一响应结构。
 * 字段保持 code/msg/data，与主应用原有 AgentResult 的 JSON 形状一致，但 ai-auth 不再反向依赖主应用。
 */
public class AuthResult<T> {

    public static final int SUCCESS_CODE = 0;
    public static final int FAIL_CODE = 1;

    private int code;
    private String msg;
    private T data;

    public AuthResult() {
    }

    public AuthResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> AuthResult<T> success() {
        return success("success", null);
    }

    public static <T> AuthResult<T> success(T data) {
        return success("success", data);
    }

    public static <T> AuthResult<T> success(String msg, T data) {
        return new AuthResult<>(SUCCESS_CODE, msg, data);
    }

    public static <T> AuthResult<T> fail() {
        return fail("fail", null);
    }

    public static <T> AuthResult<T> fail(String msg) {
        return fail(msg, null);
    }

    public static <T> AuthResult<T> fail(String msg, T data) {
        return new AuthResult<>(FAIL_CODE, msg, data);
    }
}
