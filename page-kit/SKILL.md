---
name: page-authoring
description: 为用户端社区页面生成、校验并落库「符合本项目规则的页面」——按组件库组装 ui_node 组件树（文本/标题/图片/按钮/分隔/间距/容器/卡片/图标/弹窗/行）。当需要新建或改动用户端页面、搭建首页、调整页面结构与外观（排版、间距、颜色、文案）时使用。不适用于逻辑图（ui_flow）编排、组件内核开发与站点样式 token。
---

# 生成符合本项目规则的页面

你负责的是**呈现层**：用户端页面长什么样（结构 + 样式 + 文案）。产出是**节点数组 JSON**，
不是 Vue 代码，也不是逻辑图。

## 铁律

1. **先刷新目录**：`node page-kit/pagekit.mjs catalog`（或确认 `references/component-catalog.json` 是新的）。
   typeCode 与每个组件开放的属性都以它为准——**不要凭记忆写属性名**。
2. **先读再改**：动已有页面前必须 `get <pageId|pageKey> --out x.json`。
   保存是**整树替换**——文件里没有的旧节点会被删除。
3. **改完先 validate 再 save**：`validate` 报错必须清零；提醒要人工判断。
4. **值类型不要自己猜**：颜色、长度、枚举这些规则只在后端有唯一实现，save 时才会校验。
   踩到就照后端返回的中文原因改。
5. **不要提交真实凭据**：令牌走环境变量 `PAGEKIT_TOKEN` 或默认缓存文件（已在 .gitignore）。
6. **观感交给用户**：排版好不好看由用户在浏览器确认，你的"完成"是
   **validate 通过 + save 成功 + 把页面地址与检查点交给用户**。

## 工作流

```bash
P=page-kit/pagekit.mjs
node $P catalog                                  # 1. 刷新组件库快照
node $P pages                                    # 2. 看现有页面（决定新建还是改）
node $P create-page --key about --name 关于 --route /about   # 3a. 新建页面
node $P get home --out home.json                 # 3b. 或导出已有页面
#   ...写/改节点 JSON（见下）...
node $P validate home.json                       # 4. 离线预检
node $P save home home.json                      # 5. 落库（失败会给出中文原因）
```

`save` 成功后把「哪个页面、在用户端哪个路径看、要检查哪几点」告诉用户。

## 节点数据形态

扁平数组（与保存接口一致），每个节点：

```json
{
  "id": -1,
  "parentId": null,
  "typeCode": "heading",
  "nodeName": "页面标题",
  "sort": 0,
  "visible": 1,
  "styleJson": { "textAlign": "center", "fontSize": "24px" },
  "contentJson": { "text": "新手引导" },
  "behaviorJson": {},
  "remark": ""
}
```

- `id`：新增节点用**负数临时 id**（-1、-2…）或省略；已有节点填真实 id。
- `parentId`：父节点 id，可以指向本次新增节点的临时 id；根节点为 `null`。
- **页面根是竖排**：多个根节点依次往下排。要横排就加一个 `typeCode: "row"` 容器（见下）。
- 只有**容器**（`isContainer=1`：`container` / `card` / `modal` / `row`）能放子节点。
- `styleJson` / `contentJson` / `behaviorJson` 的 key 必须在该组件类型的 schema 里
  （`references/component-catalog.json` 里逐类型列了可用 key）。

## 常用成型写法

**横排一行**（不要给根节点或普通组件设 `flexDirection`）：

```json
{ "id": -1, "parentId": null, "typeCode": "row", "sort": 2,
  "styleJson": { "flexDirection": "row", "wrap": true, "gap": "12px", "justify": "center", "alignItems": "center" } }
```

**按钮跳转 / 弹提示**（`button` 的 content 里**没有** linkUrl，导航走行为）：

```json
{ "typeCode": "button", "contentJson": { "text": "开始浏览" },
  "behaviorJson": { "onClickAction": { "action": "navigate", "url": "/home" } } }
```

可用动作：`navigate{url,newTab}`、`toast{message,toastType}`、`openModal{target}`、
`closeModal{target}`、`scrollTo{target}`、`toggleVisible{target}`（`target` 是节点 id）。

**颜色优先用站点 token**，跟随后台换肤：`var(--site-primary)`、`var(--site-text-primary)`、
`var(--site-text-secondary)`、`var(--site-bg-card)`、`var(--site-border)`。

**图片**：`contentJson.imageUrl` 填上传后拿到的 objectKey（`upload/yyyy/MM/dd/xxx.png`）
或完整地址（`https://...` / `/...`）。**图片上传在管理端界面里做**，本工具只写引用——
不要编造 objectKey。

## 收尾前自检

- [ ] `validate` 无错误，提醒逐条想清楚
- [ ] 文案是中文、面向玩家、没有占位符残留
- [ ] `builtin:banner` / `builtin:notice` / `builtin:hot_post` 用户端**暂时不渲染**，
      想要"看得见"的效果就用基础与布局组件；`builtin:board_nav` 已接真实数据
      （开放板块卡片，点击进 `/board/:id`），可放心使用
- [ ] 已 `save` 并回读确认（或 `get` 一次看结果）
- [ ] 把页面路径与检查点交给用户

细节：[references/layout-rules.md](references/layout-rules.md)、[references/pitfalls.md](references/pitfalls.md)；
可照抄样例：[examples/landing-page.json](examples/landing-page.json)；
反例（会被 validate 拦住）：[examples/invalid-page.json](examples/invalid-page.json)。
