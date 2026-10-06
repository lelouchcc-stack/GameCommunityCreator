-- 默认社区模板 · 后台菜单注册
--
-- 背景：模板数据放在 backend/src/main/resources/templates/default-community/ 下，
--       由 TemplateService 读取并写库，不需要额外的 SQL 种子。
--       但「创建社区」这个入口本身是平台菜单，得先注册管理员才点得到，所以留这份脚本。
--
-- 说明：可重复执行（ON DUPLICATE KEY UPDATE + 角色菜单防重）。
-- 依赖：已执行 sql/schema.sql 与 sql/seed.sql（sys_menu / sys_role_menu 已存在）。
-- 后续：进后台「站点配置 → 创建社区」，填社区名称与游戏名称即可建出完整社区。

SET NAMES utf8mb4;
USE ssy_db;

-- 后台菜单：站点配置(6) 下新增「创建社区」(20；1-19 已被 seed/ui_builder/ui_logic/banner_slide 占用)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status) VALUES
(20, 6, '创建社区', 'C', 'template', 'site/template/index', 'site:template:list', 'MagicStick', 0, 1, 1)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name), path = VALUES(path), component = VALUES(component),
  perms = VALUES(perms), icon = VALUES(icon), sort = VALUES(sort),
  visible = VALUES(visible), status = VALUES(status);

-- 超级管理员可见（与其它菜单一致：角色 1 拥有全部菜单）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 20 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 20);
