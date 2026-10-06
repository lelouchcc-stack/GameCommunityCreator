-- 用户端内容读取页面（v1）：板块详情 + 帖子检索 + 帖子详情
--
-- ⚠ 这三页的当前定义已被「默认社区模板」取代（比本脚本多了分类标签与 autoBoard）：
--   backend/src/main/resources/templates/default-community/pages/{board,post-list,post-detail}.json
--   由管理端「站点配置 → 创建社区」应用。本脚本保留作为历史记录与兜底，重跑会退回旧版。
--
-- 内容：
--   1. 三个业务组件的组件库种子（builtin:board_detail / builtin:post_list / builtin:post_detail），
--      都开放了「多区域样式」键（列表项/标题/元信息/封面/序号角标/空状态/动作）。
--   2. 三个搭建页面：/board/:id、/post/list、/post/:id 及其节点树。
--
-- 可重复执行：组件类型用 ON DUPLICATE KEY UPDATE；页面先删节点再插入。
-- ⚠ 重跑会覆盖这三页在搭建器里的改动。设计文档：pit.md 模块 03/04/06/07。
-- 依赖后端接口：/app/board/{id}、/app/post/page、/app/post/{id}。

SET NAMES utf8mb4;
USE ssy_db;

-- ==================== 1、组件库种子 ====================

INSERT INTO ui_component_type
(type_code, type_name, category, render_kind, is_container, is_builtin,
 style_schema, content_schema, behavior_schema, default_style, default_content, preview_icon, sort, status) VALUES
('board_detail', '板块详情(业务)', 'business', 'builtin:board_detail', 0, 1,
 '["width","maxWidth","padding","marginTop","marginBottom","backgroundColor","radius","boxShadow","coverWidth","coverHeight","coverRadius","coverFit","titleColor","titleFontSize","titleFontWeight","metaColor","metaFontSize","metaGap","bodyColor","bodyFontSize","bodyLineHeight","badgeBackgroundColor","badgeTextColor","actionColor","actionFontSize","actionRadius","itemGap"]',
 '["boardId"]',
 '[]',
 '{"width":"100%","padding":"16px","backgroundColor":"var(--site-bg-card)","radius":"12px","boxShadow":"sm"}',
 '{}',
 'Postcard', 24, 1),
('post_list', '帖子列表(业务)', 'business', 'builtin:post_list', 0, 1,
 '["width","maxWidth","padding","marginTop","marginBottom","backgroundColor","radius","boxShadow","itemPadding","itemGap","itemBorderColor","itemHoverBackgroundColor","titleColor","titleFontSize","titleFontWeight","metaColor","metaFontSize","metaGap","coverWidth","coverHeight","coverRadius","coverFit","badgeBackgroundColor","badgeTextColor","emptyColor","emptyFontSize","actionColor","actionFontSize","actionRadius"]',
 '["title","boardId","versionId","stageId","tagId","keyword","sort","limit","showCover","emptyText"]',
 '[]',
 '{"width":"100%","padding":"16px","backgroundColor":"var(--site-bg-card)","radius":"12px","boxShadow":"sm"}',
 '{"sort":"latest","limit":10,"showCover":true,"emptyText":"暂无帖子"}',
 'List', 25, 1),
('post_detail', '帖子详情(业务)', 'business', 'builtin:post_detail', 0, 1,
 '["width","maxWidth","padding","marginTop","marginBottom","backgroundColor","radius","boxShadow","titleColor","titleFontSize","titleFontWeight","metaColor","metaFontSize","metaGap","bodyColor","bodyFontSize","bodyLineHeight","coverWidth","coverHeight","coverRadius","coverFit","badgeBackgroundColor","badgeTextColor","emptyColor","emptyFontSize","itemHoverBackgroundColor","actionRadius"]',
 '["postId"]',
 '[]',
 '{"width":"100%","padding":"24px","backgroundColor":"var(--site-bg-card)","radius":"12px"}',
 '{}',
 'Document', 26, 1)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  category = VALUES(category),
  render_kind = VALUES(render_kind),
  is_container = VALUES(is_container),
  is_builtin = VALUES(is_builtin),
  style_schema = VALUES(style_schema),
  content_schema = VALUES(content_schema),
  behavior_schema = VALUES(behavior_schema),
  default_style = VALUES(default_style),
  default_content = VALUES(default_content),
  sort = VALUES(sort);

-- ==================== 2、三个页面 ====================

INSERT INTO ui_page (page_key, page_name, route_path, is_home, status, remark)
VALUES ('board',       '板块详情', '/board/:id', 0, 1, '板块详情 + 版本归档 + 帖子列表'),
       ('post-list',   '攻略检索', '/post/list',  0, 1, '帖子浏览与检索'),
       ('post-detail', '帖子详情', '/post/:id',   0, 1, '帖子正文与图片')
ON DUPLICATE KEY UPDATE
  page_name = VALUES(page_name), route_path = VALUES(route_path), status = VALUES(status);

SET @boardPageId      = (SELECT id FROM ui_page WHERE page_key = 'board');
SET @postListPageId   = (SELECT id FROM ui_page WHERE page_key = 'post-list');
SET @postDetailPageId = (SELECT id FROM ui_page WHERE page_key = 'post-detail');

-- ==================== 3、节点树 ====================
-- 先清空这三页现有节点，避免重跑产生重复

DELETE FROM ui_node WHERE page_id IN (@boardPageId, @postListPageId, @postDetailPageId);

-- 3.1 板块详情页：板块头部 + 帖子列表
INSERT INTO ui_node (page_id, parent_id, type_code, node_name, sort, visible, style_json, content_json, behavior_json, remark)
VALUES
(@boardPageId, NULL, 'board_detail', '板块头部',  0, 1, '{}', '{}', '{}', '板块信息 + 版本/阶段筛选入口'),
(@boardPageId, NULL, 'post_list',    '板块帖子', 1, 1, '{}', '{"sort":"latest","limit":10}', '{}', '读取路由板块 id，版本/阶段由查询串筛选');

-- 3.2 攻略检索页：帖子列表（热度排序）
INSERT INTO ui_node (page_id, parent_id, type_code, node_name, sort, visible, style_json, content_json, behavior_json, remark)
VALUES
(@postListPageId, NULL, 'post_list', '帖子检索', 0, 1, '{}', '{"title":"攻略检索","sort":"hot","limit":10}', '{}', '全部已发布帖子，按热度排序');

-- 3.3 帖子详情页：正文 + 图片
INSERT INTO ui_node (page_id, parent_id, type_code, node_name, sort, visible, style_json, content_json, behavior_json, remark)
VALUES
(@postDetailPageId, NULL, 'post_detail', '帖子正文', 0, 1, '{}', '{}', '{}', '读取路由帖子 id，匿名可浏览，后端累加浏览量');
