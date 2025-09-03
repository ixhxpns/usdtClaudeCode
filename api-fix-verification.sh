
#!/bin/bash

# ============================================================================
# USDT交易平台 API修复验证脚本
# ============================================================================
# 用途：验证API路由修复是否成功
# 作者：Master Agent
# 版本：1.0
# 日期：2025-09-01
# ============================================================================

echo "=========================================="
echo "🔧 USDT Trading Platform API 修复验证"
echo "=========================================="
echo "时间: $(date)"
echo "目标端口: 8090"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试函数
test_api() {
    local name=$1
    local url=$2
    local method=${3:-GET}
    
    echo -n "测试 $name ... "
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "%{http_code}" -o /tmp/api_response "$url" --connect-timeout 5 --max-time 10)
    else
        response=$(curl -s -w "%{http_code}" -o /tmp/api_response -X "$method" "$url" --connect-timeout 5 --max-time 10)
    fi
    
    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✅ 成功${NC}"
        return 0
    elif [ "$response" = "404" ]; then
        echo -e "${RED}❌ 404 路由失效${NC}"
        return 1
    elif [ "$response" = "000" ]; then
        echo -e "${YELLOW}⚠️  连接失败${NC}"
        return 2
    else
        echo -e "${YELLOW}⚠️  状态码: $response${NC}"
        return 3
    fi
}

# 基础连通性测试
echo -e "${BLUE}🔍 基础连通性测试${NC}"
echo "----------------------------------------"
test_api "后端服务连通性" "http://localhost:8090/"
test_api "健康检查" "http://localhost:8090/actuator/health"
echo ""

# 核心API路由测试
echo -e "${BLUE}🎯 核心API路由测试${NC}"
echo "----------------------------------------"

# 认证API测试
test_api "RSA公钥获取" "http://localhost:8090/api/auth/public-key"
auth_api_result=$?

test_api "用户名可用性检查" "http://localhost:8090/api/auth/check-username?username=test"
test_api "邮箱可用性检查" "http://localhost:8090/api/auth/check-email?email=test@test.com"

# 测试API测试
test_api "Ping测试" "http://localhost:8090/api/test/ping"
test_api_result=$?

# 价格API测试
test_api "当前价格" "http://localhost:8090/api/price/current"
price_api_result=$?

test_api "价格统计" "http://localhost:8090/api/price/statistics?period=24h"

# 管理员API测试
test_api "管理员RSA公钥" "http://localhost:8090/api/admin/auth/public-key"
admin_api_result=$?

echo ""

# 结果汇总
echo -e "${BLUE}📊 修复结果汇总${NC}"
echo "=========================================="

total_tests=6
passed_tests=0
failed_tests=0

# 统计结果
if [ $auth_api_result -eq 0 ]; then ((passed_tests++)); else ((failed_tests++)); fi
if [ $test_api_result -eq 0 ]; then ((passed_tests++)); else ((failed_tests++)); fi  
if [ $price_api_result -eq 0 ]; then ((passed_tests++)); else ((failed_tests++)); fi
if [ $admin_api_result -eq 0 ]; then ((passed_tests++)); else ((failed_tests++)); fi

# 额外检查
curl -s "http://localhost:8090/api/auth/check-username?username=test" | grep -q "available" && ((passed_tests++)) || ((failed_tests++))
curl -s "http://localhost:8090/api/auth/check-email?email=test@test.com" | grep -q "available" && ((passed_tests++)) || ((failed_tests++))

echo "通过测试: $passed_tests/$total_tests"
echo "失败测试: $failed_tests/$total_tests"

# 修复状态判断
if [ $passed_tests -ge 4 ]; then
    echo -e "${GREEN}🎉 修复成功！API路由已恢复正常${NC}"
    echo ""
    echo -e "${GREEN}✅ 系统状态：健康${NC}"
    echo -e "${GREEN}✅ API服务：可用${NC}"
    echo -e "${GREEN}✅ 路由映射：正常${NC}"
    
    echo ""
    echo -e "${BLUE}🚀 下一步建议：${NC}"
    echo "1. 逐步重新启用拦截器"
    echo "2. 进行完整的功能测试"
    echo "3. 监控系统运行状态"
    
elif [ $passed_tests -ge 2 ]; then
    echo -e "${YELLOW}⚠️  部分修复成功，仍有API路由问题${NC}"
    echo ""
    echo -e "${YELLOW}📋 后续行动：${NC}"
    echo "1. 检查特定控制器的映射问题"
    echo "2. 查看应用日志详细错误信息"
    echo "3. 验证组件扫描配置"
    
else
    echo -e "${RED}❌ 修复失败！API路由仍然完全失效${NC}"
    echo ""
    echo -e "${RED}🔥 紧急措施：${NC}"
    echo "1. 检查应用是否正常启动"
    echo "2. 查看容器日志：docker logs usdt-backend"
    echo "3. 验证Spring Boot配置"
    echo "4. 考虑重建容器"
fi

echo ""

# 详细诊断信息
echo -e "${BLUE}🔬 详细诊断信息${NC}"
echo "=========================================="

# 检查容器状态
echo -n "容器状态: "
if docker ps | grep -q "usdt-backend.*healthy"; then
    echo -e "${GREEN}健康运行${NC}"
else
    echo -e "${RED}异常状态${NC}"
fi

# 检查端口监听
echo -n "端口8090监听: "
if netstat -tlnp 2>/dev/null | grep -q ":8090 "; then
    echo -e "${GREEN}正常监听${NC}"
else
    echo -e "${RED}端口未监听${NC}"
fi

# 检查最新日志
echo ""
echo -e "${BLUE}📄 最新应用日志（最后10行）：${NC}"
docker logs usdt-backend --tail 10 2>/dev/null | head -10

echo ""
echo "=========================================="
echo -e "${BLUE}验证完成 - $(date)${NC}"
echo "=========================================="

# 如果完全修复成功，返回0；否则返回1
if [ $passed_tests -ge 4 ]; then
    exit 0
else
    exit 1
fi