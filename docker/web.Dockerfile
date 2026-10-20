# syntax=docker/dockerfile:1.7
# 前端镜像：三端共用（通过 APP_DIR 参数区分），构建产物由 nginx 托管
#   docker build -f docker/web.Dockerfile --build-arg APP_DIR=rental-app -t rental/app:1.0.0 .
# 构建上下文必须是项目根目录

# ---------- 构建阶段 ----------
FROM node:20-alpine AS build
ARG APP_DIR=rental-app
WORKDIR /app

# 先装依赖，利用层缓存：只有 package*.json 变化才重新安装
COPY ${APP_DIR}/package.json ${APP_DIR}/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-audit --no-fund

COPY ${APP_DIR}/ ./
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:1.27-alpine
COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
