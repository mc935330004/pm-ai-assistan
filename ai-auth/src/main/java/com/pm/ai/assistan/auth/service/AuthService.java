package com.pm.ai.assistan.auth.service;

import com.pm.ai.assistan.auth.config.AuthProperties;
import com.pm.ai.assistan.auth.dto.AuthDtos;
import com.pm.ai.assistan.auth.exception.AuthException;
import com.pm.ai.assistan.auth.exception.PmAuthorizationMissingException;
import com.pm.ai.assistan.auth.mapper.AuthPermissionMapper;
import com.pm.ai.assistan.auth.mapper.AuthRoleMapper;
import com.pm.ai.assistan.auth.mapper.AuthUserMapper;
import com.pm.ai.assistan.auth.model.AuthPermission;
import com.pm.ai.assistan.auth.model.AuthUser;
import com.pm.ai.assistan.auth.model.UserStatus;
import com.pm.ai.assistan.auth.security.AuthContext;
import com.pm.ai.assistan.auth.security.AuthPrincipal;
import com.pm.ai.assistan.auth.security.AuthSession;
import com.pm.ai.assistan.auth.security.AuthSessionService;
import com.pm.ai.assistan.auth.security.CreatedSession;
import com.pm.ai.assistan.auth.security.PermissionGrant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 认证业务服务。
 * 负责登录、账号申请、当前用户、退出登录和 PM 授权绑定。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * 账号申请默认绑定普通用户角色。
     */
    private static final String USER_ROLE = "USER";

    private final AuthUserMapper userMapper;
    private final AuthRoleMapper roleMapper;
    private final AuthPermissionMapper permissionMapper;
    private final AuthSessionService sessionService;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    /**
     * 本地账号密码登录。
     * 登录成功后把用户角色和权限写入 Redis 会话，后续请求不用每次查库。
     */
    @Transactional
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        AuthUser user = userMapper.findByAccount(request.getAccount());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("账号或密码错误");
        }
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new AuthException("账号未启用，请联系管理员");
        }

        List<String> roles = roleMapper.findRoleCodesByUserId(user.getId());
        List<AuthPermission> permissions = permissionMapper.findPermissionsByUserId(user.getId());
        CreatedSession createdSession = sessionService.createSession(user, roles, permissions);

        return AuthDtos.LoginResponse.builder()
                .token(createdSession.token())
                .tokenType("Bearer")
                .expiresIn(createdSession.expiresIn())
                .user(toUserView(user, roles, permissions, false))
                .build();
    }

    /**
     * 账号申请。
     * 默认创建 PENDING 用户并绑定 USER 角色，管理员启用后才能登录。
     */
    @Transactional
    public AuthDtos.UserView register(AuthDtos.RegisterRequest request) {
        if (!authProperties.getRegistration().isEnabled()) {
            throw new AuthException("账号申请暂未开放");
        }
        String username = request.getUsername().trim();
        if (userMapper.findByUsername(username) != null) {
            throw new AuthException("账号已存在");
        }
        String email = normalizeBlank(request.getEmail());
        if (email != null && userMapper.findByEmail(email) != null) {
            throw new AuthException("邮箱已存在");
        }

        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setDisplayName(StringUtils.hasText(request.getDisplayName()) ? request.getDisplayName().trim() : username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.PENDING);
        userMapper.insert(user);
        assignSingleRole(user.getId(), USER_ROLE);

        return AuthDtos.UserView.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(List.of(USER_ROLE))
                .permissions(List.of())
                .pmTokenBound(false)
                .build();
    }

    /**
     * 当前登录用户信息。
     * 从 Redis 会话读取，确保前端刷新后能恢复角色和权限。
     */
    public AuthDtos.UserView currentUser() {
        AuthPrincipal principal = AuthContext.currentPrincipal();
        AuthSession session = sessionService.findByHash(principal.getTokenHash())
                .orElseThrow(() -> new AuthException("登录已过期，请重新登录"));
        return toUserView(session);
    }

    /**
     * 退出登录。
     * 删除 Redis 会话后，当前 token 后续请求会被视为未登录。
     */
    public void logout() {
        AuthPrincipal principal = AuthContext.currentPrincipal();
        sessionService.deleteByHash(principal.getTokenHash());
    }

    /**
     * 为当前会话绑定 PM token。
     * 绑定后 AI 工具调用 PM 系统时会使用这个 PM token。
     */
    public AuthDtos.UserView bindPmToken(AuthDtos.BindPmTokenRequest request) {
        AuthPrincipal principal = AuthContext.currentPrincipal();
        AuthSession session = sessionService.bindPmToken(principal.getTokenHash(), request.getPmToken());
        return toUserView(session);
    }

    /**
     * 获取当前会话绑定的 PM Authorization。
     * ChatService 调用 PM API 前会走这里，避免把本地登录 token 误传给 PM。
     */
    public String currentPmAuthorization() {
        AuthPrincipal principal = AuthContext.currentPrincipal();
        return sessionService.findByHash(principal.getTokenHash())
                .map(AuthSession::getPmToken)
                .filter(StringUtils::hasText)
                .orElseThrow(PmAuthorizationMissingException::new);
    }

    /**
     * 给用户分配一个角色。
     * 账号申请场景只需要绑定 USER。
     */
    private void assignSingleRole(Long userId, String roleCode) {
        List<Long> roleIds = roleMapper.findIdsByCodes(List.of(roleCode));
        if (roleIds.isEmpty()) {
            throw new AuthException("角色不存在: " + roleCode);
        }
        roleMapper.insertUserRole(userId, roleIds.get(0));
    }

    /**
     * 把数据库用户和权限集合转换成前端用户视图。
     */
    private AuthDtos.UserView toUserView(AuthUser user,
                                         List<String> roles,
                                         List<AuthPermission> permissions,
                                         boolean pmTokenBound) {
        return AuthDtos.UserView.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(roles)
                .permissions(permissions.stream().map(AuthPermission::getCode).toList())
                .pmTokenBound(pmTokenBound)
                .build();
    }

    /**
     * 把 Redis 会话转换成前端用户视图。
     */
    private AuthDtos.UserView toUserView(AuthSession session) {
        return AuthDtos.UserView.builder()
                .userId(session.getUserId())
                .username(session.getUsername())
                .displayName(session.getDisplayName())
                .email(session.getEmail())
                .status(UserStatus.ACTIVE)
                .roles(session.getRoles())
                .permissions(session.getPermissions().stream().map(PermissionGrant::getCode).toList())
                .pmTokenBound(StringUtils.hasText(session.getPmToken()))
                .build();
    }

    /**
     * 把空字符串统一转为 null，避免唯一索引被空字符串占住。
     */
    private String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
