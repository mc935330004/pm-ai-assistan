package com.pm.ai.assistan.auth.mapper;

import com.pm.ai.assistan.auth.model.AuthPermission;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 接口权限 MyBatis Mapper。
 * SQL 统一维护在 mapper/auth/AuthPermissionMapper.xml 中，避免注解 SQL 分散在 Java 代码里。
 */
public interface AuthPermissionMapper {

    /**
     * 查询全部接口权限，角色授权页面可直接使用。
     */
    List<AuthPermission> listPermissions();

    /**
     * 按权限编码查询权限，用于启动时判断种子权限是否已存在。
     */
    AuthPermission findByCode(@Param("code") String code);

    /**
     * 查询某个角色拥有的权限。
     */
    List<AuthPermission> findPermissionsByRoleId(@Param("roleId") Long roleId);

    /**
     * 按权限 id 批量查询权限，用于校验前端提交的权限是否都存在。
     */
    List<AuthPermission> findByIds(@Param("ids") Collection<Long> ids);

    /**
     * 查询用户最终拥有的接口权限，登录时会写入会话。
     */
    List<AuthPermission> findPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 新增接口权限种子数据。
     */
    void insert(AuthPermission permission);

    /**
     * 判断角色和权限是否已经绑定，避免启动器重复插入。
     */
    int countRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /**
     * 给角色绑定接口权限。
     */
    void insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /**
     * 批量给角色绑定权限。
     */
    void insertRolePermissions(@Param("roleId") Long roleId, @Param("permissionIds") Collection<Long> permissionIds);

    /**
     * 删除角色现有权限，重新配置角色权限前先清空。
     */
    void deleteRolePermissions(@Param("roleId") Long roleId);
}
