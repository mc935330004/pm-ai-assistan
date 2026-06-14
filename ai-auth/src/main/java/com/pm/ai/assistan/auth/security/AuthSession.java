package com.pm.ai.assistan.auth.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * Redis 中保存的本地登录会话。
 * 这个对象是“当前登录态”的权威数据，前端 localStorage 只保存展示缓存。
 */
public class AuthSession {

    /**
     * 原始 token 的 SHA-256 摘要，Redis key 使用它，避免直接存明文 token。
     */
    private String tokenHash;

    /**
     * 当前登录用户 id。
     */
    private Long userId;

    /**
     * 当前登录用户名。
     */
    private String username;

    /**
     * 当前登录用户展示名称。
     */
    private String displayName;

    /**
     * 当前登录用户邮箱。
     */
    private String email;

    /**
     * 当前用户角色快照。
     */
    @Builder.Default
    private List<String> roles = new ArrayList<>();

    /**
     * 当前用户接口权限快照。
     */
    @Builder.Default
    private List<PermissionGrant> permissions = new ArrayList<>();

    /**
     * 当前会话绑定的 PM 系统 token。
     * 它和本地登录 token 分离，防止把本地 token 误传给 PM 系统。
     */
    private String pmToken;

    /**
     * 会话创建时间，用于排查登录问题。
     */
    private Instant createdAt;
}
