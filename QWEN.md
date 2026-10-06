# QWEN.md

可定制游戏社区平台（毕业设计）—— 对标米游社的双端垂直游戏社区。以「板块（= 一款游戏的分区）」组织内容，用「游戏版本」与「玩家进度阶段」两个维度组织帖子；后台可图形化配置站点外观与内容维度，不需要改代码。

## 目录结构

| 路径 | 说明 |
|---|---|
| `backend/` | Spring Boot 后端（双端共用），端口 9331 |
| `backend/src/main/resources/templates/` | **社区模板数据**（manifest + 站点配置 + 组件库定义 + 各页面节点树 + 初始内容）；「创建社区」向导读它建站 |
| `simple-template-f-admin/` | 管理端前端（Vue 3 + Element Plus），Vite 代理 `/api` → 9331 |
| `simple-template-f-user/` | 用户端前端（Vue 3） |
| `sql/schema.sql` | 21 张表建表脚本（已执行到 `ssy_db`） |
| `sql/seed.sql` | 种子数据 |
| `sql/ui_*.sql` | 增量脚本：搭建域建表与组件/原子/预设种子、登录页、页面搭建菜单、模板菜单 |
| `docker-compose.yml` + `docker/` | **一键部署**：MySQL + 后端 + nginx（托管两个前端）三个容器。见 [DEPLOY.md](DEPLOY.md) 快速路径 |
| `readme.md` | 项目介绍，**不是功能规范** |

> **只进本机、不进版本库**的内容（见 `.gitignore`）：设计叙事与需求规范（`pit.md`、
> `16-毕设数据库设计.md`、`用例图-mermaid.md`、各 `*设计.json`），以及给 AI 用的页面工具包
> `page-kit/` 与 `.qwen/`。版本库里只放**部署与运行真正需要**的东西，设计口径在开发机上看这些文件。

## 默认社区模板（Default Game Community v2）

管理端「站点配置 → 创建社区」是唯一的建站入口：选模板 → 填社区名称/游戏名称（Logo/主色/简介可选）→
创建，即得到首页、攻略与资料、社区板块、帖子与评论、发帖页、公告、个人主页一整套页面。

- 模板**只写数据**，写库全部复用 `UiPageService.saveNodes` / `SiteService.putValues`，因此属性白名单与值类型校验不会被绕过。
- 模板里不写死社区名/游戏名：`{{siteName}}` 这类占位符由 `TemplateServiceImpl` 从表单输入替换。
- 幂等：站点配置按 key upsert、组件库按 type_code upsert、页面按 page_key 新建；
  已有页面只在勾选「按模板重建页面」时才写节点；初始内容一律「该表为空才写」。
- 模板里的项写成占位符且替换后为空 ⇒ **跳过不写**（留空＝"这项我不填"，而不是清空已有值）。
- 新增一个模板 = 在 `templates/` 下加目录 + 在 `templates/index.json` 里加一行，Java 与 Vue 都不用改。
- 页面的 `route_path` 支持 `:参数`；用户端在 `registerPageRoutes` 里把带参数的路径注册成真参数路由，
  否则 `route.params` 里只有 `pathMatch`，`/post/:id` 这类页面拿不到 id。
- 模板页共 10 个：首页、攻略与资料、社区板块、板块详情（`/board/:id`）、帖子检索、帖子详情、**发布新帖（`/publish`）**、公告列表、公告详情、个人主页。
- 组件库分两部分：`sql/ui_*.sql` 装到 22 个（全新安装的平台自带），模板建站时按 `templates/**/components.json`
  upsert 到 28 个（补 6 个业务组件 + 给 11 个既有组件补开放属性，如 `button` 的 `linkType/linkUrl/openInNewTab`）。
  **改组件开放属性要连页面的模板块一起看**：页面节点上的每个 key 都必须在该类型开放的属性里，
  否则 `saveNodes` 会拒绝整棵节点树。
- 模板资源改完跑一次 `TemplateResourceTest`：它把"只有真去应用模板才会炸"的几类问题（属性没登记、
  值不合法、页面文件与 manifest 对不上、路由重复）提前到不连数据库的单元测试。

## 发帖闭环（用户端产内容的唯一入口）

- 页头导航的「发帖」、社区板块页与攻略页的「发帖」按钮（`button` 节点 + `linkType=internal` / `linkUrl=/publish`）
  都指向 `/publish`，页面上是 `builtin:post_form`。
- `post_form` 的板块列表、版本、进度阶段、标签全部按所选板块现取（`/app/board/list`、`/app/board/{id}`、`/app/tag/list`）；
  换板块会把版本/阶段/标签整组换掉——跨板块的 `versionId` 后端会直接拒。
- 封面走 `POST /app/upload/image`（MEMBER 令牌，匿名 401）；个人主页头像走同一个接口。
  上传目录与对外基址由 `UPLOAD_DIR` / `UPLOAD_PUBLIC_BASE_URL` 决定，库里只存 objectKey。
- 提交是 `POST /app/post`，落库状态**恒为待审**；管理员在「内容管理 → 帖子审核」通过后才出现在公开列表与热门里，
  作者自己在个人主页能看到待审/驳回状态与审核意见。

## 后端写接口的权限校验

`/admin/**` 有两道拦截器：`AuthInterceptor`（校验 ADMIN 令牌）+ `AdminPermInterceptor`
（非 GET 请求按 URL 前缀查 `sys_menu.perms`）。**新增后台写接口时必须把 URL 前缀登记进
`AdminPermInterceptor.RULES`**，否则会被 fail-closed 拒绝（提示里会带上方法 + 路径）。

## 技术栈

后端 Spring Boot 3.5.7 / Java 17（坐标 `com.lelcc:lelcc-backend`）/ MyBatis-Plus 3.5.5 / Hutool 5.8.25；
数据库 MySQL 9.5；前端 Vue 3 + TypeScript + Vite + Pinia + Element Plus。

## 构建与运行（先看这条）

> 部署到服务器请直接看 [DEPLOY.md](DEPLOY.md)：**想省事就 Docker Compose 一条命令**
> （`cp docker/.env.example .env` → 改两个值 → `docker compose up -d --build`，连 JDK/Node/nginx 都不用装）；
> 想手动部署那份文档也全有（`sql/install.sql` 一条命令建库、配置全部走环境变量、nginx 生产托管示例）。
> 本节只讲本地开发。

本机 `JAVA_HOME` 指向 JDK 25，直接 `mvn` 会因 Lombok 不生成 getter 而报「找不到符号」。**必须用 JDK 21**：

```
set "JAVA_HOME=C:\Program Files\Java\jdk-21" && mvn -B -DskipTests clean compile
```

前端类型检查：在对应前端目录执行 `npx vue-tsc -b --force`。

数据库（客户端 `E:\Code\CodeEditPath\MySQl\SQL9\bin\mysql.exe`，`localhost:3306`）。
**空库装一遍用 `sql/install.sql`**（按序 source 全部脚本，末尾自带自检表）；
已有库的增量改动写新的 `sql/*.sql`，不要改 `install.sql` 里的顺序去兼容：

```
set "MYSQL_PWD=123456" && mysql --default-character-set=utf8mb4 -u root -e "source E:/Code/AAAEndClass/sql/install.sql"
```

单跑某个脚本时同理。用 `MYSQL_PWD` 而不是 `-p123456`，避免密码出现在命令行里。
另外：**写含中文的 .sql 临时文件必须用 UTF-8**（PowerShell `-Encoding ascii` 会把中文变成 `?` 直接写坏数据）。

## API 约定

- **路径前缀即鉴权边界**：后台接口一律 `/admin/**`（放行 `/admin/login`），用户端一律 `/app/**`（放行 `/app/login`、`/app/register`）。**挂在其他前缀下的接口不会被鉴权**，新增 Controller 时务必遵守。
- 令牌走标准头 `Authorization: Bearer <jwt>`；JWT 载荷含 `userId` + `userType`（`ADMIN` / `MEMBER`），两种令牌不可跨端使用。
- 统一响应体 `com.lelcc.common.Result`（`code` / `message` / `data`），业务成功 `code = 200`；异常由 `GlobalExceptionHandler` 统一转换。
- 当前登录主体取 `UserContext.get()`，返回 `LoginUser(id, type)`。
- 用 Hutool 校验令牌必须用 `jwt.setKey(...).validate(0)`——`JWTUtil.verify()` 只验签、**不校验 exp**，过期令牌也会返回 true。

## 数据模型要点

- **不使用物理外键**（逻辑外键 + 索引 + 应用层校验），这是有意设计，不要改成外键。
- 双端账号分表：后台 `sys_user`、玩家 `member`。
- 内容维度：`board_version`（版本）、`progress_stage`（进度阶段）；帖子通过 `post.version_id` / `post.stage_id` 关联，两者可空表示「通用帖」。
- **「当前版本」的唯一事实源是 `board.current_version_id`**，`board_version` 没有 `is_current` 字段。
- 版本过期是**动态判定**（`post.version_id != board.current_version_id`），不存状态字段，历史版本内容不隐藏。
- 冗余计数 `post.like_count` / `favorite_count` / `comment_count` 必须与明细表在同一事务内保持一致。
- 评论两级：`parent_id` = 被回复的评论 id，`root_id` = 所属一级评论 id。
- 玩家的游玩版本是**可选**的（`member_game_progress.version_id` 可空），是否公开由用户勾选（`is_public`）。

## 协作约定

- **按模块闭环**：每完成一个可独立验证的单元（一个 CRUD 模块 / 一条完整链路 / 一次 schema 变更），就走完 生成 → 编译 → 验证 → `git commit`，不要攒一批再联调。
- 提交消息含中文时用 `git commit -F 文件`——cmd.exe 下 `git commit -m "中文"` 会转码乱码。
- 关联数据不物理删除，只改状态字段（板块 `status`、帖子 `status`、玩家 `status` = 封禁）。
- 管理端挂的是 Element Plus 中文语言包（`main.ts` 里 `app.use(ElementPlus, { locale: zhCn })`）。
  不挂的话弹窗按钮会是 OK / Cancel、分页显示 `10/page`——界面全是中文而内置文案是英文。

## 双端共享代码的四条红线（踩过的坑，别再踩）

管理端与用户端共用 `src/blocks/**`（业务组件与渲染内核）、`src/logic/**`（逻辑引擎）、
`src/utils/session.ts`（玩家登录态）。两端把令牌存在**同一个键** `localStorage.token` 上，
所以共享代码在后台上下文里跑时会看到后台令牌。以下四条是被真实 bug 逼出来的约定：

1. **共享业务组件在后台画布里（`interactive === true`）不得调用需要玩家登录态的接口**
   （`/app/member/**`、`/app/post/{id}/interaction` 等）。后台令牌去调它们只会拿到 401，
   而 401 会走到后台的请求拦截器里——曾经因此把正在编辑页面的管理员直接踢到登录页。
   这类组件要自己判断 `interactive`：不取数、不放可点的登录/跳转入口，只给静态预览。
2. **`utils/session.ts` 只认 `MEMBER` 令牌**（解 JWT 载荷里的 `userType` 判断），
   并且**绝不清除非 MEMBER 令牌**——那不是它的令牌，删掉就等于把管理员登出。
3. **后台请求拦截器只对 `/admin/**` 的 401 登出**：`/app/**` 的 401 含义是"令牌身份不对"，
   不是"后台登录态失效"，据此登出是误伤。
4. **用户端页面路由必须在首次导航之前注册**（见 `router/index.ts` 的 `beforeEach`）。
   页面路由来自后端下发的 `ui_page.route_path`，晚一步注册就会先按兜底路由匹配一次：
   `/post/1` 拿不到 `:id` 先显示"未指定"，`/board/1` 还会先用 `autoBoard` 的板块发一次请求再纠正。

改这两个目录里的任何文件，都要同步到另一端（两端是逐字节相同的副本），改完两端都跑
`node node_modules/vue-tsc/bin/vue-tsc.js -b --force`。

## 页面生成器的一个边界

业务组件取数在 `interactive === true`（管理端画布）时也照常跑，**只有需要登录态的接口例外**。
所以新增业务组件时先问一句：这个接口匿名可读吗？不是的话就得按上面的第 1 条处理。

**占位逻辑不会自己失效**：为"目标页面还没做"临时写的兜底（例如 `builtin:hot_post` 曾经点了只弹一句
「详情页待接入」）在页面落地后依然会跑，表现成"这里点不动"。补完页面后回头搜一遍
`待接入 / 还没做 / 暂未` 把占位清掉——`builtin:post_list` 与 `builtin:hot_post` 跳到同一个
`/post/:id`，两处行为必须一致。
