
#!/bin/bash

# 简化版API测试脚本
BASE_URL="http://localhost:8090/api"
UA="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"

echo "USDT Trading Platform API Test Results"
echo "======================================"
echo "Test Time: $(date)"
echo

test_count=0
pass_count=0

# 测试函数
test_endpoint() {
    local method="$1"
    local path="$2" 
    local data="$3"
    local desc="$4"
    local expected="$5"
    
    test_count=$((test_count + 1))
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -H "User-Agent: $UA" "$BASE_URL$path")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" -H "User-Agent: $UA" -H "Content-Type: application/json" -d "$data" "$BASE_URL$path")
    fi
    
    http_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "$expected" ]; then
        echo "✓ PASS - $desc (HTTP $http_code)"
        pass_count=$((pass_count + 1))
    else
        echo "✗ FAIL - $desc (Expected $expected, Got $http_code)"
        echo "  Response: $(echo "$body" | head -c 100)..."
    fi
}

echo "1. Basic Connectivity Tests"
echo "----------------------------"
test_endpoint "GET" "/api/test/ping" "" "Ping Test" "200"
test_endpoint "POST" "/api/test/echo" '{"test":"data"}' "Echo Test" "200"
test_endpoint "GET" "/api/test/cors-test" "" "CORS Test" "200"
echo

echo "2. Authentication System Tests"  
echo "-------------------------------"
test_endpoint "GET" "/api/auth/public-key" "" "Get RSA Public Key" "200"
test_endpoint "POST" "/api/auth/send-email-verification" '{"email":"test@example.com"}' "Send Verification Code" "200"
test_endpoint "POST" "/api/auth/login" '{"email":"test@example.com","password":"Test123"}' "User Login" "200"
test_endpoint "GET" "/api/auth/me" "" "Get User Info (No Token)" "401"
echo

echo "3. Price System Tests"
echo "---------------------"
test_endpoint "GET" "/api/price/current" "" "Get Current Price" "200"
test_endpoint "GET" "/api/price/realtime" "" "Get Realtime Price" "200"
echo

echo "4. Admin System Tests"
echo "---------------------"
test_endpoint "POST" "/api/admin/auth/login" '{"username":"admin","password":"admin123"}' "Admin Login" "200"
echo

echo "5. Error Handling Tests"
echo "-----------------------"
test_endpoint "POST" "/api/test/simulate-error?errorCode=400" "" "Simulate 400 Error" "200"
test_endpoint "POST" "/api/test/simulate-error?errorCode=500" "" "Simulate 500 Error" "200"
test_endpoint "GET" "/api/nonexistent" "" "404 Error Test" "404"
echo

echo "6. Security Tests"
echo "-----------------"
test_endpoint "GET" "/api/test/ping" "" "Basic Request" "200"
echo

echo "TEST SUMMARY"
echo "============"
echo "Total Tests: $test_count"
echo "Passed: $pass_count"
echo "Failed: $((test_count - pass_count))"
echo "Success Rate: $((pass_count * 100 / test_count))%"
echo
echo "Completed at: $(date)"