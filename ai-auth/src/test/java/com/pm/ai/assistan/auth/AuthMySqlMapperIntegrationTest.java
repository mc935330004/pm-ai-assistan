package com.pm.ai.assistan.auth;

import com.pm.ai.assistan.auth.mapper.AuthPermissionMapper;
import com.pm.ai.assistan.auth.mapper.AuthRoleMapper;
import com.pm.ai.assistan.auth.mapper.AuthUserMapper;
import com.pm.ai.assistan.auth.model.AuthPermission;
import com.pm.ai.assistan.auth.model.AuthUser;
import com.pm.ai.assistan.auth.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MySQL + MyBatis 集成测试。
 * 这个测试直接连接 application.yml 中的 mc-agent 库，验证 MyBatis 对 MySQL 的读写。
 */
@SpringBootTest(classes = AiAuthTestApplication.class)
@Transactional
class AuthMySqlMapperIntegrationTest {

    @Autowired
    private AuthUserMapper userMapper;

    @Autowired
    private AuthRoleMapper roleMapper;

    @Autowired
    private AuthPermissionMapper permissionMapper;

    /**
     * 验证用户创建、用户查询、用户角色绑定都通过 MyBatis 写入/读取 MySQL。
     * 测试方法有事务，结束后临时用户会自动回滚。
     */
    @Test
    void userRoleAndPermissionMappersReadAndWriteMysql() {
        String username = "mapper_test_user_" + System.nanoTime();
        String email = username + "@example.com";

        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setDisplayName("Mapper 测试用户");
        user.setEmail(email);
        user.setPasswordHash("$2a$10$testPasswordHashForMapperOnly");
        user.setStatus(UserStatus.PENDING);

        userMapper.insert(user);

        AuthUser created = userMapper.findByUsername(username);
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(userMapper.findByAccount(username).getId()).isEqualTo(created.getId());
        assertThat(userMapper.findByAccount(email).getId()).isEqualTo(created.getId());
        assertThat(userMapper.findByEmail(email).getUsername()).isEqualTo(username);
        assertThat(userMapper.listUsers(UserStatus.PENDING, "Mapper 测试").stream()
                .map(AuthUser::getUsername))
                .contains(username);

        List<Long> roleIds = roleMapper.findIdsByCodes(List.of("USER"));
        assertThat(roleIds).hasSize(1);
        roleMapper.insertUserRoles(created.getId(), roleIds);
        assertThat(roleMapper.findRoleCodesByUserId(created.getId())).containsExactly("USER");

        List<AuthPermission> userPermissions = permissionMapper.findPermissionsByUserId(created.getId());
        assertThat(userPermissions.stream().map(AuthPermission::getCode))
                .contains("AI_CHAT", "AUTH_ME", "AUTH_LOGOUT", "AUTH_PM_TOKEN");
    }

    /**
     * 验证角色权限可以通过 MyBatis 全量覆盖写入 MySQL。
     * 对 USER 角色的临时改动会在测试结束时回滚。
     */
    @Test
    void rolePermissionMapperCanReplacePermissionsInMysql() {
        Long userRoleId = roleMapper.findIdsByCodes(List.of("USER")).get(0);
        List<Long> permissionIds = permissionMapper.listPermissions().stream()
                .filter(permission -> List.of("AI_CHAT", "AUTH_ME").contains(permission.getCode()))
                .map(AuthPermission::getId)
                .toList();

        permissionMapper.deleteRolePermissions(userRoleId);
        permissionMapper.insertRolePermissions(userRoleId, permissionIds);

        assertThat(permissionMapper.findPermissionsByRoleId(userRoleId).stream().map(AuthPermission::getCode))
                .containsExactlyInAnyOrder("AI_CHAT", "AUTH_ME");
    }
}
