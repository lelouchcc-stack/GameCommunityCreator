---
name: page-authoring
description: 生成、校验并落库用户端「符合本项目规则的页面」（管理端页面搭建的组件树）。当需要新建/修改用户端页面、搭首页、调页面结构外观与文案时使用。触发词：页面、搭建、组件树、排版、首页、page、ui_node。不适用于逻辑图 ui_flow 编排、组件内核开发、站点样式 token。
---

# 生成符合本项目规则的页面

本仓库把完整工具包放在 **`page-kit/`**（随项目开源，供任意 agent 使用）。先读它，不要凭记忆写：

1. `page-kit/SKILL.md` —— 工作流与铁律（**先读这个**）
2. `page-kit/references/layout-rules.md` —— 排版与嵌套约定（后端不拦但必须遵守）
3. `page-kit/references/pitfalls.md` —— 已知坑（length 单值、整树替换语义、业务组件取数、图片引用…）
4. `page-kit/references/component-catalog.json` —— 组件库快照，**typeCode 与每个组件开放的属性以它为准**
5. `page-kit/examples/` —— 可照抄的合规样例 + 一个会被拦住的 反例

命令（Node 18+，零依赖）：

```bash
P=page-kit/pagekit.mjs
node $P login --user admin --password admin123   # 只需一次，令牌缓存到 .qwen/tmp/（已 gitignore）
node $P catalog                                  # 刷新组件库快照（组件库改过就必须重拉）
node $P pages                                    # 看现有页面
node $P get home --out home.json                 # 改已有页面前先导出
node $P validate home.json                       # 离线预检（值类型由后端在 save 时校验）
node $P save home home.json                      # 整树保存：未提交的旧节点会被删除
node $P create-page --key about --name 关于 --route /about
```

关键约束（详见上面的 reference 文件）：

- 页面根**固定竖排**；要横排就加 `typeCode: "row"` 容器，别给根节点设 `flexDirection`。
- 只有容器能放子节点：`container` / `card` / `modal` / `row`。
- `styleJson`/`contentJson`/`behaviorJson` 的 key 必须在该组件类型的 schema 内；尺寸值**只能单值**（`"8px"`，不是 `"8px 18px"`）。
- 保存＝**整树替换**：先 `get` 再改再 `save`，否则会删掉没写进去的旧节点。
- `builtin:*`（banner / notice / hot_post / board_nav）四个业务组件在用户端**都渲染真实数据**，可以放心用。
- 图片只写引用（objectKey 或完整地址），上传在管理端界面做，不要编造 key。

**完成定义**：`validate` 无错误 + `save` 成功 + 把「哪个页面、用户端哪个路径看、要检查哪几点」
交给用户去浏览器确认（观感不由你判定）。
