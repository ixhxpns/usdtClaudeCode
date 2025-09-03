#!/bin/bash

echo "Redis连接问题修复脚本"
echo "===================="
echo

# 1. 显示当前问题
echo "1. 问题分析"
echo "----------"
echo "✗ 应用配置的REDIS_HOST=redis（正确）"
echo "✗ 但Redis服务在容器中可能无法解析"
echo "✗ 导致Redisson连接初始化失败"
echo

# 2. 检查当前服务状态
echo "2. 当前服务状态"
echo "--------------"
docker-compose ps

echo

# 3. 修复方案选择
echo "3. 修复方案"
echo "----------"
echo "选择修复方案:"
echo "1) 重启所有服务（推荐）"
echo "2) 仅重启后端服务"
echo "3) 检查网络连接"
echo "4) 查看详细日志"
echo

read -p "请选择方案 (1-4): " choice

case $choice in
    1)
        echo "正在重启所有服务..."
        docker-compose down
        echo "等待5秒..."
        sleep 5
        docker-compose up -d
        echo "服务重启完成，等待健康检查..."
        sleep 30
        echo "检查服务状态:"
        docker-compose ps
        ;;
    2)
        echo "正在重启后端服务..."
        docker-compose restart backend
        echo "等待后端服务启动..."
        sleep 20
        echo "检查后端状态:"
        docker logs usdt-backend --tail 20
        ;;
    3)
        echo "检查网络连接..."
        echo "Redis服务解析:"
        docker exec usdt-backend nslookup redis 2>/dev/null || echo "nslookup不可用"
        echo
        echo "尝试连接Redis:"
        docker exec usdt-backend telnet redis 6379 2>/dev/null || echo "telnet不可用，尝试其他方法"
        ;;
    4)
        echo "查看详细日志..."
        echo "=== 后端日志 ==="
        docker logs usdt-backend --tail 50
        echo
        echo "=== Redis日志 ==="
        docker logs usdt-redis --tail 20
        ;;
    *)
        echo "无效选择"
        exit 1
        ;;
esac

echo
echo "4. 验证修复结果"
echo "--------------"
echo "测试API端点..."

sleep 5

# 测试基础端点
echo -n "测试Ping端点... "
response=$(curl -s -H "User-Agent: Mozilla/5.0" http://localhost:8090/api/api/test/ping)
if echo "$response" | grep -q "success.*true"; then
    echo "✓ 成功"
else
    echo "✗ 失败"
fi

# 测试需要Redis的端点
echo -n "测试价格端点... "
response=$(curl -s -H "User-Agent: Mozilla/5.0" http://localhost:8090/api/api/price/current)
if echo "$response" | grep -q "success.*true"; then
    echo "✓ 成功 - Redis连接已修复"
else
    echo "✗ 失败 - Redis连接仍有问题"
    echo "响应: $response" | head -c 100
fi

echo
echo "5. 进一步诊断（如果仍有问题）"
echo "----------------------------"
if echo "$response" | grep -q "success.*false"; then
    echo "Redis问题仍然存在，建议："
    echo "1. 检查Redisson版本兼容性"
    echo "2. 检查Redis配置文件"
    echo "3. 查看应用启动日志"
    echo "4. 考虑临时禁用Redis依赖组件"
    echo
    echo "运行以下命令查看更多信息:"
    echo "docker logs usdt-backend | grep -i error"
    echo "docker exec usdt-backend java -version"
else
    echo "✅ Redis连接问题已修复！"
    echo "现在可以进行完整的API测试了。"
fi

echo
echo "修复脚本完成！"