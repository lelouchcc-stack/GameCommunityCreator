-- 后台「原子库」「预设库」菜单注册
--
-- 背景：这两处页面（site/logicNode、site/preset）+ 它们的接口早就存在，
--       但菜单只存在于开发库（当初是手工加的），**不在任何 SQL 脚本里**。
--       结果是全新安装后后台看不到这两个入口——页面写好了却点不到。
--       本脚本把它补上，install.sql 已按序引用。
--
-- 说明：可重复执行。菜单 id 17/18 是空的（seed 占 1-15、ui_builder 占 16、
--       banner_slide 占 19、ui_template 占 20、admin_notice 占 21）。
-- 归属：站点配置(6) 下，排在「页面搭建」之后——它们是页面搭建要用到的两座库。

SET NAMES utf8mb4;
USE ssy_db;

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status) VALUES
(17, 6, '原子库', 'C', 'logic-node', 'site/logicNode/index', 'site:logic:list', 'Connection', 4, 1, 1),
(18, 6, '预设库', 'C', 'preset',     'site/preset/index',    'site:preset:list', 'Files',      5, 1, 1)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name), path = VALUES(path), component = VALUES(component),
  perms = VALUES(perms), icon = VALUES(icon), sort = VALUES(sort),
  visible = VALUES(visible), status = VALUES(status);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 17 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 17);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 18 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 18);
