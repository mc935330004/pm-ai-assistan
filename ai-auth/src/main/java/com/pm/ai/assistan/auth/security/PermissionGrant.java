package com.pm.ai.assistan.auth.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * 登录会话中的权限快照。
 * 只保留鉴权需要的字段，避免每个请求都回数据库查权限。
 */
public class PermissionGrant {

    /**
     * 权限编码，对应 Spring Security authority。
     */
    private String code;

    /**
     * HTTP 方法，当前主要用于后续扩展细粒度路径匹配。
     */
    private String method;

    /**
     * 路径模式，例如 /ai/**。
     */
    private String pathPattern;
}
