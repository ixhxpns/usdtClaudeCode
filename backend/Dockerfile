# 使用官方OpenJDK运行时镜像
FROM eclipse-temurin:21-jre

# 安装必要工具和安全更新
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    tzdata \
    bash \
    && rm -rf /var/lib/apt/lists/*

# 创建应用用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 设置工作目录
WORKDIR /app

# 复制已构建的JAR文件
COPY target/usdt-trading-platform-1.0.0.jar app.jar

# 创建必要目录并设置权限
RUN mkdir -p logs uploads temp \
    && chown -R appuser:appuser /app \
    && chmod -R 755 /app

# 切换到应用用户
USER appuser

# 健康检查（延长启动时间）
HEALTHCHECK --interval=60s --timeout=30s --start-period=300s --retries=10 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# JVM参数优化（减少内存使用）
ENV JAVA_OPTS="-Xms256m -Xmx1g \
    -XX:+UseG1GC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=50.0 \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true \
    -Djava.awt.headless=true"

# 设置时区
ENV TZ=UTC

# 启动应用（使用exec形式以便信号处理）
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]