-- 用户端登录 / 注册页面增量脚本
-- 内容：2 个业务组件种子（login_form / register_form）、2 个用户端页面（/login、/register）与初版节点
-- 说明：本文件可重复执行（类型种子用 ON DUPLICATE KEY UPDATE、页面插节点做防重）；
--       稳定后应并入 sql/schema.sql 与 sql/seed.sql。
-- 设计文档：用户端登录注册-搭建方案设计.json

SET NAMES utf8mb4;
USE ssy_db;

-- ==================== 一、表单业务组件种子（2 条） ====================
-- 自包含业务组件（render_kind=builtin:*）：登录表单自己渲染、自己调 /app/login，
-- 因此行为里没有 onClickAction；外观/文案全部由属性面板配置。

INSERT INTO ui_component_type
(type_code, type_name, category, render_kind, is_container, is_builtin,
 style_schema, content_schema, behavior_schema, default_style, default_content, preview_icon, sort, status) VALUES
('login_form', '登录表单(业务)', 'business', 'builtin:login_form', 0, 1,
 '["width","maxWidth","align","padding","backgroundColor","radius","boxShadow","gap"]',
 '["title","submitText","usernamePlaceholder","passwordPlaceholder","redirectTo","showRegisterLink"]',
 '[]',
 '{"width":"100%","maxWidth":"360px","align":"center","padding":"24px","backgroundColor":"var(--site-bg-card)","radius":"12px","boxShadow":"sm","gap":"14px"}',
 '{"title":"登录","submitText":"登录","usernamePlaceholder":"请输入用户名","passwordPlaceholder":"请输入密码","redirectTo":"/","showRegisterLink":true}',
 'Promotion', 30, 1),
('register_form', '注册表单(业务)', 'business', 'builtin:register_form', 0, 1,
 '["width","maxWidth","align","padding","backgroundColor","radius","boxShadow","gap"]',
 '["title","submitText","usernamePlaceholder","passwordPlaceholder","nicknamePlaceholder","redirectTo","showLoginLink"]',
 '[]',
 '{"width":"100%","maxWidth":"360px","align":"center","padding":"24px","backgroundColor":"var(--site-bg-card)","radius":"12px","boxShadow":"sm","gap":"14px"}',
 '{"title":"注册","submitText":"注册","usernamePlaceholder":"4-20 位字母、数字或下划线","passwordPlaceholder":"6-20 位密码","nicknamePlaceholder":"昵称（可选，默认同用户名）","redirectTo":"/","showLoginLink":true}',
 'Promotion', 31, 1)
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

-- ==================== 二、两个页面 ====================
-- 只登记页面与路由；外观与节点在管理端「页面搭建」里维护（改版不改代码）。

INSERT INTO ui_page (page_key, page_name, route_path, is_home, status, remark)
VALUES ('login', '登录', '/login', 0, 1, '玩家登录页，外观由页面搭建器维护'),
       ('register', '注册', '/register', 0, 1, '玩家注册页，外观由页面搭建器维护')
ON DUPLICATE KEY UPDATE
  page_name = VALUES(page_name), route_path = VALUES(route_path), status = VALUES(status);

-- ==================== 三、初版节点（仅在页面还没有任何节点时插入） ====================

INSERT INTO ui_node (page_id, parent_id, type_code, node_name, sort, visible, content_json, remark)
SELECT p.id, NULL, 'login_form', '登录表单', 0, 1,
       '{"title":"登录","submitText":"登录","usernamePlaceholder":"请输入用户名","passwordPlaceholder":"请输入密码","redirectTo":"/","showRegisterLink":true}',
       '登录页初版：一个登录表单，可继续在搭建器里加标题/背景等'
FROM ui_page p
WHERE p.page_key = 'login'
  AND NOT EXISTS (SELECT 1 FROM ui_node n WHERE n.page_id = p.id);

INSERT INTO ui_node (page_id, parent_id, type_code, node_name, sort, visible, content_json, remark)
SELECT p.id, NULL, 'register_form', '注册表单', 0, 1,
       '{"title":"注册","submitText":"注册","usernamePlaceholder":"4-20 位字母、数字或下划线","passwordPlaceholder":"6-20 位密码","nicknamePlaceholder":"昵称（可选，默认同用户名）","redirectTo":"/","showLoginLink":true}',
       '注册页初版：一个注册表单，可继续在搭建器里加标题/背景等'
FROM ui_page p
WHERE p.page_key = 'register'
  AND NOT EXISTS (SELECT 1 FROM ui_node n WHERE n.page_id = p.id);
