#!/bin/bash

echo "🚀 等待依赖服务启动..."

# 等待MySQL
echo "⏳ 等待MySQL..."
for i in {1..60}; do
    if mysql -h mysql -u root -pUsdtTrading123! -e "SELECT 1;" >/dev/null 2>&1; then
        echo "✅ MySQL已就绪"
        break
    fi
    if [ $i -eq 60 ]; then
        echo "❌ MySQL启动超时"
        exit 1
    fi
    echo "等待MySQL... ($i/60)"
    sleep 2
done

# 等待Redis
echo "⏳ 等待Redis..."
for i in {1..30}; do
    if echo "PING" | nc redis 6379 | grep -q PONG; then
        echo "✅ Redis已就绪"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "⚠️ Redis连接失败，但继续启动"
        break
    fi
    echo "等待Redis... ($i/30)"
    sleep 1
done

echo "🎯 启动应用..."
exec java $JAVA_OPTS -jar /app/app.jar
