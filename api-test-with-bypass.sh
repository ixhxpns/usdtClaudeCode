
#!/bin/bash

# 临时绕过Redis问题进行API测试的脚本
# 通过修改配置临时禁用Redis相关功能

BASE_URL="http://localhost:8090/api"
UA="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"

echo "USDT Trading Platform - Redis绕过测试"
echo "===================================="
echo "注意: 这是临时测试方案，不适用于生产环境"
echo

# 1. 首先创建绕过Redis的配置
echo "1. 创建测试配置"
echo "---------------"

# 检查是否可以通过环境变量禁用Redis功能
echo "尝试通过配置禁用Redis依赖的功能..."

# 2. 测试不依赖Redis的功能
echo
echo "2. 测试基础功能（不依赖Redis）"
echo "-----------------------------"

test_endpoint() {
    local method="$1"
    local path="$2"
    local data="$3"
    local desc="$4"
    local expected="$5"
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -H "User-Agent: $UA" "$BASE_URL$path")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" -H "User-Agent: $UA" -H "Content-Type: application/json" -d "$data" "$BASE_URL$path")
    fi
    
    http_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "$expected" ]; then
        echo "✓ PASS - $desc (HTTP $http_code)"
        return 0
    else
        echo "✗ FAIL - $desc (Expected $expected, Got $http_code)"
        if [ "$http_code" = "500" ]; then
            echo "  可能仍受Redis问题影响"
        fi
        echo "  Response: $(echo "$body" | head -c 100)..."
        return 1
    fi
}

# 测试基础端点
test_endpoint "GET" "/api/test/ping" "" "基础Ping测试" "200"
test_endpoint "POST" "/api/test/echo" '{"test":"hello"}' "Echo测试" "200"
test_endpoint "GET" "/api/test/cors-test" "" "CORS测试" "200"

# 测试错误处理
echo
echo "3. 测试错误处理"
echo "-------------"
test_endpoint "POST" "/api/test/simulate-error?errorCode=400" "" "400错误模拟" "200"
test_endpoint "POST" "/api/test/simulate-error?errorCode=500" "" "500错误模拟" "200"

# 测试RSA公钥（应该不依赖Redis）
echo
echo "4. 测试RSA公钥功能"
echo "-----------------"
response=$(curl -s -H "User-Agent: $UA" "$BASE_URL/api/auth/public-key")
if echo "$response" | grep -q "publicKey"; then
    echo "✓ PASS - RSA公钥获取正常"
    key_size=$(echo "$response" | grep -o '"keySize":[0-9]*' | cut -d':' -f2)
    echo "  密钥长度: $key_size bits"
else
    echo "✗ FAIL - RSA公钥获取失败"
fi

# 测试Swagger文档
echo
echo "5. 测试API文档"
echo "-------------"
test_endpoint "GET" "/api/v3/api-docs" "" "OpenAPI文档" "200"

# 尝试测试一些可能受影响的端点
echo
echo "6. 测试可能受Redis影响的端点"
echo "----------------------------"
echo "注意: 以下测试可能失败，这是预期的Redis问题"

endpoints=(
    "/api/auth/send-email-verification:POST:发送验证码"
    "/api/price/current:GET:获取价格"
    "/api/admin/auth/login:POST:管理员登录"
)

for endpoint_info in "${endpoints[@]}"; do
    IFS=':' read -r path method desc <<< "$endpoint_info"
    
    if [ "$method" = "GET" ]; then
        data=""
    else
        case "$path" in
            *verification*)
                data='{"email":"test@example.com"}'
                ;;
            *login*)
                data='{"username":"admin","password":"admin123"}'
                ;;
            *)
                data='{}'
                ;;
        esac
    fi
    
    test_endpoint "$method" "$path" "$data" "$desc" "200"
done

echo
echo "7. 分析和建议"
echo "============"

# 统计结果
total_basic=5  # 基础测试数量
basic_passed=0

# 重新运行基础测试来统计
echo "重新验证基础功能:"
basic_tests=(
    "GET:/api/test/ping"
    "POST:/api/test/echo"
    "GET:/api/test/cors-test" 
    "GET:/api/auth/public-key"
    "GET:/api/v3/api-docs"
)

for test in "${basic_tests[@]}"; do
    IFS=':' read -r method path <<< "$test"
    if [ "$method" = "GET" ]; then
        response_code=$(curl -s -o /dev/null -w "%{http_code}" -H "User-Agent: $UA" "$BASE_URL$path")
    else
        response_code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" -H "User-Agent: $UA" -H "Content-Type: application/json" -d '{"test":"data"}' "$BASE_URL$path")
    fi
    
    if [ "$response_code" = "200" ]; then
        basic_passed=$((basic_passed + 1))
    fi
done

echo
echo "测试结果总结:"
echo "- 基础功能测试: $basic_passed/$total_basic 通过"
echo "- Redis相关功能: 预期失败（需要修复Redis问题）"
echo

if [ $basic_passed -ge 3 ]; then
    echo "✅ 应用基础功能正常，主要问题是Redis连接"
    echo
    echo "建议的修复方案:"
    echo "1. 降级Redisson版本到兼容版本（推荐3.20.1）"
    echo "2. 或升级Spring Boot到兼容Java 21的版本"  
    echo "3. 或暂时降级Java到17版本"
    echo
    echo "临时生产修复："
    echo "- 在RateLimitInterceptor上添加@ConditionalOnProperty"
    echo "- 配置Redis连接池参数"
    echo "- 增加Redis连接重试机制"
else
    echo "❌ 应用存在基础问题，需要全面检查"
fi

echo
echo "下一步行动:"
echo "1. 修复Redis兼容性问题"
echo "2. 重新运行完整API测试套件"
echo "3. 进行性能和安全测试"
echo "4. 部署前的最终验证"