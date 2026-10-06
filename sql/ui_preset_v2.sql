-- 预设库增量 v2：修正错误的、标注跑不通的、补齐缺失的
--
-- 背景（2026-10-05 预设库盘点结论）：
--   1. preset.interact 语义错误——图里把 act.like → act.favorite 串成一串，而两者都是"点击即切换"，
--      调用一次会把点赞与收藏同时翻转。改为拆成 preset.like / preset.favorite 两个单一职责预设，
--      旧的那个停用（保留图，便于对照）。
--   2. preset.publishPost / preset.report 依赖的后端接口不存在（POST /app/post 发帖、
--      POST /app/report 用户端举报），现在插进图里只会跑失败 —— 停用并把原因写进备注，
--      接口落地后把 status 改回 1 即可。备注里"缺按动作结果分支的原子"那句已过期：
--      cond.isEmpty(result) → branch 就是登录图在用的写法，preset.publishPost 自己也已导出 result。
--   3. 原子库里有 5 个已可用、却没有任何预设覆盖的能力：act.like / act.favorite / act.comment /
--      act.updateProfile / act.logout，对应接口全部就绪（已用有效玩家令牌逐个探活）。
--      本脚本把它们补成 5 个预设，**不需要新增任何原子**。
--      命名用"场景名"而不是照抄原子名：「逻辑编排」左栏同时列原子与预设，
--      预设若叫「点赞(可取消)」会和动作分组里的同名原子撞在一起，看不出哪条是原子团。
--   4. op.markExpired 对 data.postList 的返回值是多余的：/app/post/page 的返回里
--      expired 已由后端按 board.current_version_id 算好（见 PostServiceImpl.markExpired），
--      所以不再为"列表过期标记"单独建预设。
--
-- 可重复执行：更新用 preset_code+version 定位，插入用 ON DUPLICATE KEY UPDATE（uk_preset_ver）。
-- 位置说明：全量原子/预设种子在 sql/ui_logic.sql；本文件是它的增量，与 ui_board_nav_props.sql 同例。

SET NAMES utf8mb4;
USE ssy_db;

-- ==================== 一、修正与停用 ====================

-- 语义错误的旧预设：停用，并把替代品写清楚
UPDATE ui_preset
SET status = 0,
    remark = '（已停用）原图把 act.like → act.favorite 串成一串，而两者都是"点击即切换"，调用一次会同时翻转点赞与收藏。请改用 preset.like / preset.favorite。'
WHERE preset_code = 'preset.interact' AND version = 1;

-- 依赖的后端接口尚未实现：停用并说明，避免插进图里才发现跑不通
UPDATE ui_preset
SET status = 0,
    remark = '（已停用）依赖 POST /app/post（用户端发帖）——该接口尚未实现，预设现在跑不通。发帖模块落地后把 status 改回 1 即可。原备注：入参为 boardId/versionId/title/content；成功后调用方可用 cond.isEmpty(result) → branch 判断结果再跳转（该写法见 preset.login，不需要新原子）。'
WHERE preset_code = 'preset.publishPost' AND version = 1;

UPDATE ui_preset
SET status = 0,
    remark = '（已停用）依赖 POST /app/report（用户端举报）——该接口尚未实现（后台只有 /admin/report/**），预设现在跑不通。接口落地后把 status 改回 1 即可。打开举报弹窗属页面图（act.openModal），本预设只覆盖弹窗内的提交。'
WHERE preset_code = 'preset.report' AND version = 1;

-- 停用中的举报预设与动作分组的原子 act.report 同名，改个名：将来接口落地重新启用后，
-- 「逻辑编排」左栏才不会出现两处一模一样的「提交举报」
UPDATE ui_preset
SET preset_name = '提交举报（待接口）'
WHERE preset_code = 'preset.report' AND version = 1;

-- ==================== 二、补齐缺失的能力（5 个预设） ====================
-- 端口约定与 ui_logic.sql 一致：触发原子是图的根；内部图不带触发，入口 = 没有入边 signal 的节点；
-- 出参用 exports 声明来自哪个内部端口；数据用 config 里的 $ref/$param 传，边只连控制流。

INSERT INTO ui_preset
(preset_code, preset_name, version, is_builtin, category, inputs_schema, outputs_schema, graph_json, status, remark) VALUES

-- 1) 点赞（可取消）—— 拆自 preset.interact，单一职责
('preset.like', '帖子点赞（含登录引导）', 1, 1, 'interact',
 '[{"key":"postId","type":"number","required":true}]',
 '[{"key":"result","type":"result"}]',
 '{"nodes":[{"id":"p1","typeCode":"cond.isLogin","label":"是否登录"},{"id":"p2","typeCode":"branch","label":"已登录?","config":{"cond":"$ref.p1.boolean"}},{"id":"p3","typeCode":"act.like","label":"点赞/取消","config":{"postId":"$param.postId"}},{"id":"p4","typeCode":"act.toast","label":"已更新","config":{"message":"已更新点赞","type":"success"}},{"id":"p5","typeCode":"act.navigate","label":"去登录","config":{"to":"/login"}}],"edges":[{"from":{"node":"p1","port":"out"},"to":{"node":"p2","port":"signal"}},{"from":{"node":"p2","port":"true"},"to":{"node":"p3","port":"signal"}},{"from":{"node":"p3","port":"out"},"to":{"node":"p4","port":"signal"}},{"from":{"node":"p2","port":"false"},"to":{"node":"p5","port":"signal"}}],"exports":{"result":"p3.result"}}',
 1, '点赞与取消共用一个原子（act.like 点击即切换）。调用方用 cond.isEmpty(result) 判断是否成功。'),

-- 2) 收藏（可取消）
('preset.favorite', '帖子收藏（含登录引导）', 1, 1, 'interact',
 '[{"key":"postId","type":"number","required":true}]',
 '[{"key":"result","type":"result"}]',
 '{"nodes":[{"id":"p1","typeCode":"cond.isLogin","label":"是否登录"},{"id":"p2","typeCode":"branch","label":"已登录?","config":{"cond":"$ref.p1.boolean"}},{"id":"p3","typeCode":"act.favorite","label":"收藏/取消","config":{"postId":"$param.postId"}},{"id":"p4","typeCode":"act.toast","label":"已更新","config":{"message":"已更新收藏","type":"success"}},{"id":"p5","typeCode":"act.navigate","label":"去登录","config":{"to":"/login"}}],"edges":[{"from":{"node":"p1","port":"out"},"to":{"node":"p2","port":"signal"}},{"from":{"node":"p2","port":"true"},"to":{"node":"p3","port":"signal"}},{"from":{"node":"p3","port":"out"},"to":{"node":"p4","port":"signal"}},{"from":{"node":"p2","port":"false"},"to":{"node":"p5","port":"signal"}}],"exports":{"result":"p3.result"}}',
 1, '同 preset.like：act.favorite 点击即切换。'),

-- 3) 发表评论/回复——覆盖 act.comment（一级评论 parentId 留空即为评论）
('preset.comment', '提交评论或回复', 1, 1, 'interact',
 '[{"key":"postId","type":"number","required":true},{"key":"content","type":"text","required":true},{"key":"parentId","type":"number"}]',
 '[{"key":"result","type":"result"}]',
 '{"nodes":[{"id":"p1","typeCode":"cond.isLogin","label":"是否登录"},{"id":"p2","typeCode":"branch","label":"已登录?","config":{"cond":"$ref.p1.boolean"}},{"id":"p3","typeCode":"cond.isValid","label":"内容非空","config":{"value":"$param.content","rule":"notBlank"}},{"id":"p4","typeCode":"branch","label":"可以提交?","config":{"cond":"$ref.p3.boolean"}},{"id":"p5","typeCode":"act.comment","label":"提交","config":{"postId":"$param.postId","content":"$param.content","parentId":"$param.parentId"}},{"id":"p6","typeCode":"act.toast","label":"已发布","config":{"message":"评论已发布","type":"success"}},{"id":"p7","typeCode":"act.toast","label":"内容为空提示","config":{"message":"评论内容不能为空","type":"warning"}},{"id":"p8","typeCode":"act.navigate","label":"去登录","config":{"to":"/login"}}],"edges":[{"from":{"node":"p1","port":"out"},"to":{"node":"p2","port":"signal"}},{"from":{"node":"p2","port":"true"},"to":{"node":"p3","port":"signal"}},{"from":{"node":"p3","port":"out"},"to":{"node":"p4","port":"signal"}},{"from":{"node":"p4","port":"true"},"to":{"node":"p5","port":"signal"}},{"from":{"node":"p5","port":"out"},"to":{"node":"p6","port":"signal"}},{"from":{"node":"p4","port":"false"},"to":{"node":"p7","port":"signal"}},{"from":{"node":"p2","port":"false"},"to":{"node":"p8","port":"signal"}}],"exports":{"result":"p5.result"}}',
 1, 'parentId 传被回复的评论 id 即为"回复"，留空即一级评论。发表后调用方自行刷新列表。'),

-- 4) 修改个人资料
('preset.updateProfile', '保存个人资料', 1, 1, 'member',
 '[{"key":"nickname","type":"text"},{"key":"intro","type":"text"},{"key":"avatar","type":"image"}]',
 '[{"key":"result","type":"result"}]',
 '{"nodes":[{"id":"p1","typeCode":"cond.isLogin","label":"是否登录"},{"id":"p2","typeCode":"branch","label":"已登录?","config":{"cond":"$ref.p1.boolean"}},{"id":"p3","typeCode":"act.updateProfile","label":"保存资料","config":{"nickname":"$param.nickname","intro":"$param.intro","avatar":"$param.avatar"}},{"id":"p4","typeCode":"act.toast","label":"已保存","config":{"message":"资料已更新","type":"success"}},{"id":"p5","typeCode":"act.navigate","label":"去登录","config":{"to":"/login"}}],"edges":[{"from":{"node":"p1","port":"out"},"to":{"node":"p2","port":"signal"}},{"from":{"node":"p2","port":"true"},"to":{"node":"p3","port":"signal"}},{"from":{"node":"p3","port":"out"},"to":{"node":"p4","port":"signal"}},{"from":{"node":"p2","port":"false"},"to":{"node":"p5","port":"signal"}}],"exports":{"result":"p3.result"}}',
 1, '三个入参都可空：留空的字段后端按"不改"处理（昵称留空则沿用原名）。'),

-- 5) 退出登录——纯界面动作，无入参无出参
('preset.logout', '退出当前账号', 1, 1, 'member',
 '[]', '[]',
 '{"nodes":[{"id":"p1","typeCode":"act.logout","label":"清登录态"},{"id":"p2","typeCode":"act.toast","label":"已退出","config":{"message":"已退出登录","type":"info"}},{"id":"p3","typeCode":"act.navigate","label":"回首页","config":{"to":"/"}}],"edges":[{"from":{"node":"p1","port":"out"},"to":{"node":"p2","port":"signal"}},{"from":{"node":"p2","port":"out"},"to":{"node":"p3","port":"signal"}}],"exports":{}}',
 1, '清掉登录态后回首页。act.logout 是界面动作（runner=ui），不发请求。')

ON DUPLICATE KEY UPDATE
  preset_name = VALUES(preset_name),
  is_builtin = VALUES(is_builtin),
  category = VALUES(category),
  inputs_schema = VALUES(inputs_schema),
  outputs_schema = VALUES(outputs_schema),
  graph_json = VALUES(graph_json),
  status = VALUES(status),
  remark = VALUES(remark);

-- 校验用查询：应看到 10 个启用预设、3 个停用（interact / publishPost / report）
-- SELECT preset_code, preset_name, status, category FROM ui_preset ORDER BY status DESC, id;
