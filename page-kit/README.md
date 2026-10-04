# page-kit —— 让 AI agent 生成「符合本项目规则的页面」

本工具包让任意 AI agent 能**直接产出可落库的页面数据**：控制的是用户端的页面结构与外观
（管理端「页面搭建」里的组件树），**不写 Vue 代码**，符合本项目「零代码配置」的定位。

- 覆盖范围：**呈现层** = `ui_page`（页面）+ `ui_node`（组件树）。
- 不覆盖：逻辑图 `ui_flow`（数据从哪来 / 点击走图）、组件库定义、站点样式 token。
- 依赖：Node 18+，**零 npm 依赖**（只用 Node 内置能力）。
- 产出形态：节点数组 JSON —— 与后端整树保存接口同一形状，校验通过即可 `save` 落库。

## 快速开始

```bash
# 1) 起后端（本仓库：backend 目录，端口 9331），然后登录一次拿令牌
node page-kit/pagekit.mjs login --user admin --password admin123

# 2) 拉一份组件库快照（validate 的依据；组件库改了要重拉）
node page-kit/pagekit.mjs catalog

# 3) 看看有哪些页面
node page-kit/pagekit.mjs pages

# 4) 校验 + 落库（示例页面可直接用）
node page-kit/pagekit.mjs validate page-kit/examples/landing-page.json
node page-kit/pagekit.mjs save home page-kit/examples/landing-page.json

# 5) 改已有页面：先导出，再改，再回灌
node page-kit/pagekit.mjs get home --out my-home.json
#   ...编辑 my-home.json...
node page-kit/pagekit.mjs save home my-home.json
```

令牌缓存在 `.qwen/tmp/pagekit-token.json`（该目录已 gitignore）。生产或别人的机器上请改用环境变量：

```bash
PAGEKIT_TOKEN=<jwt> node page-kit/pagekit.mjs pages
node page-kit/pagekit.mjs --base http://your-host:9331 pages   # 换后端地址
```

## 命令一览

| 命令 | 作用 |
|---|---|
| `login --user <账号> --password <密码>` | 登录并把令牌写入缓存文件 |
| `catalog [--out <file>]` | 导出组件库快照到 `references/component-catalog.json` |
| `pages` | 列出页面（id / pageKey / routePath / 节点数） |
| `get <pageId\|pageKey> [--out <file>]` | 导出页面节点（可回灌保存的扁平数组） |
| `validate <file>` | 离线结构预检（错误阻断，约定问题给提醒） |
| `save <pageId\|pageKey> <file>` | 整树保存（**未提交的旧节点会被删除**） |
| `create-page --key <k> --name <n> [--route <r>]` | 新建页面 |
| `delete-page <pageId\|pageKey>` | 删除页面（首页会被后端拒绝） |

通用参数：`--base <url>`（默认 `http://localhost:9331`）、`--json`（机器可读输出）、
`--token` / `--token-file` / `--catalog`。退出码：`0` 成功、`1` 失败 —— agent 可直接据此分支。

## 给 agent 用

- **Qwen Code**：本仓库已放好项目技能 `.qwen/skills/page-authoring/SKILL.md`，模型会按需自动调用。
- **其他 agent（Claude Code / Codex / 自研）**：本目录就是一份普通技能包——把 `page-kit/`
  挂到该 agent 的技能目录，或直接让它读 [SKILL.md](SKILL.md)。脚本与 agent 无关，用 `node` 直接跑。
- 不提供 MCP server：受众是任意 agent，多一个要起、要带令牌的进程不划算；日后需要的话把这层
  包一下即可，语料与脚本不用动。

## 规则以谁为准

| 层 | 谁保证 |
|---|---|
| 组件是否开放该属性、值类型（颜色/长度/枚举…）、父节点是否容器 | **后端保存接口**（权威；失败原子回滚，不会写半套） |
| 由组件库快照决定的结构规则 + 树的引用完整性 | 本脚本 `validate`（离线，不复制值类型规则，因此不会漂移） |
| 排版与观感（页面根竖排、横排用「行」、builtin 组件暂不渲染…） | [references/layout-rules.md](references/layout-rules.md) + 你在浏览器里看 |

细则与坑见 [references/](references/)，可照抄的样例见 [examples/](examples/)。
