-- =====================================================================
-- 002_multi_page.sql —— v1 单页模型 → v2 多页面模型（幂等迁移，MySQL 8/9）
--
-- 契约来源：docs/API.md §10、docs/PAGE_SCHEMA.md §8
-- 前置条件：
--   1. 目标库已完成 v1（或 v2）建表：至少存在 community_project / page_config /
--      page_publication，以及 v2 的 site_page / site_page_draft / site_page_publication /
--      site_settings / site_navigation（先执行 database/schema.sql）。
--   2. 允许重复执行：所有写入都带 NOT EXISTS / 唯一键守卫，第二次执行不产生任何新行。
--   3. 旧表 page_config / page_publication **不删除**，仅作为迁移来源保留（迁移后新代码不再写）。
-- 迁移内容：
--   1. 每个旧 project 建 home 页面：id = page_home_{projectId}
--      status：有发布快照 → PUBLISHED，否则 DRAFT；
--      draft_version 取 page_config.config_version（缺失按 1），
--      published_version 取最新快照版本。
--   2. page_config → site_page_draft；page_publication → site_page_publication。
--      两个 JSON 均用 JSON_SET 升级为 v2（补 schemaVersion=2、pageId=<home pageId>）；
--      JSON_SET 返回 JSON，落 MEDIUMTEXT 前显式 CAST AS CHAR。
--   3. 每个 project 建 site_settings（默认 DARK）与至少一条 header 首页导航。
--   4. 有发布快照的 project 状态校正为 PUBLISHED。
--
-- 迁移后验证 SQL（手动执行，期望值见每行注释）：
--   -- 旧项目数 == home 页面数（两值相等）
--   SELECT (SELECT COUNT(*) FROM community_project) AS projects,
--          (SELECT COUNT(*) FROM site_page WHERE page_type = 'home') AS home_pages;
--   -- 应为 0：旧草稿未被迁移的行
--   SELECT COUNT(*) FROM page_config c
--    WHERE NOT EXISTS (SELECT 1 FROM site_page_draft d
--                       WHERE d.page_id = CONCAT('page_home_', c.project_id));
--   -- 应为 0：旧发布快照未被迁移的行
--   SELECT COUNT(*) FROM page_publication pub
--    WHERE NOT EXISTS (SELECT 1 FROM site_page_publication sp
--                       WHERE sp.page_id = CONCAT('page_home_', pub.project_id)
--                         AND sp.config_version = pub.config_version);
--   -- 应为 0：迁移后的 home 草稿 JSON 不是合法 v2 JSON 的行
--   SELECT COUNT(*) FROM site_page_draft d
--    WHERE d.page_id LIKE 'page_home_%'
--      AND (JSON_VALID(d.config_json) = 0
--           OR JSON_EXTRACT(d.config_json, '$.schemaVersion') <> 2
--           OR JSON_UNQUOTE(JSON_EXTRACT(d.config_json, '$.pageId')) <> d.page_id);
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 旧项目 → home 页面
-- ---------------------------------------------------------------------
INSERT INTO site_page
    (id, project_id, name, slug, page_type, template_key, status, sort_order,
     draft_version, published_version, created_at, updated_at)
SELECT CONCAT('page_home_', p.id),
       p.id,
       '首页',
       'home',
       'home',
       'default-home',
       CASE WHEN EXISTS (SELECT 1 FROM page_publication pub WHERE pub.project_id = p.id)
            THEN 'PUBLISHED' ELSE 'DRAFT' END,
       0,
       COALESCE((SELECT c.config_version FROM page_config c WHERE c.project_id = p.id), 1),
       (SELECT MAX(pub2.config_version) FROM page_publication pub2 WHERE pub2.project_id = p.id),
       p.created_at,
       p.updated_at
FROM community_project p
WHERE NOT EXISTS (SELECT 1 FROM site_page sp WHERE sp.id = CONCAT('page_home_', p.id));

-- ---------------------------------------------------------------------
-- 2. 旧草稿 → site_page_draft（JSON 升级 v2：schemaVersion=2 + pageId）
-- ---------------------------------------------------------------------
INSERT INTO site_page_draft (page_id, config_json, config_version, updated_at)
SELECT CONCAT('page_home_', c.project_id),
       CASE WHEN JSON_VALID(c.config_json) = 1
            THEN CAST(JSON_SET(c.config_json,
                               '$.schemaVersion', 2,
                               '$.pageId', CONCAT('page_home_', c.project_id)) AS CHAR)
            ELSE c.config_json END,
       c.config_version,
       c.updated_at
FROM page_config c
WHERE EXISTS (SELECT 1 FROM community_project p WHERE p.id = c.project_id)
  AND NOT EXISTS (SELECT 1 FROM site_page_draft d
                   WHERE d.page_id = CONCAT('page_home_', c.project_id));

-- ---------------------------------------------------------------------
-- 3. 旧发布快照 → site_page_publication（同样升级 JSON 到 v2）
-- ---------------------------------------------------------------------
INSERT INTO site_page_publication (page_id, project_id, config_version, config_json, published_at)
SELECT CONCAT('page_home_', pub.project_id),
       pub.project_id,
       pub.config_version,
       CASE WHEN JSON_VALID(pub.config_json) = 1
            THEN CAST(JSON_SET(pub.config_json,
                               '$.schemaVersion', 2,
                               '$.pageId', CONCAT('page_home_', pub.project_id)) AS CHAR)
            ELSE pub.config_json END,
       pub.published_at
FROM page_publication pub
WHERE EXISTS (SELECT 1 FROM community_project p WHERE p.id = pub.project_id)
  AND NOT EXISTS (SELECT 1 FROM site_page_publication sp
                   WHERE sp.page_id = CONCAT('page_home_', pub.project_id)
                     AND sp.config_version = pub.config_version);

-- ---------------------------------------------------------------------
-- 4. 站点设置（默认深色主题）
-- ---------------------------------------------------------------------
INSERT INTO site_settings (project_id, theme_mode, updated_at)
SELECT p.id, 'DARK', p.updated_at
FROM community_project p
WHERE NOT EXISTS (SELECT 1 FROM site_settings s WHERE s.project_id = p.id);

-- ---------------------------------------------------------------------
-- 5. 默认导航（迁移后的旧站只有 home 页面，header 先给一条首页入口）
-- ---------------------------------------------------------------------
INSERT INTO site_navigation
    (project_id, location, title, target_type, target_page_id, url, sort_order, visible)
SELECT p.id, 'header', '首页', 'page', CONCAT('page_home_', p.id), '', 0, 1
FROM community_project p
WHERE NOT EXISTS (SELECT 1 FROM site_navigation n WHERE n.project_id = p.id);

-- ---------------------------------------------------------------------
-- 6. 项目状态校正：有发布快照即视为已发布
-- ---------------------------------------------------------------------
UPDATE community_project p
   SET p.status = 'PUBLISHED'
 WHERE p.status <> 'PUBLISHED'
   AND EXISTS (SELECT 1 FROM site_page_publication sp WHERE sp.project_id = p.id);
