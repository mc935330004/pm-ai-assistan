package com.pm.ai.assistan.auth.controller;

import com.pm.ai.assistan.auth.dto.AuthDtos;
import com.pm.ai.assistan.auth.dto.AuthResult;
import com.pm.ai.assistan.auth.model.AuthPermission;
import com.pm.ai.assistan.auth.model.AuthRole;
import com.pm.ai.assistan.auth.service.AuthAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员控制器。
 * 这些接口都需要 AUTH_ADMIN 权限，主要用于用户、角色、权限维护。
 */
@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class AuthAdminController {

    private final AuthAdminService authAdminService;

    /**
     * 查询用户列表。
     * status 可传 PENDING/ACTIVE/DISABLED，keyword 可模糊搜索账号、展示名、邮箱。
     */
    @GetMapping("/users")
    public AuthResult<List<AuthDtos.UserView>> listUsers(@RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String keyword) {
        return AuthResult.success(authAdminService.listUsers(status, keyword));
    }

    /**
     * 查询单个成员详情。
     * 成员管理抽屉打开时会重新加载，避免页面拿到旧角色或旧状态。
     */
    @GetMapping("/users/{userId}")
    public AuthResult<AuthDtos.UserView> getUser(@PathVariable Long userId) {
        return AuthResult.success(authAdminService.getUser(userId));
    }

    /**
     * 管理员创建用户。
     * 适合内部系统直接开通账号。
     */
    @PostMapping("/users")
    public AuthResult<AuthDtos.UserView> createUser(@Valid @RequestBody AuthDtos.AdminCreateUserRequest request) {
        return AuthResult.success("用户已创建", authAdminService.createUser(request));
    }

    /**
     * 启用用户。
     */
    @PostMapping("/users/{userId}/enable")
    public AuthResult<AuthDtos.UserView> enableUser(@PathVariable Long userId) {
        return AuthResult.success("用户已启用", authAdminService.enableUser(userId));
    }

    /**
     * 禁用用户。
     */
    @PostMapping("/users/{userId}/disable")
    public AuthResult<AuthDtos.UserView> disableUser(@PathVariable Long userId) {
        return AuthResult.success("用户已禁用", authAdminService.disableUser(userId));
    }

    /**
     * 分配用户角色。
     */
    @PutMapping("/users/{userId}/roles")
    public AuthResult<AuthDtos.UserView> assignRoles(@PathVariable Long userId,
                                                     @RequestBody AuthDtos.RoleAssignRequest request) {
        return AuthResult.success("角色已更新", authAdminService.assignRoles(userId, request.getRoleCodes()));
    }

    /**
     * 保存成员管理页的用户配置。
     * 状态和角色放在一个事务里提交，避免前端分两次请求造成半成功。
     */
    @PutMapping("/users/{userId}/config")
    public AuthResult<AuthDtos.UserView> updateUserConfig(@PathVariable Long userId,
                                                          @RequestBody AuthDtos.UserConfigRequest request) {
        return AuthResult.success("成员配置已更新", authAdminService.updateUserConfig(userId, request));
    }

    /**
     * 查询全部角色。
     */
    @GetMapping("/roles")
    public AuthResult<List<AuthRole>> listRoles() {
        return AuthResult.success(authAdminService.listRoles());
    }

    /**
     * 查询全部接口权限。
     */
    @GetMapping("/permissions")
    public AuthResult<List<AuthPermission>> listPermissions() {
        return AuthResult.success(authAdminService.listPermissions());
    }

    /**
     * 查询指定角色已经拥有的权限。
     */
    @GetMapping("/roles/{roleId}/permissions")
    public AuthResult<List<AuthPermission>> listRolePermissions(@PathVariable Long roleId) {
        return AuthResult.success(authAdminService.listRolePermissions(roleId));
    }

    /**
     * 分配角色权限。
     */
    @PutMapping("/roles/{roleId}/permissions")
    public AuthResult<List<AuthPermission>> assignRolePermissions(@PathVariable Long roleId,
                                                                  @RequestBody AuthDtos.PermissionAssignRequest request) {
        return AuthResult.success("角色权限已更新", authAdminService.assignRolePermissions(roleId, request.getPermissionIds()));
    }
}
