package com.pm.ai.assistan.auth.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.ai.assistan.auth.config.AuthProperties;
import com.pm.ai.assistan.auth.exception.AuthException;
import com.pm.ai.assistan.auth.model.AuthPermission;
import com.pm.ai.assistan.auth.model.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
/**
 * 本地登录会话服务。
 * 统一处理 token 生成、token hash、Redis 保存、PM token 绑定。
 */
public class AuthSessionService {

    /**
     * Redis key 前缀，完整 key 为 auth:session:{tokenHash}。
     */
    private static final String SESSION_KEY_PREFIX = "auth:session:";

    /**
     * 标准 Authorization 头前缀。
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 安全随机数生成器，用于生成不可预测的登录 token。
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthProperties authProperties;

    /**
     * 登录成功后创建 Redis 会话。
     * 原始 token 只返回给前端，Redis 中只保存 hash。
     */
    public CreatedSession createSession(AuthUser user, List<String> roles, List<AuthPermission> permissions) {
        String token = generateToken();
        String tokenHash = hashToken(token);
        AuthSession session = AuthSession.builder()
                .tokenHash(tokenHash)
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .roles(roles)
                .permissions(toGrants(permissions))
                .createdAt(Instant.now())
                .build();
        save(session);
        return new CreatedSession(token, authProperties.getTokenTtl().toSeconds(), session);
    }

    /**
     * 用前端传来的原始 token 查 Redis 会话。
     */
    public Optional<AuthSession> findByRawToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        return findByHash(hashToken(token));
    }

    /**
     * 用 token hash 查 Redis 会话。
     * SecurityContext 中只保留 hash，后续绑定 PM token 会走这个方法。
     */
    public Optional<AuthSession> findByHash(String tokenHash) {
        String value = redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + tokenHash);
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, AuthSession.class));
        } catch (JsonProcessingException e) {
            throw new AuthException("登录会话解析失败");
        }
    }

    /**
     * 删除会话，用于退出登录。
     */
    public void deleteByHash(String tokenHash) {
        if (StringUtils.hasText(tokenHash)) {
            redisTemplate.delete(SESSION_KEY_PREFIX + tokenHash);
        }
    }

    /**
     * 给当前本地登录会话绑定 PM token。
     * AI 查询 PM 数据时只读取这个 PM token，不会透传本地登录 token。
     */
    public AuthSession bindPmToken(String tokenHash, String pmToken) {
        AuthSession session = findByHash(tokenHash).orElseThrow(() -> new AuthException("登录已过期，请重新登录"));
        session.setPmToken(normalizeBearer(pmToken));
        save(session);
        return session;
    }

    /**
     * 把 Redis 会话转换成 Spring Security 当前用户主体。
     */
    public AuthPrincipal toPrincipal(AuthSession session) {
        return AuthPrincipal.builder()
                .tokenHash(session.getTokenHash())
                .userId(session.getUserId())
                .username(session.getUsername())
                .displayName(session.getDisplayName())
                .email(session.getEmail())
                .roles(session.getRoles())
                .permissions(session.getPermissions())
                .build();
    }

    /**
     * 从 Authorization 请求头提取 Bearer token。
     * 非 Bearer 格式直接视为未登录，交给 Spring Security 返回 401。
     */
    public Optional<String> extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return Optional.empty();
        }
        if (!authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return Optional.empty();
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? Optional.of(token) : Optional.empty();
    }

    /**
     * 保存会话到 Redis，并设置统一过期时间。
     */
    private void save(AuthSession session) {
        try {
            // 序列化会话
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + session.getTokenHash(), json, authProperties.getTokenTtl());
        } catch (JsonProcessingException e) {
            throw new AuthException("登录会话创建失败");
        }
    }

    /**
     * 生成 256-bit 随机 token，并用 URL 安全 Base64 输出。
     */
    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 计算 token 摘要。
     * Redis key 使用摘要，降低 Redis 泄露时 token 被直接复用的风险。
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new AuthException("token 加密算法不可用");
        }
    }

    /**
     * 规范化 PM token。
     * 前端可以传裸 token，后端统一补成 Bearer token 再转发给 PM。
     */
    private String normalizeBearer(String token) {
        String value = token == null ? "" : token.trim();
        if (!StringUtils.hasText(value)) {
            throw new AuthException("PM token 不能为空");
        }
        if (value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return value;
        }
        return BEARER_PREFIX + value;
    }

    /**
     * 把数据库权限实体压缩成会话中的权限快照。
     */
    private List<PermissionGrant> toGrants(List<AuthPermission> permissions) {
        return permissions.stream()
                .map(permission -> PermissionGrant.builder()
                        .code(permission.getCode())
                        .method(permission.getMethod())
                        .pathPattern(permission.getPathPattern())
                        .build())
                .toList();
    }
}
