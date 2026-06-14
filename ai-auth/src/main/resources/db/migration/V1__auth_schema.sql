-- 登录权限模块基础表结构。
-- 这些表由 ai-auth 模块维护，主应用只通过 AuthService 和安全过滤链使用认证能力。

create table if not exists auth_user (
    id bigint not null auto_increment comment '用户主键',
    username varchar(64) not null comment '登录账号，唯一',
    display_name varchar(100) null comment '展示名称',
    password_hash varchar(120) not null comment 'BCrypt 加密后的密码',
    email varchar(160) null comment '邮箱',
    status varchar(20) not null comment '状态：ACTIVE/PENDING/DISABLED',
    created_at timestamp not null default current_timestamp comment '创建时间',
    updated_at timestamp not null default current_timestamp on update current_timestamp comment '更新时间',
    primary key (id),
    unique key uk_auth_user_username (username),
    unique key uk_auth_user_email (email),
    key idx_auth_user_status (status)
) engine = InnoDB default charset = utf8mb4 collate = utf8mb4_0900_ai_ci comment = '本地登录用户表';

create table if not exists auth_role (
    id bigint not null auto_increment comment '角色主键',
    code varchar(64) not null comment '角色编码，例如 ADMIN、USER',
    name varchar(100) not null comment '角色名称',
    description varchar(255) null comment '角色说明',
    created_at timestamp not null default current_timestamp comment '创建时间',
    primary key (id),
    unique key uk_auth_role_code (code)
) engine = InnoDB default charset = utf8mb4 collate = utf8mb4_0900_ai_ci comment = '角色表';

create table if not exists auth_permission (
    id bigint not null auto_increment comment '权限主键',
    code varchar(100) not null comment '权限编码，例如 AI_CHAT',
    name varchar(120) not null comment '权限名称',
    method varchar(16) not null comment 'HTTP 方法：GET/POST/*',
    path_pattern varchar(160) not null comment '接口路径模式，例如 /ai/**',
    description varchar(255) null comment '权限说明',
    created_at timestamp not null default current_timestamp comment '创建时间',
    primary key (id),
    unique key uk_auth_permission_code (code),
    key idx_auth_permission_route (method, path_pattern)
) engine = InnoDB default charset = utf8mb4 collate = utf8mb4_0900_ai_ci comment = '接口权限表';

create table if not exists auth_user_role (
    user_id bigint not null comment '用户 id',
    role_id bigint not null comment '角色 id',
    created_at timestamp not null default current_timestamp comment '创建时间',
    primary key (user_id, role_id),
    key idx_auth_user_role_role_id (role_id),
    constraint fk_auth_user_role_user foreign key (user_id) references auth_user (id),
    constraint fk_auth_user_role_role foreign key (role_id) references auth_role (id)
) engine = InnoDB default charset = utf8mb4 collate = utf8mb4_0900_ai_ci comment = '用户角色关系表';

create table if not exists auth_role_permission (
    role_id bigint not null comment '角色 id',
    permission_id bigint not null comment '权限 id',
    created_at timestamp not null default current_timestamp comment '创建时间',
    primary key (role_id, permission_id),
    key idx_auth_role_permission_permission_id (permission_id),
    constraint fk_auth_role_permission_role foreign key (role_id) references auth_role (id),
    constraint fk_auth_role_permission_permission foreign key (permission_id) references auth_permission (id)
) engine = InnoDB default charset = utf8mb4 collate = utf8mb4_0900_ai_ci comment = '角色权限关系表';
