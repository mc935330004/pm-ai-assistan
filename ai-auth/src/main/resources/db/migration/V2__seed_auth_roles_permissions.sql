-- 登录权限模块基础角色和接口权限。
-- 脚本使用 insert ignore，重复执行或本地已有种子数据时不会造成重复写入。

insert ignore into auth_role (code, name, description)
values
    ('ADMIN', '管理员', '系统管理员，拥有全部接口权限'),
    ('USER', '普通用户', '普通业务用户'),
    ('PM', '项目经理', '管理项目计划、任务流和团队协作'),
    ('AGENT_OPERATOR', 'Agent 运营', '维护智能体任务、运行状态和知识库'),
    ('VIEWER', '观察者', '只读查看项目、任务和运行数据');

insert ignore into auth_permission (code, name, method, path_pattern, description)
values
    ('AI_CHAT', 'AI 聊天', 'POST', '/ai/**', '访问 AI 聊天接口'),
    ('AUTH_ME', '当前用户', 'GET', '/auth/me', '查看当前登录用户'),
    ('AUTH_LOGOUT', '退出登录', 'POST', '/auth/logout', '退出当前登录会话'),
    ('AUTH_PM_TOKEN', '绑定 PM 授权', 'POST', '/auth/pm-token', '绑定 PM 系统授权'),
    ('AUTH_ADMIN', '用户管理', '*', '/auth/admin/**', '管理用户和角色');

-- 普通用户具备登录后的基础能力和 PM 授权绑定能力。
insert ignore into auth_role_permission (role_id, permission_id)
select role.id, permission.id
from auth_role role
inner join auth_permission permission on permission.code in ('AI_CHAT', 'AUTH_ME', 'AUTH_LOGOUT', 'AUTH_PM_TOKEN')
where role.code = 'USER';

-- 项目经理和 Agent 运营角色同样具备基础 AI 查询和 PM 授权绑定能力。
insert ignore into auth_role_permission (role_id, permission_id)
select role.id, permission.id
from auth_role role
inner join auth_permission permission on permission.code in ('AI_CHAT', 'AUTH_ME', 'AUTH_LOGOUT', 'AUTH_PM_TOKEN')
where role.code in ('PM', 'AGENT_OPERATOR');

-- 观察者只给只读查看和 AI 查询入口，不给 PM token 绑定能力。
insert ignore into auth_role_permission (role_id, permission_id)
select role.id, permission.id
from auth_role role
inner join auth_permission permission on permission.code in ('AI_CHAT', 'AUTH_ME', 'AUTH_LOGOUT')
where role.code = 'VIEWER';

-- 管理员拥有全部接口权限。
insert ignore into auth_role_permission (role_id, permission_id)
select role.id, permission.id
from auth_role role
cross join auth_permission permission
where role.code = 'ADMIN';
