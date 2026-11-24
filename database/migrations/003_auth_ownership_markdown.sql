-- =====================================================================
-- 003_auth_ownership_markdown.sql —— v3：开发者账号 + 项目所有权 + Markdown 正文
--
-- 契约来源：docs/API.md §11–§13、docs/BUGFIX_PLAN.md §3–§4
-- 前置条件：目标库已完成 v1/v2 建表（database/schema.sql）。
-- 特点：
--   1. **幂等**：CREATE TABLE IF NOT EXISTS + information_schema 条件 ADD COLUMN/INDEX，
--      重复执行不报错、不重复添加；
--   2. **不清库、不删数据**：只新增表/列并回填 body_markdown，旧项目 owner_user_id 保持 NULL，
--      由后端 BootstrapAccountInitializer 用 APP_BOOTSTRAP_USERNAME 认领；
--   3. MySQL 8/9 语法（prepared statement 动态 DDL）。
--
-- 迁移后验证 SQL（手动执行，期望值见注释）：
--   -- 1：app_user 表存在且可查
--   SELECT COUNT(*) FROM app_user;
--   -- 1（列已存在）：owner_user_id / body_markdown 列都已加上
--   SELECT COUNT(*) FROM information_schema.COLUMNS
--    WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME IN ('owner_user_id','body_markdown');
--   -- 0：旧正文没有被回填遗漏（body_markdown 仍为 NULL 的行数应为 0）
--   SELECT (SELECT COUNT(*) FROM changelog WHERE body_markdown IS NULL)
--        + (SELECT COUNT(*) FROM doc_article WHERE body_markdown IS NULL)
--        + (SELECT COUNT(*) FROM announcement WHERE body_markdown IS NULL) AS missing_markdown;
--   -- 旧项目仍保留、owner 待 bootstrap 认领：
--   SELECT id, slug, owner_user_id FROM community_project;
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 开发者账号表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '开发者账号 ID',
  username      VARCHAR(50)  NOT NULL COMMENT '登录名（3–30，[A-Za-z0-9_-]，全局唯一）',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希（禁止明文）',
  display_name  VARCHAR(60)  NOT NULL COMMENT '显示名称（1–60）',
  created_at    DATETIME     NOT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_user_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '开发者账号（v3：站点所有者）';

-- ---------------------------------------------------------------------
-- 2. community_project.owner_user_id（存在则跳过）
-- ---------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'community_project'
     AND COLUMN_NAME = 'owner_user_id');
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE community_project ADD COLUMN owner_user_id BIGINT NULL COMMENT ''所属开发者账号（v3；逻辑外键 app_user.id，旧数据为 NULL 由 bootstrap 认领）'' AFTER status',
  'SELECT ''community_project.owner_user_id already exists'' AS skipped');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- owner 索引（存在则跳过）
SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'community_project'
     AND INDEX_NAME = 'idx_community_project_owner');
SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE community_project ADD INDEX idx_community_project_owner (owner_user_id)',
  'SELECT ''idx_community_project_owner already exists'' AS skipped');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 3. body_markdown 列（changelog / doc_article / announcement，存在则跳过）
-- ---------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'changelog' AND COLUMN_NAME = 'body_markdown');
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE changelog ADD COLUMN body_markdown MEDIUMTEXT NULL COMMENT ''正文 Markdown（v3；读取优先，空回退 content）'' AFTER content',
  'SELECT ''changelog.body_markdown already exists'' AS skipped');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'doc_article' AND COLUMN_NAME = 'body_markdown');
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE doc_article ADD COLUMN body_markdown MEDIUMTEXT NULL COMMENT ''正文 Markdown（v3；读取优先，空回退 content）'' AFTER content',
  'SELECT ''doc_article.body_markdown already exists'' AS skipped');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcement' AND COLUMN_NAME = 'body_markdown');
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE announcement ADD COLUMN body_markdown MEDIUMTEXT NULL COMMENT ''公告正文 Markdown（v3；读取优先，空回退 content）'' AFTER content',
  'SELECT ''announcement.body_markdown already exists'' AS skipped');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 4. 回填：旧正文 = 纯文本，直接作为 Markdown 初值（幂等）
-- ---------------------------------------------------------------------
UPDATE changelog SET body_markdown = content WHERE body_markdown IS NULL;
UPDATE doc_article SET body_markdown = content WHERE body_markdown IS NULL;
UPDATE announcement SET body_markdown = content WHERE body_markdown IS NULL;
