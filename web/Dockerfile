# syntax=docker/dockerfile:1.7
# ==================== 前端构建（多阶段） ====================
# 阶段一：主前端构建（web/）
# 阶段二：Terra 值守模式 Dashboard 构建（terra/）
# 阶段三：Nginx 运行时

# ==================== 共享 npm 环境变量 ====================
ARG NPM_CONFIG_AUDIT=false
ARG NPM_CONFIG_FUND=false
ARG NPM_CONFIG_PROGRESS=false
ARG NPM_CONFIG_LOGLEVEL=warn
ARG NPM_CONFIG_FETCH_TIMEOUT=600000
ARG NPM_CONFIG_FETCH_RETRIES=5

# ---- 阶段一：主前端构建 ----
# 使用 slim 而非 alpine：避免 musl libc 触发 sass/esbuild 等原生模块的源码编译
FROM node:20-slim AS builder

ARG NPM_CONFIG_AUDIT
ARG NPM_CONFIG_FUND
ARG NPM_CONFIG_PROGRESS
ARG NPM_CONFIG_LOGLEVEL
ARG NPM_CONFIG_FETCH_TIMEOUT
ARG NPM_CONFIG_FETCH_RETRIES

ENV NPM_CONFIG_AUDIT=${NPM_CONFIG_AUDIT} \
    NPM_CONFIG_FUND=${NPM_CONFIG_FUND} \
    NPM_CONFIG_PROGRESS=${NPM_CONFIG_PROGRESS} \
    NPM_CONFIG_LOGLEVEL=${NPM_CONFIG_LOGLEVEL} \
    NPM_CONFIG_FETCH_TIMEOUT=${NPM_CONFIG_FETCH_TIMEOUT} \
    NPM_CONFIG_FETCH_RETRIES=${NPM_CONFIG_FETCH_RETRIES}

WORKDIR /app

# 切国内镜像源（境内服务器拉 npmjs 是慢的最大单点）
RUN npm config set registry https://registry.npmmirror.com

# 复制依赖清单（lockfile 必须存在）
COPY web/package.json web/package-lock.json ./

# npm ci 严格按 lockfile 安装；BuildKit 缓存挂载跨次构建复用 ~/.npm
RUN --mount=type=cache,target=/root/.npm \
    npm ci --prefer-offline --no-audit --no-fund

# 复制源码（构建上下文为项目根目录）
COPY web/ .

# 构建生产版本（package.json 中 build 已去除 vue-tsc，类型检查走 build:check）
RUN npm run build

# ---- 阶段二：Terra 值守模式 Dashboard 构建 ----
FROM node:20-slim AS terra-builder

ARG NPM_CONFIG_AUDIT
ARG NPM_CONFIG_FUND
ARG NPM_CONFIG_PROGRESS
ARG NPM_CONFIG_LOGLEVEL
ARG NPM_CONFIG_FETCH_TIMEOUT
ARG NPM_CONFIG_FETCH_RETRIES

ENV NPM_CONFIG_AUDIT=${NPM_CONFIG_AUDIT} \
    NPM_CONFIG_FUND=${NPM_CONFIG_FUND} \
    NPM_CONFIG_PROGRESS=${NPM_CONFIG_PROGRESS} \
    NPM_CONFIG_LOGLEVEL=${NPM_CONFIG_LOGLEVEL} \
    NPM_CONFIG_FETCH_TIMEOUT=${NPM_CONFIG_FETCH_TIMEOUT} \
    NPM_CONFIG_FETCH_RETRIES=${NPM_CONFIG_FETCH_RETRIES}

WORKDIR /terra

# 切国内镜像源
RUN npm config set registry https://registry.npmmirror.com

# 复制依赖清单
COPY terra/package.json terra/package-lock.json* ./

# 安装依赖（允许 lockfile 不存在时回退到 npm install）
RUN --mount=type=cache,target=/root/.npm \
    npm ci --prefer-offline --no-audit --no-fund || npm install --no-audit --no-fund

# 复制源码
COPY terra/ .

# 构建生产版本（basePath=/terra/）
RUN npm run build

# ---- 阶段三：Nginx 运行时 ----
FROM nginx:alpine

# 删除默认配置
RUN rm /etc/nginx/conf.d/default.conf

# 复制 Nginx 配置
COPY web/nginx.conf /etc/nginx/conf.d/

# 复制主前端构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Terra 值守模式 Dashboard 构建产物（覆盖 public/terra/ 下的预构建文件）
COPY --from=terra-builder /terra/dist /usr/share/nginx/html/terra

# 创建日志目录并修复权限（nginx 以 nginx 用户运行 + read_only 容器）
RUN mkdir -p /var/log/nginx && touch /var/log/nginx/nginx.pid && chown -R nginx:nginx /var/log/nginx

EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
