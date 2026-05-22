# ==================== 阶段二：前端构建 ====================
# 多阶段构建：Node 构建 + Nginx 部署

# ---- 构建阶段 ----
FROM node:20-alpine AS builder

WORKDIR /app

# 复制依赖文件
COPY package.json package-lock.json* ./

# 安装依赖（利用 Docker 缓存层）
RUN npm ci --prefer-offline

# 复制源码
COPY . .

# 构建生产版本
RUN npm run build

# ---- 运行阶段 ----
FROM nginx:alpine

# 删除默认配置
RUN rm /etc/nginx/conf.d/default.conf

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 创建日志目录
RUN mkdir -p /var/log/nginx && touch /var/log/nginx/nginx.pid && chown -R nginx:nginx /var/log/nginx

EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]