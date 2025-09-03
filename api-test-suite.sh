
#!/bin/bash

# USDT交易平台API测试套件
# 作者: API Testing Specialist
# 日期: $(date)

BASE_URL="http://localhost:8090/api"
USER_AGENT="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
TEST_EMAIL="testuser@example.com"
TEST_PASSWORD="Test123456"
ADMIN_EMAIL="admin@example.com" 
ADMIN_PASSWORD="Admin123456"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试计数器
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 日志文件
LOG_FILE="api-test-results-$(date +%Y%m%d_%H%M%S).log"
DETAILED_LOG="api-test-detailed-$(date +%Y%m%d_%H%M%S).log"

# 存储token
USER_TOKEN=""
ADMIN_TOKEN=""

# 函数: 打印测试标题
print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo "$1" >> "$LOG_FILE"
    echo "========================================" >> "$LOG_FILE"
}

# 函数: 打印测试子标题
print_subheader() {
    echo -e "${YELLOW}--- $1 ---${NC}"
    echo "--- $1 ---" >> "$LOG_FILE"
}

# 函数: 执行HTTP请求并记录结果
test_api() {
    local method="$1"
    local endpoint="$2"
    local data="$3"
    local expected_code="$4"
    local test_name="$5"
    local headers="$6"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    # 构建curl命令
    local cmd="curl -s -X $method"
    cmd="$cmd -H 'User-Agent: $USER_AGENT'"
    cmd="$cmd -H 'Content-Type: application/json'"
    
    # 添加额外头部
    if [[ -n "$headers" ]]; then
        while IFS= read -r header; do
            cmd="$cmd -H '$header'"
        done <<< "$headers"
    fi
    
    # 添加数据
    if [[ -n "$data" && "$method" != "GET" ]]; then
        cmd="$cmd -d '$data'"
    fi
    
    # 添加URL
    cmd="$cmd '$BASE_URL$endpoint'"
    
    # 执行请求
    local start_time=$(date +%s%N)
    local response=$(eval "$cmd -w '\\n%{http_code}\\n%{time_total}'")
    local end_time=$(date +%s%N)
    
    # 解析响应
    local body=$(echo "$response" | head -n -2)
    local http_code=$(echo "$response" | tail -n 2 | head -n 1)
    local response_time=$(echo "$response" | tail -n 1)
    
    # 记录详细信息
    {
        echo "=================================================="
        echo "Test: $test_name"
        echo "Method: $method $endpoint"
        echo "Request Data: $data"
        echo "Expected Code: $expected_code"
        echo "Actual Code: $http_code"
        echo "Response Time: ${response_time}s"
        echo "Response Body: $body"
        echo "Timestamp: $(date)"
        echo
    } >> "$DETAILED_LOG"
    
    # 检查结果
    if [[ "$http_code" == "$expected_code" ]]; then
        echo -e "${GREEN}✓ PASS${NC} - $test_name (${response_time}s)"
        echo "✓ PASS - $test_name (Code: $http_code, Time: ${response_time}s)" >> "$LOG_FILE"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        
        # 提取token如果是登录请求
        if [[ "$endpoint" == *"/login" && "$http_code" == "200" ]]; then
            local token=$(echo "$body" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
            if [[ "$endpoint" == *"/admin/"* ]]; then
                ADMIN_TOKEN="$token"
                echo "Admin token extracted: ${token:0:20}..." >> "$DETAILED_LOG"
            else
                USER_TOKEN="$token"
                echo "User token extracted: ${token:0:20}..." >> "$DETAILED_LOG"
            fi
        fi
        
        return 0
    else
        echo -e "${RED}✗ FAIL${NC} - $test_name (Expected: $expected_code, Got: $http_code)"
        echo "✗ FAIL - $test_name (Expected: $expected_code, Got: $http_code)" >> "$LOG_FILE"
        echo "  Response: $body" >> "$LOG_FILE"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# 函数: 性能测试
performance_test() {
    local endpoint="$1"
    local test_name="$2"
    local concurrent_users="$3"
    local total_requests="$4"
    
    print_subheader "Performance Test: $test_name"
    
    local cmd="ab -n $total_requests -c $concurrent_users -H 'User-Agent: $USER_AGENT' '$BASE_URL$endpoint'"
    echo "Running: $cmd"
    
    local result=$(eval "$cmd" 2>/dev/null)
    local rps=$(echo "$result" | grep "Requests per second" | awk '{print $4}')
    local avg_time=$(echo "$result" | grep "Time per request" | head -1 | awk '{print $4}')
    
    echo -e "RPS: ${GREEN}$rps${NC}, Average Time: ${GREEN}${avg_time}ms${NC}"
    echo "Performance - $test_name: RPS=$rps, AvgTime=${avg_time}ms" >> "$LOG_FILE"
}

# 开始测试
echo -e "${BLUE}USDT Trading Platform API Test Suite${NC}"
echo -e "${BLUE}Started at: $(date)${NC}"
echo "USDT Trading Platform API Test Suite" > "$LOG_FILE"
echo "Started at: $(date)" >> "$LOG_FILE"

# 1. 基础连通性测试
print_header "1. 基础连通性测试"
test_api "GET" "/api/test/ping" "" "200" "Ping测试"
test_api "POST" "/api/test/echo" '{"test":"data"}' "200" "Echo测试"
test_api "GET" "/api/test/cors-test" "" "200" "CORS测试"

# 2. 错误处理测试
print_header "2. 错误处理测试"
test_api "POST" "/api/test/simulate-error?errorCode=400" "" "200" "模拟400错误"
test_api "POST" "/api/test/simulate-error?errorCode=401" "" "200" "模拟401错误"
test_api "POST" "/api/test/simulate-error?errorCode=403" "" "200" "模拟403错误"
test_api "POST" "/api/test/simulate-error?errorCode=404" "" "200" "模拟404错误"
test_api "POST" "/api/test/simulate-error?errorCode=500" "" "200" "模拟500错误"

# 3. 认证系统测试
print_header "3. 用户认证系统测试"

# 获取RSA公钥
test_api "GET" "/api/auth/public-key" "" "200" "获取RSA公钥"

# 用户注册流程
test_api "POST" "/api/auth/send-email-verification" "{\"email\":\"$TEST_EMAIL\"}" "200" "发送注册验证码"
test_api "POST" "/api/auth/register" "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\",\"verificationCode\":\"123456\"}" "200" "用户注册"

# 用户登录
test_api "POST" "/api/auth/login" "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}" "200" "用户登录"

# 4. 权限验证测试
print_header "4. 权限验证测试"

# 无token访问需要认证的端点
test_api "GET" "/api/auth/me" "" "401" "无token访问用户信息"

# 有效token访问
if [[ -n "$USER_TOKEN" ]]; then
    test_api "GET" "/api/auth/me" "" "200" "有效token访问用户信息" "Authorization: Bearer $USER_TOKEN"
    test_api "GET" "/api/wallet/balance" "" "200" "获取钱包余额" "Authorization: Bearer $USER_TOKEN"
fi

# 5. 管理员认证测试
print_header "5. 管理员认证系统测试"
test_api "POST" "/api/admin/auth/login" "{\"username\":\"admin\",\"password\":\"admin123\"}" "200" "管理员登录"

# 6. KYC功能测试
print_header "6. KYC功能测试"
if [[ -n "$USER_TOKEN" ]]; then
    test_api "GET" "/api/kyc/status" "" "200" "查询KYC状态" "Authorization: Bearer $USER_TOKEN"
    test_api "POST" "/api/kyc/basic" "{\"fullName\":\"Test User\",\"idNumber\":\"123456789\",\"birthDate\":\"1990-01-01\"}" "200" "提交基本KYC信息" "Authorization: Bearer $USER_TOKEN"
fi

# 7. 价格系统测试
print_header "7. 价格系统测试"
test_api "GET" "/api/price/current" "" "200" "获取当前价格(公开)"
test_api "GET" "/api/price/realtime" "" "200" "获取实时价格详情"

# 8. 交易功能测试
print_header "8. 交易功能测试"
if [[ -n "$USER_TOKEN" ]]; then
    test_api "GET" "/api/trading/price" "" "200" "获取交易价格" "Authorization: Bearer $USER_TOKEN"
    test_api "GET" "/api/trading/orders" "" "200" "获取订单列表" "Authorization: Bearer $USER_TOKEN"
    test_api "POST" "/api/trading/buy" "{\"amount\":100,\"paymentMethod\":\"bank\"}" "200" "创建买入订单" "Authorization: Bearer $USER_TOKEN"
fi

# 9. 安全性测试
print_header "9. 安全性测试"
test_api "GET" "/api/auth/me" "" "401" "SQL注入测试 - Union Select" "Authorization: Bearer ' UNION SELECT 1,2,3--"
test_api "POST" "/api/auth/login" "{\"email\":\"admin@example.com\",\"password\":\"'; DROP TABLE users; --\"}" "400" "SQL注入测试 - 密码字段"
test_api "GET" "/api/test/ping" "" "200" "XSS测试" "X-Custom-Header: <script>alert('xss')</script>"

# 10. 性能测试
print_header "10. 性能测试"
if command -v ab >/dev/null 2>&1; then
    performance_test "/api/test/ping" "Ping端点性能" 10 100
    performance_test "/api/price/current" "价格查询性能" 20 200
else
    echo "Apache Bench (ab) not found, skipping performance tests"
fi

# 11. 边界值测试
print_header "11. 边界值测试"
test_api "POST" "/api/auth/register" "{\"email\":\"\"}" "400" "空邮箱注册"
test_api "POST" "/api/auth/register" "{\"email\":\"invalid-email\"}" "400" "无效邮箱格式"
test_api "POST" "/api/auth/register" "{\"password\":\"123\"}" "400" "过短密码"

# 12. 并发测试
print_header "12. 并发测试"
concurrent_test() {
    local endpoint="$1"
    local test_name="$2"
    local concurrent=5
    
    echo "Running $concurrent concurrent requests to $endpoint..."
    for i in $(seq 1 $concurrent); do
        test_api "GET" "$endpoint" "" "200" "$test_name #$i" &
    done
    wait
}

concurrent_test "/api/test/ping" "并发Ping测试"
concurrent_test "/api/price/current" "并发价格查询"

# 13. 数据一致性测试
print_header "13. 数据一致性测试"
if [[ -n "$USER_TOKEN" ]]; then
    # 测试钱包余额一致性
    for i in {1..3}; do
        test_api "GET" "/api/wallet/balance" "" "200" "余额一致性测试 #$i" "Authorization: Bearer $USER_TOKEN"
        sleep 1
    done
fi

# 14. API文档一致性测试
print_header "14. API文档一致性测试"
test_api "GET" "/api/v3/api-docs" "" "200" "OpenAPI文档可访问性"

# 生成测试报告
print_header "测试总结"
echo -e "${GREEN}总测试数: $TOTAL_TESTS${NC}"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"
echo -e "${YELLOW}成功率: $((PASSED_TESTS * 100 / TOTAL_TESTS))%${NC}"

{
    echo
    echo "========================================" 
    echo "测试总结"
    echo "========================================"
    echo "总测试数: $TOTAL_TESTS"
    echo "通过: $PASSED_TESTS"
    echo "失败: $FAILED_TESTS"  
    echo "成功率: $((PASSED_TESTS * 100 / TOTAL_TESTS))%"
    echo "完成时间: $(date)"
} >> "$LOG_FILE"

echo -e "${BLUE}详细日志保存在: $DETAILED_LOG${NC}"
echo -e "${BLUE}测试报告保存在: $LOG_FILE${NC}"

# 如果有失败的测试，返回非零退出码
if [[ $FAILED_TESTS -gt 0 ]]; then
    exit 1
fi