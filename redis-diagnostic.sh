#!/bin/bash

echo "Redis连接诊断工具"
echo "=================="
echo

# 1. 检查Redis容器状态
echo "1. Redis容器状态检查"
echo "-------------------"
redis_status=$(docker ps --filter "name=usdt-redis" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}")
echo "$redis_status"
echo

# 2. 测试Redis连接
echo "2. Redis连接测试"
echo "----------------"
if redis-cli ping > /dev/null 2>&1; then
    echo "✓ 本地Redis连接正常"
    redis-cli info server | grep redis_version
else
    echo "✗ 本地Redis连接失败"
fi
echo

# 3. 检查容器间网络连接
echo "3. 容器网络连接测试"
echo "------------------"
network_test=$(docker exec usdt-backend ping -c 1 usdt-redis 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "✓ 后端容器可以访问Redis容器"
else
    echo "✗ 后端容器无法访问Redis容器"
    echo "尝试使用IP地址连接..."
    redis_ip=$(docker inspect usdt-redis -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')
    echo "Redis容器IP: $redis_ip"
    docker exec usdt-backend ping -c 1 $redis_ip
fi
echo

# 4. 检查应用配置
echo "4. 应用Redis配置检查"
echo "-------------------"
echo "检查环境变量:"
docker exec usdt-backend env | grep -i redis || echo "未找到Redis环境变量"
echo

# 5. 检查Java依赖
echo "5. Java依赖检查"
echo "--------------"
echo "检查Redisson相关类："
docker exec usdt-backend ls -la /app/app.jar > /dev/null 2>&1 && echo "应用JAR存在" || echo "应用JAR不存在"

# 尝试查看JAR中的Redisson类
docker exec usdt-backend sh -c "jar -tf /app/app.jar | grep -i redisson | head -10" 2>/dev/null || echo "无法检查JAR内容"
echo

# 6. 检查应用日志中的Redis错误
echo "6. Redis相关错误日志"
echo "------------------"
echo "最近的Redis错误："
docker logs usdt-backend 2>&1 | grep -i "redis\|redisson" | tail -5
echo

# 7. 网络诊断
echo "7. Docker网络诊断"
echo "-----------------"
network_name=$(docker inspect usdt-backend -f '{{range $net, $conf := .NetworkSettings.Networks}}{{$net}}{{end}}')
echo "后端容器网络: $network_name"

redis_network=$(docker inspect usdt-redis -f '{{range $net, $conf := .NetworkSettings.Networks}}{{$net}}{{end}}')
echo "Redis容器网络: $redis_network"

if [ "$network_name" = "$redis_network" ]; then
    echo "✓ 容器在同一网络中"
else
    echo "✗ 容器不在同一网络中"
fi
echo

# 8. 端口检查
echo "8. 端口检查"
echo "----------"
echo "Redis端口6379状态："
netstat -an | grep 6379 || echo "端口6379未监听"
echo

# 9. 修复建议
echo "9. 修复建议"
echo "----------"
echo "基于以上检查结果，建议："
echo "1. 如果容器网络不通，重启容器："
echo "   docker-compose down && docker-compose up -d"
echo
echo "2. 如果Redis配置问题，检查application.yml："
echo "   spring.data.redis.host应该是'usdt-redis'（容器名）"
echo
echo "3. 如果是依赖问题，检查pom.xml中Redisson版本兼容性"
echo
echo "4. 临时绕过Redis依赖（仅用于测试）："
echo "   可以注释掉@Component注解来禁用RateLimitInterceptor"

echo
echo "诊断完成！"