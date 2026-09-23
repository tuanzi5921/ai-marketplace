-- =============================================================================
-- 迁移 02：为账号密码兜底登录增加 sys_user.account / password_hash
--
-- 适用于「表已按旧版 schema.sql 建好」的现网库；对全新库也能安全执行
-- （列已存在时 ALTER 会被跳过）。整体幂等，可重复执行。
--
-- 两个占位符由部署流程用 sed 替换后再执行，不要把真实哈希提交进仓库：
--   __ADMIN_ACCOUNT__       管理员本地登录账号，默认 admin
--   __ADMIN_PASSWORD_HASH__ BCrypt 哈希，用后端同一套 Hutool BCrypt 生成
--
-- 生成哈希的方法（开发机需有 JDK17 与 hutool-all jar）：
--   javac -cp hutool-all-5.8.32.jar -d /tmp/h /tmp/Hash.java   # 见 deploy/README.md
--   java  -cp "/tmp/h;hutool-all-5.8.32.jar" Hash '明文口令'
-- =============================================================================

USE ai_marketplace;

-- MySQL 的 ALTER TABLE 不支持 ADD COLUMN IF NOT EXISTS，
-- 因此先查 information_schema 再决定是否执行，保证脚本可重复运行。

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'ai_marketplace'
      AND table_name   = 'sys_user'
      AND column_name  = 'account') = 0,
  'ALTER TABLE sys_user ADD COLUMN account VARCHAR(64) NULL COMMENT ''本地登录账号（账号密码兜底登录用，多数用户为 NULL）'' AFTER wecom_userid',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = 'ai_marketplace'
      AND table_name   = 'sys_user'
      AND column_name  = 'password_hash') = 0,
  'ALTER TABLE sys_user ADD COLUMN password_hash VARCHAR(100) NULL COMMENT ''BCrypt 口令哈希，明文永不入库'' AFTER account',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 唯一索引同样做存在性判断；MySQL 唯一索引允许多行 NULL，
-- 所以企微 SSO 自动创建、没有本地账号的用户不受影响
SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = 'ai_marketplace'
      AND table_name   = 'sys_user'
      AND index_name   = 'uk_account') = 0,
  'ALTER TABLE sys_user ADD UNIQUE KEY uk_account (account)',
  'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 全新库里 01-init-admin.sql 已经写好了 account；现网库里 admin 那一行是旧版脚本建的，
-- 刚加上的 account 列还是 NULL。因此这里同时按 account 与 wecom_userid 占位值定位，
-- 两种情况都能命中——否则会出现「脚本跑完没报错、但口令根本没写进去」。
-- password_hash 已有值时不覆盖，避免把管理员改过的口令重置回初始值。
UPDATE sys_user
   SET account       = '__ADMIN_ACCOUNT__',
       password_hash = IF(password_hash IS NULL OR password_hash = '',
                          '__ADMIN_PASSWORD_HASH__', password_hash)
 WHERE account = '__ADMIN_ACCOUNT__'
    OR (account IS NULL AND wecom_userid = '__ADMIN_ACCOUNT__');

SELECT id, wecom_userid, account, username, roles, enabled,
       IF(password_hash IS NULL OR password_hash = '', '未设置', '已设置') AS 口令状态
  FROM sys_user
 ORDER BY id;
