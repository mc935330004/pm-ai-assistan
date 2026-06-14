package com.pm.ai.assistan.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证模块 DTO 集合。
 * 放在一个文件里是为了让登录模块接口入参、出参集中可见，后续拆分也方便。
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    /**
     * 登录接口入参。
     */
    @Data
    public static class LoginRequest {

        /**
         * 登录账号，可以是用户名或邮箱。
         */
        @NotBlank(message = "账号不能为空")
        private String account;

        /**
         * 明文密码只在登录请求中短暂出现，服务端用 BCrypt 校验。
         */
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    /**
     * 账号申请接口入参。
     * 申请成功后的用户默认 PENDING，需要管理员启用。
     */
    @Data
    public static class RegisterRequest {

        /**
         * 用户名，必须唯一。
         */
        @NotBlank(message = "账号不能为空")
        @Size(min = 3, max = 64, message = "账号长度应为 3-64 位")
        private String username;

        /**
         * 用户设置的初始密码，写库前会加密。
         */
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 72, message = "密码长度应为 6-72 位")
        private String password;

        /**
         * 展示名称，可为空，空时使用 username。
         */
        private String displayName;

        /**
         * 邮箱，可用于后续通知审核结果。
         */
        private String email;
    }

    /**
     * 管理员创建用户入参。
     * 和账号申请不同，管理员创建的用户默认可直接启用。
     */
    @Data
    public static class AdminCreateUserRequest {

        @NotBlank(message = "账号不能为空")
        @Size(min = 3, max = 64, message = "账号长度应为 3-64 位")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 72, message = "密码长度应为 6-72 位")
        private String password;

        private String displayName;

        private String email;

        /**
         * ACTIVE/PENDING/DISABLED；为空时默认 ACTIVE。
         */
        private String status;

        /**
         * 初始角色；为空时默认 USER。
         */
        private List<String> roleCodes = new ArrayList<>();
    }

    /**
     * 绑定 PM 系统授权的入参。
     */
    @Data
    public static class BindPmTokenRequest {

        /**
         * PM 系统 Bearer token，允许前端传裸 token 或 Bearer token。
         */
        @NotBlank(message = "PM token 不能为空")
        private String pmToken;
    }

    /**
     * 管理员分配用户角色的入参。
     */
    @Data
    public static class RoleAssignRequest {

        /**
         * 要分配给用户的角色编码集合，例如 ADMIN、USER。
         */
        private List<String> roleCodes = new ArrayList<>();
    }

    /**
     * 成员管理页保存用户配置的入参。
     * 前端一次提交状态和角色，后端在一个事务内统一落库。
     */
    @Data
    public static class UserConfigRequest {

        /**
         * 用户状态：ACTIVE/PENDING/DISABLED；为空时不修改状态。
         */
        private String status;

        /**
         * 用户最终拥有的角色编码集合，采用全量覆盖方式保存。
         */
        private List<String> roleCodes = new ArrayList<>();
    }

    /**
     * 管理员分配角色权限的入参。
     */
    @Data
    public static class PermissionAssignRequest {

        /**
         * 要绑定给角色的权限 id 集合。
         */
        private List<Long> permissionIds = new ArrayList<>();
    }

    /**
     * 登录接口出参。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {

        /**
         * 本地系统登录 token，不是 PM token。
         */
        private String token;

        /**
         * token 类型，固定为 Bearer。
         */
        private String tokenType;

        /**
         * token 剩余有效期，单位秒。
         */
        private long expiresIn;

        /**
         * 当前登录用户信息和权限快照。
         */
        private UserView user;
    }

    /**
     * 前端展示和路由权限判断使用的用户视图。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserView {

        private Long userId;

        private String username;

        private String displayName;

        private String email;

        private String status;

        /**
         * 用户创建时间，成员列表用它辅助展示最近记录时间。
         */
        private LocalDateTime createdAt;

        /**
         * 用户最后更新时间，状态变更后会刷新。
         */
        private LocalDateTime updatedAt;

        @Builder.Default
        private List<String> roles = new ArrayList<>();

        @Builder.Default
        private List<String> permissions = new ArrayList<>();

        /**
         * 当前会话是否已经绑定 PM 系统 token。
         */
        private boolean pmTokenBound;
    }
}
