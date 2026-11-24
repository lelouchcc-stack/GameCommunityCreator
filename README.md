# GameCommunityCreator

**配置驱动的多页面游戏社区搭建系统** —— 游戏开发者注册登录后，为自己的游戏搭建一整套可配置的门户站点：首页、更新日志、公开文档、制作团队、问题反馈、友情链接、自定义页面。页面通过三栏可视化编辑器搭建，草稿与发布快照相互隔离；玩家与访客无需登录即可浏览门户并提交反馈。

改配置即可改站点，无需改代码。

---

## 目录

- [核心特性](#核心特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [Docker 一键部署](#docker-一键部署)
- [配置项](#配置项)
- [路由与功能地图](#路由与功能地图)
- [数据模型](#数据模型)
- [辅助脚本](#辅助脚本)
- [测试](#测试)
- [安全说明](#安全说明)

---

## 核心特性

- **配置驱动渲染**：一个站点 = 多张页面，每张页面由 `PageConfig` JSON（`schemaVersion=2`）描述，含主题、组件列表与动作协议。后端做权威校验（未知字段/非法值直接拒绝），前端按 JSON 渲染。
- **11 类可配组件**：`hero` / `about` / `notice_list` / `post_list` / `changelog_list` / `doc_list` / `team_list` / `link_list` / `issue_form` / `footer` / `button`。按钮动作仅支持站内跳转（`navigate`）、外链（`externalLink`）与无动作（`none`）。
- **三栏可视化编辑器**：左侧组件面板（新增/删除/上移下移）、中间画布、右侧属性面板；轻校验（前端即时提示）+ 权威校验（后端）+ 乐观锁（`expectedVersion` 冲突返回 409）。
- **草稿与发布快照隔离**：编辑只写 `site_page_draft`；`publish` 生成不可变 `site_page_publication` 快照；`publish-site` 批量发布。公开端只读快照，草稿改动不影响线上内容。
- **开发者认证与所有权隔离**：注册 / 登录 / 登出 / `me`，Sa-Token 1.46.0 + JWT Simple（JWT 格式 token + 服务端登录态 + BCrypt 口令）。`community_project.owner_user_id` 绑定所有权，未认证 401、越权 403，多开发者的项目/页面/内容/发布完全隔离。
- **Markdown 正文**：更新日志、公开文档、公告正文支持 Markdown（编辑/预览/分栏三模式）。渲染走 `marked` + `DOMPurify` 白名单清洗：禁止原始 HTML、事件属性、`javascript:` / `data:` / `file:` 链接，外链自动加 `noopener noreferrer`。
- **公开门户**：`/community` 门户广场 + 多页面路由、深浅色主题、导航、内容模块展示；匿名可读，问题反馈匿名可提交。
- **默认模板开箱即用**：创建站点自动生成 6 张默认页面（首页 + 更新日志 / 公开文档 / 制作团队 / 问题反馈 / 友情链接），每张带默认模板与示例内容。
- **管理端独立主题**：`DARK / LIGHT / SYSTEM` 三态，`localStorage['gcc.admin.theme']` 持久化，不影响公开站点主题与发布版本。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 21、Spring Boot 3.5.7、Spring Web / Validation / JDBC（JdbcTemplate）、Sa-Token 1.46.0（JWT Simple）、Hutool JWT、spring-security-crypto（BCrypt） |
| 数据库 | MySQL 8.x（utf8mb4 / InnoDB，逻辑外键 + 索引，无物理外键） |
| 前端 | Vue 3.5、TypeScript 5.7、Vite 6、Vue Router 4、Pinia 4、marked + DOMPurify |
| 测试 | Vitest + @vue/test-utils + jsdom；Playwright（浏览器端到端验收脚本） |
| 部署 | Docker / Docker Compose（MySQL 8.4 + 后端镜像 + nginx 静态托管与 `/api` 反代） |

## 项目结构

```
GameCommunityCreator/
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/com/gcc/community/
│   │   ├── controller/       # REST 控制器（11 个）
│   │   ├── domain/           # 领域实体（对应数据库表）
│   │   ├── dto/              # 请求/响应 DTO
│   │   ├── repository/       # JdbcTemplate 数据访问
│   │   ├── service/          # 业务逻辑
│   │   ├── security/         # 认证、初始账号初始化、权限校验
│   │   ├── config/           # Sa-Token / CORS / 密码等配置
│   │   └── common/           # 统一响应、分页、异常处理
│   ├── src/main/resources/application.yml
│   └── Dockerfile            # 多阶段构建（maven → JRE）
├── frontend/                 # Vue 3 + Vite 前端
│   ├── src/
│   │   ├── views/admin/      # 管理端：总览/模板/组件库/站点/页面/编辑器/预览/内容/设置
│   │   ├── views/public/     # 公开端：门户、更新日志、文档、团队、反馈、链接、公告、进展
│   │   ├── views/auth/       # 登录 / 注册
│   │   ├── editor/           # 三栏编辑器组件
│   │   ├── runtime/          # JSON 驱动的门户渲染运行时
│   │   ├── api/              # 接口封装（含 token 注入）
│   │   └── stores/           # Pinia（认证态等）
│   ├── tests/                # Vitest 单测
│   ├── Dockerfile
│   └── nginx.conf            # 静态托管 + /api 反代
├── database/
│   ├── schema.sql            # 建表（14 张表，幂等）
│   ├── seed.sql              # 演示数据（幂等）
│   └── migrations/           # 旧库升级脚本（002/003/004）
├── deploy/
│   ├── docker-compose.yml    # MySQL + 后端 + 前端 一键拉起
│   ├── .env.example          # 部署环境变量模板
│   └── README.md             # 部署说明
├── scripts/                  # 数据库初始化、冒烟测试、浏览器 E2E、截图脚本
└── README.md
```

## 快速开始

### 环境要求

| 依赖 | 版本 |
|---|---|
| JDK | 21+ |
| Maven | 3.9+ |
| Node.js | 18+（推荐 20+） |
| MySQL | 8.0+（推荐 8.4） |

### 1. 初始化数据库

```bash
# 建库 + 建表 + 导入演示数据（可重复执行，默认库名 gcc_mvp）
bash scripts/init-db.sh

# 或手动执行：
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS gcc_mvp DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p --default-character-set=utf8mb4 gcc_mvp < database/schema.sql
mysql -u root -p --default-character-set=utf8mb4 gcc_mvp < database/seed.sql
```

Windows 下可用 `scripts/init-db.bat`。

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认监听 `http://localhost:8080`，健康检查 `GET /api/health`。

数据库连接与初始账号可用环境变量覆盖：

```bash
DB_URL='jdbc:mysql://localhost:3306/gcc_mvp?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' \
DB_USERNAME=root DB_PASSWORD=123456 \
APP_BOOTSTRAP_USERNAME=dev APP_BOOTSTRAP_PASSWORD=dev123456 \
JWT_SECRET='please-change-to-a-random-secret-at-least-32-bytes' \
mvn spring-boot:run
```

> 首次启动时，只有提供 `APP_BOOTSTRAP_USERNAME` + `APP_BOOTSTRAP_PASSWORD`（≥8 位且同时含字母与数字）才会自动创建初始开发者账号并认领演示项目；不提供则直接访问 `/register` 自助注册。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

打开 `http://localhost:5173`。Vite 已把 `/api` 反代到 `http://localhost:8080`（可用 `VITE_API_PROXY_TARGET` 修改）。

### 4. 试试看

1. 用初始开发者账号（或刚注册的账号）登录 → 进入管理端总览；
2. 创建游戏站点，自动生成 6 张默认页面；
3. 进入任意页面的可视化编辑器，调整组件并保存草稿；
4. 预览草稿 → 发布 → 打开 `/community/{slug}` 查看公开门户。

## Docker 一键部署

```bash
cd deploy
cp .env.example .env          # 至少修改 MYSQL_ROOT_PASSWORD / JWT_SECRET / APP_BOOTSTRAP_*
docker compose up -d --build
# 浏览器访问 http://localhost:${FRONTEND_PORT:-80}
```

- `database/schema.sql` + `database/seed.sql` 挂载到 MySQL 容器的 `/docker-entrypoint-initdb.d/`，仅在数据卷**首次初始化**时执行；要重置数据：`docker compose down -v && docker compose up -d --build`。
- 前端 nginx 把 `/api/` 反代到 `backend:8080`，浏览器只访问一个端口。
- MySQL 与后端调试端口只绑定 `127.0.0.1`，不暴露到局域网/公网。
- 校验 compose 配置（不需要真实密码）：`docker compose --env-file .env.example config`。

## 配置项

### 后端（环境变量）

| 变量 | 默认值 | 说明 |
|---|---|---|
| `SERVER_PORT` | `8080` | 服务端口 |
| `DB_URL` | 本机 `gcc_mvp` | JDBC 连接串 |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `123456` | 数据库账号（生产务必改） |
| `JWT_SECRET` | 内置开发用弱密钥 | HS256 签名密钥，**≥32 字节**，生产必须覆盖 |
| `SA_TOKEN_TIMEOUT` | `7200` | 登录态有效期（秒），不自动续签 |
| `CORS_ALLOWED_ORIGINS` | 本地开发地址 | 允许的前端来源，逗号分隔 |
| `UPLOAD_DIR` | `./data/uploads` | 上传图片保存目录（已加入 `.gitignore`） |
| `APP_BOOTSTRAP_USERNAME` | 空 | 初始开发者用户名（3–30 位字母/数字/下划线/中划线） |
| `APP_BOOTSTRAP_PASSWORD` | 空 | 初始开发者密码（≥8 位且含字母与数字） |
| `APP_BOOTSTRAP_DISPLAY_NAME` | `独立开发者` | 初始开发者显示名 |

页面配置 JSON 有大小上限（`app.page-config.max-json-bytes`，默认 200 KB），上传图片单文件上限 5 MB。

### 前端（构建期 / 开发期）

| 变量 | 默认值 | 说明 |
|---|---|---|
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Vite dev/preview 的 `/api` 反代目标 |

## 路由与功能地图

**管理端**（需登录）

| 路径 | 页面 |
|---|---|
| `/login`、`/register` | 开发者登录 / 注册 |
| `/admin/overview` | 控制台总览 |
| `/admin/templates` | 模板中心 |
| `/admin/components` | 组件库 |
| `/admin/settings` | 管理端设置（主题等） |
| `/admin/projects` | 我的游戏站点 |
| `/admin/projects/new` | 创建站点 |
| `/admin/projects/:id/pages` | 页面列表 |
| `/admin/projects/:id/pages/:pageId/editor` | 可视化编辑器 |
| `/admin/projects/:id/pages/:pageId/preview` | 草稿预览 |
| `/admin/projects/:id/settings` | 导航与主题 |
| `/admin/projects/:id/content/:module` | 内容管理（更新日志/文档/团队/链接/反馈等） |

**公开端**（匿名可访问）

| 路径 | 页面 |
|---|---|
| `/community` | 游戏门户广场 |
| `/community/:slug` | 门户首页 |
| `/community/:slug/p/:pageSlug` | 自定义页面 |
| `/community/:slug/changelog`、`/changelog/:entryId` | 更新日志与详情 |
| `/community/:slug/docs`、`/docs/:docSlug` | 公开文档与详情 |
| `/community/:slug/team` | 制作团队 |
| `/community/:slug/report` | 问题反馈 |
| `/community/:slug/progress` | 问题处理进展 |
| `/community/:slug/links` | 友情链接 |
| `/community/:slug/news/:announcementId` | 公告详情 |

REST 接口统一以 `/api` 为前缀，后端控制器位于 `backend/src/main/java/com/gcc/community/controller/`。

## 数据模型

`database/schema.sql` 共 14 张表（全部 `CREATE TABLE IF NOT EXISTS`，可重复执行；应用层维护逻辑外键与事务级联删除）：

| 分组 | 表 |
|---|---|
| 认证 | `app_user` |
| 站点 | `community_project`、`game_info`、`announcement` |
| 多页面 | `site_page`、`site_page_draft`、`site_page_publication`、`site_settings`、`site_navigation` |
| 内容模块 | `changelog`、`doc_article`、`team_member`、`issue_report`、`friend_link` |

旧库升级见 `database/migrations/`（`002_multi_page.sql`、`003_auth_ownership_markdown.sql`、`004_issue_progress.sql`），全新部署不需要执行。

## 辅助脚本

| 脚本 | 用途 |
|---|---|
| `scripts/init-db.sh` / `init-db.bat` | 建库 + 建表 + 导入演示数据（幂等） |
| `scripts/smoke-test.mjs` | 后端接口冒烟测试 |
| `scripts/e2e-browser.mjs` | Playwright 浏览器端到端验收 |
| `scripts/capture-screenshots.mjs` | 页面截图采集 |

## 测试

```bash
# 前端单测（Vitest）
cd frontend && npm run typecheck && npm test

# 后端编译与测试（当前无单测，构建即验证）
cd backend && mvn -B test

# 前端浏览器端到端（需后端与前端已启动）
node scripts/e2e-browser.mjs
```

## 安全说明

- **认证**：Sa-Token + JWT Simple，token 只从 `Authorization: Bearer` 请求头读取，不走 Cookie（避免 CSRF）；口令用 BCrypt 存储。
- **越权防护**：所有管理端接口校验登录态与 `owner_user_id` 所有权，越权返回 403。
- **XSS 防护**：Markdown 渲染统一经 DOMPurify 白名单清洗，禁用原始 HTML 与危险协议链接。
- **生产必改**：`JWT_SECRET`（≥32 字节随机值）、`DB_PASSWORD`、`MYSQL_ROOT_PASSWORD`、初始开发者口令；并在 nginx/网关终止 HTTPS。
- **未入库内容**：`backend/data/`（运行期上传图片）、`.env`、构建产物与 IDE 配置均已写入 `.gitignore`。

## 许可

本仓库暂未附带 LICENSE 文件，如需开源使用请自行补充许可协议。
