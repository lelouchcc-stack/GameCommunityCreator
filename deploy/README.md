# deploy/ —— Docker Compose 一键部署（v3：认证 + 所有权 + Markdown）

三个容器：`mysql`（MySQL 8.4）+ `backend`（Spring Boot，镜像内为 JRE 21）+ `frontend`（nginx 托管前端构建产物并反代 `/api`）。

## 快速开始

```bash
cd deploy
cp .env.example .env          # 必须改 MYSQL_ROOT_PASSWORD、APP_BOOTSTRAP_PASSWORD
docker compose up -d --build
# 浏览器打开 http://localhost（或 .env 里的 FRONTEND_PORT）
```

> **v3 首次启动必须配置 `APP_BOOTSTRAP_USERNAME` / `APP_BOOTSTRAP_PASSWORD`**：
> 后端启动时用它创建初始开发者并认领 `owner_user_id IS NULL` 的旧项目（如 seed 的 `stellar-drift` / `echo-garden`）。
> 不配置且用户表为空时：只记录警告、不创建明文默认口令，旧项目在管理端不可见（可访问 `/register` 注册后新建项目）。

首次启动时 MySQL 数据卷为空，会自动按文件名顺序执行初始化脚本：

- `database/schema.sql` → `/docker-entrypoint-initdb.d/01-schema.sql`（建库建表）
- `database/seed.sql` → `/docker-entrypoint-initdb.d/02-seed.sql`（演示数据，`owner_user_id` 留空由后端认领）

**已有 v1/v2 数据库升级**：不要删数据卷，手工按序执行迁移（幂等）：

```bash
MYSQL_PWD=<root 口令> mysql -u root --default-character-set=utf8mb4 gcc_mvp < database/migrations/002_multi_page.sql
MYSQL_PWD=<root 口令> mysql -u root --default-character-set=utf8mb4 gcc_mvp < database/migrations/003_auth_ownership_markdown.sql
# 然后重启 backend 容器（BootstrapAccountInitializer 会创建初始开发者并认领 owner_user_id IS NULL 的项目）
docker compose restart backend
```

**重新初始化数据库**：初始化脚本只在空数据卷时执行，需要重来就 `docker compose down -v` 再 `up`（`-v` 会删掉 `gcc-mysql-data` 卷，请确认没有要保留的数据）。

## 环境变量（`.env.example` 为示例值，**不含真实口令**）

| 变量 | 默认 | 说明 |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | 示例占位 | **必填**：MySQL root 密码，部署前必须改 |
| `MYSQL_DATABASE` | `gcc_mvp` | 数据库名 |
| `MYSQL_PORT` | `3306` | 宿主机端口，**仅绑定 `127.0.0.1`**（不暴露公网/局域网） |
| `BACKEND_PORT` | `8080` | 后端调试端口，**仅绑定 `127.0.0.1`**（浏览器正常走 nginx 反代） |
| `FRONTEND_PORT` | `80` | 浏览器访问端口（唯一对外端口） |
| `CORS_ALLOWED_ORIGINS` | `http://localhost` | 后端 CORS 白名单（同源部署填前端访问地址） |
| `APP_BOOTSTRAP_USERNAME` | `dev` | **首次启动必填**：初始开发者用户名（3–30 位字母/数字/下划线/中划线） |
| `APP_BOOTSTRAP_PASSWORD` | 示例占位 | **首次启动必填**：初始开发者密码（≥8 位且同时含字母与数字；部署前改强密码） |
| `APP_BOOTSTRAP_DISPLAY_NAME` | `独立开发者` | 初始开发者显示名 |
| `JWT_SECRET` | 示例占位 | **生产必填**：Sa-Token JWT 插件签名密钥（HS256，≥32 字节随机强密钥；示例值不可用于生产） |
| `SA_TOKEN_TIMEOUT` | `7200` | 登录态有效期（秒，默认 7200 = 2 小时；不自动续签，对应 `sa-token.timeout`） |

## 只校验配置（本机 Docker 守护进程未启动时）

```bash
cd deploy
docker compose --env-file .env.example config
```

该命令只做 compose 文件解析、变量替换与语法校验，不需要 Docker 守护进程。

## 生产 HTTPS 与安全说明

1. **HTTPS 终止**：在 nginx/frontend 容器或外层网关配置 TLS 证书；浏览器只访问 443/80。
2. **认证（Sa-Token + JWT Simple）**：后端使用 Sa-Token 1.46.0 + `sa-token-jwt` 插件，token 为 **JWT 格式**（HS256），前端统一放在 `Authorization: Bearer <token>` 请求头；签名密钥 `sa-token.jwt-secret-key` 由 `JWT_SECRET` 注入（≥32 字节随机值，禁止使用示例值），有效期由 `SA_TOKEN_TIMEOUT`（秒，默认 7200 = 2 小时）控制。生产必须 HTTPS，避免 token 在链路上被窃听。
3. **登录态与登出**：Simple 模式下登录态由 Sa-Token 服务端保存（默认内存 `SaTokenDao`），`logout` / 踢人会让旧 token 立即失效；**默认内存仅适合单实例部署，多实例/集群需接入 Sa-Token Redis 插件**（当前 compose 为单实例）。重启 backend 会清空内存会话，用户需重新登录。
4. **端口暴露**：MySQL 与后端调试端口均只绑定 `127.0.0.1`；对外只开 frontend 端口。
5. **初始开发者**：`APP_BOOTSTRAP_*` 仅首次启动生效；上线后可在数据库/管理端维护账号（MVP 不提供账号管理 UI 时请用 SQL 或重复启动逻辑变更）。
6. **数据库账号**：当前 compose 使用 root 连接数据库；公网部署建议改为最小权限专用账号（`DB_USERNAME` / `DB_PASSWORD`）。
7. **XSS**：内容正文 Markdown 经 DOMPurify 清洗后渲染，页面配置禁止 `eval`/`new Function`/配置驱动 `v-html`。
8. **备份**：定期备份 `gcc-mysql-data` 卷与发布快照数据。

> 部署后自检：`curl http://localhost/api/health` 应返回 `{"code":200,...,"status":"UP","database":"UP"}`；
> 打开登录页用初始开发者登录，应能看到被认领的 seed 站点（`stellar-drift` / `echo-garden`）。

## 常用操作

| 操作 | 命令 |
|---|---|
| 查看服务状态 | `docker compose ps` |
| 查看后端日志 | `docker compose logs -f backend` |
| 只重建前端 | `docker compose up -d --build frontend` |
| 重启后端（迁移/环境变量变更后） | `docker compose restart backend` |
| 停止并保留数据 | `docker compose down` |
| 停止并删除数据卷 | `docker compose down -v` |

## 说明

- 前端 nginx 的 `/api/` 反代目标是 compose 网络内的 `backend:8080`，不需要改前端代码。
- 后端数据库/认证配置（`JWT_SECRET` / `SA_TOKEN_TIMEOUT` 等）全部通过环境变量注入，镜像里没有任何硬编码密码；数据库只存 BCrypt 哈希。
- 更多安全边界与上线前补齐项见 [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)。
