# 前端镜像：多阶段构建 —— node 构建两个前端的静态产物，nginx 托管。
#
# 两个站点挂在同一个 nginx 的两个端口上：80 用户端（社区本身，必须在根路径）、8081 管理端。
# 前端**一行都不用改**：请求走 /api（nginx 剥掉前缀转给后端），图片走 /static（转给后端上传目录），
# 两个站点各自是独立的 server 块、根路径都是 /，所以 frontend 的 router base 保持默认即可。

FROM node:22-alpine AS build

# Vite 7 要求 Node ^20.19 || >=22.12，22-alpine 满足；20-alpine 得挑到 20.19 以上才行
WORKDIR /src

# 先只拷清单文件装依赖：源码改动不会让依赖层缓存失效
COPY simple-template-f-user/package.json simple-template-f-user/package-lock.json ./simple-template-f-user/
COPY simple-template-f-admin/package.json simple-template-f-admin/package-lock.json ./simple-template-f-admin/
RUN cd simple-template-f-user && npm ci --no-audit --no-fund \
 && cd ../simple-template-f-admin && npm ci --no-audit --no-fund

COPY simple-template-f-user ./simple-template-f-user
COPY simple-template-f-admin ./simple-template-f-admin

# npm run build = vue-tsc -b && vite build：类型有错就直接失败，镜像也不会产出
RUN cd simple-template-f-user && npm run build \
 && cd ../simple-template-f-admin && npm run build


FROM nginx:alpine

# 一次 COPY 覆盖默认站点配置（default.conf 里包含用户端与管理端两个 server 块）
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /src/simple-template-f-user/dist /usr/share/nginx/html/site
COPY --from=build /src/simple-template-f-admin/dist /usr/share/nginx/html/console

EXPOSE 80 8081

HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=6 \
    CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
