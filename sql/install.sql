-- 一键安装：按正确顺序执行全部增量脚本
--
-- 用法（**必须在仓库根目录执行**，因为 source 的相对路径按客户端工作目录解析）：
--     mysql --default-character-set=utf8mb4 -u root -p < sql/install.sql
-- 或者在 mysql 客户端里：
--     source sql/install.sql
--
-- 前置：目标库必须**是空的**。schema.sql 用的是 CREATE TABLE（没有 IF NOT EXISTS、也不 DROP），
--       重复执行会在第一张已存在的表上直接报 1050 并中止——这是有意的，避免误删线上数据。
--       要清空重装：DROP DATABASE ssy_db; 再跑本脚本。
--
-- 执行完本脚本得到的是"平台本身"：表结构、原子/预设/组件库、后台菜单、登录注册页、
-- 以及一套旧版首页（会被下一步的模板覆盖）。
-- **默认社区模板（首页/攻略/板块/帖子/公告/个人主页）不在本脚本里**：
--   它由后端读 classpath:templates/default-community 建站，需要先启动后端，
--   再到后台「站点配置 → 创建社区」点一下（见 DEPLOY.md 第五节）。
--
-- 顺序不能随意调换，依赖关系见每段注释。

SET NAMES utf8mb4;

-- 1) 建库建表：21 张表（权限域/站点配置域/板块与内容维度域/玩家与内容域/运营域）
source sql/schema.sql

-- 2) 基础种子：后台账号、角色菜单、站点配置、示例板块/版本/阶段/标签/帖子、公告、home_module
--    必须先于 ui_builder.sql：后者要把 home_module 的记录迁移成首页组件树
source sql/seed.sql

-- 3) 轮播图表 + 后台「轮播图管理」菜单（表不在 schema.sql 里，是后续增量）
source sql/banner_slide.sql

-- 4) 用户端搭建域：ui_page / ui_component_type / ui_node 三张表 + 基础与业务组件种子
--    + 后台「页面搭建」菜单 + 首页页面（含 home_module → ui_node 的迁移）
source sql/ui_builder.sql

-- 5) 逻辑域：ui_logic_node_type / ui_preset / ui_flow 三张表 + 48 个原子 + 8 个内置预设
source sql/ui_logic.sql

-- 6) 登录注册页（/login、/register）+ preset.register + 给 act.navigate 补上 /login /register
--    依赖第 5 步的原子与预设表
source sql/ui_login.sql

-- 7) 内容读取页（/board/:id、/post/list、/post/:id）+ board_detail / post_list / post_detail 三个业务组件
source sql/ui_content_read.sql

-- 8) 帖子互动与评论区组件（post_actions / comment_list）+ 往 /post/:id 追加节点
--    必须排在第 7 步之后：它靠 ui_page.page_key='post-detail' 找页面，页面还没建就插不进去
source sql/ui_comment.sql

-- 9) 首页节点树（旧版；默认模板建站时会覆盖这版首页）
source sql/ui_home.sql

-- 10) 给 board_nav 组件开放内容属性（展示数量/列数/是否显示封面与简介）
source sql/ui_board_nav_props.sql

-- 11) 后台「原子库」「预设库」菜单（此前只存在于开发库，全新安装会看不到这两个入口）
source sql/admin_logic_menus.sql

-- 12) 后台「创建社区」菜单（模板建站入口）
source sql/ui_template.sql

-- 13) 后台「公告管理」菜单（公告 CRUD 接口在 NoticeController，见 /admin/notice/**）
source sql/admin_notice.sql

-- 14) 预设库增补与修正（拆 preset.interact、停用两个依赖未实现接口的预设、补 5 个新预设）
source sql/ui_preset_v2.sql

-- ==================== 自检 ====================
-- 期望结果（括号内为数量）：
--   表（28）= schema 21 + banner_slide 1 + 搭建域 3 + 逻辑域 3
--   组件类型（22）：SQL 只装到 22；默认社区模板建站时会再补 6 个业务组件（游戏简介/版本更新/
--                    公告列表/公告详情/个人主页/发帖表单）与给 11 个既有组件补开放属性
--                    （含给 button 开放 linkType/linkUrl/openInNewTab），共 28 个
--   逻辑原子（48）、预设（13，其中 10 启用）、后台菜单（21）、用户端页面（6）
SELECT '表' AS 项目, COUNT(*) AS 数量 FROM information_schema.tables WHERE table_schema = DATABASE()
UNION ALL SELECT '组件类型(模板前)', COUNT(*) FROM ui_component_type
UNION ALL SELECT '逻辑原子', COUNT(*) FROM ui_logic_node_type
UNION ALL SELECT '预设(总)', COUNT(*) FROM ui_preset
UNION ALL SELECT '预设(启用)', COUNT(*) FROM ui_preset WHERE status = 1
UNION ALL SELECT '后台菜单', COUNT(*) FROM sys_menu
UNION ALL SELECT '用户端页面', COUNT(*) FROM ui_page
UNION ALL SELECT '站点配置项', COUNT(*) FROM site_config
UNION ALL SELECT '示例板块', COUNT(*) FROM board;
-- 期望：表 28 / 组件类型 22 / 原子 48 / 预设 13 / 启用预设 10 / 菜单 21 / 页面 6 / 站点配置 8 / 板块 1
--
-- 装完的下一步（DEPLOY.md 第五节）：
--   1. 启动后端（编译需 JDK 21）
--   2. 打开管理端 → 登录 admin / admin123 → 站点配置 → 创建社区 → 填社区名称与游戏名称 → 创建
--   3. 打开用户端，能看到首页、攻略、板块、帖子、公告、个人主页
