package com.pm.ai.assistan.unit;

import lombok.Data;
import lombok.Getter;
@Data
public class AgentResult<T> {

    public static final int SUCCESS_CODE = 0;
    public static final int FAIL_CODE = 1;

    private int code;
    private String msg;
    private T data;

    public AgentResult() {
    }

    public AgentResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> AgentResult<T> success() {
        return success("success", null);
    }

    public static <T> AgentResult<T> success(T data) {
        return success("success", data);
    }

    public static <T> AgentResult<T> success(String msg, T data) {
        return new AgentResult<>(SUCCESS_CODE, msg, data);
    }

    public static <T> AgentResult<T> fail() {
        return fail("fail", null);
    }

    public static <T> AgentResult<T> fail(String msg) {
        return fail(msg, null);
    }

    public static <T> AgentResult<T> fail(String msg, T data) {
        return new AgentResult<>(FAIL_CODE, msg, data);
    }

}
