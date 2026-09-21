-- 企业 AI 应用市场 — 初始管理员账号
-- schema.sql 只带了 mp_category 的分类种子数据，sys_user 是空表，
-- 不插入这条记录的话系统里没有任何人拥有 ADMIN / OPERATOR 角色，后台无法运营。
--
-- 代码中实际使用的角色字符串（见 AuthInterceptor 与各 Service）：
--   USER 普通员工 / OPERATOR 运营 / ADMIN 超管 / JUDGE 评委
-- roles 字段为逗号分隔的多角色集合。
--
-- wecom_userid 是 SSO 主键且有唯一约束。这里先占位为 admin，
-- 等确认了真实超管的企微 userId 后，用下面注释里的 UPDATE 替换，
-- 该用户下次走企微 SSO 登录就会直接命中这条记录并继承 ADMIN 角色。

USE ai_marketplace;

INSERT IGNORE INTO sys_user
    (wecom_userid, username, email, mobile, department, avatar_url, roles, points, enabled, deleted)
VALUES
    ('admin', '系统管理员', NULL, NULL, '信息技术部', NULL, 'ADMIN,OPERATOR,USER', 0, 1, 0);

-- 绑定真实企微 userId（把 <真实企微userId> 换掉后执行）：
-- UPDATE sys_user SET wecom_userid = '<真实企微userId>' WHERE wecom_userid = 'admin';

SELECT id, wecom_userid, username, department, roles, enabled FROM sys_user;
