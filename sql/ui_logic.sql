-- 用户端图形化搭建 v3 · 阶段 3（逻辑层）增量脚本
-- 内容：3 张新表（ui_logic_node_type / ui_preset / ui_flow）、原子库种子、内置预设种子
-- 说明：本文件的"原子库种子"与"内置预设种子"两节均**由设计文档
--       用户端模块化样式设计.json 的 atomCatalog / presetLibrary 生成**（一次性生成器，
--       未入库），改设计后请重新生成或手工同步本节；DDL 用 IF NOT EXISTS、种子用
--       ON DUPLICATE KEY UPDATE，整份文件可重复执行。稳定后应并入 sql/schema.sql 与 seed.sql。
--
-- 原子种子的端口约定（编辑器与后端校验都按它走）：
--   * 触发原子（kind=trigger）是图的根，没有输入，输出一个 out(signal)
--   * 其余原子隐式带一个输入 signal(signal) 与一个输出 out(signal)，控制流沿 signal 推
--   * branch 例外：输入 cond(boolean)，输出 true(signal) / false(signal) 两支
--   * 数据端口用设计里的名字做端口名与类型（如 listPost、pageInfo、result）
--   * inputs_schema 是"可连线的数据输入"，config_schema 是"节点自己的设置项"（不连线）

SET NAMES utf8mb4;
USE ssy_db;

-- ==================== 一、逻辑层（3 张表） ====================

CREATE TABLE IF NOT EXISTS ui_logic_node_type (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  type_code      VARCHAR(60) NOT NULL COMMENT '原子标识,如 data.postList',
  type_name      VARCHAR(50) NOT NULL COMMENT '原子名称',
  kind           VARCHAR(20) NOT NULL COMMENT 'trigger/source/operator/condition/action/ui',
  category       VARCHAR(20) COMMENT '分组中文名(触发/数据源/算子/条件/动作/界面动作)',
  runner         VARCHAR(10) NOT NULL DEFAULT 'ui' COMMENT 'api=调后端接口 / ui=前端本地动作',
  source_api     VARCHAR(120) COMMENT 'runner=api 时对应的接口',
  inputs_schema  TEXT COMMENT '可连线输入端口(JSON 数组)',
  outputs_schema TEXT COMMENT '输出端口(JSON 数组)',
  config_schema  TEXT COMMENT '节点自身设置项(JSON 数组,不连线)',
  preview_icon   VARCHAR(50) COMMENT '编辑器图标',
  sort           INT NOT NULL DEFAULT 0 COMMENT '排序',
  status         TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  remark         VARCHAR(200) COMMENT '备注',
  create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_logic_type_code (type_code)
) ENGINE=InnoDB COMMENT='原子库(逻辑节点类型)表';

CREATE TABLE IF NOT EXISTS ui_preset (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  preset_code    VARCHAR(60) NOT NULL COMMENT '预设标识,如 preset.login',
  preset_name    VARCHAR(50) NOT NULL COMMENT '预设名称',
  version        INT NOT NULL DEFAULT 1 COMMENT '版本,引用方锁定版本',
  is_builtin     TINYINT NOT NULL DEFAULT 0 COMMENT '1内置(只读,可复制为自定义)',
  category       VARCHAR(20) COMMENT '分组',
  inputs_schema  TEXT COMMENT '预设入参(JSON 数组,图内以 $param.x 引用)',
  outputs_schema TEXT COMMENT '预设出参(JSON 数组)',
  graph_json     TEXT COMMENT '原子团子图(JSON:nodes+edges)',
  status         TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  remark         VARCHAR(200) COMMENT '备注',
  create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_preset_ver (preset_code, version)
) ENGINE=InnoDB COMMENT='原子团/预设库表';

CREATE TABLE IF NOT EXISTS ui_flow (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  page_id     BIGINT NOT NULL COMMENT '所属页面(逻辑外键 ui_page.id)',
  flow_key    VARCHAR(30) NOT NULL COMMENT '图标识,如 init / submit',
  flow_name   VARCHAR(50) COMMENT '图名称',
  graph_json  TEXT COMMENT '逻辑图(JSON:nodes+edges)',
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  remark      VARCHAR(200) COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_page_flow (page_id, flow_key)
) ENGINE=InnoDB COMMENT='页面逻辑图表';

-- ==================== 二、原子库种子（44 条，由设计文档 atomCatalog 生成） ====================

INSERT INTO ui_logic_node_type
(type_code, type_name, kind, category, runner, source_api, inputs_schema, outputs_schema, config_schema, preview_icon, sort, status, remark) VALUES
('on.pageEnter', '进入页面', 'trigger', '触发', 'ui', NULL, '[]', '[{"key":"out","type":"signal"}]', '[]', 'VideoPlay', 100, 1, NULL),
('on.click', '点击组件', 'trigger', '触发', 'ui', NULL, '[]', '[{"key":"out","type":"signal"}]', '[{"key":"nodeId","type":"nodeRef","required":true}]', 'VideoPlay', 101, 1, NULL),
('on.submit', '提交表单', 'trigger', '触发', 'ui', NULL, '[]', '[{"key":"out","type":"signal"}]', '[{"key":"nodeId","type":"nodeRef","required":true}]', 'VideoPlay', 102, 1, NULL),
('data.boardList', '板块列表', 'source', '数据源', 'api', 'GET /app/board/list', '[{"key":"signal","type":"signal"},{"key":"onlyOpen","type":"boolean"}]', '[{"key":"listBoard","type":"listBoard"},{"key":"out","type":"signal"}]', '[]', 'Download', 203, 1, NULL),
('data.boardDetail', '板块详情', 'source', '数据源', 'api', 'GET /app/board/{boardId}', '[{"key":"signal","type":"signal"},{"key":"boardId","type":"number","required":true}]', '[{"key":"board","type":"board"},{"key":"listBoardVersion","type":"listBoardVersion"},{"key":"listProgressStage","type":"listProgressStage"},{"key":"out","type":"signal"}]', '[]', 'Download', 204, 1, NULL),
('data.postList', '帖子列表', 'source', '数据源', 'api', 'GET /app/post/page', '[{"key":"signal","type":"signal"},{"key":"boardId","type":"number"},{"key":"versionId","type":"number"},{"key":"stageId","type":"number"},{"key":"tagId","type":"number"},{"key":"keyword","type":"text"},{"key":"sort","type":"text","options":["latest","hot"]},{"key":"page","type":"number"},{"key":"size","type":"number"}]', '[{"key":"listPost","type":"listPost"},{"key":"pageInfo","type":"pageInfo"},{"key":"out","type":"signal"}]', '[]', 'Download', 205, 1, NULL),
('data.postDetail', '帖子详情', 'source', '数据源', 'api', 'GET /app/post/{postId}', '[{"key":"signal","type":"signal"},{"key":"postId","type":"number","required":true}]', '[{"key":"post","type":"post"},{"key":"out","type":"signal"}]', '[]', 'Download', 206, 1, NULL),
('data.commentList', '评论列表', 'source', '数据源', 'api', 'GET /app/comment/page', '[{"key":"signal","type":"signal"},{"key":"postId","type":"number","required":true},{"key":"page","type":"number"},{"key":"size","type":"number"}]', '[{"key":"listComment","type":"listComment"},{"key":"pageInfo","type":"pageInfo"},{"key":"out","type":"signal"}]', '[]', 'Download', 207, 1, NULL),
('data.noticeList', '公告列表', 'source', '数据源', 'api', 'GET /app/notice/list', '[{"key":"signal","type":"signal"},{"key":"limit","type":"number"}]', '[{"key":"listNotice","type":"listNotice"},{"key":"out","type":"signal"}]', '[]', 'Download', 208, 1, NULL),
('data.currentMember', '当前用户', 'source', '数据源', 'api', 'GET /app/member/me', '[{"key":"signal","type":"signal"}]', '[{"key":"member","type":"member"},{"key":"out","type":"signal"}]', '[]', 'Download', 209, 1, NULL),
('data.myPosts', '我的帖子', 'source', '数据源', 'api', 'GET /app/member/my-posts', '[{"key":"signal","type":"signal"},{"key":"page","type":"number"},{"key":"size","type":"number"}]', '[{"key":"listPost","type":"listPost"},{"key":"pageInfo","type":"pageInfo"},{"key":"out","type":"signal"}]', '[]', 'Download', 210, 1, NULL),
('data.myFavorites', '我的收藏', 'source', '数据源', 'api', 'GET /app/member/my-favorites', '[{"key":"signal","type":"signal"},{"key":"page","type":"number"},{"key":"size","type":"number"}]', '[{"key":"listPost","type":"listPost"},{"key":"pageInfo","type":"pageInfo"},{"key":"out","type":"signal"}]', '[]', 'Download', 211, 1, NULL),
('data.myReports', '我的举报', 'source', '数据源', 'api', 'GET /app/member/my-reports', '[{"key":"signal","type":"signal"},{"key":"page","type":"number"}]', '[{"key":"listReport","type":"listReport"},{"key":"pageInfo","type":"pageInfo"},{"key":"out","type":"signal"}]', '[]', 'Download', 212, 1, NULL),
('data.myProgress', '我的游戏进度', 'source', '数据源', 'api', 'GET /app/member/progress', '[{"key":"signal","type":"signal"},{"key":"boardId","type":"number"}]', '[{"key":"any","type":"any"},{"key":"out","type":"signal"}]', '[]', 'Download', 213, 1, NULL),
('data.runtime', '运行环境', 'source', '数据源', 'ui', NULL, '[{"key":"signal","type":"signal"}]', '[{"key":"any","type":"any"},{"key":"out","type":"signal"}]', '[{"key":"key","type":"text","options":["routeParam.id","query.k","page"]}]', 'Download', 214, 1, NULL),
('data.componentValue', '读组件当前值', 'source', '数据源', 'ui', NULL, '[{"key":"signal","type":"signal"}]', '[{"key":"any","type":"any"},{"key":"out","type":"signal"}]', '[{"key":"nodeId","type":"nodeRef","required":true},{"key":"field","type":"text","note":"该组件 content/behavior 里的字段名,如 text/imageUrl"}]', 'Download', 215, 1, '表单类图的字段来源:登录/发帖/举报等把某个输入组件的当前值拉进图里'),
('op.filterByVersion', '按版本过滤', 'operator', '算子', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"list","type":"listPost","required":true},{"key":"versionId","type":"number"}]', '[{"key":"listPost","type":"listPost"},{"key":"out","type":"signal"}]', '[]', 'Filter', 316, 1, '前端数组过滤;原设计标 runner=api 但无对应接口,2026-10-04 改为 ui'),
('op.filterByStage', '按阶段过滤', 'operator', '算子', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"list","type":"listPost","required":true},{"key":"stageId","type":"number"}]', '[{"key":"listPost","type":"listPost"},{"key":"out","type":"signal"}]', '[]', 'Filter', 317, 1, '同 op.filterByVersion'),
('op.filterByKeyword', '关键字过滤', 'operator', '算子', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"list","type":"listPost","required":true},{"key":"keyword","type":"text"}]', '[{"key":"listPost","type":"listPost"},{"key":"out","type":"signal"}]', '[]', 'Filter', 318, 1, NULL),
('op.sort', '排序', 'operator', '算子', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"list","type":"listPost","required":true},{"key":"by","type":"text","options":["latest","hot"]}]', '[{"key":"listPost","type":"listPost"},{"key":"out","type":"signal"}]', '[]', 'Filter', 319, 1, NULL),
('op.take', '取前N', 'operator', '算子', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"list","type":"any","required":true},{"key":"n","type":"number"}]', '[{"key":"any","type":"any"},{"key":"out","type":"signal"}]', '[]', 'Filter', 320, 1, NULL),
('op.and', '与', 'operator', '算子', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"a","type":"boolean","required":true},{"key":"b","type":"boolean","required":true}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Filter', 321, 1, '多字段校验用;布尔组合属于 L4 流程控制,不是 L5 通用计算'),
('op.or', '或', 'operator', '算子', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"a","type":"boolean","required":true},{"key":"b","type":"boolean","required":true}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Filter', 322, 1, NULL),
('op.not', '非', 'operator', '算子', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"value","type":"boolean","required":true}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Filter', 323, 1, NULL),
('op.markExpired', '标记版本过期', 'operator', '算子', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"list","type":"listPost","required":true},{"key":"currentVersionId","type":"number"}]', '[{"key":"listPost","type":"listPost"},{"key":"out","type":"signal"}]', '[]', 'Filter', 324, 1, '按 post.versionId!==currentVersionId 打角标;前端判定,无后端接口'),
('cond.isLogin', '是否登录', 'condition', '条件', 'ui', NULL, '[{"key":"signal","type":"signal"}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Switch', 425, 1, NULL),
('cond.isEmpty', '是否为空', 'condition', '条件', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"value","type":"any","required":true}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Switch', 426, 1, NULL),
('cond.isOwner', '是否本人', 'condition', '条件', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"memberId","type":"number","required":true}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Switch', 427, 1, NULL),
('cond.isBoardOpen', '板块是否开放', 'condition', '条件', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"board","type":"board","required":true}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Switch', 428, 1, NULL),
('cond.isPostVisible', '帖子是否可见', 'condition', '条件', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"post","type":"post","required":true}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Switch', 429, 1, 'status=1 已发布'),
('cond.isValid', '字段是否合法', 'condition', '条件', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"value","type":"any","required":true},{"key":"rule","type":"text","options":["notBlank","maxLen","isNumber"]},{"key":"arg","type":"number"}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Switch', 430, 1, NULL),
('branch', '分支', 'condition', '条件', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"cond","type":"boolean","required":true}]', '[{"key":"true","type":"signal"},{"key":"false","type":"signal"}]', '[]', 'Switch', 431, 1, NULL),
('act.login', '登录', 'action', '动作', 'api', 'POST /app/login', '[{"key":"signal","type":"signal"},{"key":"username","type":"text","required":true},{"key":"password","type":"text","required":true}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 532, 1, NULL),
('act.register', '注册', 'action', '动作', 'api', 'POST /app/register', '[{"key":"signal","type":"signal"},{"key":"username","type":"text","required":true},{"key":"password","type":"text","required":true},{"key":"nickname","type":"text"}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 533, 1, NULL),
('act.logout', '退出登录', 'action', '动作', 'ui', NULL, '[{"key":"signal","type":"signal"}]', '[{"key":"out","type":"signal"}]', '[]', 'Promotion', 534, 1, NULL),
('act.updateProfile', '修改个人资料', 'action', '动作', 'api', 'PUT /app/member/profile', '[{"key":"signal","type":"signal"},{"key":"nickname","type":"text"},{"key":"intro","type":"text"},{"key":"avatar","type":"image"}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 535, 1, NULL),
('act.setGameProgress', '设置游戏进度', 'action', '动作', 'api', 'PUT /app/member/progress', '[{"key":"signal","type":"signal"},{"key":"boardId","type":"number","required":true},{"key":"versionId","type":"number"},{"key":"stageId","type":"number"},{"key":"isPublic","type":"boolean"}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 536, 1, NULL),
('act.submitPost', '发布帖子', 'action', '动作', 'api', 'POST /app/post', '[{"key":"signal","type":"signal"},{"key":"boardId","type":"number","required":true},{"key":"title","type":"text","required":true},{"key":"content","type":"text","required":true},{"key":"cover","type":"image"},{"key":"versionId","type":"number","required":true},{"key":"stageId","type":"number"},{"key":"tagIds","type":"any"}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 537, 1, NULL),
('act.like', '点赞(可取消)', 'action', '动作', 'api', 'POST /app/post/{postId}/like', '[{"key":"signal","type":"signal"},{"key":"postId","type":"number","required":true}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 538, 1, NULL),
('act.favorite', '收藏(可取消)', 'action', '动作', 'api', 'POST /app/post/{postId}/favorite', '[{"key":"signal","type":"signal"},{"key":"postId","type":"number","required":true}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 539, 1, NULL),
('act.comment', '发表评论/回复', 'action', '动作', 'api', 'POST /app/comment', '[{"key":"signal","type":"signal"},{"key":"postId","type":"number","required":true},{"key":"content","type":"text","required":true},{"key":"parentId","type":"number"}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 540, 1, NULL),
('act.report', '提交举报', 'action', '动作', 'api', 'POST /app/report', '[{"key":"signal","type":"signal"},{"key":"postId","type":"number","required":true},{"key":"reason","type":"text","required":true},{"key":"remark","type":"text"}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 541, 1, NULL),
('act.navigate', '跳转', 'ui', '界面动作', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"to","type":"text","required":true,"options":["/","/board/:id","/post/:id","/profile","back"]}]', '[{"key":"out","type":"signal"}]', '[]', 'Pointer', 642, 1, NULL),
('act.openModal', '打开弹窗', 'ui', '界面动作', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"nodeId","type":"nodeRef","required":true}]', '[{"key":"out","type":"signal"}]', '[]', 'Pointer', 643, 1, NULL),
('act.closeModal', '关闭弹窗', 'ui', '界面动作', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"nodeId","type":"nodeRef"}]', '[{"key":"out","type":"signal"}]', '[]', 'Pointer', 644, 1, NULL),
('act.toast', '提示', 'ui', '界面动作', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"message","type":"text","required":true},{"key":"type","type":"text","options":["info","success","warning","error"]}]', '[{"key":"out","type":"signal"}]', '[]', 'Pointer', 645, 1, NULL),
('act.setVisible', '显示/隐藏组件', 'ui', '界面动作', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"nodeId","type":"nodeRef","required":true},{"key":"visible","type":"boolean","required":true}]', '[{"key":"out","type":"signal"}]', '[]', 'Pointer', 646, 1, NULL),
('act.scrollTo', '滚动到组件', 'ui', '界面动作', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"nodeId","type":"nodeRef","required":true}]', '[{"key":"out","type":"signal"}]', '[]', 'Pointer', 647, 1, NULL)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  kind = VALUES(kind),
  category = VALUES(category),
  runner = VALUES(runner),
  source_api = VALUES(source_api),
  inputs_schema = VALUES(inputs_schema),
  outputs_schema = VALUES(outputs_schema),
  config_schema = VALUES(config_schema),
  preview_icon = VALUES(preview_icon),
  sort = VALUES(sort),
  remark = VALUES(remark);

-- ==================== 三、内置预设种子（7 个原子团） ====================
-- 预设 = 原子团，放预设库供页面图引用（≈ ComfyUI 的节点模板）。三条图内约定见
-- 设计文档 presetLibrary.graphConventions，要点：
--   * 内部图不带触发（预设跨页面，触发器的 nodeId 只能指具体页面节点）；
--     入口 = 没有入边 signal 的节点，由调用方接进预设节点的 signal 输入驱动
--   * 出参用 graph_json.exports 声明来自哪个内部端口
--   * 数据用 config 里的 $ref 写，边只连控制流（signal）

INSERT INTO ui_preset
(preset_code, preset_name, version, is_builtin, category, inputs_schema, outputs_schema, graph_json, status, remark) VALUES
('preset.login', '用户登录', 1, 1, 'member', '[{"key":"username","type":"text","required":true},{"key":"password","type":"text","required":true}]', '[{"key":"result","type":"result"}]', '{"nodes":[{"id":"p1","typeCode":"cond.isValid","label":"用户名非空","config":{"value":"$param.username","rule":"notBlank"}},{"id":"p2","typeCode":"cond.isValid","label":"密码非空","config":{"value":"$param.password","rule":"notBlank"}},{"id":"p3","typeCode":"op.and","label":"都非空","config":{"a":"$ref.p1.boolean","b":"$ref.p2.boolean"}},{"id":"p4","typeCode":"branch","label":"校验通过?","config":{"cond":"$ref.p3.boolean"}},{"id":"p5","typeCode":"act.login","label":"登录","config":{"username":"$param.username","password":"$param.password"}},{"id":"p6","typeCode":"act.toast","label":"提示补全","config":{"message":"请填写用户名和密码","type":"warning"}}],"edges":[{"from":{"node":"p1","port":"out"},"to":{"node":"p2","port":"signal"}},{"from":{"node":"p2","port":"out"},"to":{"node":"p3","port":"signal"}},{"from":{"node":"p3","port":"out"},"to":{"node":"p4","port":"signal"}},{"from":{"node":"p4","port":"true"},"to":{"node":"p5","port":"signal"}},{"from":{"node":"p4","port":"false"},"to":{"node":"p6","port":"signal"}}],"exports":{"result":"p5.result"}}', 1, NULL),
('preset.profile', '查看个人信息', 1, 1, 'member', '[]', '[{"key":"member","type":"member"},{"key":"listPost","type":"listPost"}]', '{"nodes":[{"id":"p1","typeCode":"cond.isLogin","label":"是否登录"},{"id":"p2","typeCode":"branch","label":"已登录?","config":{"cond":"$ref.p1.boolean"}},{"id":"p3","typeCode":"data.currentMember","label":"当前用户"},{"id":"p4","typeCode":"data.myPosts","label":"我的帖子"},{"id":"p5","typeCode":"act.navigate","label":"去登录","config":{"to":"/"}}],"edges":[{"from":{"node":"p1","port":"out"},"to":{"node":"p2","port":"signal"}},{"from":{"node":"p2","port":"true"},"to":{"node":"p3","port":"signal"}},{"from":{"node":"p3","port":"out"},"to":{"node":"p4","port":"signal"}},{"from":{"node":"p2","port":"false"},"to":{"node":"p5","port":"signal"}}],"exports":{"member":"p3.member","listPost":"p4.listPost"}}', 1, '登录页路由未落地(navigate 的 options 里也没有 /login)，未登录分支先跳 /'),
('preset.boardBrowse', '板块浏览取数', 1, 1, 'content', '[{"key":"boardId","type":"number","required":true}]', '[{"key":"board","type":"board"},{"key":"listPost","type":"listPost"},{"key":"pageInfo","type":"pageInfo"}]', '{"nodes":[{"id":"p1","typeCode":"data.boardDetail","label":"板块详情","config":{"boardId":"$param.boardId"}},{"id":"p2","typeCode":"cond.isBoardOpen","label":"板块开放?","config":{"board":"$ref.p1.board"}},{"id":"p3","typeCode":"branch","label":"开放?","config":{"cond":"$ref.p2.boolean"}},{"id":"p4","typeCode":"data.postList","label":"帖子列表","config":{"boardId":"$ref.p1.board.id","sort":"latest"}},{"id":"p5","typeCode":"op.markExpired","label":"标记版本过期","config":{"list":"$ref.p4.listPost","currentVersionId":"$ref.p1.board.currentVersionId"}},{"id":"p6","typeCode":"act.toast","label":"未开放提示","config":{"message":"该板块暂未开放","type":"warning"}}],"edges":[{"from":{"node":"p1","port":"out"},"to":{"node":"p2","port":"signal"}},{"from":{"node":"p2","port":"out"},"to":{"node":"p3","port":"signal"}},{"from":{"node":"p3","port":"true"},"to":{"node":"p4","port":"signal"}},{"from":{"node":"p4","port":"out"},"to":{"node":"p5","port":"signal"}},{"from":{"node":"p3","port":"false"},"to":{"node":"p6","port":"signal"}}],"exports":{"board":"p1.board","listPost":"p5.listPost","pageInfo":"p4.pageInfo"}}', 1, '未传 versionId：『按我的版本筛』要先有会员进度接口(data.myProgress)落地，玩家侧未开工'),
('preset.postDetail', '帖子详情取数', 1, 1, 'content', '[{"key":"postId","type":"number","required":true}]', '[{"key":"post","type":"post"},{"key":"listComment","type":"listComment"}]', '{"nodes":[{"id":"p1","typeCode":"data.postDetail","label":"帖子详情","config":{"postId":"$param.postId"}},{"id":"p2","typeCode":"cond.isPostVisible","label":"可见?","config":{"post":"$ref.p1.post"}},{"id":"p3","typeCode":"branch","label":"可见?","config":{"cond":"$ref.p2.boolean"}},{"id":"p4","typeCode":"data.commentList","label":"评论列表","config":{"postId":"$param.postId"}},{"id":"p5","typeCode":"act.toast","label":"不可见提示","config":{"message":"内容不可见","type":"warning"}}],"edges":[{"from":{"node":"p1","port":"out"},"to":{"node":"p2","port":"signal"}},{"from":{"node":"p2","port":"out"},"to":{"node":"p3","port":"signal"}},{"from":{"node":"p3","port":"true"},"to":{"node":"p4","port":"signal"}},{"from":{"node":"p3","port":"false"},"to":{"node":"p5","port":"signal"}}],"exports":{"post":"p1.post","listComment":"p4.listComment"}}', 1, NULL),
('preset.publishPost', '发布攻略帖', 1, 1, 'content', '[{"key":"boardId","type":"number","required":true},{"key":"versionId","type":"number","required":true},{"key":"title","type":"text","required":true},{"key":"content","type":"text","required":true}]', '[{"key":"result","type":"result"}]', '{"nodes":[{"id":"p1","typeCode":"cond.isValid","label":"标题非空","config":{"value":"$param.title","rule":"notBlank"}},{"id":"p2","typeCode":"branch","label":"可以提交?","config":{"cond":"$ref.p1.boolean"}},{"id":"p3","typeCode":"act.submitPost","label":"发布帖子","config":{"boardId":"$param.boardId","versionId":"$param.versionId","title":"$param.title","content":"$param.content"}},{"id":"p4","typeCode":"act.toast","label":"已提交","config":{"message":"已提交","type":"success"}},{"id":"p5","typeCode":"act.toast","label":"标题空提示","config":{"message":"标题不能为空","type":"warning"}}],"edges":[{"from":{"node":"p1","port":"out"},"to":{"node":"p2","port":"signal"}},{"from":{"node":"p2","port":"true"},"to":{"node":"p3","port":"signal"}},{"from":{"node":"p3","port":"out"},"to":{"node":"p4","port":"signal"}},{"from":{"node":"p2","port":"false"},"to":{"node":"p5","port":"signal"}}],"exports":{"result":"p3.result"}}', 1, '入参由设计稿的 boardId 扩为 4 个(act.submitPost 必填 versionId/标题/正文)；缺『按动作结果分支』的原子，成功后跳转暂用提示代替'),
('preset.interact', '点赞与收藏', 1, 1, 'interact', '[{"key":"postId","type":"number","required":true}]', '[{"key":"result","type":"result"}]', '{"nodes":[{"id":"p1","typeCode":"cond.isLogin","label":"是否登录"},{"id":"p2","typeCode":"branch","label":"已登录?","config":{"cond":"$ref.p1.boolean"}},{"id":"p3","typeCode":"act.like","label":"点赞/取消","config":{"postId":"$param.postId"}},{"id":"p4","typeCode":"act.favorite","label":"收藏/取消","config":{"postId":"$param.postId"}},{"id":"p5","typeCode":"act.toast","label":"已更新","config":{"message":"已更新","type":"success"}},{"id":"p6","typeCode":"act.navigate","label":"去登录","config":{"to":"/"}}],"edges":[{"from":{"node":"p1","port":"out"},"to":{"node":"p2","port":"signal"}},{"from":{"node":"p2","port":"true"},"to":{"node":"p3","port":"signal"}},{"from":{"node":"p3","port":"out"},"to":{"node":"p4","port":"signal"}},{"from":{"node":"p4","port":"out"},"to":{"node":"p5","port":"signal"}},{"from":{"node":"p2","port":"false"},"to":{"node":"p6","port":"signal"}}],"exports":{"result":"p4.result"}}', 1, '同 preset.profile：未登录分支先跳 /'),
('preset.report', '提交举报', 1, 1, 'interact', '[{"key":"postId","type":"number","required":true},{"key":"reason","type":"text","required":true},{"key":"remark","type":"text"}]', '[{"key":"result","type":"result"}]', '{"nodes":[{"id":"p1","typeCode":"cond.isValid","label":"原因非空","config":{"value":"$param.reason","rule":"notBlank"}},{"id":"p2","typeCode":"branch","label":"可以提交?","config":{"cond":"$ref.p1.boolean"}},{"id":"p3","typeCode":"act.report","label":"提交举报","config":{"postId":"$param.postId","reason":"$param.reason","remark":"$param.remark"}},{"id":"p4","typeCode":"act.toast","label":"已提交","config":{"message":"已提交，待处理","type":"success"}},{"id":"p5","typeCode":"act.toast","label":"请选原因","config":{"message":"请选择举报原因","type":"warning"}}],"edges":[{"from":{"node":"p1","port":"out"},"to":{"node":"p2","port":"signal"}},{"from":{"node":"p2","port":"true"},"to":{"node":"p3","port":"signal"}},{"from":{"node":"p3","port":"out"},"to":{"node":"p4","port":"signal"}},{"from":{"node":"p2","port":"false"},"to":{"node":"p5","port":"signal"}}],"exports":{"result":"p3.result"}}', 1, '打开举报弹窗属页面图(act.openModal)；本预设只覆盖弹窗内的提交')
ON DUPLICATE KEY UPDATE
  preset_name = VALUES(preset_name),
  is_builtin = VALUES(is_builtin),
  category = VALUES(category),
  inputs_schema = VALUES(inputs_schema),
  outputs_schema = VALUES(outputs_schema),
  graph_json = VALUES(graph_json),
  remark = VALUES(remark);
