package com.pm.ai.assistan.auth.mapper;

import com.pm.ai.assistan.auth.model.AuthUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户表 MyBatis Mapper。
 * 这里只保留 Java 方法定义，具体 SQL 统一写在 mapper/auth/AuthUserMapper.xml 中，方便后续维护复杂查询。
 */
public interface AuthUserMapper {

    /**
     * 登录时按账号查询用户，同时支持 username 和 email。
     */
    AuthUser findByAccount(@Param("account") String account);

    /**
     * 按用户名查询用户，注册去重和默认管理员初始化都会使用。
     */
    AuthUser findByUsername(@Param("username") String username);

    /**
     * 按邮箱查询用户，用于账号申请时做邮箱唯一性校验。
     */
    AuthUser findByEmail(@Param("email") String email);

    /**
     * 管理员操作用户时按主键重新加载最新用户信息。
     */
    AuthUser findById(@Param("id") Long id);

    /**
     * 管理端用户列表，支持按状态和关键字筛选。
     */
    List<AuthUser> listUsers(@Param("status") String status, @Param("keyword") String keyword);

    /**
     * 新增用户，数据库生成的 id 会回填到 user.id。
     */
    void insert(AuthUser user);

    /**
     * 更新用户基础信息，预留给个人资料维护或管理员编辑使用。
     */
    int updateProfile(AuthUser user);

    /**
     * 管理员启用或禁用用户，返回受影响行数。
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
