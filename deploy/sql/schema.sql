# 企业 AI 应用市场 — 数据库初始化脚本
# MySQL 8.0+ / UTF8MB4

CREATE DATABASE IF NOT EXISTS ai_marketplace DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_marketplace;

-- ----------- 1. 用户与角色 -----------
CREATE TABLE IF NOT EXISTS sys_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  wecom_userid VARCHAR(64)  NOT NULL COMMENT '企微 userId（SSO 主键）',
  username    VARCHAR(64)  NOT NULL COMMENT '员工姓名',
  email       VARCHAR(128)          COMMENT '邮箱',
  mobile      VARCHAR(20)           COMMENT '手机号',
  department  VARCHAR(128)          COMMENT '所属部门（来自企微通讯录）',
  avatar_url  VARCHAR(512)          COMMENT '头像',
  roles       VARCHAR(128)          COMMENT '角色集合：以逗号分隔，如 USER,ADMIN,JUDGE',
  points      INT          NOT NULL DEFAULT 0 COMMENT '累计贡献积分',
  enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '启用状态',
  deleted     TINYINT(1)   NOT NULL DEFAULT 0,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wecom_userid (wecom_userid),
  KEY idx_dept (department)
) COMMENT='员工用户表';

CREATE TABLE IF NOT EXISTS sys_judge (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  user_id       BIGINT      NOT NULL COMMENT '关联用户 ID',
  department    VARCHAR(128)         COMMENT '负责领域/赛道',
  recommend_by  VARCHAR(64)          COMMENT '评委推荐来源（部门推荐 / 超管任命）',
  onboard_at    DATETIME             COMMENT '任期开始',
  offboard_at   DATETIME             COMMENT '任期结束',
  deleted       TINYINT(1)  NOT NULL DEFAULT 0,
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user (user_id)
) COMMENT='评委信息表';

-- ----------- 2. 作品与版本 -----------
CREATE TABLE IF NOT EXISTS mp_submission (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  title            VARCHAR(128) NOT NULL COMMENT '作品标题',
  author_id        BIGINT       NOT NULL COMMENT '提交者 ID',
  short_desc       VARCHAR(256)          COMMENT '一句话简介',
  detail_desc      TEXT                  COMMENT '详细说明 / 使用文档（Markdown）',
  type             VARCHAR(16)  NOT NULL COMMENT '作品类型：SOURCE / EXECUTABLE / SAAS / DOCUMENT',
  business_domain  VARCHAR(32)           COMMENT '业务领域标签',
  usage_scenario   VARCHAR(32)           COMMENT '使用场景标签',
  tech_stack       VARCHAR(128)          COMMENT '技术栈标签，逗号分隔',
  cover_url        VARCHAR(512)          COMMENT '封面图',
  tags             VARCHAR(256)          COMMENT '其它标签，逗号分隔',
  version          VARCHAR(32)  NOT NULL DEFAULT '1.0.0' COMMENT '当前最新版本号',
  latest_artifact_id BIGINT              COMMENT '最新作品附件 ID',
  download_count   INT          NOT NULL DEFAULT 0 COMMENT '累计下载量',
  rating_avg       DECIMAL(4,2)          DEFAULT 0.00 COMMENT '综合评分（评委3 + 员工7 加权）',
  rating_count     INT          NOT NULL DEFAULT 0 COMMENT '累计评分人次',
  status           VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：DRAFT/PENDING/APPROVED/REJECTED/PUBLISHED/UNLISTED/FROZEN',
  reject_reason    VARCHAR(512)          COMMENT '驳回原因',
  reviewed_by      BIGINT                COMMENT '审核人 ID',
  reviewed_at      DATETIME              COMMENT '审核时间',
  published_at     DATETIME              COMMENT '发布时间',
  deleted          TINYINT(1)   NOT NULL DEFAULT 0,
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_author (author_id),
  KEY idx_status (status),
  KEY idx_type (type),
  KEY idx_domain (business_domain),
  KEY idx_download (download_count)
) COMMENT='作品（Submission）主表';

CREATE TABLE IF NOT EXISTS mp_submission_artifact (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  submission_id   BIGINT       NOT NULL,
  version         VARCHAR(32)  NOT NULL COMMENT '该附件对应的版本号',
  file_name       VARCHAR(256) NOT NULL COMMENT '显示文件名',
  stored_path     VARCHAR(512) NOT NULL COMMENT '存储路径（企业存储中相对路径或对象 Key）',
  file_size_bytes BIGINT                COMMENT '文件大小（字节）',
  content_type    VARCHAR(128)          COMMENT 'MIME 类型',
  file_hash       VARCHAR(64)           COMMENT 'SHA-256 校验值',
  changelog       VARCHAR(512)          COMMENT '版本变更说明',
  is_latest       TINYINT(1)   NOT NULL DEFAULT 1,
  deleted         TINYINT(1)   NOT NULL DEFAULT 0,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sub (submission_id)
) COMMENT='作品附件 / 版本迭代表';

CREATE TABLE IF NOT EXISTS mp_saas_link (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  submission_id BIGINT       NOT NULL UNIQUE,
  access_url    VARCHAR(512) NOT NULL COMMENT '在线服务访问 URL',
  credentials   VARCHAR(512)          COMMENT '访问凭证说明（加密存）',
  availability  VARCHAR(16)           COMMENT '可用性：OK / UNSTABLE / DOWN',
  PRIMARY KEY (id)
) COMMENT='SAAS / 在线平台作品的访问信息';

-- ----------- 3. 评分与评论 -----------
CREATE TABLE IF NOT EXISTS mp_rating (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  submission_id BIGINT       NOT NULL,
  user_id       BIGINT       NOT NULL,
  rated_by_role VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT 'USER / JUDGE',
  ease_of_use   TINYINT(1)   NOT NULL COMMENT '易用性 1-5',
  business_value TINYINT(1)  NOT NULL COMMENT '业务价值 1-5',
  tech_quality  TINYINT(1)   NOT NULL COMMENT '技术质量 1-5',
  innovation    TINYINT(1)   NOT NULL COMMENT '创新性 1-5',
  comment_text  VARCHAR(512)          COMMENT '附评文字',
  rated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_sub (user_id, submission_id),
  KEY idx_sub (submission_id)
) COMMENT='多维评分记录表（每人每作品限一条）';

CREATE TABLE IF NOT EXISTS mp_comment (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  submission_id BIGINT       NOT NULL,
  user_id       BIGINT       NOT NULL,
  parent_id     BIGINT                COMMENT '回复父评论 ID',
  content       VARCHAR(1024) NOT NULL,
  report_count  INT          NOT NULL DEFAULT 0 COMMENT '被举报次数',
  hidden        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '被运营隐藏',
  deleted       TINYINT(1)   NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sub (submission_id),
  KEY idx_parent (parent_id)
) COMMENT='作品评论表（先发后审，举报制）';

CREATE TABLE IF NOT EXISTS mp_comment_report (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  comment_id   BIGINT       NOT NULL,
  reporter_id  BIGINT       NOT NULL,
  reason       VARCHAR(128)          COMMENT '举报理由',
  handled      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '运营已处理',
  handled_by   BIGINT                COMMENT '处理人 ID',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_comment (comment_id)
) COMMENT='评论举报记录表';

-- ----------- 4. 审核 -----------
CREATE TABLE IF NOT EXISTS mp_review_log (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  submission_id  BIGINT       NOT NULL,
  step_name      VARCHAR(32)  NOT NULL COMMENT 'MANUAL / AI_TEXT / AI_CODE / AI_BINARY / AI_DATA',
  step_status    VARCHAR(16)  NOT NULL COMMENT 'SKIPPED / PASSED / REJECTED / PENDING',
  reviewer_id    BIGINT                COMMENT '人工审核人 ID（AI 时为空）',
  result_detail  TEXT                  COMMENT 'JSON: 具体原因/命中项',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sub (submission_id)
) COMMENT='审核步骤流水（按 ADR-0003 ReviewStep 设计）';

-- ----------- 5. 积分与激励 -----------
CREATE TABLE IF NOT EXISTS mp_point_transaction (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  user_id      BIGINT       NOT NULL,
  delta        INT          NOT NULL COMMENT '积分增减值',
  balance      INT          NOT NULL COMMENT '交易后余额',
  reason       VARCHAR(128) NOT NULL COMMENT '原因：SUBMIT/DOWNLOAD_THRESHOLD/RATING_THRESHOLD/ADMIN_ADJUST 等',
  ref_id       BIGINT                COMMENT '关联 ID（作品 ID / 评论 ID 等）',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user (user_id)
) COMMENT='贡献积分流水';

-- ----------- 6. 大赛双模 -----------
CREATE TABLE IF NOT EXISTS mp_competition (
  id                     BIGINT       NOT NULL AUTO_INCREMENT,
  name                   VARCHAR(128) NOT NULL DEFAULT 'AI 技能大赛',
  status                 VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/JUDGING/CLOSED',
  submit_start_at        DATETIME              COMMENT '提交开始',
  submit_end_at          DATETIME              COMMENT '提交结束',
  review_at              DATETIME              COMMENT '评审日期',
  shortlist_threshold    INT                   DEFAULT 0 COMMENT '入围积分阈值',
  top_n_per_track        INT                   DEFAULT 5 COMMENT '每赛道取前 N 名入围',
  awards_config          TEXT                  COMMENT 'JSON：奖项配置',
  tracks_config          TEXT                  COMMENT 'JSON：赛道配置数组',
  phase                  VARCHAR(16)           COMMENT '当前阶段：SUBMIT/REVIEW/AWARD（全期宽松 SOP）',
  created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) COMMENT='赛事配置表（双模架构：一个平台仅一个 ACTIVE 赛事）';

CREATE TABLE IF NOT EXISTS mp_competition_entry (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  competition_id BIGINT       NOT NULL,
  submission_id  BIGINT       NOT NULL,
  author_id      BIGINT       NOT NULL,
  track          VARCHAR(32)           COMMENT '所属赛道',
  points         INT                   COMMENT '参赛期间累计积分',
  shortlisted    TINYINT(1)            DEFAULT 0 COMMENT '是否入围',
  judge_rank     INT                   COMMENT '赛道最终名次',
  award          VARCHAR(32)           COMMENT '奖项：FIRST/SECOND/THIRD/HONOR',
  PRIMARY KEY (id),
  UNIQUE KEY uk_comp_sub (competition_id, submission_id),
  KEY idx_author (author_id),
  KEY idx_track (competition_id, track)
) COMMENT='作品参赛报名表';

-- ----------- 7. 分类标签 -----------
CREATE TABLE IF NOT EXISTS mp_category (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  dim         VARCHAR(32)  NOT NULL COMMENT '维度：BUSINESS_DOMAIN / USAGE_SCENARIO / TYPE / TECH_STACK',
  code        VARCHAR(64)  NOT NULL COMMENT '标签代码',
  name        VARCHAR(64)  NOT NULL COMMENT '标签显示名',
  sort_order  INT          NOT NULL DEFAULT 0,
  enabled     TINYINT(1)   NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dim_code (dim, code)
) COMMENT='分类标签字典';

-- ----------- 8. 操作日志与审计 -----------
CREATE TABLE IF NOT EXISTS sys_audit_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  category    VARCHAR(32)  NOT NULL COMMENT 'SUBMISSION / REVIEW / PERMISSION / SENSITIVE',
  user_id     BIGINT                COMMENT '操作人',
  action      VARCHAR(64)  NOT NULL COMMENT '具体动作',
  target_type VARCHAR(32)           COMMENT '目标类型：SUBMISSION/COMMENT/USER/COMPETITION 等',
  target_id   BIGINT                COMMENT '目标 ID',
  detail      VARCHAR(1024)         COMMENT '操作细节快照',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_cat (category),
  KEY idx_time (created_at)
) COMMENT='4 类操作审计日志';

-- ----------- 9. 下载记录 -----------
CREATE TABLE IF NOT EXISTS mp_download_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  submission_id BIGINT       NOT NULL,
  artifact_id   BIGINT       NOT NULL,
  user_id       BIGINT       NOT NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_sub (user_id, submission_id),
  KEY idx_time (created_at)
) COMMENT='下载记录（v1 必须下载后才能评分的判据来源）';

-- ----------- 10. 部门负责人推荐评委记录 -----------
CREATE TABLE IF NOT EXISTS mp_judge_recommendation (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  referrer_id     BIGINT       NOT NULL COMMENT '推荐人（部门负责人）ID',
  candidate_id    BIGINT       NOT NULL COMMENT '候选人用户 ID',
  department      VARCHAR(128)          COMMENT '推荐赛道',
  reason          VARCHAR(256)          COMMENT '推荐理由',
  status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  handled_by      BIGINT                COMMENT '运营处理人',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) COMMENT='部门负责人推荐评委记录';

-- ----------- 初始化分类标签（默认值） -----------
INSERT IGNORE INTO mp_category (dim, code, name, sort_order) VALUES
  -- 业务领域
  ('BUSINESS_DOMAIN', 'AI',       'AI',       1),
  ('BUSINESS_DOMAIN', 'RND',      '产研',      2),
  ('BUSINESS_DOMAIN', 'HR',       'HR',       3),
  ('BUSINESS_DOMAIN', 'FINANCE',  '财务',      4),
  ('BUSINESS_DOMAIN', 'SALES',    '销售',      5),
  ('BUSINESS_DOMAIN', 'OPS',      '运营',      6),
  ('BUSINESS_DOMAIN', 'LEGAL',    '法务',      7),
  ('BUSINESS_DOMAIN', 'GENERAL',  '行政',      8),
  -- 使用场景
  ('USAGE_SCENARIO', 'PERSONAL',   '个人提效',  1),
  ('USAGE_SCENARIO', 'TEAM',       '团队协作',  2),
  ('USAGE_SCENARIO', 'PROCESS',    '业务流程',  3),
  ('USAGE_SCENARIO', 'DATA',       '数据分析',  4),
  ('USAGE_SCENARIO', 'MANAGEMENT', '管理决策',  5),
  -- 作品类型（提交形态）
  ('TYPE', 'SOURCE',      '源码 / 代码仓库', 1),
  ('TYPE', 'EXECUTABLE',  '可执行包 / 安装包', 2),
  ('TYPE', 'SAAS',        '在线部署 SaaS / 平台', 3),
  ('TYPE', 'DOCUMENT',    '文档 / 方案 / Prompt 库', 4),
  -- 技术栈
  ('TECH_STACK', 'PYTHON',  'Python',    1),
  ('TECH_STACK', 'JAVA',    'Java',      2),
  ('TECH_STACK', 'JS_TS',   'JS / TS',   3),
  ('TECH_STACK', 'GO',      'Go',        4),
  ('TECH_STACK', 'LOW_CODE','低代码 / no-code', 5),
  ('TECH_STACK', 'PROMPT',  'AI Prompt', 6),
  ('TECH_STACK', 'OTHER',   '其它',      99);
