package com.pm.ai.assistan.auth.model;

public final class UserStatus {

    /**
     * 已启用，可以登录系统。
     */
    public static final String ACTIVE = "ACTIVE";

    /**
     * 待审核，注册/申请后默认状态，不能登录。
     */
    public static final String PENDING = "PENDING";

    /**
     * 已禁用，管理员主动关闭后不能登录。
     */
    public static final String DISABLED = "DISABLED";

    private UserStatus() {
    }
}
