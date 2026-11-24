# =============================================================================
# 前端镜像：多阶段构建
#   构建阶段：node:22-alpine 执行 npm ci + npm run build（产出 dist/）
#   运行阶段：nginx:1.27-alpine 托管 dist/ 并反代 /api/ 到 backend:8080
#
# 构建上下文为 frontend/，只拷贝构建需要的文件（不把 node_modules/、dist/ 带进镜像）。
#   docker build -t gcc-frontend:latest frontend/
# =============================================================================

# ---------- 构建阶段 ----------
FROM node:22-alpine AS build

WORKDIR /build

# 依赖层：package.json + package-lock.json 未变时复用缓存
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

# 源码与构建配置
COPY index.html env.d.ts tsconfig.json tsconfig.app.json vite.config.ts ./
COPY public ./public
COPY src ./src
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:1.27-alpine

# 站点配置（SPA history fallback + /api 反代）
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 构建产物
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80

# nginx 官方镜像默认 CMD 即为前台运行 nginx
