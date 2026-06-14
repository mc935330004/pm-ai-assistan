package com.pm.ai.assistan.auth.controller;

import com.pm.ai.assistan.auth.dto.AuthDtos;
import com.pm.ai.assistan.auth.dto.AuthResult;
import com.pm.ai.assistan.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
/**
 * 登录认证控制器。
 * 提供前端登录、账号申请、当前用户、退出登录、绑定 PM token 等接口。
 */
public class AuthController {

    private final AuthService authService;

    /**
     * 本地账号密码登录。
     * 返回本系统 token，不是 PM 系统 token。
     */
    @PostMapping("/login")
    public AuthResult<AuthDtos.LoginResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return AuthResult.success(authService.login(request));
    }

    /**
     * 账号申请。
     * 创建 PENDING 用户，管理员启用前不能登录。
     */
    @PostMapping("/register")
    public AuthResult<AuthDtos.UserView> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return AuthResult.success("账号申请已提交，请等待管理员启用", authService.register(request));
    }

    /**
     * 查询当前登录用户。
     * 前端刷新页面后会用它恢复用户、角色、权限信息。
     */
    @GetMapping("/me")
    public AuthResult<AuthDtos.UserView> me() {
        return AuthResult.success(authService.currentUser());
    }

    /**
     * 退出登录。
     * 删除 Redis 会话，让当前 token 立即失效。
     */
    @PostMapping("/logout")
    public AuthResult<Void> logout() {
        authService.logout();
        return AuthResult.success("退出成功", null);
    }

    /**
     * 绑定 PM 系统授权。
     * 本地登录 token 和 PM token 分离保存，AI 查询 PM 数据时会读取这里绑定的 PM token。
     */
    @PostMapping("/pm-token")
    public AuthResult<AuthDtos.UserView> bindPmToken(@Valid @RequestBody AuthDtos.BindPmTokenRequest request) {
        return AuthResult.success("PM 授权绑定成功", authService.bindPmToken(request));
    }
}
