package com.pm.ai.assistan.auth.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 本地登录用户表对应的实体。
 * 这里只保存登录系统自己的账号信息，不保存 PM 系统账号密码。
 */
public class AuthUser {

    /**
     * 用户主键，对应 auth_user.id。
     */
    private Long id;

    /**
     * 登录账号，后端要求唯一。
     */
    private String username;

    /**
     * 页面展示名称，未填写时默认使用 username。
     */
    private String displayName;

    /**
     * BCrypt 加密后的密码摘要，永远不返回给前端。
     */
    private String passwordHash;

    /**
     * 用户邮箱，用于后续扩展通知或找回密码。
     */
    private String email;

    /**
     * 用户状态：ACTIVE/PENDING/DISABLED。
     */
    private String status;

    /**
     * 创建时间，由数据库默认值写入。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间，状态变更时刷新。
     */
    private LocalDateTime updatedAt;
}
