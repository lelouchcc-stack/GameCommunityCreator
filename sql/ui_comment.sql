-- 评论互动组件（v1）：builtin:post_actions / builtin:comment_list
--
-- ⚠ /post/:id 的节点树现由「默认社区模板」定义
--   （backend/src/main/resources/templates/default-community/pages/post-detail.json）。
--   本脚本保留作为历史记录与兜底；组件库种子部分仍是权威来源。
--
-- 内容：
--   1. 两个业务组件的组件库种子（含多区域样式键）。
--   2. 往 /post/:id 帖子详情页追加「互动区 + 评论区」节点（幂等：先删同类型节点再插）。
--
-- 可重复执行。依赖：先执行 sql/ui_content_read.sql 建好 /post/:id 页面。
-- 后端接口：GET /app/post/{id}/interaction、POST /app/post/{id}/like|favorite、/app/comment/**。

SET NAMES utf8mb4;
USE ssy_db;

-- ==================== 1、组件库种子 ====================

INSERT INTO ui_component_type
(type_code, type_name, category, render_kind, is_container, is_builtin,
 style_schema, content_schema, behavior_schema, default_style, default_content, preview_icon, sort, status) VALUES
('post_actions', '帖子互动(业务)', 'business', 'builtin:post_actions', 0, 1,
 '["width","align","marginTop","marginBottom","actionColor","actionBackgroundColor","actionFontSize","actionRadius","badgeBackgroundColor","badgeTextColor"]',
 '["postId"]',
 '[]',
 '{"align":"left","marginTop":"12px","marginBottom":"12px"}',
 '{}',
 'Star', 27, 1),
('comment_list', '评论区(业务)', 'business', 'builtin:comment_list', 0, 1,
 '["width","maxWidth","padding","marginTop","marginBottom","backgroundColor","radius","boxShadow","itemPadding","itemBorderColor","titleColor","titleFontSize","titleFontWeight","metaColor","metaFontSize","metaGap","bodyColor","bodyFontSize","bodyLineHeight","emptyColor","emptyFontSize","actionColor","actionFontSize","actionRadius"]',
 '["postId"]',
 '[]',
 '{"width":"100%","padding":"16px","backgroundColor":"var(--site-bg-card)","radius":"12px"}',
 '{}',
 'ChatDotRound', 28, 1)
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

-- ==================== 2、帖子详情页追加互动与评论节点 ====================

DELETE n FROM ui_node n
JOIN ui_page p ON p.id = n.page_id
WHERE p.page_key = 'post-detail' AND n.type_code IN ('post_actions', 'comment_list');

INSERT INTO ui_node (page_id, parent_id, type_code, node_name, sort, visible, style_json, content_json, behavior_json, remark)
SELECT p.id, NULL, v.type_code, v.node_name, v.sort, 1, '{}', '{}', '{}', v.remark
FROM ui_page p
JOIN (
            SELECT 'post_actions' AS type_code, '互动区' AS node_name, 1 AS sort, '点赞 / 收藏' AS remark
  UNION ALL SELECT 'comment_list', '评论区', 2, '一级评论 + 回复'
) v
WHERE p.page_key = 'post-detail';
