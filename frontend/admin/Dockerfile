# 构建阶段 - 使用最新稳定版Node.js
FROM --platform=linux/amd64 node:20-alpine AS builder

# 安装构建依赖
RUN apk add --no-cache \
    python3 \
    make \
    g++ \
    && rm -rf /var/cache/apk/*

WORKDIR /app

# 复制package文件并安装依赖（利用Docker缓存层）
COPY package*.json ./
COPY yarn.lock* ./

# 清理可能存在的缓存和锁定文件
RUN rm -rf node_modules package-lock.json

# 设置环境变量解决架构问题
ENV npm_config_target_platform=linux
ENV npm_config_target_arch=x64
ENV npm_config_cache=/tmp/.npm
ENV npm_config_build_from_source=true

# 设置npm配置以提高安装速度
RUN npm config set registry https://registry.npmjs.org/ \
    && npm install --no-audit --no-fund --silent

# 复制源代码
COPY . .

# 构建生产版本
RUN npm run build \
    && npm cache clean --force

# 运行阶段 - 使用Nginx Alpine镜像
FROM nginx:1.25-alpine

# 安装必要工具
RUN apk add --no-cache \
    curl \
    tzdata \
    && rm -rf /var/cache/apk/*

# 复制自定义Nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html/

# 创建必要目录并设置权限
RUN mkdir -p /var/cache/nginx/client_temp \
    /var/cache/nginx/proxy_temp \
    /var/cache/nginx/fastcgi_temp \
    /var/cache/nginx/uwsgi_temp \
    /var/cache/nginx/scgi_temp \
    /var/log/nginx \
    && chown -R nginx:nginx /usr/share/nginx/html \
    && chown -R nginx:nginx /var/cache/nginx \
    && chown -R nginx:nginx /var/log/nginx \
    && chmod -R 755 /usr/share/nginx/html

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:80/ || exit 1

# 设置时区
ENV TZ=UTC

# 暴露端口
EXPOSE 80

# 使用非特权用户运行（如果需要）
# USER nginx

# 启动Nginx
CMD ["nginx", "-g", "daemon off;"]