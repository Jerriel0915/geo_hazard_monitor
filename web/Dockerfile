# syntax=docker/dockerfile:1.7
# ==================== 阶段二：前端构建 ====================
# 多阶段构建：Node 构建 + Nginx 部署

# ---- 构建阶段 ----
# 使用 slim 而非 alpine：避免 musl libc 触发 sass/esbuild 等原生模块的源码编译
FROM node:20-slim AS builder

WORKDIR /app

# 关闭噪音，调大超时（大依赖如 three/blockly/字体包容易超时）
ENV NPM_CONFIG_AUDIT=false \
    NPM_CONFIG_FUND=false \
    NPM_CONFIG_PROGRESS=false \
    NPM_CONFIG_LOGLEVEL=warn \
    NPM_CONFIG_FETCH_TIMEOUT=600000 \
    NPM_CONFIG_FETCH_RETRIES=5

# 切国内镜像源（境内服务器拉 npmjs 是慢的最大单点）
RUN npm config set registry https://registry.npmmirror.com

# 复制依赖清单（lockfile 必须存在；.dockerignore 已不再排除）
COPY package.json package-lock.json ./

# npm ci 严格按 lockfile 安装；BuildKit 缓存挂载跨次构建复用 ~/.npm
RUN --mount=type=cache,target=/root/.npm \
    npm ci --prefer-offline --no-audit --no-fund

# 复制源码
COPY . .

# 构建生产版本（package.json 中 build 已去除 vue-tsc，类型检查走 build:check）
RUN npm run build

# ---- 运行阶段 ----
FROM nginx:alpine

# 删除默认配置
RUN rm /etc/nginx/conf.d/default.conf

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 创建日志目录并修复权限（nginx 以 nginx 用户运行 + read_only 容器）
RUN mkdir -p /var/log/nginx && touch /var/log/nginx/nginx.pid && chown -R nginx:nginx /var/log/nginx

EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
