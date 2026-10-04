-- 用户端图形化搭建 v3 · 阶段 3（逻辑层）增量脚本
-- 内容：3 张新表（ui_logic_node_type / ui_preset / ui_flow）、原子库种子（44 条）、
--       逻辑层相关菜单
-- 说明：本文件可重复执行（DDL 用 IF NOT EXISTS，种子用 ON DUPLICATE KEY UPDATE）。
--       稳定后应并入 sql/schema.sql 与 sql/seed.sql。
-- 设计文档：用户端模块化样式设计.json（v3.0）的 atomCatalog / database 节
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
('data.boardDetail', '板块详情', 'source', '数据源', 'api', 'GET /app/board/{id}', '[{"key":"signal","type":"signal"},{"key":"boardId","type":"number","required":true}]', '[{"key":"board","type":"board"},{"key":"listBoardVersion","type":"listBoardVersion"},{"key":"listProgressStage","type":"listProgressStage"},{"key":"out","type":"signal"}]', '[]', 'Download', 204, 1, NULL),
('data.postList', '帖子列表', 'source', '数据源', 'api', 'GET /app/post/page', '[{"key":"signal","type":"signal"},{"key":"boardId","type":"number"},{"key":"versionId","type":"number"},{"key":"stageId","type":"number"},{"key":"tagId","type":"number"},{"key":"keyword","type":"text"},{"key":"sort","type":"text","options":["latest","hot"]},{"key":"page","type":"number"},{"key":"size","type":"number"}]', '[{"key":"listPost","type":"listPost"},{"key":"pageInfo","type":"pageInfo"},{"key":"out","type":"signal"}]', '[]', 'Download', 205, 1, NULL),
('data.postDetail', '帖子详情', 'source', '数据源', 'api', 'GET /app/post/{id}', '[{"key":"signal","type":"signal"},{"key":"postId","type":"number","required":true}]', '[{"key":"post","type":"post"},{"key":"out","type":"signal"}]', '[]', 'Download', 206, 1, NULL),
('data.commentList', '评论列表', 'source', '数据源', 'api', 'GET /app/comment/page', '[{"key":"signal","type":"signal"},{"key":"postId","type":"number","required":true},{"key":"page","type":"number"},{"key":"size","type":"number"}]', '[{"key":"listComment","type":"listComment"},{"key":"pageInfo","type":"pageInfo"},{"key":"out","type":"signal"}]', '[]', 'Download', 207, 1, NULL),
('data.noticeList', '公告列表', 'source', '数据源', 'api', 'GET /app/notice/list', '[{"key":"signal","type":"signal"},{"key":"limit","type":"number"}]', '[{"key":"listNotice","type":"listNotice"},{"key":"out","type":"signal"}]', '[]', 'Download', 208, 1, NULL),
('data.currentMember', '当前用户', 'source', '数据源', 'api', 'GET /app/member/me', '[{"key":"signal","type":"signal"}]', '[{"key":"member","type":"member"},{"key":"out","type":"signal"}]', '[]', 'Download', 209, 1, NULL),
('data.myPosts', '我的帖子', 'source', '数据源', 'api', 'GET /app/member/my-posts', '[{"key":"signal","type":"signal"},{"key":"page","type":"number"},{"key":"size","type":"number"}]', '[{"key":"listPost","type":"listPost"},{"key":"pageInfo","type":"pageInfo"},{"key":"out","type":"signal"}]', '[]', 'Download', 210, 1, NULL),
('data.myFavorites', '我的收藏', 'source', '数据源', 'api', 'GET /app/member/my-favorites', '[{"key":"signal","type":"signal"},{"key":"page","type":"number"},{"key":"size","type":"number"}]', '[{"key":"listPost","type":"listPost"},{"key":"pageInfo","type":"pageInfo"},{"key":"out","type":"signal"}]', '[]', 'Download', 211, 1, NULL),
('data.myReports', '我的举报', 'source', '数据源', 'api', 'GET /app/member/my-reports', '[{"key":"signal","type":"signal"},{"key":"page","type":"number"}]', '[{"key":"listReport","type":"listReport"},{"key":"pageInfo","type":"pageInfo"},{"key":"out","type":"signal"}]', '[]', 'Download', 212, 1, NULL),
('data.myProgress', '我的游戏进度', 'source', '数据源', 'api', 'GET /app/member/progress', '[{"key":"signal","type":"signal"},{"key":"boardId","type":"number"}]', '[{"key":"any","type":"any"},{"key":"out","type":"signal"}]', '[]', 'Download', 213, 1, NULL),
('data.runtime', '运行环境', 'source', '数据源', 'ui', NULL, '[{"key":"signal","type":"signal"}]', '[{"key":"any","type":"any"},{"key":"out","type":"signal"}]', '[{"key":"key","type":"text","options":["routeParam.id","query.k","page"]}]', 'Download', 214, 1, NULL),
('op.filterByVersion', '按版本过滤', 'operator', '算子', 'api', NULL, '[{"key":"signal","type":"signal"},{"key":"list","type":"listPost","required":true},{"key":"versionId","type":"number"}]', '[{"key":"listPost","type":"listPost"},{"key":"out","type":"signal"}]', '[]', 'Filter', 315, 1, NULL),
('op.filterByStage', '按阶段过滤', 'operator', '算子', 'api', NULL, '[{"key":"signal","type":"signal"},{"key":"list","type":"listPost","required":true},{"key":"stageId","type":"number"}]', '[{"key":"listPost","type":"listPost"},{"key":"out","type":"signal"}]', '[]', 'Filter', 316, 1, NULL),
('op.filterByKeyword', '关键字过滤', 'operator', '算子', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"list","type":"listPost","required":true},{"key":"keyword","type":"text"}]', '[{"key":"listPost","type":"listPost"},{"key":"out","type":"signal"}]', '[]', 'Filter', 317, 1, NULL),
('op.sort', '排序', 'operator', '算子', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"list","type":"listPost","required":true},{"key":"by","type":"text","options":["latest","hot"]}]', '[{"key":"listPost","type":"listPost"},{"key":"out","type":"signal"}]', '[]', 'Filter', 318, 1, NULL),
('op.take', '取前N', 'operator', '算子', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"list","type":"any","required":true},{"key":"n","type":"number"}]', '[{"key":"any","type":"any"},{"key":"out","type":"signal"}]', '[]', 'Filter', 319, 1, NULL),
('op.markExpired', '标记版本过期', 'operator', '算子', 'api', NULL, '[{"key":"signal","type":"signal"},{"key":"list","type":"listPost","required":true},{"key":"currentVersionId","type":"number"}]', '[{"key":"listPost","type":"listPost"},{"key":"out","type":"signal"}]', '[]', 'Filter', 320, 1, '前端亦可按 post.versionId!==currentVersionId 判定角标'),
('cond.isLogin', '是否登录', 'condition', '条件', 'ui', NULL, '[{"key":"signal","type":"signal"}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Switch', 421, 1, NULL),
('cond.isEmpty', '是否为空', 'condition', '条件', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"value","type":"any","required":true}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Switch', 422, 1, NULL),
('cond.isOwner', '是否本人', 'condition', '条件', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"memberId","type":"number","required":true}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Switch', 423, 1, NULL),
('cond.isBoardOpen', '板块是否开放', 'condition', '条件', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"board","type":"board","required":true}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Switch', 424, 1, NULL),
('cond.isPostVisible', '帖子是否可见', 'condition', '条件', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"post","type":"post","required":true}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Switch', 425, 1, 'status=1 已发布'),
('cond.isValid', '字段是否合法', 'condition', '条件', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"value","type":"any","required":true},{"key":"rule","type":"text","options":["notBlank","maxLen","isNumber"]},{"key":"arg","type":"number"}]', '[{"key":"boolean","type":"boolean"},{"key":"out","type":"signal"}]', '[]', 'Switch', 426, 1, NULL),
('branch', '分支', 'condition', '条件', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"cond","type":"boolean","required":true}]', '[{"key":"true","type":"signal"},{"key":"false","type":"signal"}]', '[]', 'Switch', 427, 1, NULL),
('act.login', '登录', 'action', '动作', 'api', 'POST /app/login', '[{"key":"signal","type":"signal"},{"key":"username","type":"text","required":true},{"key":"password","type":"text","required":true}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 528, 1, NULL),
('act.register', '注册', 'action', '动作', 'api', 'POST /app/register', '[{"key":"signal","type":"signal"},{"key":"username","type":"text","required":true},{"key":"password","type":"text","required":true},{"key":"nickname","type":"text"}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 529, 1, NULL),
('act.logout', '退出登录', 'action', '动作', 'ui', NULL, '[{"key":"signal","type":"signal"}]', '[{"key":"out","type":"signal"}]', '[]', 'Promotion', 530, 1, NULL),
('act.updateProfile', '修改个人资料', 'action', '动作', 'api', 'PUT /app/member/profile', '[{"key":"signal","type":"signal"},{"key":"nickname","type":"text"},{"key":"intro","type":"text"},{"key":"avatar","type":"image"}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 531, 1, NULL),
('act.setGameProgress', '设置游戏进度', 'action', '动作', 'api', 'PUT /app/member/progress', '[{"key":"signal","type":"signal"},{"key":"boardId","type":"number","required":true},{"key":"versionId","type":"number"},{"key":"stageId","type":"number"},{"key":"isPublic","type":"boolean"}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 532, 1, NULL),
('act.submitPost', '发布帖子', 'action', '动作', 'api', 'POST /app/post', '[{"key":"signal","type":"signal"},{"key":"boardId","type":"number","required":true},{"key":"title","type":"text","required":true},{"key":"content","type":"text","required":true},{"key":"cover","type":"image"},{"key":"versionId","type":"number","required":true},{"key":"stageId","type":"number"},{"key":"tagIds","type":"any"}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 533, 1, NULL),
('act.like', '点赞(可取消)', 'action', '动作', 'api', 'POST /app/post/{id}/like', '[{"key":"signal","type":"signal"},{"key":"postId","type":"number","required":true}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 534, 1, NULL),
('act.favorite', '收藏(可取消)', 'action', '动作', 'api', 'POST /app/post/{id}/favorite', '[{"key":"signal","type":"signal"},{"key":"postId","type":"number","required":true}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 535, 1, NULL),
('act.comment', '发表评论/回复', 'action', '动作', 'api', 'POST /app/comment', '[{"key":"signal","type":"signal"},{"key":"postId","type":"number","required":true},{"key":"content","type":"text","required":true},{"key":"parentId","type":"number"}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 536, 1, NULL),
('act.report', '提交举报', 'action', '动作', 'api', 'POST /app/report', '[{"key":"signal","type":"signal"},{"key":"postId","type":"number","required":true},{"key":"reason","type":"text","required":true},{"key":"remark","type":"text"}]', '[{"key":"result","type":"result"},{"key":"out","type":"signal"}]', '[]', 'Promotion', 537, 1, NULL),
('act.navigate', '跳转', 'ui', '界面动作', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"to","type":"text","required":true,"options":["/","/board/:id","/post/:id","/profile","back"]}]', '[{"key":"out","type":"signal"}]', '[]', 'Pointer', 638, 1, NULL),
('act.openModal', '打开弹窗', 'ui', '界面动作', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"nodeId","type":"nodeRef","required":true}]', '[{"key":"out","type":"signal"}]', '[]', 'Pointer', 639, 1, NULL),
('act.closeModal', '关闭弹窗', 'ui', '界面动作', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"nodeId","type":"nodeRef"}]', '[{"key":"out","type":"signal"}]', '[]', 'Pointer', 640, 1, NULL),
('act.toast', '提示', 'ui', '界面动作', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"message","type":"text","required":true},{"key":"type","type":"text","options":["info","success","warning","error"]}]', '[{"key":"out","type":"signal"}]', '[]', 'Pointer', 641, 1, NULL),
('act.setVisible', '显示/隐藏组件', 'ui', '界面动作', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"nodeId","type":"nodeRef","required":true},{"key":"visible","type":"boolean","required":true}]', '[{"key":"out","type":"signal"}]', '[]', 'Pointer', 642, 1, NULL),
('act.scrollTo', '滚动到组件', 'ui', '界面动作', 'ui', NULL, '[{"key":"signal","type":"signal"},{"key":"nodeId","type":"nodeRef","required":true}]', '[{"key":"out","type":"signal"}]', '[]', 'Pointer', 643, 1, NULL)
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
