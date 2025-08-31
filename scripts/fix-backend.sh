#!/bin/bash

# RSA公钥问题快速修复脚本
# 用于重新构建和部署后端服务

echo "🔧 开始修复后端RSA公钥问题..."

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker未运行，请启动Docker后重试"
    exit 1
fi

# 检查容器状态
echo "📊 检查当前容器状态..."
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 重新构建后端镜像
echo "🏗️  重新构建后端镜像..."
cd backend
docker build -t usdtclaudecode-backend:fix . || {
    echo "❌ 后端镜像构建失败"
    exit 1
}

# 停止现有后端容器
echo "🛑 停止现有后端容器..."
docker stop usdt-backend || true
docker rm usdt-backend || true

# 启动新的后端容器
echo "🚀 启动修复后的后端容器..."
docker run -d \
    --name usdt-backend \
    --network usdtclaudecode_usdt-network \
    -p 8090:8080 \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e DB_HOST=mysql \
    -e DB_PORT=3306 \
    -e DB_NAME=usdt_trading_platform \
    -e DB_USERNAME=root \
    -e DB_PASSWORD=UsdtTrading123! \
    -e REDIS_HOST=redis \
    -e REDIS_PORT=6379 \
    -e RSA_PUBLIC_KEY="${RSA_PUBLIC_KEY}" \
    -e RSA_PRIVATE_KEY="${RSA_PRIVATE_KEY}" \
    usdtclaudecode-backend:fix

# 等待服务启动
echo "⏳ 等待后端服务启动..."
sleep 10

# 检查健康状态
echo "🩺 检查后端健康状态..."
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    if curl -f http://localhost:8090/api/actuator/health > /dev/null 2>&1; then
        echo "✅ 后端服务健康检查通过"
        break
    else
        echo "⏳ 等待后端服务启动... ($attempt/$max_attempts)"
        sleep 2
        attempt=$((attempt + 1))
    fi
done

if [ $attempt -gt $max_attempts ]; then
    echo "❌ 后端服务启动超时"
    echo "📋 查看后端日志:"
    docker logs usdt-backend --tail 50
    exit 1
fi

# 测试RSA公钥端点
echo "🔑 测试RSA公钥端点..."
endpoints=(
    "http://localhost:8090/api/admin/auth/public-key"
    "http://localhost:8090/api/auth/public-key"
)

for endpoint in "${endpoints[@]}"; do
    echo "📡 测试: $endpoint"
    if curl -f "$endpoint" -H "Content-Type: application/json" > /dev/null 2>&1; then
        echo "✅ $endpoint 可访问"
    else
        echo "❌ $endpoint 不可访问"
    fi
done

# 显示最终状态
echo ""
echo "🎉 后端修复完成！"
echo ""
echo "📊 当前服务状态:"
docker ps --filter "name=usdt-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "🔍 后续测试步骤:"
echo "1. 访问前端管理界面: http://localhost:3000"
echo "2. 使用诊断工具检查API状态"
echo "3. 尝试管理员登录"

echo ""
echo "📋 如果问题仍然存在，请查看日志:"
echo "docker logs usdt-backend"