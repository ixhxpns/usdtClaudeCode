FROM eclipse-temurin:21-jre

# 安装必要工具
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    tzdata \
    bash \
    mysql-client \
    && rm -rf /var/lib/apt/lists/*

# 创建应用用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 设置工作目录
WORKDIR /app

# 复制JAR文件
COPY target/usdt-trading-platform-1.0.0.jar app.jar

# 复制等待脚本
COPY wait-for-services.sh /app/wait-for-services.sh

# 设置权限
RUN mkdir -p logs uploads temp \
    && chown -R appuser:appuser /app \
    && chmod -R 755 /app \
    && chmod +x /app/wait-for-services.sh

# 切换到应用用户
USER appuser

# 设置JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Djava.awt.headless=true -Dspring.profiles.active=simple"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8090/api/admin/auth/public-key || exit 1

# 启动命令
CMD ["/app/wait-for-services.sh"]
