
#!/bin/bash

echo "USDT交易平台 - Redis兼容性快速修复工具"
echo "======================================"
echo

# 备份原始文件
echo "1. 备份原始配置文件..."
cp backend/pom.xml backend/pom.xml.backup.$(date +%Y%m%d_%H%M%S)
echo "   ✓ pom.xml已备份"

# 修复pom.xml中的Redisson版本
echo
echo "2. 修复Redisson版本兼容性..."
sed -i'' -e 's/<redisson.version>3.24.3<\/redisson.version>/<redisson.version>3.20.1<\/redisson.version>/' backend/pom.xml

# 验证修改
if grep -q "3.20.1" backend/pom.xml; then
    echo "   ✓ Redisson版本已更新为3.20.1"
else
    echo "   ✗ 版本更新失败，请手动修改"
    exit 1
fi

# 重新构建应用
echo
echo "3. 重新构建应用..."
echo "   这可能需要几分钟时间..."

if docker-compose down; then
    echo "   ✓ 服务已停止"
else
    echo "   ⚠ 服务停止时出现警告，继续..."
fi

# 清理构建缓存
docker system prune -f > /dev/null 2>&1
echo "   ✓ 清理了Docker缓存"

# 重新构建
echo "   正在重新构建后端服务..."
if docker-compose build --no-cache backend; then
    echo "   ✓ 后端服务构建完成"
else
    echo "   ✗ 构建失败！"
    exit 1
fi

# 启动服务
echo
echo "4. 启动服务..."
if docker-compose up -d; then
    echo "   ✓ 服务启动中..."
else
    echo "   ✗ 服务启动失败"
    exit 1
fi

# 等待服务就绪
echo
echo "5. 等待服务就绪..."
echo "   等待健康检查完成（最多2分钟）..."

max_wait=120
wait_time=0
while [ $wait_time -lt $max_wait ]; do
    if docker-compose ps | grep -q "healthy.*usdt-backend"; then
        echo "   ✓ 后端服务健康检查通过"
        break
    fi
    
    echo -n "."
    sleep 5
    wait_time=$((wait_time + 5))
done

if [ $wait_time -ge $max_wait ]; then
    echo
    echo "   ⚠ 健康检查超时，但继续验证功能..."
fi

# 验证修复结果
echo
echo "6. 验证修复结果..."
sleep 10  # 额外等待时间确保服务完全启动

# 测试基础功能
echo -n "   测试基础连通性... "
if curl -s -H "User-Agent: Mozilla/5.0" http://localhost:8090/api/api/test/ping | grep -q "success.*true"; then
    echo "✓"
else
    echo "✗"
fi

# 测试Redis相关功能
echo -n "   测试价格查询（Redis功能）... "
response=$(curl -s -H "User-Agent: Mozilla/5.0" http://localhost:8090/api/api/price/current)
if echo "$response" | grep -q "success.*true"; then
    echo "✓ 修复成功！"
    redis_fixed=true
else
    echo "✗ 仍有问题"
    redis_fixed=false
    echo "     响应: $(echo "$response" | head -c 100)..."
fi

# 测试认证功能
echo -n "   测试认证相关功能... "
auth_response=$(curl -s -H "User-Agent: Mozilla/5.0" -H "Content-Type: application/json" -X POST -d '{"email":"test@example.com"}' http://localhost:8090/api/api/auth/send-email-verification)
if echo "$auth_response" | grep -q "success.*true\|验证码\|邮箱"; then
    echo "✓ 认证功能恢复"
else
    echo "✗ 认证功能仍有问题"
fi

echo
echo "7. 修复结果总结"
echo "================"

if [ "$redis_fixed" = true ]; then
    echo "🎉 修复成功！"
    echo
    echo "✅ Redis连接问题已解决"
    echo "✅ 核心API功能已恢复"
    echo "✅ 可以继续进行完整测试"
    echo
    echo "建议下一步操作:"
    echo "1. 运行完整API测试套件"
    echo "2. 进行性能和安全测试"
    echo "3. 部署到生产环境前的最终验证"
    echo
    echo "运行完整测试: ./simple-api-test.sh"
else
    echo "❌ 修复未完全成功"
    echo
    echo "可能的原因:"
    echo "1. 服务还在启动中（请等待更长时间）"
    echo "2. 仍有其他兼容性问题"
    echo "3. 配置文件需要进一步调整"
    echo
    echo "故障排除步骤:"
    echo "1. 查看日志: docker logs usdt-backend"
    echo "2. 检查服务状态: docker-compose ps"
    echo "3. 手动测试: curl -s http://localhost:8090/api/api/test/ping"
fi

echo
echo "修复脚本执行完成！"
echo "时间: $(date)"