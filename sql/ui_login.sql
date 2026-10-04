-- 用户端登录 / 注册页面（v1.1：基础组件组合 + 页面逻辑图）
--
-- ⚠ 本脚本会重建 /login 与 /register 两页：**先删掉这两页现有的全部节点**再插入初版，
--   并删除 v1.0 的两个业务组件（login_form / register_form）。若之后在搭建器里改过这两页，
--   重跑会覆盖你的改动——重跑前请先确认。
--
-- 可重复执行：删节点 / 删组件类型是幂等的；页面与流程图、注册预设都用 ON DUPLICATE KEY UPDATE。
-- 设计文档：用户端登录注册-搭建方案设计.json（v1.1）
--
-- 页面结构（登录；注册同构，多一个昵称输入框）：
--   表单(form) → 文本标题 / 输入框用户名 / 输入框密码 / 按钮提交(buttonType=submit) / 按钮去注册
-- 提交逻辑是**页面图**（FlowEditor 可画可改），不是代码：
--   on.submit(表单) → data.componentValue×N → preset.login / preset.register
--                   → cond.isEmpty(结果) → 分支 → 成功才 act.navigate(跳首页)

SET NAMES utf8mb4;
USE ssy_db;

-- ==================== 0、清掉 v1.0 的业务组件 ====================

DELETE FROM ui_node WHERE type_code IN ('login_form', 'register_form');
DELETE FROM ui_component_type WHERE type_code IN ('login_form', 'register_form');

-- ==================== 1、两个页面（存在则更新路由） ====================

INSERT INTO ui_page (page_key, page_name, route_path, is_home, status, remark)
VALUES ('login', '登录', '/login', 0, 1, '玩家登录页：基础组件组合 + submit 图'),
       ('register', '注册', '/register', 0, 1, '玩家注册页：基础组件组合 + submit 图')
ON DUPLICATE KEY UPDATE
  page_name = VALUES(page_name), route_path = VALUES(route_path), status = VALUES(status);

SET @loginPageId    = (SELECT id FROM ui_page WHERE page_key = 'login');
SET @registerPageId = (SELECT id FROM ui_page WHERE page_key = 'register');

-- ==================== 2、页面节点树 ====================
-- 复用几份样式，避免同一串 JSON 写五遍

SET @formStyle  = '{"width":"100%","maxWidth":"360px","align":"center","padding":"24px","backgroundColor":"var(--site-bg-card)","radius":"12px","boxShadow":"sm","flexDirection":"column","alignItems":"stretch","gap":"14px"}';
SET @titleStyle = '{"fontSize":"20px","fontWeight":"600","textColor":"var(--site-text-primary)","textAlign":"center"}';
SET @inputStyle = '{"width":"100%","padding":"9px 12px","backgroundColor":"var(--site-bg-page)","borderWidth":"1px","borderStyle":"solid","borderColor":"var(--site-border)","radius":"8px","fontSize":"14px","textColor":"var(--site-text-primary)","cursor":"text"}';
SET @submitStyle = '{"width":"100%","padding":"10px 16px","backgroundColor":"var(--site-primary)","textColor":"#FFFFFF","fontSize":"14px","fontWeight":"600","textAlign":"center","radius":"8px","cursor":"pointer"}';
-- 互跳按钮按"文字链接"观感配：不要按钮默认带来的背景/阴影/悬停效果
SET @linkStyle  = '{"width":"auto","align":"center","padding":"4px","radius":"0","fontSize":"13px","textColor":"var(--site-primary)","backgroundColor":"var(--site-bg-card)","texture":"transparent","boxShadow":"none","hoverEffect":"none","clickEffect":"none","cursor":"pointer"}';

DELETE FROM ui_node WHERE page_id IN (@loginPageId, @registerPageId);

-- 2.1 表单容器（两个页面的根节点；on.submit 的 nodeId 指向它）
INSERT INTO ui_node (page_id, parent_id, type_code, node_name, sort, visible, style_json, content_json, behavior_json, remark)
VALUES
(@loginPageId, NULL, 'form', '登录表单', 0, 1, @formStyle, '{}', '{}',
 '表单容器：提交时触发本页 on.submit 的图（nodeId 指向它）'),
(@registerPageId, NULL, 'form', '注册表单', 0, 1, @formStyle, '{}', '{}',
 '表单容器：提交时触发本页 on.submit 的图（nodeId 指向它）');

-- 2.2 登录页子节点
INSERT INTO ui_node (page_id, parent_id, type_code, node_name, sort, visible, style_json, content_json, behavior_json, remark)
SELECT f.page_id, f.id, v.type_code, v.node_name, v.sort, 1, v.style_json, v.content_json, v.behavior_json, NULL
FROM ui_node f
JOIN (
            SELECT 'text'   AS type_code, '标题'   AS node_name, 0 AS sort, @titleStyle  AS style_json, '{"text":"登录"}' AS content_json, '{}' AS behavior_json
  UNION ALL SELECT 'input',                '用户名',              1,          @inputStyle,  '{"placeholder":"请输入用户名","inputType":"text"}',     '{}'
  UNION ALL SELECT 'input',                '密码',                2,          @inputStyle,  '{"placeholder":"请输入密码","inputType":"password"}',   '{}'
  UNION ALL SELECT 'button',               '提交',                3,          @submitStyle, '{"text":"登录","buttonType":"submit"}',                 '{}'
  UNION ALL SELECT 'button',               '去注册',              4,          @linkStyle,   '{"text":"还没有账号？立即注册"}',                        '{"onClickAction":{"action":"navigate","url":"/register"}}'
) v
WHERE f.page_id = @loginPageId AND f.type_code = 'form';

-- 2.3 注册页子节点（多一个可选的昵称输入框）
INSERT INTO ui_node (page_id, parent_id, type_code, node_name, sort, visible, style_json, content_json, behavior_json, remark)
SELECT f.page_id, f.id, v.type_code, v.node_name, v.sort, 1, v.style_json, v.content_json, v.behavior_json, NULL
FROM ui_node f
JOIN (
            SELECT 'text'   AS type_code, '标题'   AS node_name, 0 AS sort, @titleStyle  AS style_json, '{"text":"注册"}' AS content_json, '{}' AS behavior_json
  UNION ALL SELECT 'input',                '用户名',              1,          @inputStyle,  '{"placeholder":"4-20 位字母、数字或下划线","inputType":"text"}',      '{}'
  UNION ALL SELECT 'input',                '密码',                2,          @inputStyle,  '{"placeholder":"6-20 位密码","inputType":"password"}',              '{}'
  UNION ALL SELECT 'input',                '昵称',                3,          @inputStyle,  '{"placeholder":"昵称（可选，默认同用户名）","inputType":"text"}',     '{}'
  UNION ALL SELECT 'button',               '提交',                4,          @submitStyle, '{"text":"注册","buttonType":"submit"}',                             '{}'
  UNION ALL SELECT 'button',               '去登录',              5,          @linkStyle,   '{"text":"已有账号？立即登录"}',                                      '{"onClickAction":{"action":"navigate","url":"/login"}}'
) v
WHERE f.page_id = @registerPageId AND f.type_code = 'form';

-- ==================== 3、注册预设（preset.register） ====================
-- 与 preset.login 同构：非空校验 → 分支 → 注册 / 提示；注册即登录，act.register 已带 savesToken。

INSERT INTO ui_preset
(preset_code, preset_name, version, is_builtin, category, inputs_schema, outputs_schema, graph_json, status, remark) VALUES
('preset.register', '用户注册', 1, 1, 'member',
 '[{"key":"username","type":"text","required":true},{"key":"password","type":"text","required":true},{"key":"nickname","type":"text"}]',
 '[{"key":"result","type":"result"}]',
 '{"nodes":[{"id":"p1","typeCode":"cond.isValid","label":"用户名非空","config":{"value":"$param.username","rule":"notBlank"}},{"id":"p2","typeCode":"cond.isValid","label":"密码非空","config":{"value":"$param.password","rule":"notBlank"}},{"id":"p3","typeCode":"op.and","label":"都非空","config":{"a":"$ref.p1.boolean","b":"$ref.p2.boolean"}},{"id":"p4","typeCode":"branch","label":"校验通过?","config":{"cond":"$ref.p3.boolean"}},{"id":"p5","typeCode":"act.register","label":"注册","config":{"username":"$param.username","password":"$param.password","nickname":"$param.nickname"}},{"id":"p6","typeCode":"act.toast","label":"已注册","config":{"message":"注册成功，已自动登录","type":"success"}},{"id":"p7","typeCode":"act.toast","label":"提示补全","config":{"message":"请填写用户名和密码","type":"warning"}}],"edges":[{"from":{"node":"p1","port":"out"},"to":{"node":"p2","port":"signal"}},{"from":{"node":"p2","port":"out"},"to":{"node":"p3","port":"signal"}},{"from":{"node":"p3","port":"out"},"to":{"node":"p4","port":"signal"}},{"from":{"node":"p4","port":"true"},"to":{"node":"p5","port":"signal"}},{"from":{"node":"p5","port":"out"},"to":{"node":"p6","port":"signal"}},{"from":{"node":"p4","port":"false"},"to":{"node":"p7","port":"signal"}}],"exports":{"result":"p5.result"}}',
 1, NULL)
ON DUPLICATE KEY UPDATE
  preset_name = VALUES(preset_name),
  is_builtin = VALUES(is_builtin),
  category = VALUES(category),
  inputs_schema = VALUES(inputs_schema),
  outputs_schema = VALUES(outputs_schema),
  graph_json = VALUES(graph_json);

-- ==================== 4、两页的 submit 图 ====================
-- 图里要引用节点的真实 id，而 id 是自增的，所以先按"页面 + 节点名"取出来再拼 JSON。
-- 失败路径：act.login / act.register 抛错时引擎会调 host.onError 弹出后端消息，
-- 分支的"结果为空"支不接任何节点，天然不跳转。

SET @loginFormId   = (SELECT id FROM ui_node WHERE page_id = @loginPageId    AND node_name = '登录表单');
SET @loginUserNode = (SELECT id FROM ui_node WHERE page_id = @loginPageId    AND node_name = '用户名');
SET @loginPassNode = (SELECT id FROM ui_node WHERE page_id = @loginPageId    AND node_name = '密码');
SET @regFormId     = (SELECT id FROM ui_node WHERE page_id = @registerPageId AND node_name = '注册表单');
SET @regUserNode   = (SELECT id FROM ui_node WHERE page_id = @registerPageId AND node_name = '用户名');
SET @regPassNode   = (SELECT id FROM ui_node WHERE page_id = @registerPageId AND node_name = '密码');
SET @regNickNode   = (SELECT id FROM ui_node WHERE page_id = @registerPageId AND node_name = '昵称');

INSERT INTO ui_flow (page_id, flow_key, flow_name, graph_json, status, remark)
VALUES (@loginPageId, 'submit', '登录提交',
  CONCAT('{"nodes":[',
    '{"id":"s1","typeCode":"on.submit","label":"提交表单","config":{"nodeId":', @loginFormId, '}},',
    '{"id":"s2","typeCode":"data.componentValue","label":"读用户名","config":{"nodeId":', @loginUserNode, ',"field":"value"}},',
    '{"id":"s3","typeCode":"data.componentValue","label":"读密码","config":{"nodeId":', @loginPassNode, ',"field":"value"}},',
    '{"id":"s4","typeCode":"preset","presetCode":"preset.login","presetVersion":1,"label":"用户登录","config":{"username":"$ref.s2.any","password":"$ref.s3.any"}},',
    '{"id":"s5","typeCode":"cond.isEmpty","label":"登录失败?","config":{"value":"$ref.s4.result"}},',
    '{"id":"s6","typeCode":"branch","label":"结果为空?","config":{"cond":"$ref.s5.boolean"}},',
    '{"id":"s7","typeCode":"act.navigate","label":"回首页","config":{"to":"/"}}',
  '],"edges":[',
    '{"from":{"node":"s1","port":"out"},"to":{"node":"s2","port":"signal"}},',
    '{"from":{"node":"s2","port":"out"},"to":{"node":"s3","port":"signal"}},',
    '{"from":{"node":"s3","port":"out"},"to":{"node":"s4","port":"signal"}},',
    '{"from":{"node":"s4","port":"out"},"to":{"node":"s5","port":"signal"}},',
    '{"from":{"node":"s5","port":"out"},"to":{"node":"s6","port":"signal"}},',
    '{"from":{"node":"s6","port":"false"},"to":{"node":"s7","port":"signal"}}',
  '],"exports":{}}'),
  1, '提交 → 读两个输入框 → preset.login → 判结果为空分支 → 成功才跳首页')
ON DUPLICATE KEY UPDATE graph_json = VALUES(graph_json), flow_name = VALUES(flow_name), remark = VALUES(remark);

INSERT INTO ui_flow (page_id, flow_key, flow_name, graph_json, status, remark)
VALUES (@registerPageId, 'submit', '注册提交',
  CONCAT('{"nodes":[',
    '{"id":"s1","typeCode":"on.submit","label":"提交表单","config":{"nodeId":', @regFormId, '}},',
    '{"id":"s2","typeCode":"data.componentValue","label":"读用户名","config":{"nodeId":', @regUserNode, ',"field":"value"}},',
    '{"id":"s3","typeCode":"data.componentValue","label":"读密码","config":{"nodeId":', @regPassNode, ',"field":"value"}},',
    '{"id":"s4","typeCode":"data.componentValue","label":"读昵称","config":{"nodeId":', @regNickNode, ',"field":"value"}},',
    '{"id":"s5","typeCode":"preset","presetCode":"preset.register","presetVersion":1,"label":"用户注册","config":{"username":"$ref.s2.any","password":"$ref.s3.any","nickname":"$ref.s4.any"}},',
    '{"id":"s6","typeCode":"cond.isEmpty","label":"注册失败?","config":{"value":"$ref.s5.result"}},',
    '{"id":"s7","typeCode":"branch","label":"结果为空?","config":{"cond":"$ref.s6.boolean"}},',
    '{"id":"s8","typeCode":"act.navigate","label":"回首页","config":{"to":"/"}}',
  '],"edges":[',
    '{"from":{"node":"s1","port":"out"},"to":{"node":"s2","port":"signal"}},',
    '{"from":{"node":"s2","port":"out"},"to":{"node":"s3","port":"signal"}},',
    '{"from":{"node":"s3","port":"out"},"to":{"node":"s4","port":"signal"}},',
    '{"from":{"node":"s4","port":"out"},"to":{"node":"s5","port":"signal"}},',
    '{"from":{"node":"s5","port":"out"},"to":{"node":"s6","port":"signal"}},',
    '{"from":{"node":"s6","port":"out"},"to":{"node":"s7","port":"signal"}},',
    '{"from":{"node":"s7","port":"false"},"to":{"node":"s8","port":"signal"}}',
  '],"exports":{}}'),
  1, '提交 → 读三个输入框（昵称可空）→ preset.register → 判结果为空分支 → 成功才跳首页')
ON DUPLICATE KEY UPDATE graph_json = VALUES(graph_json), flow_name = VALUES(flow_name), remark = VALUES(remark);

-- ==================== 5、把登录页地址接进逻辑层 ====================
-- 两处都用精确字符串替换，重复执行不会二次生效（第二次匹配不到）。

UPDATE ui_logic_node_type
SET inputs_schema = REPLACE(inputs_schema,
        '["/","/board/:id","/post/:id","/profile","back"]',
        '["/","/login","/register","/board/:id","/post/:id","/profile","back"]')
WHERE type_code = 'act.navigate';

UPDATE ui_preset
SET graph_json = REPLACE(graph_json, '"config":{"to":"/"}', '"config":{"to":"/login"}'),
    remark = '未登录分支跳 /login'
WHERE preset_code IN ('preset.profile', 'preset.interact')
  AND version = 1;
