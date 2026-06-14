package com.pm.ai.assistan.auth.security;

/**
 * 创建登录会话后的返回值。
 *
 * @param token 原始 token，只返回给前端一次
 * @param expiresIn 过期时间，单位秒
 * @param session 已写入 Redis 的会话对象
 */
public record CreatedSession(String token, long expiresIn, AuthSession session) {
}
