-- 后台公告管理 · 菜单注册
--
-- 背景：公告此前只有用户端只读接口（/app/notice/list|page|{id}），
--       后台没有维护入口——发公告只能改数据库。本次补上 CRUD 后，把菜单注册上。
--
-- 位置：挂在「内容管理」(id 9) 下，与板块/内容维度/标签/帖子审核同一处（公告是内容的一类）。
-- 说明：可重复执行。数据库表 notice 已存在（sql/schema.sql），本脚本只登记菜单。
-- 后续：公告改动是即时的——后台保存后用户端刷新即可见，不需要额外"发布"动作（草稿状态除外）。

SET NAMES utf8mb4;
USE ssy_db;

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status) VALUES
(21, 9, '公告管理', 'C', 'notice', 'content/notice/index', 'notice:list', 'Bell', 5, 1, 1)
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name), path = VALUES(path), component = VALUES(component),
  perms = VALUES(perms), icon = VALUES(icon), sort = VALUES(sort),
  visible = VALUES(visible), status = VALUES(status);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 21 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 21);
