package com.pm.ai.assistan.auth.mapper;

import com.pm.ai.assistan.auth.model.AuthRole;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 角色表 MyBatis Mapper。
 * SQL 统一维护在 mapper/auth/AuthRoleMapper.xml 中，接口只描述角色和用户角色关系的操作能力。
 */
public interface AuthRoleMapper {

    /**
     * 查询全部角色，管理端分配角色时使用。
     */
    List<AuthRole> listRoles();

    /**
     * 按角色编码查询角色，用于启动时判断种子角色是否已存在。
     */
    AuthRole findByCode(@Param("code") String code);

    /**
     * 按主键查询角色，角色详情和授权校验可复用。
     */
    AuthRole findById(@Param("id") Long id);

    /**
     * 把角色编码批量转换为角色 id。
     */
    List<Long> findIdsByCodes(@Param("codes") Collection<String> codes);

    /**
     * 查询用户拥有的角色编码，登录成功后会写入 Redis 会话。
     */
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 新增角色种子数据。
     */
    void insert(AuthRole role);

    /**
     * 给用户增加一个角色。
     */
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 批量给用户增加角色。
     */
    void insertUserRoles(@Param("userId") Long userId, @Param("roleIds") Collection<Long> roleIds);

    /**
     * 删除用户现有角色，重新分配角色前先清空。
     */
    void deleteUserRoles(@Param("userId") Long userId);
}
