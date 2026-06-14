package com.pm.ai.assistan.auth.service;

import com.pm.ai.assistan.auth.config.AuthProperties;
import com.pm.ai.assistan.auth.mapper.AuthPermissionMapper;
import com.pm.ai.assistan.auth.mapper.AuthRoleMapper;
import com.pm.ai.assistan.auth.mapper.AuthUserMapper;
import com.pm.ai.assistan.auth.model.AuthPermission;
import com.pm.ai.assistan.auth.model.AuthRole;
import com.pm.ai.assistan.auth.model.AuthUser;
import com.pm.ai.assistan.auth.model.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
/**
 * 认证模块启动初始化器。
 * 应用启动时补齐默认角色、接口权限和管理员账号，方便本地直接使用。
 */
public class AuthBootstrap implements ApplicationRunner {

    private final AuthProperties authProperties;
    private final AuthUserMapper userMapper;
    private final AuthRoleMapper roleMapper;
    private final AuthPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Spring Boot 启动完成后执行。
     * 这里的 ensure* 方法都是幂等的，重复启动不会重复插入。
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AuthRole adminRole = ensureRole("ADMIN", "管理员", "系统管理员，拥有全部接口权限");
        AuthRole userRole = ensureRole("USER", "普通用户", "普通业务用户");
        AuthRole pmRole = ensureRole("PM", "项目经理", "管理项目计划、任务流和团队协作");
        AuthRole agentOperatorRole = ensureRole("AGENT_OPERATOR", "Agent 运营", "维护智能体任务、运行状态和知识库");
        AuthRole viewerRole = ensureRole("VIEWER", "观察者", "只读查看项目、任务和运行数据");

        AuthPermission aiChat = ensurePermission("AI_CHAT", "AI 聊天", "POST", "/ai/**", "访问 AI 聊天接口");
        AuthPermission authMe = ensurePermission("AUTH_ME", "当前用户", "GET", "/auth/me", "查看当前登录用户");
        AuthPermission authLogout = ensurePermission("AUTH_LOGOUT", "退出登录", "POST", "/auth/logout", "退出当前登录会话");
        AuthPermission authPmToken = ensurePermission("AUTH_PM_TOKEN", "绑定 PM 授权", "POST", "/auth/pm-token", "绑定 PM 系统授权");
        AuthPermission authAdmin = ensurePermission("AUTH_ADMIN", "用户管理", "*", "/auth/admin/**", "管理用户和角色");

        ensureRolePermission(userRole, aiChat);
        ensureRolePermission(userRole, authMe);
        ensureRolePermission(userRole, authLogout);
        ensureRolePermission(userRole, authPmToken);

        // 成员管理页展示的业务角色也在启动时补齐权限，避免新环境角色存在但无法访问基础功能。
        ensureRolePermission(pmRole, aiChat);
        ensureRolePermission(pmRole, authMe);
        ensureRolePermission(pmRole, authLogout);
        ensureRolePermission(pmRole, authPmToken);

        ensureRolePermission(agentOperatorRole, aiChat);
        ensureRolePermission(agentOperatorRole, authMe);
        ensureRolePermission(agentOperatorRole, authLogout);
        ensureRolePermission(agentOperatorRole, authPmToken);

        ensureRolePermission(viewerRole, aiChat);
        ensureRolePermission(viewerRole, authMe);
        ensureRolePermission(viewerRole, authLogout);

        List.of(aiChat, authMe, authLogout, authPmToken, authAdmin)
                .forEach(permission -> ensureRolePermission(adminRole, permission));

        if (authProperties.getDefaultAdmin().isEnabled()) {
            ensureDefaultAdmin(adminRole);
        }
    }

    /**
     * 确保角色存在，不存在则创建。
     */
    private AuthRole ensureRole(String code, String name, String description) {
        AuthRole role = roleMapper.findByCode(code);
        if (role != null) {
            return role;
        }
        role = new AuthRole();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        roleMapper.insert(role);
        return role;
    }

    /**
     * 确保接口权限存在，不存在则创建。
     */
    private AuthPermission ensurePermission(String code,
                                            String name,
                                            String method,
                                            String pathPattern,
                                            String description) {
        AuthPermission permission = permissionMapper.findByCode(code);
        if (permission != null) {
            return permission;
        }
        permission = new AuthPermission();
        permission.setCode(code);
        permission.setName(name);
        permission.setMethod(method);
        permission.setPathPattern(pathPattern);
        permission.setDescription(description);
        permissionMapper.insert(permission);
        return permission;
    }

    /**
     * 确保角色和权限已绑定。
     */
    private void ensureRolePermission(AuthRole role, AuthPermission permission) {
        if (permissionMapper.countRolePermission(role.getId(), permission.getId()) == 0) {
            permissionMapper.insertRolePermission(role.getId(), permission.getId());
        }
    }

    /**
     * 创建默认管理员。
     * 如果同名用户已存在，不会覆盖密码，避免重启导致管理员密码被重置。
     */
    private void ensureDefaultAdmin(AuthRole adminRole) {
        String username = authProperties.getDefaultAdmin().getUsername();
        AuthUser existing = userMapper.findByUsername(username);
        if (existing != null) {
            return;
        }
        if (!StringUtils.hasText(authProperties.getDefaultAdmin().getPassword())) {
            throw new IllegalStateException("默认管理员密码未配置，请设置 AUTH_DEFAULT_ADMIN_PASSWORD 或关闭默认管理员初始化");
        }

        AuthUser admin = new AuthUser();
        admin.setUsername(username);
        admin.setDisplayName(authProperties.getDefaultAdmin().getDisplayName());
        admin.setPasswordHash(passwordEncoder.encode(authProperties.getDefaultAdmin().getPassword()));
        admin.setStatus(UserStatus.ACTIVE);
        userMapper.insert(admin);
        roleMapper.insertUserRole(admin.getId(), adminRole.getId());
    }
}
