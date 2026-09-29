# QWEN.md

可定制游戏社区平台（毕业设计）—— 对标米游社的双端垂直游戏社区。以「板块（= 一款游戏的分区）」组织内容，用「游戏版本」与「玩家进度阶段」两个维度组织帖子；后台可图形化配置站点外观与内容维度，不需要改代码。

## 目录结构

| 路径 | 说明 |
|---|---|
| `backend/` | Spring Boot 后端（双端共用），端口 9331 |
| `simple-template-f-admin/` | 管理端前端（Vue 3 + Element Plus），Vite 代理 `/api` → 9331 |
| `simple-template-f-user/` | 用户端前端（Vue 3） |
| `sql/schema.sql` | 21 张表建表脚本（已执行到 `ssy_db`） |
| `sql/seed.sql` | 种子数据 |
| `16-毕设数据库设计.md` | 数据库设计文档，**设计叙事以它为准** |
| `readme.md` | 项目介绍，**不是功能规范** |

## 技术栈

后端 Spring Boot 3.5.7 / Java 17（坐标 `com.lelcc:lelcc-backend`）/ MyBatis-Plus 3.5.5 / Hutool 5.8.25；
数据库 MySQL 9.5；前端 Vue 3 + TypeScript + Vite + Pinia + Element Plus。

## 构建与运行（先看这条）

本机 `JAVA_HOME` 指向 JDK 25，直接 `mvn` 会因 Lombok 不生成 getter 而报「找不到符号」。**必须用 JDK 21**：

```
set "JAVA_HOME=C:\Program Files\Java\jdk-21" && mvn -B -DskipTests clean compile
```

前端类型检查：在对应前端目录执行 `npx vue-tsc -b --force`。

数据库（客户端 `E:\Code\CodeEditPath\MySQl\SQL9\bin\mysql.exe`，`localhost:3306`）：

```
set "MYSQL_PWD=123456" && mysql --default-character-set=utf8mb4 -u root -e "source E:/Code/AAAEndClass/sql/xxx.sql"
```

用 `MYSQL_PWD` 而不是 `-p123456`，避免密码出现在命令行里。

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
