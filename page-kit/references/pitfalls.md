# 已知坑（都在实测里踩过）

## 1. `length` 类型只接受**单值**

`padding`、`gap`、`marginTop`、`fontSize`、`radius` 这类尺寸值只能是 `16px` / `100%` / `auto` / `0`
这种**一个值**；CSS 简写会被后端拒：

```
✘ 400 按钮 / 样式：内边距格式不合法     ← padding: "8px 18px"
✔ padding: "8px"
```

本地 `validate` 会对「带空格的尺寸值」提前提醒，但最终判定在后端。
允许的写法：`auto`、`0`、数字+`px|%|rem|em|vh|vw`。

## 2. 保存是**整树替换**：没提交的旧节点会被删掉

`save` 提交的是这一页的完整节点集合；**文件里没出现的既有节点会被删除**（不是合并）。
所以改已有页面的正确姿势是 `get` 导出 → 在导出结果上改 → `save` 回灌，不要只写"这次新增的那几个"。

## 3. 保存失败不会写半套

后端先全量校验再落库，且在一个事务里；校验失败会直接返回原因，页面保持原样。
所以"save 报错"不是灾难，照提示改完再 save 即可。

## 4. `builtin:*` 业务组件大多在用户端**什么都不渲染**（board_nav 除外）

`banner` / `notice` / `hot_post` 是内置业务组件，渲染内核（阶段 4）还没写：
在管理端画布里能看到占位框，在用户端看不到任何内容。想要"看得见"的效果，用
基础与布局组件（文本/标题/图片/按钮/分隔/间距/容器/卡片/图标/行/弹窗）自己搭。

**例外：`board_nav`（板块导航）已接入**（2026-10-04）——它直接取 `/app/board/list`
的开放板块，渲染成卡片网格；内容属性 `limit`（数量）/`columns`（列数 1-4）/
`showCover`/`showIntro` 可配，用户端点击卡片进入 `/board/:id`。
（板块详情页本身属 pit.md 模块 03/04，目前是占位页。）

## 5. 图片：只写引用，不要编造 objectKey

`contentJson.imageUrl` 接受两类值：

- 完整地址：`https://...` 或 `/...`
- 上传得到的 objectKey：`upload/2026/10/04/xxxxxxxx.png`（**形如日期的前缀是硬要求**）

上传是在**管理端界面**里做的（板块封面、站点 Logo、搭建器属性面板都有上传控件），
本工具没有上传命令。不要自己拼一个 objectKey —— 拼出来的路径在存储里不存在。

另：种子里站点 Logo 默认值 `/static/logo.png` 在仓库里**没有对应文件**，
新装站点没上传 Logo 之前这个地址是 404。

## 6. 颜色写法

后端接受：3/6/8 位 hex（`#fff`、`#3B82F6`、`#3B82F6FF`）、`rgb()` / `rgba()`、
以及站点 token `var(--site-*)`。**空串＝未设置**（清空某项不会拦住保存）。
优先用 token，后台换肤才会跟着变。

## 7. `button` 的内容里**没有** `linkUrl`

`button` 的 `contentSchema` 只有 `text` / `icon` / `visibleWhen`。跳转、弹提示一律写行为：

```json
"behaviorJson": { "onClickAction": { "action": "navigate", "url": "/home" } }
```

（`image` 组件则相反：它的 content 里有 `linkType` / `linkUrl`。）

## 8. 文本类组件没有 `margin`

`text` / `heading` 的 styleSchema 只有 `width, align, padding, backgroundColor, fontSize, fontWeight,
textColor, textAlign, lineHeight, letterSpacing`（heading 更少）。
要间距就用外层容器的 `padding`，别给叶子加 margin。

## 9. 报错信息怎么读

后端返回的是「`<组件名> / <样式|内容|行为>`：`<原因>`」，例如
「按钮 / 样式：内边距格式不合法」「文本 / 内容：文本内容不能超过 200 个字」。
按组件名定位到是哪个节点，按字段名改值即可。

## 10. 组件库变了要重拉快照

`validate` 依据的是 `references/component-catalog.json`。管理端改了组件库（新增组件、
调整某类型开放的属性）之后，**必须重跑 `pagekit catalog`**，否则本地预检会与后端不一致。

## 11. 令牌与凭据

默认令牌文件是 `.qwen/tmp/pagekit-token.json`（已 gitignore）。
在别的机器或在脚本里跑，用环境变量 `PAGEKIT_TOKEN`；**不要**把令牌写进任何会被提交的文件。

## 12. 页面与首页

- `pageKey` 全局唯一；`routePath` 是用户端访问路径。
- 首页（`isHome=1`）全站唯一，且**不可删除**——`delete-page` 对首页会被后端拒绝。
- `pages` 里的 `nodeCount` 是**含子节点**的总数。
