package com.pm.ai.assistan.auth.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 接口权限实体。
 * 一个权限描述一个 HTTP 方法 + 路径模式，登录后会写入 Redis 会话。
 */
public class AuthPermission {

    /**
     * 权限主键，对应 auth_permission.id。
     */
    private Long id;

    /**
     * 权限编码，例如 AI_CHAT、AUTH_ADMIN。
     */
    private String code;

    /**
     * 权限名称，用于页面展示。
     */
    private String name;

    /**
     * HTTP 方法，支持具体方法或 *。
     */
    private String method;

    /**
     * 接口路径模式，例如 /ai/**。
     */
    private String pathPattern;

    /**
     * 权限说明，帮助后续维护权限含义。
     */
    private String description;

    /**
     * 创建时间，由数据库默认值写入。
     */
    private LocalDateTime createdAt;
}
