-- 用户端图形化搭建 v3 · 阶段 1（呈现层）增量脚本
-- 内容：3 张新表（ui_page / ui_component_type / ui_node）、阶段1 组件库种子、
--       home_module -> ui_node 迁移
-- 说明：本文件可重复执行（DDL 用 IF NOT EXISTS，类型种子用 ON DUPLICATE KEY UPDATE，
--       页面与节点插入做防重）。稳定后应并入 sql/schema.sql 与 sql/seed.sql。
-- 设计文档：用户端模块化样式设计.json（v3.0）的 uiModel / database 节

SET NAMES utf8mb4;
USE ssy_db;

-- ==================== 一、用户端搭建域（3 张表） ====================

CREATE TABLE IF NOT EXISTS ui_page (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  page_key    VARCHAR(30)  NOT NULL COMMENT '页面标识,如 home/about',
  page_name   VARCHAR(50)  NOT NULL COMMENT '页面名称',
  route_path  VARCHAR(100) COMMENT '用户端路由,如 / 或 /about',
  is_home     TINYINT NOT NULL DEFAULT 0 COMMENT '1首页 0普通页',
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  remark      VARCHAR(200) COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_page_key (page_key)
) ENGINE=InnoDB COMMENT='用户端页面表';

CREATE TABLE IF NOT EXISTS ui_component_type (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  type_code       VARCHAR(40) NOT NULL COMMENT '组件类型标识',
  type_name       VARCHAR(50) NOT NULL COMMENT '组件名称',
  category        VARCHAR(20) NOT NULL DEFAULT 'base' COMMENT 'base/layout/display/interactive/business',
  render_kind     VARCHAR(40) NOT NULL COMMENT '渲染内核;业务组件为 builtin:xxx',
  is_container    TINYINT NOT NULL DEFAULT 0 COMMENT '1可容纳子节点',
  is_builtin      TINYINT NOT NULL DEFAULT 0 COMMENT '1业务组件(代码实现)',
  style_schema    TEXT COMMENT '开放的样式属性 key 数组(JSON)',
  content_schema  TEXT COMMENT '开放的内容属性 key 数组(JSON)',
  behavior_schema TEXT COMMENT '开放的行为属性 key 数组(JSON)',
  default_style   TEXT COMMENT '默认样式值(JSON)',
  default_content TEXT COMMENT '默认内容值(JSON)',
  preview_icon    VARCHAR(50) COMMENT '管理端列表图标',
  sort            INT NOT NULL DEFAULT 0 COMMENT '排序',
  status          TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  remark          VARCHAR(200) COMMENT '备注',
  create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_type_code (type_code)
) ENGINE=InnoDB COMMENT='组件库(组件类型)表';

CREATE TABLE IF NOT EXISTS ui_node (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  page_id       BIGINT NOT NULL COMMENT '所属页面',
  parent_id     BIGINT NULL COMMENT '父节点,null=顶层',
  type_code     VARCHAR(40) NOT NULL COMMENT '组件类型标识',
  node_name     VARCHAR(50) COMMENT '编辑器内节点名',
  sort          INT NOT NULL DEFAULT 0 COMMENT '同级排序',
  visible       TINYINT NOT NULL DEFAULT 1 COMMENT '1显示 0隐藏',
  style_json    TEXT COMMENT '样式覆盖值(JSON)',
  content_json  TEXT COMMENT '内容(JSON,可含 $ref)',
  behavior_json TEXT COMMENT '行为(JSON)',
  remark        VARCHAR(200) COMMENT '备注',
  create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_page_parent (page_id, parent_id)
) ENGINE=InnoDB COMMENT='用户端页面节点表(树)';

-- ==================== 二、阶段1 组件库种子 ====================
-- 颜色类默认值写成 var(--site-*)，留空/未覆盖时跟随站点主题

INSERT INTO ui_component_type
(type_code, type_name, category, render_kind, is_container, is_builtin,
 style_schema, content_schema, behavior_schema, default_style, default_content, preview_icon, sort, status) VALUES
('text', '文本', 'base', 'text', 0, 0,
 '["width","align","padding","backgroundColor","fontSize","fontWeight","textColor","textAlign","lineHeight","letterSpacing"]',
 '["text","visibleWhen"]',
 '[]',
 '{"align":"left","textAlign":"left","fontSize":"14px","fontWeight":"400","textColor":"var(--site-text-primary)","lineHeight":1.5}',
 '{"text":"文本内容"}',
 'Document', 1, 1),
('heading', '标题', 'base', 'heading', 0, 0,
 '["align","padding","fontSize","fontWeight","textColor","textAlign"]',
 '["text"]',
 '[]',
 '{"align":"left","textAlign":"left","fontSize":"20px","fontWeight":"600","textColor":"var(--site-text-primary)"}',
 '{"text":"标题"}',
 'Tickets', 2, 1),
('image', '图片', 'base', 'image', 0, 0,
 '["width","maxWidth","align","radius","opacity"]',
 '["imageUrl","imageFit","linkType","linkUrl","visibleWhen"]',
 '["onClickAction"]',
 '{"width":"auto","maxWidth":"100%","align":"left","radius":"8px","opacity":1}',
 '{"imageUrl":"","imageFit":"cover","linkType":"none"}',
 'Picture', 3, 1),
('button', '按钮', 'base', 'button', 0, 0,
 '["width","height","padding","align","backgroundColor","texture","radius","fontSize","fontWeight","textColor","hoverEffect","clickEffect","boxShadow","transitionMs","cursor"]',
 '["text","icon","visibleWhen"]',
 '["emit","onClickAction"]',
 '{"width":"auto","height":"auto","padding":"8px 18px","align":"left","backgroundColor":"var(--site-primary)","textColor":"#FFFFFF","fontSize":"14px","fontWeight":"600","radius":"8px","texture":"none","hoverEffect":"lift","clickEffect":"scale","boxShadow":"sm","transitionMs":150,"cursor":"pointer"}',
 '{"text":"按钮"}',
 'Pointer', 4, 1),
('divider', '分割线', 'base', 'divider', 0, 0,
 '["width","borderWidth","borderStyle","borderColor","marginTop","marginBottom"]',
 '[]',
 '[]',
 '{"width":"100%","borderWidth":"1px","borderStyle":"solid","borderColor":"var(--site-border)","marginTop":"12px","marginBottom":"12px"}',
 '{}',
 'Minus', 5, 1),
('spacer', '间距', 'base', 'spacer', 0, 0,
 '["height"]',
 '[]',
 '[]',
 '{"height":"16px"}',
 '{}',
 'Sort', 6, 1),
('container', '容器', 'layout', 'container', 1, 0,
 '["width","maxWidth","padding","marginTop","marginBottom","backgroundColor","backgroundImage","backgroundSize","texture","radius","borderWidth","borderStyle","borderColor","boxShadow","flexDirection","justify","alignItems","gap","wrap"]',
 '["visibleWhen"]',
 '[]',
 '{"width":"auto","maxWidth":"100%","padding":"16px","backgroundColor":"var(--site-bg-card)","radius":"8px","flexDirection":"column","justify":"start","alignItems":"stretch","gap":"12px","wrap":true}',
 '{}',
 'Grid', 7, 1),
('card', '卡片', 'layout', 'container', 1, 0,
 '["width","maxWidth","padding","marginTop","marginBottom","backgroundColor","texture","radius","boxShadow","hoverEffect","flexDirection","gap"]',
 '["visibleWhen"]',
 '[]',
 '{"width":"auto","maxWidth":"100%","padding":"16px","backgroundColor":"var(--site-bg-card)","radius":"8px","boxShadow":"sm","hoverEffect":"none","flexDirection":"column","gap":"12px"}',
 '{}',
 'Postcard', 8, 1),
('banner', '首页轮播(业务)', 'business', 'builtin:banner', 0, 1,
 '["height","radius","marginBottom"]',
 '["autoplay","interval","limit"]',
 '[]',
 '{"height":"240px","radius":"8px","marginBottom":"16px"}',
 '{"autoplay":true,"interval":5000,"limit":5}',
 'PictureFilled', 20, 1),
('notice', '公告(业务)', 'business', 'builtin:notice', 0, 1,
 '["padding","backgroundColor","radius","fontSize"]',
 '["limit"]',
 '[]',
 '{"padding":"12px 16px","backgroundColor":"var(--site-bg-card)","radius":"8px","fontSize":"14px"}',
 '{"limit":5}',
 'Bell', 21, 1),
('hot_post', '热门帖子(业务)', 'business', 'builtin:hot_post', 0, 1,
 '["padding","backgroundColor","radius"]',
 '["limit"]',
 '[]',
 '{"padding":"16px","backgroundColor":"var(--site-bg-card)","radius":"8px"}',
 '{"limit":10}',
 'Star', 22, 1),
('board_nav', '板块导航(业务)', 'business', 'builtin:board_nav', 0, 1,
 '["padding","backgroundColor","radius"]',
 '[]',
 '[]',
 '{"padding":"16px","backgroundColor":"var(--site-bg-card)","radius":"8px"}',
 '{}',
 'Menu', 23, 1)
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

-- ==================== 三、首页页面 + home_module 迁移 ====================

INSERT INTO ui_page (page_key, page_name, route_path, is_home, status, remark)
VALUES ('home', '首页', '/', 1, 1, '用户端首页，由 home_module 迁移而来')
ON DUPLICATE KEY UPDATE page_name = VALUES(page_name), route_path = VALUES(route_path), is_home = VALUES(is_home);

-- 只在首页尚无节点时迁移一次，避免重复执行产生重复节点
INSERT INTO ui_node (page_id, parent_id, type_code, node_name, sort, visible, content_json, remark)
SELECT p.id, NULL, m.module_type, m.module_name, m.sort, m.enabled, m.config_json, '由 home_module 迁移'
FROM home_module m
JOIN ui_page p ON p.page_key = 'home'
WHERE NOT EXISTS (SELECT 1 FROM ui_node n WHERE n.page_id = p.id);
