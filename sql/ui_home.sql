-- 用户端首页（/）：米游社式版面，全由管理端搭建器搭出来的节点树
--
-- ⚠ 本脚本会重建首页：**先删掉首页现有的全部节点**再插入初版。
--   若之后在搭建器里改过首页，重跑会覆盖你的改动——重跑前请先确认。
--   本脚本只动 ui_node，不动首页已有的逻辑图（ui_flow）。
--
-- 可重复执行：节点是按页先删后插；页面行用 ON DUPLICATE KEY UPDATE。
-- 页面结构（自上而下）：
--   分区快捷条(row) → 三个按钮，用 scrollTo 滚到下面三个区块
--   首页轮播(banner) → 图与标题在后台「站点配置 → 轮播图管理」维护
--   社区公告(notice) → notice 表已发布公告
--   内容两栏(row) → 左 65% 热门帖流(hot_post) ／ 右 33% 人气板块(board_nav) + 关于社区
--
-- 三个业务组件（banner / notice / hot_post）与 board_nav 都取真数据，
-- 对应接口 /app/banner/list、/app/notice/list、/app/post/hot 均已对匿名放行。

SET NAMES utf8mb4;
USE ssy_db;

SET @homePageId = (SELECT id FROM ui_page WHERE page_key = 'home');

-- 复用几份样式，避免同一串 JSON 写多遍
SET @navRowStyle    = '{"flexDirection":"row","wrap":true,"gap":"8px","justify":"start","alignItems":"center","padding":"0"}';
SET @tabBtnStyle    = '{"backgroundColor":"var(--site-bg-card)","textColor":"var(--site-text-secondary)","fontSize":"14px","fontWeight":"400","padding":"8px","radius":"6px","texture":"transparent","boxShadow":"none","hoverEffect":"none","clickEffect":"none","cursor":"pointer"}';
SET @bannerStyle    = '{"height":"260px","radius":"12px"}';
SET @noticeStyle    = '{"padding":"14px","backgroundColor":"var(--site-bg-card)","radius":"10px","fontSize":"14px"}';
SET @colsRowStyle   = '{"flexDirection":"row","wrap":true,"gap":"16px","justify":"start","alignItems":"stretch","padding":"0"}';
SET @colStyle       = '{"width":"65%","padding":"18px","backgroundColor":"var(--site-bg-card)","radius":"10px","flexDirection":"column","justify":"start","alignItems":"stretch","gap":"12px"}';
SET @sideColStyle   = '{"width":"33%","padding":"18px","backgroundColor":"var(--site-bg-card)","radius":"10px","flexDirection":"column","justify":"start","alignItems":"stretch","gap":"12px"}';
SET @heading18Style = '{"fontSize":"18px","fontWeight":"600"}';
SET @heading16Style = '{"fontSize":"16px","fontWeight":"600"}';
SET @subTextStyle   = '{"fontSize":"13px","textColor":"var(--site-text-secondary)"}';
SET @noPaddingStyle = '{"padding":"0"}';
SET @dividerStyle   = '{"borderWidth":"1px","borderStyle":"solid","borderColor":"var(--site-border)","marginTop":"4px","marginBottom":"4px"}';

DELETE FROM ui_node WHERE page_id = @homePageId;

-- ==================== 1、第一层：快捷条 / 轮播 / 公告 / 两栏行 ====================

INSERT INTO ui_node (page_id, parent_id, type_code, node_name, sort, visible, style_json, content_json, behavior_json, remark)
VALUES
(@homePageId, NULL, 'row', '分区快捷条', 0, 1, @navRowStyle, '{}', '{}',
 '三个快捷入口不做跳转，用 scrollTo 滚到下方对应区块'),
(@homePageId, NULL, 'banner', '首页轮播', 1, 1, @bannerStyle, '{"autoplay":true,"interval":5000,"limit":5}', '{}',
 '图与标题在后台「站点配置 → 轮播图管理」维护，这里只定高度和圆角'),
(@homePageId, NULL, 'notice', '社区公告', 2, 1, @noticeStyle, '{"limit":3}', '{}',
 '取 notice 表已发布公告'),
(@homePageId, NULL, 'row', '内容两栏', 3, 1, @colsRowStyle, '{}', '{}',
 '左 65% 帖流、右 33% 侧栏；留 2% 余量给 gap，窄屏自动换行');

SET @navRow = (SELECT id FROM ui_node WHERE page_id = @homePageId AND node_name = '分区快捷条');
SET @colsRow = (SELECT id FROM ui_node WHERE page_id = @homePageId AND node_name = '内容两栏');

-- ==================== 2、第二层：左右两栏容器 ====================

INSERT INTO ui_node (page_id, parent_id, type_code, node_name, sort, visible, style_json, content_json, behavior_json, remark)
VALUES
(@homePageId, @colsRow, 'container', '左栏-推荐内容', 0, 1, @colStyle, '{}', '{}', ''),
(@homePageId, @colsRow, 'container', '右栏-侧栏', 1, 1, @sideColStyle, '{}', '{}', '');

SET @leftCol = (SELECT id FROM ui_node WHERE page_id = @homePageId AND node_name = '左栏-推荐内容');
SET @rightCol = (SELECT id FROM ui_node WHERE page_id = @homePageId AND node_name = '右栏-侧栏');

-- ==================== 3、第三层：两栏里的叶子节点（含三个业务组件） ====================

INSERT INTO ui_node (page_id, parent_id, type_code, node_name, sort, visible, style_json, content_json, behavior_json, remark)
VALUES
(@homePageId, @leftCol, 'heading', '左栏标题', 0, 1, @heading18Style, '{"text":"热门帖子"}', '{}', ''),
(@homePageId, @leftCol, 'text', '左栏副标题', 1, 1, @subTextStyle, '{"text":"按置顶、点赞与浏览排出来的社区热帖"}', '{}', ''),
(@homePageId, @leftCol, 'hot_post', '热门帖列表', 2, 1, @noPaddingStyle, '{"limit":6}', '{}',
 '取 post 表已发布帖；帖子详情页未做，点击先弹提示'),
(@homePageId, @rightCol, 'heading', '侧栏标题-板块', 0, 1, @heading16Style, '{"text":"人气板块"}', '{}', ''),
(@homePageId, @rightCol, 'board_nav', '板块导航', 1, 1, @noPaddingStyle, '{"limit":4,"columns":1,"showCover":true,"showIntro":true}', '{}',
 '取 /app/board/list 的开放板块，点击进 /board/:id'),
(@homePageId, @rightCol, 'divider', '侧栏分隔线', 2, 1, @dividerStyle, '{}', '{}', ''),
(@homePageId, @rightCol, 'heading', '侧栏标题-关于', 3, 1, @heading16Style, '{"text":"关于社区"}', '{}', ''),
(@homePageId, @rightCol, 'text', '社区说明', 4, 1, @subTextStyle,
 '{"text":"以板块为单位组织内容，用游戏版本与玩家进度两条线索引帖子；发帖时选好版本，帖子会自动带上版本角标。"}', '{}',
 '静态说明文案：站点简介在页头已展示，这里补充玩法说明');

-- ==================== 4、快捷条按钮：target 指向上面插好的真实节点 id ====================

SET @noticeNodeId = (SELECT id FROM ui_node WHERE page_id = @homePageId AND node_name = '社区公告');
SET @hotPostNodeId = (SELECT id FROM ui_node WHERE page_id = @homePageId AND node_name = '热门帖列表');
SET @boardNavNodeId = (SELECT id FROM ui_node WHERE page_id = @homePageId AND node_name = '板块导航');

INSERT INTO ui_node (page_id, parent_id, type_code, node_name, sort, visible, style_json, content_json, behavior_json, remark)
VALUES
(@homePageId, @navRow, 'button', '快捷入口-热门推荐', 0, 1, @tabBtnStyle, '{"text":"热门推荐"}',
 CONCAT('{"onClickAction":{"action":"scrollTo","target":', @hotPostNodeId, '}}'), '滚到左栏热门帖子'),
(@homePageId, @navRow, 'button', '快捷入口-社区公告', 1, 1, @tabBtnStyle, '{"text":"社区公告"}',
 CONCAT('{"onClickAction":{"action":"scrollTo","target":', @noticeNodeId, '}}'), '滚到公告条'),
(@homePageId, @navRow, 'button', '快捷入口-人气板块', 2, 1, @tabBtnStyle, '{"text":"人气板块"}',
 CONCAT('{"onClickAction":{"action":"scrollTo","target":', @boardNavNodeId, '}}'), '滚到右栏板块导航');
