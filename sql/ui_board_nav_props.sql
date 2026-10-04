-- 板块导航业务组件增量：给 ui_component_type.board_nav 开放内容属性
-- 背景：/app/board/list 上线后，board_nav 需要"展示数量/列数/是否显示封面/简介"几个开关，
--       这些 key 已登记进 ComponentPropertyRegistry，这里把它们挂到该组件类型的 content_schema。
-- 说明：本文件只改 board_nav 一行，可重复执行；全量种子见 sql/ui_builder.sql。

SET NAMES utf8mb4;
USE ssy_db;

UPDATE ui_component_type
SET content_schema = '["limit","columns","showCover","showIntro"]',
    default_content = '{"limit":8,"columns":3,"showCover":true,"showIntro":true}'
WHERE type_code = 'board_nav';
