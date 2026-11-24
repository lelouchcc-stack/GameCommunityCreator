-- =====================================================================
-- 004_issue_progress.sql —— v3.2：问题反馈处理结果 + 公开展示开关
--
-- 契约来源：答辩前缺陷修复任务（task-17）§2
-- 前置条件：目标库已完成 v1/v2/v3 建表（database/schema.sql）。
-- 特点：
--   1. **幂等**：information_schema + prepared statement 条件 ADD COLUMN，重复执行只输出 skipped；
--   2. **不清库、不删数据**：新列带默认值（resolution=''、is_public=0），旧问题记录自动获得安全默认；
--   3. 仅 RESOLVED 且 is_public=1 的问题会出现在公开进展接口（由后端业务校验保证）。
--
-- 迁移后验证 SQL（手动执行，期望值见注释）：
--   -- 2：两列都已存在
--   SELECT COUNT(*) FROM information_schema.COLUMNS
--    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'issue_report'
--      AND COLUMN_NAME IN ('resolution', 'is_public');
--   -- 0：旧数据均获得默认值（无 NULL）
--   SELECT COUNT(*) FROM issue_report WHERE resolution IS NULL OR is_public IS NULL;
--   -- 旧问题数据保留：
--   SELECT COUNT(*) AS issues FROM issue_report;
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. issue_report.resolution（处理结果，存在则跳过）
-- ---------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'issue_report'
     AND COLUMN_NAME = 'resolution');
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE issue_report ADD COLUMN resolution VARCHAR(1000) NOT NULL DEFAULT '''' COMMENT ''处理结果（管理员填写，仅 RESOLVED 有意义）'' AFTER status',
  'SELECT ''issue_report.resolution already exists'' AS skipped');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. issue_report.is_public（是否允许公开展示，存在则跳过）
-- ---------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'issue_report'
     AND COLUMN_NAME = 'is_public');
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE issue_report ADD COLUMN is_public TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否允许公开展示（仅 RESOLVED 且 =1 才公开）'' AFTER resolution',
  'SELECT ''issue_report.is_public already exists'' AS skipped');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 3. 兜底：旧行若因历史原因出现 NULL，回填安全默认值（幂等）
-- ---------------------------------------------------------------------
UPDATE issue_report SET resolution = '' WHERE resolution IS NULL;
UPDATE issue_report SET is_public = 0 WHERE is_public IS NULL;
