-- =============================================================================
-- 迁移 03：账号密码登录改造（对应提交 6d828df「删除企微 SSO」）
--
-- 做两件事：
--   1) sys_user.wecom_userid 由 NOT NULL 放宽为可空——企微 SSO 已下线，
--      新建用户不再有企微 userId。列与 uk_wecom_userid 唯一索引都保留，
--      历史数据不动；MySQL 唯一索引允许多行 NULL，不影响新用户。
--   2) 新增 must_change_password，配合后端「首次登录强制改密」。
--
-- 可重复执行：MySQL 的 ALTER TABLE 不支持 ADD COLUMN IF NOT EXISTS，
-- 一律先查 information_schema 再决定是否执行（与 02-add-local-login.sql 同风格）。
--
-- 执行方式（数据层 131，本机 root 走 auth_socket）：
--   sudo mysql ai_marketplace < deploy/sql/03-account-login.sql
-- =============================================================================

USE ai_marketplace;

-- ---- 1. wecom_userid 放宽为可空 ------------------------------------------------
-- 只在仍是 NOT NULL 时才改，避免重复执行时无意义的表重建
SET @ddl := IF(
  (SELECT is_nullable FROM information_schema.columns
    WHERE table_schema = 'ai_marketplace'
      AND table_name   = 'sys_user'
      AND column_name  = 'wecom_userid') = 'NO',
  'ALTER TABLE sys_user MODIFY COLUMN wecom_userid VARCHAR(64) NULL COMMENT ''企微 userId（已弃用，账号密码体系下留 NULL）''',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ---- 2. 新增首次改密标志 ------------------------------------------------------
SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'ai_marketplace'
      AND table_name   = 'sys_user'
      AND column_name  = 'must_change_password') = 0,
  'ALTER TABLE sys_user ADD COLUMN must_change_password TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''首次登录是否需改密 1=需要 0=已改'' AFTER enabled',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ---- 3. 校验结果 --------------------------------------------------------------
-- 期望：wecom_userid 的 Null=YES；must_change_password 存在且 Default=1
SHOW COLUMNS FROM sys_user LIKE 'wecom_userid';
SHOW COLUMNS FROM sys_user LIKE 'must_change_password';

-- 现存账号的口令哈希不受本迁移影响：schema.sql 里那条 admin 是 INSERT IGNORE，
-- 与 uk_account 冲突会被跳过，因此现网 admin 保留部署时生成的强随机口令，
-- 不会退化成仓库中公开的默认口令 Welcome@2026。
-- 新列 DEFAULT 1 会让现存账号下次登录被要求改密，这是本次改造的预期行为。
SELECT id, wecom_userid, account, username, roles, enabled, must_change_password,
       IF(password_hash IS NULL OR password_hash = '', '未设置', '已设置') AS 口令状态
  FROM sys_user ORDER BY id;
