package com.pm.ai.assistan.auth.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 角色实体。
 * 用户通过角色获得一组接口权限。
 */
public class AuthRole {

    /**
     * 角色主键，对应 auth_role.id。
     */
    private Long id;

    /**
     * 角色编码，例如 ADMIN、USER，代码中主要使用这个字段判断。
     */
    private String code;

    /**
     * 角色名称，用于后台展示。
     */
    private String name;

    /**
     * 角色说明，帮助管理员理解角色用途。
     */
    private String description;

    /**
     * 创建时间，由数据库默认值写入。
     */
    private LocalDateTime createdAt;
}
