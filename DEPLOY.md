# 部署说明

面向"把 GameCommunityCreator 装到一台服务器上给玩家用"。
日常开发（本地跑起来改代码）看 [AGENTS.md](AGENTS.md)，本文只讲部署。

整条链路是：

```
建库（sql/install.sql）→ 起后端（环境变量配连接）→ 构建前端（dist/）→ 托管两个前端
    → 管理端「创建社区」点一下 → 得到完整的游戏社区
```

有两条路径，挑一条走完即可：

| 路径 | 适合谁 | 需要装什么 | 走哪几节 |
|---|---|---|---|
| **Docker Compose（推荐）** | 只想把站跑起来 / 换机器重装 | 只要 Docker | 「快速路径」 |
| 手动部署 | 要自己调 JDK / MySQL / nginx 细节，或机器上没有 Docker | JDK 21 + Maven + MySQL + Node + nginx | 「环境要求」→「手动路径 · 一 ~ 四」 |

两条路径最后都停在同一步：**「五、首次建站」**（管理端点一下「创建社区」），
以及上线前的 **「六、上线前检查清单」**。

---

## 快速路径：Docker Compose 一键部署

一条命令把 MySQL、后端、nginx（托管两个前端）全起来。**不需要装 JDK / Maven / Node / nginx**，
只要机器上有 Docker（含 compose v2）。

> **Windows 上要先有 WSL2**：Docker Desktop 装好不等于引擎能用。如果 `docker info` 里
> `Server:` 一段是空的、`docker desktop status` 显示 `stopped`，多半是"适用于 Linux 的 Windows 子系统"
> 没装。用**管理员**终端执行 `wsl --install` 然后重启系统，再开 Docker Desktop 等它变绿。
> 判断依据：`wsl --status` 报"未安装用于 Linux 的 Windows 子系统"就是这个情况。

### 三步

> **Windows 上可以直接双击 `deploy.bat`**，下面三步它全包了：自检 Docker（含"引擎没起来/WSL2 缺失"的提示）、
> 首次运行自动生成 `.env`（随机口令与密钥，并自动避让被占用的 9331 端口）、构建启动、等三个容器
> healthy 后打印访问地址与账号。想看它做了什么、或者要在 Linux/Mac 上部署，用下面这三步。

```bash
# 1) 准备唯一一份配置：数据库口令 + 令牌签名密钥
cp docker/.env.example .env          # Windows: copy docker\.env.example .env
#    改 DB_ROOT_PASSWORD，并把 JWT_SECRET 换成 openssl rand -hex 32 的输出

# 2) 起（第一次会构建镜像，慢；之后重启是秒级）
docker compose up -d --build

# 3) 看状态，三个都 healthy 就成了
docker compose ps
```

起来之后：

| 入口 | 地址 | 说明 |
|---|---|---|
| 用户端（社区本身） | `http://<host>/` | 必须挂在根路径，容器里就是这么配的 |
| 管理端（后台） | `http://<host>:8081/` | 用种子账号 `admin` / `admin123` 登录，**第一件事是改密码** |
| 后端接口（仅本机） | `http://127.0.0.1:9331/` | 只绑在宿主机回环上，方便本机 curl 调试；对外访问一律走 nginx |

端口想换就在 `.env` 里改 `USER_PORT` / `ADMIN_PORT`。

### 首次启动它做了什么

1. `db` 容器初始化数据目录时执行 `sql/install.sql`（建库 + 种子数据，末尾打印一张自检表）；
2. `backend` 构建镜像（Maven 拉依赖 → 打 fat jar）→ 启动，健康检查通过后才算就绪；
3. `web` 构建两个前端的静态产物 → nginx 起两个站点，`/api`、`/static` 转给后端。

第一次要下 Maven 与 npm 依赖，慢是正常的（几分钟到十几分钟，看网速）。
`docker compose logs -f backend` 可以看到后端启动日志。

### 数据在哪、怎么备份

| 数据 | 位置 | 说明 |
|---|---|---|
| 数据库 | 命名卷 `gcc_db-data` | `docker compose down` 不会删，`down -v` 才会 |
| 上传的图片 | 命名卷 `gcc_uploads` | 头像、封面、轮播图。库里只存 objectKey，**卷删了老图片就全成碎图** |

```bash
# 备份数据库
docker compose exec db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 ssy_db' > backup.sql
# 备份上传文件
docker run --rm -v gcc_uploads:/data -v "$PWD:/backup" alpine tar czf /backup/uploads.tar.gz -C /data .
# 清空重装（连数据一起删，慎用）
docker compose down -v && docker compose up -d --build
```

想彻底不管图片文件，可以把 `.env` 外的上传配置改成对象存储：在 `docker-compose.yml` 的
backend 环境里加 `UPLOAD_PROVIDER: s3` 与 `S3_*` 几项（见下表），图片直接走云存储/CDN。

### 日常运维

```bash
docker compose logs -f backend     # 跟后端日志
docker compose restart backend     # 只重启后端
docker compose up -d --build web   # 前端改了代码，重建 web
docker compose down                # 停（保留数据卷）
docker compose exec db mysql -uroot -p ssy_db   # 进数据库
```

改了 `docker/.env.example` 里的 `DB_ROOT_PASSWORD` 对**已初始化**的数据卷不生效：
root 口令是首次初始化时写进数据库的。要改就 `docker compose down -v` 重建（数据会丢），
或者在库里执行 `ALTER USER`。

> ⚠️ 管理端 8081 默认对外开着，生产环境请把它绑到内网（`"127.0.0.1:8081:8081"`）、
> 前面再套一层带 HTTPS 的网关，或至少限制来源 IP。两端登录态都是 JWT，明文 HTTP 等于把令牌交出去。

### 这套东西和手动路径的关系

`docker/` 下的文件只是把「手动路径 · 一 ~ 四」自动化了，两边用的是同一份 `sql/install.sql`、
同一份 `application.yaml`（都靠环境变量覆盖）、同一套 nginx 反代规则。
所以想搬到宿主机手动跑，照着第四节抄一份配置即可，反过来也一样。
容器里唯一与宿主机不同的地方是路径：上传目录是 `/data/uploads`（挂的命名卷）。

> **验证记录**（真机跑过一遍，不是纸上方案）：Docker 29.8.2 / Compose v5.5.1，
> 镜像 `mysql:8.4`（服务端 8.4.11）/ `maven:3.9-eclipse-temurin-21` / `eclipse-temurin:21-jre-alpine` /
> `node:22-alpine` / `nginx:alpine`。全新库首次 `up -d --build`（含两个镜像的构建）→ 三个容器 healthy；
> 容器库自检 28 表 / 22 组件类型 / 22 菜单 / 48 原子 / 13 预设（12 启用）/ 2 个种子玩家；
> 浏览器走完「管理端创建社区 → 用户端首页渲染 → 板块页发帖 → 封面经 nginx 上传 → 待审不可见 →
> 管理端审核通过 → 详情页封面图加载成功」，全程无 4xx 与 console 报错；
> 举报闭环（帖子详情举报 → 我的举报 → 管理端处理 → 用户看到处理结果）、个人主页的
> 收藏/举报/进度面板、后台玩家管理的封禁解封都在这套容器栈上验过；
> `docker compose down -v` 后重新 `up -d` 的"清空重装"路径验过；
> `docker compose restart backend` 后帖子与封面都还在（落盘在 `gcc_uploads` 卷里）。
> 唯一没法替你验的是**部署机自己的网络**：拉镜像时遇到过一次 `auth.docker.io` 解析到 IPv6 超时，
> 重试即好（见常见问题）。

---

## 环境要求（手动路径，Docker 路径可以跳过）

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | **21**（编译）/ 17+（运行） | **不要用 JDK 25**：Lombok 注解处理器会失效，编译报"找不到符号" |
| Maven | 3.8+ | 编译后端、打可执行 jar |
| MySQL | 8.0+ | 开发环境用 9.5 验证过 |
| Node.js | 18+ | **只在构建前端时需要**，运行时不依赖 |
| Nginx（或同类反代） | 任意近版本 | 托管两个前端的静态产物并把 `/api`、`/static` 转给后端 |

---

## 手动路径 · 一、建库：一条命令

```bash
# 必须在仓库根目录执行（install.sql 里用相对路径 source 其它脚本）
mysql --default-character-set=utf8mb4 -u root -p < sql/install.sql
```

或者进 mysql 客户端后：

```sql
source sql/install.sql
```

**要求目标库是空的。** `schema.sql` 用的是裸 `CREATE TABLE`（没有 `IF NOT EXISTS`、也不 `DROP`），
重复执行会在第一张已存在的表上报 `ERROR 1050` 并**立即中止**——这是有意的，避免误删线上数据。
要清空重装：`DROP DATABASE ssy_db;` 再跑一次。

脚本跑完会自己打一张自检表，期望值：

| 项目 | 数量 |
|---|---|
| 表 | 28 |
| 组件类型（模板前） | 22 |
| 逻辑原子 | 48 |
| 预设（总 / 启用） | 13 / 12 |
| 后台菜单 | 22 |
| 用户端页面 | 6 |
| 站点配置项 | 8 |
| 示例板块 | 1 |

> 「组件类型 22」是模板应用前的数量：默认社区模板建站时会再补 6 个业务组件
> （游戏简介/版本更新/公告列表/公告详情/个人主页/发帖表单），最终 28 个。

这一步装出来的是**平台本身**（表、原子/预设/组件库、后台菜单、登录注册页，以及一套旧版首页）。
**完整社区（首页/攻略/板块/帖子/公告/个人主页）由第五节的"创建社区"生成**，不在这个脚本里。

---

## 手动路径 · 二、后端

### 2.1 配置：全部走环境变量

配置项的默认值只服务本地开发，**生产必须用环境变量覆盖**（不要改 `application.yaml`，容易跟着代码提交上去）。

| 环境变量 | 默认值 | 生产必改 | 说明 |
|---|---|---|---|
| `DB_HOST` | `localhost` | | 数据库地址 |
| `DB_PORT` | `3306` | | |
| `DB_NAME` | `ssy_db` | | 与建库时一致 |
| `DB_USERNAME` | `root` | ✔ | 建议单独建一个只对本库有权限的账号 |
| `DB_PASSWORD` | `123456` | ✔ | |
| `DB_SSL_MODE` | `PREFERRED` | | 连接受管数据库时按云厂商要求改（如 `REQUIRED`） |
| `SERVER_PORT` | `9331` | | 后端监听端口 |
| `JWT_SECRET` | 开发用固定串 | ✔ | 换成长随机串；泄露＝任何人都能自己签后台令牌 |
| `JWT_EXPIRATION` | `86400000` | | 令牌有效期（毫秒），默认 24 小时 |
| `UPLOAD_PROVIDER` | `local` | | `local`（本地磁盘）或 `s3`（任意 S3 兼容对象存储） |
| `UPLOAD_DIR` | `./uploads` | ✔ | 本地存储根目录，**必须指向持久化盘** |
| `UPLOAD_PUBLIC_BASE_URL` | `/static` | | 图片对外基址，配合 nginx 的 `/static` 转发生效 |
| `S3_ENDPOINT` / `S3_REGION` / `S3_BUCKET` / `S3_ACCESS_KEY` / `S3_SECRET_KEY` / `S3_PUBLIC_BASE_URL` / `S3_PATH_STYLE` | 空 | `provider=s3` 时必填 | 阿里云 OSS / 腾讯 COS / MinIO / R2 等都走 S3 协议；自建网关一般要 `S3_PATH_STYLE=true` |

生成一个够用的密钥：

```bash
openssl rand -hex 32
```

### 2.2 打包与运行

```bash
cd backend
export JAVA_HOME=/path/to/jdk-21        # Windows: set JAVA_HOME=C:\Program Files\Java\jdk-21
mvn -B -DskipTests package
# 产物：target/lelcc-backend-0.0.1-SNAPSHOT.jar（可执行 fat jar，约 48MB）
```

运行时：

```bash
export DB_HOST=127.0.0.1 DB_NAME=ssy_db DB_USERNAME=gcc DB_PASSWORD='<强口令>'
export JWT_SECRET='<openssl rand -hex 32 的输出>'
export UPLOAD_DIR=/var/lib/gcc/uploads
java -jar target/lelcc-backend-0.0.1-SNAPSHOT.jar
```

systemd（可选）：

```ini
# /etc/systemd/system/gcc.service
[Unit]
Description=GameCommunityCreator backend
After=network.target mysql.service

[Service]
User=gcc
WorkingDirectory=/opt/gcc/backend
Environment=DB_HOST=127.0.0.1
Environment=DB_NAME=ssy_db
Environment=DB_USERNAME=gcc
Environment=DB_PASSWORD=CHANGE_ME
Environment=JWT_SECRET=CHANGE_ME
Environment=UPLOAD_DIR=/var/lib/gcc/uploads
ExecStart=/usr/bin/java -jar /opt/gcc/backend/lelcc-backend-0.0.1-SNAPSHOT.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

### 2.3 上传目录

`UPLOAD_DIR` 里放玩家/管理员的头像、帖子封面、轮播图。三点必须做到：

1. 指向持久化目录（容器部署别留在容器层，否则重启即丢图）；
2. 纳入备份；
3. 由后端经 `/static/**` 对外暴露——所以 nginx 里 **`/static` 要转发给后端**（见下）。

想省掉这条链路就切 `UPLOAD_PROVIDER=s3`，图片直接走对象存储/CDN，后端不再存文件。

---

## 手动路径 · 三、构建前端

两个前端互相独立，各自的产物都是纯静态文件。

```bash
cd simple-template-f-user  && npm ci && npm run build    # 产物 simple-template-f-user/dist
cd simple-template-f-admin && npm ci && npm run build    # 产物 simple-template-f-admin/dist
```

`npm run build` 会先跑 `vue-tsc` 类型检查再打包，类型有错会直接失败——这是有意的。

---

## 手动路径 · 四、生产托管（nginx）

四个要点：

1. **用户端必须挂在域名根路径**。它的路由表来自后端下发的 `ui_page.route_path`，前端 `base` 是 `/`，
   挂到 `/site` 这类子路径下会 404。
   管理端放另一个域名或另一个端口即可。
2. **`/api` 转发时要剥掉前缀**：前端请求 `/api/admin/login`，后端认的是 `/admin/login`。
   nginx 里靠 `proxy_pass` 末尾那个 `/` 完成剥离。
3. **SPA 回退**：两个前端都用 history 路由，`/guide`、`/post/1` 这类地址刷新时必须回退到 `index.html`。
4. **`X-Forwarded-For` 要带上**：操作日志按它记录真实来源 IP（后端优先读这个头，没有才用直连地址）。

```nginx
# 用户端：社区站点本身
server {
    listen 80;
    server_name community.example.com;

    root /opt/gcc/user-dist;
    index index.html;

    # 上传的图片走后端，5MB 上限上面留点余量
    client_max_body_size 6m;

    # 前端调 /api/xxx，后端认 /xxx —— 末尾的 / 负责剥掉 /api 前缀
    location /api/ {
        proxy_pass http://127.0.0.1:9331/;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 图片等静态资源由后端从 UPLOAD_DIR 提供，路径原样转发（不加末尾 /）
    location /static/ {
        proxy_pass http://127.0.0.1:9331;
        proxy_set_header Host            $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # history 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }
}

# 管理端：后台，建议单独域名并加访问限制
server {
    listen 80;
    server_name admin.example.com;

    root /opt/gcc/admin-dist;
    index index.html;

    client_max_body_size 6m;

    location /api/ {
        proxy_pass http://127.0.0.1:9331/;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /static/ {
        proxy_pass http://127.0.0.1:9331;
        proxy_set_header Host            $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

> 管理端是运营入口，别和玩家端共用账号体系也别忘了加限制：至少上 HTTPS，必要时用
> `allow/deny` 或 VPN 限制来源 IP。

**必须上 HTTPS**：两端登录态都是 URL 之外的 JWT（存在浏览器里、随请求头走），
明文 HTTP 等于把令牌交给链路上的任何人。

---

## 五、首次建站（两条路径都要做）

建库脚本装出来的只有"平台"，社区内容要靠这一步生成：

1. 启动后端；
2. 打开管理端（`admin.example.com`）→ 用种子账号 **`admin` / `admin123`** 登录
   → **第一件事就是去「系统管理 → 用户管理」改掉这个密码**；
3. 进「站点配置 → 创建社区」：
   - 社区名称（必填）、游戏名称（必填）：这两项决定了页头、页脚、首页游戏简介里的文案；
   - 社区简介 / Logo / 主题主色 / 版权文案：可选，留空则用默认值或自动生成；
   - 「按模板重建页面」：新站保持开启；站点已经上线、只想补页面时才关掉；
   - 「同时创建初始内容」：新站保持开启，会建出游戏板块、初始版本、进度阶段、默认标签与一条欢迎公告；
4. 点「创建」——10 个页面（首页 / 攻略与资料 / 社区板块 / 板块详情 / 帖子检索 / 帖子详情 /
   发布新帖 / 公告列表 / 公告详情 / 个人主页）会一次性建好，页头导航里也会带上「发帖」入口；
5. 打开用户端，刷新即可看到完整社区。

之后的日常维护入口：

| 想改什么 | 去哪 |
|---|---|
| 社区名称、游戏名、Logo、主色、页头导航 | 站点配置 → 站点信息 |
| 首页版式（模块增删、顺序、文案） | 站点配置 → 页面搭建 |
| 发公告 / 版本更新说明 | 内容管理 → 公告管理 |
| 首页轮播图 | 站点配置 → 轮播图管理 |
| 板块、游戏版本、进度阶段、标签 | 内容管理 → 板块管理 / 内容维度 / 标签管理 |
| 帖子审核、举报处理 | 内容管理 → 帖子审核 / 举报处理 |
| 玩家（用户端注册账号）封禁 / 解封 | 系统管理 → 玩家管理 |
| 运营数据 | 数据统计 |

---

## 六、上线前检查清单

- [ ] 改掉后台 `admin / admin123`
- [ ] 处理种子玩家账号（`player1` / `player2`，口令 `member123`）：删除或改密
- [ ] `JWT_SECRET` 换成长随机串
- [ ] 数据库使用独立账号与强口令，不要用 `root`
- [ ] `UPLOAD_DIR` 指向持久化目录，并纳入备份
- [ ] 数据库定时备份（`mysqldump`）
- [ ] 全站 HTTPS
- [ ] 站点信息里的 Logo、版权文案、导航菜单按需调整
- [ ] 确认 `/admin/**` 接口确实拿不到未授权访问（后端已做双重校验：令牌 + 写操作权限标识）
- [ ] Docker 部署：管理端端口别裸奔（绑内网 / 套 HTTPS 网关 / 限来源 IP）
- [ ] Docker 部署：`.env` 不进版本库（已在 `.gitignore` 里），别把真实口令写回 `.env.example`

---

## 七、常见问题

**编译报"找不到符号"（一堆 getter 找不到）**
JDK 版本不对。用 JDK 21 编译；JDK 25 下 Lombok 注解处理器不生效。

**页面能打开但所有接口都 401**
nginx 的 `/api/` 转发没剥前缀。确认 `proxy_pass` 末尾带 `/`。

**刷新 `/post/1` 变 404**
缺 SPA 回退，`location /` 里要有 `try_files $uri $uri/ /index.html;`。

**图片不显示**
按顺序查三处：nginx 有没有转发 `/static/`；`UPLOAD_DIR` 是否指向真实存在的目录；
`UPLOAD_PUBLIC_BASE_URL` 是否与 nginx 里暴露的路径一致。

**`install.sql` 报 `ERROR 1050 Table ... already exists`**
正常：它只对空库执行。要么换空库，要么先 `DROP DATABASE ssy_db;`。

**用户端打开了但什么都没有 / 页面是空的**
还没走第五节的「创建社区」。建库脚本只装平台，不装社区页面。

**管理端进去看到「XX 页面还没实现」**
该菜单对应的前端页面文件缺失，路由会回退到占位页。目前 22 个菜单都有对应页面，
若日志里出现 `[router] 菜单组件不存在，已回退占位页：xxx`，说明 `sys_menu.component`
填的路径在 `simple-template-f-admin/src/views/` 下不存在。

**`docker compose up` 之后用户端一直 502**
`web` 是等 `backend` 健康之后才起的，后端没起来就转发不到。先看 `docker compose ps`
里 backend 是不是 healthy，再看 `docker compose logs backend`。
最常见的两条原因：`.env` 里的 `DB_ROOT_PASSWORD` 与数据库里已初始化的不一致；
或者数据库还在初始化（首次要建 28 张表，耐心等健康检查转绿）。

**`docker info` 里 Server 段是空的 / 报 500 Internal Server Error**
Linux 引擎没起来。Windows 上几乎都是因为 WSL2 没装：用管理员终端跑 `wsl --install` 再重启，
然后开 Docker Desktop 等状态变成 running。`docker desktop status` 会直接告诉你 `stopped` 还是 `running`。

**端口冲突起不来**
本地开着开发用的后端时，宿主机 9331 被占着，而 compose 默认把容器后端也映射到 9331。
在 `.env` 里把 `BACKEND_PORT` 改成别的（例如 19331）即可 —— 这个映射只是为了本机调试，
对外访问走的是 nginx，跟它无关。同理 `USER_PORT`（默认 80）与 `ADMIN_PORT`（默认 8081）也能改。

**改了 `.env` 里的 `DB_ROOT_PASSWORD`，容器里还是旧口令**
root 口令是**首次**初始化数据目录时写进数据库的，之后改 `.env` 不会同步。要么
`docker compose down -v` 重建（库数据一起没），要么进库执行 `ALTER USER`。

**Docker 里上传的图片 404**
先 `docker compose exec backend ls -l /data/uploads` 看文件在不在。
不在就是 `uploads` 卷被删过（`down -v`）；在的话检查 nginx 的 `/static/` 转发与
`UPLOAD_PUBLIC_BASE_URL` 是否都是 `/static`。

**端口被占用了**
`.env` 里改 `USER_PORT`（默认 80，多半是宿主机的其它 web 服务占着）或 `ADMIN_PORT`。

**能用别的库名吗**
不能，除非改 SQL。`sql/*.sql` 里写死了 `USE ssy_db`，`docker-compose.yml` 里
`DB_NAME` 也就固定成 `ssy_db` 了。

**第一次 `up --build` 特别慢**
两个镜像都要下依赖：后端拉 Maven 依赖 + 打 fat jar，web 要 `npm ci` 两次再构建两个前端。
之后就都是缓存命中了。想快点可以在部署机上先 `npm ci` / `mvn dependency:go-offline` 预热缓存。

**拉镜像报 `failed to fetch oauth token: ... dial tcp [2a03:2880:...]:443: connectex: ...`
（或其它连 `auth.docker.io` 超时的错）**
这是本机到 Docker Hub 的鉴权域名走不通，跟本项目无关，通常是自己网络对 IPv6 的处理有问题：
`auth.docker.io` 同时有 A 与 AAAA 记录，解析到 IPv6 而 IPv6 出口不通时就卡在这里。
处理顺序：先**直接重试**（实测重试一次就过了，镜像缓存也会累积）；还不行就在 Docker Desktop
设置里配一个可用的镜像加速（`registryMirrors`），绕开鉴权域名。判断是不是网络问题很简单：
`docker pull nginx:alpine` 单独拉一个小镜像试试，能拉下来就说明是偶发。
