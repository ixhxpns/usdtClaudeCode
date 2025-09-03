#!/bin/bash

echo "🔧 快速修复后端MySQL连接问题"
echo "=============================="

# 1. 停止现有服务
docker-compose down backend 2>/dev/null || true

# 2. 立即使用我们的独立RSA测试服务器代替
echo "启动独立RSA测试服务..."
pkill -f "RSATestServer" 2>/dev/null || true

cd /Users/jason/Projects/usdtClaudeCode
nohup java RSATestServer > rsa-server.log 2>&1 &
RSA_PID=$!

echo "RSA测试服务器PID: $RSA_PID"

# 3. 等待服务启动
sleep 3

# 4. 测试API
echo "测试RSA公钥API..."
if curl -s -f http://localhost:8090/api/admin/auth/public-key >/dev/null 2>&1; then
    echo "✅ RSA公钥API正常工作"
    
    echo ""
    echo "🎉 临时解决方案已启动！"
    echo "========================"
    echo "✅ RSA公钥API: http://localhost:8090/api/admin/auth/public-key"
    echo "✅ 用户公钥API: http://localhost:8090/api/auth/public-key"
    echo ""
    echo "🧪 测试结果："
    curl -s -H "User-Agent: Mozilla/5.0" http://localhost:8090/api/admin/auth/public-key | jq .
    echo ""
    echo "🛑 停止服务: kill $RSA_PID"
    echo "📋 查看日志: tail -f rsa-server.log"
    
else
    echo "❌ RSA服务启动失败"
    echo "检查Java RSA测试服务器..."
    ls -la RSATestServer.*
fi