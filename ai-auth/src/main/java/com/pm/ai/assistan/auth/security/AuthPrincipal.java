package com.pm.ai.assistan.auth.security;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
/**
 * 放入 Spring SecurityContext 的当前用户主体。
 * Controller/Service 层通过它获取当前登录用户信息。
 */
public class AuthPrincipal {

    /**
     * 当前会话 hash，用于从 Redis 重新读取会话和绑定 PM token。
     */
    private String tokenHash;

    /**
     * 用户 id。
     */
    private Long userId;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 展示名称。
     */
    private String displayName;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 角色集合，会转换成 ROLE_* authority。
     */
    @Builder.Default
    private List<String> roles = new ArrayList<>();

    /**
     * 接口权限集合，会转换成权限编码 authority。
     */
    @Builder.Default
    private List<PermissionGrant> permissions = new ArrayList<>();
}
