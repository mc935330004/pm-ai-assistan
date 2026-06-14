package com.pm.ai.assistan.auth.service;

import com.pm.ai.assistan.auth.dto.AuthDtos;
import com.pm.ai.assistan.auth.exception.AuthException;
import com.pm.ai.assistan.auth.mapper.AuthPermissionMapper;
import com.pm.ai.assistan.auth.mapper.AuthRoleMapper;
import com.pm.ai.assistan.auth.mapper.AuthUserMapper;
import com.pm.ai.assistan.auth.model.AuthPermission;
import com.pm.ai.assistan.auth.model.AuthRole;
import com.pm.ai.assistan.auth.model.AuthUser;
import com.pm.ai.assistan.auth.model.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户、角色、权限管理业务服务。
 * 供管理员审核账号申请、创建用户、禁用用户、调整角色和权限使用。
 */
@Service
@RequiredArgsConstructor
public class AuthAdminService {

    private static final String USER_ROLE = "USER";
    private static final List<String> VALID_USER_STATUSES = List.of(UserStatus.ACTIVE, UserStatus.PENDING, UserStatus.DISABLED);

    private final AuthUserMapper userMapper;
    private final AuthRoleMapper roleMapper;
    private final AuthPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 查询用户列表。
     * status 为空查全部，keyword 可按用户名、展示名、邮箱模糊查询。
     */
    public List<AuthDtos.UserView> listUsers(String status, String keyword) {
        return userMapper.listUsers(status, keyword).stream()
                .map(this::toUserView)
                .toList();
    }

    /**
     * 查询单个成员详情。
     * 打开成员详情弹窗时使用，确保前端看到的是数据库最新状态。
     */
    public AuthDtos.UserView getUser(Long userId) {
        return toUserView(loadUser(userId));
    }

    /**
     * 管理员直接创建用户。
     * 适合内部系统由管理员开通账号的模式。
     */
    @Transactional
    public AuthDtos.UserView createUser(AuthDtos.AdminCreateUserRequest request) {
        assertUsernameAvailable(request.getUsername());
        assertEmailAvailable(request.getEmail());

        AuthUser user = new AuthUser();
        user.setUsername(request.getUsername().trim());
        user.setDisplayName(StringUtils.hasText(request.getDisplayName()) ? request.getDisplayName().trim() : request.getUsername().trim());
        user.setEmail(normalizeBlank(request.getEmail()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(normalizeStatus(request.getStatus(), UserStatus.ACTIVE));
        userMapper.insert(user);

        List<String> roleCodes = CollectionUtils.isEmpty(request.getRoleCodes()) ? List.of(USER_ROLE) : request.getRoleCodes();
        assignRolesInternal(user.getId(), roleCodes);
        return toUserView(loadUser(user.getId()));
    }

    /**
     * 启用用户，让 PENDING/DISABLED 用户恢复登录资格。
     */
    @Transactional
    public AuthDtos.UserView enableUser(Long userId) {
        updateStatus(userId, UserStatus.ACTIVE);
        return toUserView(loadUser(userId));
    }

    /**
     * 禁用用户，阻止用户再次登录。
     */
    @Transactional
    public AuthDtos.UserView disableUser(Long userId) {
        updateStatus(userId, UserStatus.DISABLED);
        return toUserView(loadUser(userId));
    }

    /**
     * 分配用户角色。
     * 这里采用全量覆盖，方便前端直接提交最终选中的角色集合。
     */
    @Transactional
    public AuthDtos.UserView assignRoles(Long userId, List<String> roleCodes) {
        loadUser(userId);
        assignRolesInternal(userId, roleCodes);
        return toUserView(loadUser(userId));
    }

    /**
     * 保存成员管理页提交的用户配置。
     * 状态和角色在一个事务里完成，避免页面保存后出现角色成功但状态失败的中间态。
     */
    @Transactional
    public AuthDtos.UserView updateUserConfig(Long userId, AuthDtos.UserConfigRequest request) {
        if (request == null) {
            throw new AuthException("用户配置不能为空");
        }

        AuthUser user = loadUser(userId);
        String normalizedStatus = normalizeStatus(request.getStatus(), user.getStatus());
        if (!normalizedStatus.equals(user.getStatus())) {
            updateStatus(userId, normalizedStatus);
        }

        assignRolesInternal(userId, request.getRoleCodes());
        return toUserView(loadUser(userId));
    }

    /**
     * 查询全部角色。
     */
    public List<AuthRole> listRoles() {
        return roleMapper.listRoles();
    }

    /**
     * 查询全部权限。
     */
    public List<AuthPermission> listPermissions() {
        return permissionMapper.listPermissions();
    }

    /**
     * 查询某个角色当前拥有的权限。
     */
    public List<AuthPermission> listRolePermissions(Long roleId) {
        assertRoleExists(roleId);
        return permissionMapper.findPermissionsByRoleId(roleId);
    }

    /**
     * 分配角色权限。
     * 采用全量覆盖：先清空角色旧权限，再批量写入新权限。
     */
    @Transactional
    public List<AuthPermission> assignRolePermissions(Long roleId, List<Long> permissionIds) {
        assertRoleExists(roleId);
        if (CollectionUtils.isEmpty(permissionIds)) {
            throw new AuthException("至少选择一个权限");
        }
        List<Long> normalizedPermissionIds = permissionIds.stream()
                .distinct()
                .toList();
        List<AuthPermission> permissions = permissionMapper.findByIds(normalizedPermissionIds);
        if (permissions.size() != normalizedPermissionIds.size()) {
            throw new AuthException("包含不存在的权限");
        }

        permissionMapper.deleteRolePermissions(roleId);
        permissionMapper.insertRolePermissions(roleId, normalizedPermissionIds);
        return permissionMapper.findPermissionsByRoleId(roleId);
    }

    /**
     * 内部角色分配逻辑。
     * 单独抽出来让“创建用户”和“修改角色”共用同一套校验。
     */
    private void assignRolesInternal(Long userId, List<String> roleCodes) {
        if (CollectionUtils.isEmpty(roleCodes)) {
            throw new AuthException("至少选择一个角色");
        }
        List<String> normalizedRoleCodes = roleCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (normalizedRoleCodes.isEmpty()) {
            throw new AuthException("至少选择一个角色");
        }
        List<Long> roleIds = roleMapper.findIdsByCodes(normalizedRoleCodes);
        if (roleIds.size() != normalizedRoleCodes.size()) {
            throw new AuthException("包含不存在的角色");
        }
        roleMapper.deleteUserRoles(userId);
        roleMapper.insertUserRoles(userId, roleIds);
    }

    /**
     * 更新用户状态，并检查用户是否存在。
     */
    private void updateStatus(Long userId, String status) {
        if (userMapper.updateStatus(userId, status) == 0) {
            throw new AuthException("用户不存在");
        }
    }

    /**
     * 规范化用户状态。
     * 前端可以不传状态；传了就必须是系统支持的枚举值，避免脏状态写入 MySQL。
     */
    private String normalizeStatus(String status, String defaultStatus) {
        String normalizedStatus = StringUtils.hasText(status) ? status.trim() : defaultStatus;
        if (!VALID_USER_STATUSES.contains(normalizedStatus)) {
            throw new AuthException("用户状态不合法");
        }
        return normalizedStatus;
    }

    /**
     * 按 id 加载用户，不存在时抛业务异常。
     */
    private AuthUser loadUser(Long userId) {
        AuthUser user = userMapper.findById(userId);
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        return user;
    }

    /**
     * 检查用户名是否可用。
     */
    private void assertUsernameAvailable(String username) {
        if (userMapper.findByUsername(username.trim()) != null) {
            throw new AuthException("账号已存在");
        }
    }

    /**
     * 检查邮箱是否可用；邮箱为空时跳过。
     */
    private void assertEmailAvailable(String email) {
        String normalizedEmail = normalizeBlank(email);
        if (normalizedEmail != null && userMapper.findByEmail(normalizedEmail) != null) {
            throw new AuthException("邮箱已存在");
        }
    }

    /**
     * 检查角色是否存在。
     */
    private void assertRoleExists(Long roleId) {
        if (roleMapper.findById(roleId) == null) {
            throw new AuthException("角色不存在");
        }
    }

    /**
     * 管理端用户视图。
     * 管理列表里不展示 PM token 绑定状态，因为 PM token 是会话级数据。
     */
    private AuthDtos.UserView toUserView(AuthUser user) {
        List<String> roles = roleMapper.findRoleCodesByUserId(user.getId());
        List<AuthPermission> permissions = permissionMapper.findPermissionsByUserId(user.getId());
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
                .pmTokenBound(false)
                .build();
    }

    /**
     * 把空字符串统一转为 null，避免唯一索引被空字符串占住。
     */
    private String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
