-- 可定制游戏社区平台 · 种子数据
-- 前置：先执行 sql/schema.sql 建好 21 张表
-- 账号：后台 admin / admin123；玩家 player1 / member123、player2 / member123
--       （上面两个 BCrypt 哈希是用 Hutool 生成并 checkpw 反向校验过的，部署前请改成你自己的密码）
-- 设计要点：board 与 board_version 互相引用，所以先把 current_version_id 留空，插完版本再 UPDATE 回填
-- 重跑：本文件用固定主键，重复执行会主键冲突。需要重跑请先取消下面「清空」块的注释

SET NAMES utf8mb4;
USE ssy_db;

-- ==================== 清空（重跑前取消注释） ====================
-- DELETE FROM notice;
-- DELETE FROM report;
-- DELETE FROM post_favorite;
-- DELETE FROM post_like;
-- DELETE FROM comment;
-- DELETE FROM post_tag;
-- DELETE FROM post;
-- DELETE FROM tag;
-- DELETE FROM member_game_progress;
-- DELETE FROM member;
-- DELETE FROM progress_stage;
-- DELETE FROM board_version;
-- DELETE FROM board;
-- DELETE FROM home_module;
-- DELETE FROM site_config;
-- DELETE FROM sys_role_menu;
-- DELETE FROM sys_menu;
-- DELETE FROM sys_user_role;
-- DELETE FROM sys_role;
-- DELETE FROM sys_user;

-- ==================== 系统权限域 ====================

INSERT INTO sys_user (id, username, password, nickname, status, remark) VALUES
(1, 'admin', '$2a$10$bFhDAYwKkPLy1SfCRfN0auDPvw1S7aIWLE45aTCyS3.vVo8NacXyS', '超级管理员', 1, '种子账号，密码 admin123，部署前请修改');

INSERT INTO sys_role (id, role_name, role_key, sort, status, remark) VALUES
(1, '超级管理员', 'admin', 1, 1, '拥有全部权限'),
(2, '运营', 'ops', 2, 1, '内容审核与举报处理');

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status) VALUES
(1,  0, '系统管理', 'M', '/system',        NULL,                   NULL,                'Setting',        1, 1, 1),
(2,  1, '用户管理', 'C', 'user',          'system/user/index',    'system:user:list',  'User',           1, 1, 1),
(3,  1, '角色管理', 'C', 'role',          'system/role/index',    'system:role:list',  'UserFilled',     2, 1, 1),
(4,  1, '菜单管理', 'C', 'menu',          'system/menu/index',    'system:menu:list',  'Menu',           3, 1, 1),
(5,  1, '操作日志', 'C', 'log',           'system/log/index',     'system:log:list',   'Document',       4, 1, 1),
(6,  0, '站点配置', 'M', '/site',         NULL,                   NULL,                'Tools',          2, 1, 1),
(7,  6, '站点信息', 'C', 'info',          'site/info/index',      'site:config:list',  'Postcard',       1, 1, 1),
(8,  6, '首页模块', 'C', 'home-module',   'site/homeModule/index','site:module:list',  'Grid',           2, 1, 1),
(9,  0, '内容管理', 'M', '/content',      NULL,                   NULL,                'Notebook',       3, 1, 1),
(10, 9, '板块管理', 'C', 'board',         'content/board/index',  'board:list',        'Files',          1, 1, 1),
(11, 9, '内容维度', 'C', 'dimension',     'content/dimension/index','dimension:list',  'Collection',     2, 1, 1),
(12, 9, '标签管理', 'C', 'tag',           'content/tag/index',    'tag:list',          'PriceTag',       3, 1, 1),
(13, 9, '帖子审核', 'C', 'post',          'content/post/index',   'post:audit',        'ChatLineSquare', 4, 1, 1),
(14, 0, '举报处理', 'C', '/report',       'report/index',         'report:list',       'Warning',        4, 1, 1),
(15, 0, '数据统计', 'C', '/stat',         'stat/index',           'stat:view',         'DataLine',       5, 1, 1);

INSERT INTO sys_role_menu (role_id, menu_id) SELECT 1, id FROM sys_menu;

-- ==================== 站点配置域 ====================

INSERT INTO site_config (config_key, config_value, remark) VALUES
('site_name',     '游戏社区',           '站点名称，显示在浏览器标题与导航栏'),
('game_name',     '',                   '游戏名称，显示在页头副标题与游戏简介区块'),
('site_intro',    '面向独立游戏开发者与玩家的游戏社区', '站点简介，显示在页脚与关于区块'),
('logo',          '',                   '站点 Logo 地址；留空时页头用站点名首字徽标兜底'),
('nav_items',     '[{"label":"首页","url":"/"},{"label":"攻略与资料","url":"/guide"},{"label":"社区板块","url":"/boards"},{"label":"公告","url":"/notice"}]', '页头/页脚导航菜单(JSON 数组)'),
('primary_color', '#5B5FE9',            '主题主色，注入用户端 CSS 变量'),
('dark_mode',     'false',              '是否默认深色模式'),
('copyright',     '',                   '页脚版权信息；留空时按站点名与年份生成');

INSERT INTO home_module (module_type, module_name, sort, enabled, config_json) VALUES
('banner',    '首页轮播', 1, 1, '{"autoplay":true,"interval":5000}'),
('notice',    '公告栏',   2, 1, '{"limit":5}'),
('hot_post',  '热门帖子', 3, 1, '{"limit":10}'),
('board_nav', '板块导航', 4, 1, '{}');

-- ==================== 板块与内容维度域 ====================

INSERT INTO board (id, name, intro, sort, status, current_version_id) VALUES
(1, '示例游戏', '一个用于演示的示例游戏板块，可在后台自由增删改。', 1, 1, NULL);

INSERT INTO board_version (id, board_id, version_no, version_name, release_time, update_note, status) VALUES
(1, 1, '1.2.0', '风起之时', '2026-06-18 10:00:00', '新增风元素角色与「秘境探索」玩法。', 1),
(2, 1, '1.3.0', '星海回响', '2026-09-10 10:00:00', '开放新地图「星海」；调整部分角色数值；修复若干已知问题。', 1);

UPDATE board SET current_version_id = 2 WHERE id = 1;

INSERT INTO progress_stage (id, board_id, stage_name, description, sort, status) VALUES
(1, 1, '新手期', '刚入坑，主线未通关', 1, 1),
(2, 1, '中期',   '主线通关，开始刷装备与深渊', 2, 1),
(3, 1, '满级',   '已满级，专注高难副本与配队研究', 3, 1);

-- ==================== 玩家与内容域 ====================

INSERT INTO member (id, username, password, nickname, intro, status) VALUES
(1, 'player1', '$2a$10$8Zz3hwyRKMhWbzHKIlfIluY32YC5vAa4E1zWlRt6X4r488BqRUsGq', '小明', '喜欢研究配队和刷本。', 1),
(2, 'player2', '$2a$10$8Zz3hwyRKMhWbzHKIlfIluY32YC5vAa4E1zWlRt6X4r488BqRUsGq', '阿远', '常年在新手期徘徊。', 1);

-- player1 公开自己在玩 1.3.0 满级；player2 选择不公开，且仍停留在旧版本 1.2.0
INSERT INTO member_game_progress (member_id, board_id, version_id, stage_id, is_public) VALUES
(1, 1, 2, 3, 1),
(2, 1, 1, 1, 0);

INSERT INTO tag (id, board_id, tag_name, tag_type, sort, status) VALUES
(1, NULL, '攻略', 1, 1, 1),
(2, NULL, '讨论', 1, 2, 1),
(3, NULL, '同人', 1, 3, 1),
(4, 1,    '配队', 2, 4, 1),
(5, 1,    '低配', 2, 5, 1);

-- status: 0待审 1已发布 2已下架；version_id/stage_id 为空表示通用帖
INSERT INTO post (id, board_id, member_id, version_id, stage_id, title, content, view_count, like_count, favorite_count, comment_count, status, audit_remark, is_top, create_time) VALUES
(1, 1, 1, 2,    3,    '1.3.0 满级配队推荐：三套主流阵容', '本文整理当前版本三套通用阵容，分别适配低配、中配与满配。', 128, 1, 1, 2, 1, NULL, 1, '2026-09-11 09:30:00'),
(2, 1, 1, 2,    NULL, '星海地图资源点整理', '星海新地图的宝箱与采集点位置汇总，按区域给出路线。', 76, 1, 0, 0, 1, NULL, 0, '2026-09-12 14:20:00'),
(3, 1, 2, 1,    1,    '1.2.0 新手期该先做什么？', '刚入坑建议先推主线到解锁日常，再考虑抽卡方向。', 310, 0, 0, 0, 1, NULL, 0, '2026-06-20 20:15:00'),
(4, 1, 2, 2,    2,    '关于新版本的几点疑问（待审核）', '新地图的掉落概率是不是调过了？感觉比上个版本低。', 0, 0, 0, 0, 0, NULL, 0, '2026-09-28 21:05:00'),
(5, 1, 1, NULL, NULL, '这个社区怎么用？版规与发帖说明', '发帖前请先选择所属版本与进度阶段，方便同好找到你的内容。', 521, 0, 0, 0, 1, NULL, 1, '2026-09-01 08:00:00'),
(6, 1, 2, 2,    3,    '（已下架示例）标题党测试帖', '这是一条用于演示「已下架」状态的帖子。', 12, 0, 0, 0, 2, '标题与内容不符，已下架', 0, '2026-09-15 11:00:00');

INSERT INTO post_tag (post_id, tag_id) VALUES
(1, 1), (1, 4), (2, 1), (3, 1), (4, 2), (5, 2), (6, 2);

-- 一级评论 root_id=0；回复的 root_id 指向所属一级评论
INSERT INTO comment (id, post_id, member_id, parent_id, root_id, content, like_count, status, create_time) VALUES
(1, 1, 2, 0, 0, '这套阵容我用过，确实稳，就是第三个角色练度要求有点高。', 1, 1, '2026-09-11 10:05:00'),
(2, 1, 1, 1, 1, '对，建议先把那个角色拉到 80 级再上，不然容易翻车。', 0, 1, '2026-09-11 10:30:00');

-- 冗余计数与下面两张表保持一致：post1 有 1 赞 1 收藏 2 评论，post2 有 1 赞
INSERT INTO post_like (post_id, member_id) VALUES (1, 2), (2, 2);

INSERT INTO post_favorite (post_id, member_id) VALUES (1, 2);

INSERT INTO report (post_id, member_id, reason, description, status, handle_user_id, handle_time) VALUES
(2, 1, '垃圾广告', '疑似推广外站，请核查。', 0, NULL, NULL),
(6, 1, '内容不实', '标题与内容完全不符。', 1, 1, '2026-09-15 15:00:00');

-- ==================== 运营域 ====================

INSERT INTO notice (id, title, content, type, sort, status) VALUES
(1, '欢迎来到示例游戏社区', '这里是公告内容，可在后台「站点配置」中管理。', 1, 1, 1),
(2, '1.3.0「星海回响」版本更新说明', '开放新地图「星海」，调整部分角色数值，详情见游戏内公告。', 2, 2, 1);
