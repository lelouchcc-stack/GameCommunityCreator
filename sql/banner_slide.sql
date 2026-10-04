-- 首页轮播图增量：新增 banner_slide 表（站点配置域），并登记后台「轮播图管理」菜单
-- 背景：首页要像米游社那样有个能换图的轮播位，管理端可传多张图并配标题/跳转。
--       站点配置的键值表（site_config）一个 key 只放一个短字符串，装不下"有序的一组图"；
--       首页模块表（home_module）只有一列 JSON 且已随 ui_node 组件树弃用，故单建此表。
-- 说明：可重复执行；全量建表见 sql/schema.sql，全量种子见 sql/seed.sql。

SET NAMES utf8mb4;
USE ssy_db;

CREATE TABLE IF NOT EXISTS banner_slide (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(50)  COMMENT '标题,叠加显示在图上,可空',
  subtitle    VARCHAR(100) COMMENT '副标题/一句话说明,可空',
  image       VARCHAR(255) NOT NULL COMMENT '图片(objectKey 或完整地址)',
  link_url    VARCHAR(255) COMMENT '点击跳转地址,空=不可点',
  sort        INT     NOT NULL DEFAULT 0,
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '1展示 0隐藏',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='首页轮播图表';

-- 后台菜单：站点配置(6) 下新增「轮播图管理」(19；16-18 已被 ui_builder/ui_logic 占用)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status) VALUES
(19, 6, '轮播图管理', 'C', 'banner', 'site/banner/index', 'site:banner:list', 'Picture', 6, 1, 1)
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name), path = VALUES(path), component = VALUES(component),
  perms = VALUES(perms), icon = VALUES(icon), sort = VALUES(sort), visible = VALUES(visible), status = VALUES(status);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 19 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 19);

-- 种子轮播图：先借用仓库里已有的几张上传图，后台「轮播图管理」里可随时替换
INSERT INTO banner_slide (id, title, subtitle, image, link_url, sort, status) VALUES
(1, '示例游戏 · 1.3.0「星海回响」', '新版本已上线，攻略与资源点整理中', 'upload/2026/10/05/cec78c43cbb4412fba759659437db30d.jpg', '/board/1', 1, 1),
(2, '新手引导',                    '第一次来？先看社区版规与发帖说明',   'upload/2026/10/04/4a353a4021c2447ca8c6edd1e2d3d30d.jpg', '/board/1', 2, 1),
(3, '攻略征集中',                  '满级配队、资源规划，欢迎来投稿',     'upload/2026/10/04/2e7eca77cbda4a8095007fdd3e222b6f.png', '/board/1', 3, 1)
ON DUPLICATE KEY UPDATE
  title = VALUES(title), subtitle = VALUES(subtitle), image = VALUES(image),
  link_url = VALUES(link_url), sort = VALUES(sort), status = VALUES(status);
