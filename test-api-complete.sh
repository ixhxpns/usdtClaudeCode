#!/bin/bash

# USDT交易平台 - 完整API測試腳本
# Master Agent 指令執行

echo "🎯 USDT交易平台 - 完整API功能測試"
echo "=========================================="
echo ""


BASE_URL="http://localhost:8090"
RESULTS_FILE="/tmp/api_test_results.json"
echo "{}" > $RESULTS_FILE

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 測試函數
test_api() {
    local endpoint=$1
    local description=$2
    local method=${3:-GET}
    local data=${4:-""}
    
    echo -n "測試 $description ... "
    
    if [ "$method" = "POST" ] && [ -n "$data" ]; then
        response=$(curl -s -w "%{http_code}" -X POST -H "Content-Type: application/json" -d "$data" "$BASE_URL$endpoint" 2>/dev/null)
    else
        response=$(curl -s -w "%{http_code}" "$BASE_URL$endpoint" 2>/dev/null)
    fi
    
    if [ $? -eq 0 ]; then
        http_code="${response: -3}"
        body="${response%???}"
        
        if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
            echo -e "${GREEN}✅ 成功${NC} (HTTP $http_code)"
            # 嘗試解析JSON
            if echo "$body" | jq . >/dev/null 2>&1; then
                echo "   響應: $(echo "$body" | jq -c .)"
            else
                echo "   響應: $body" | head -c 100
            fi
        else
            echo -e "${YELLOW}⚠️  HTTP $http_code${NC}"
            echo "   響應: $(echo "$body" | head -c 100)"
        fi
    else
        echo -e "${RED}❌ 連接失敗${NC}"
    fi
    echo ""
}

# 基礎測試
echo "📋 基礎API測試"
echo "--------------------"
test_api "/api/test/ping" "Ping測試"
test_api "/api/test/echo" "Echo測試" "POST" '{"message": "test"}'
test_api "/api/test/cors-test" "CORS測試"

# RSA加密測試
echo ""
echo "🔐 RSA加密模組測試"
echo "--------------------"

test_api "/api/auth/public-key" "獲取RSA公鑰"
test_api "/api/admin/auth/public-key" "獲取管理員RSA公鑰"

# 價格模組測試
echo ""
echo "💰 價格查詢模組測試"
echo "--------------------"
test_api "/api/price/current" "獲取當前價格"
test_api "/api/price/history?period=24h&interval=1h" "獲取價格歷史"
test_api "/api/price/statistics?period=24h" "獲取價格統計"

# 認證模組測試
echo ""
echo "🔑 認證系統測試"
echo "--------------------"
test_api "/api/auth/send-email-verification" "發送郵件驗證" "POST" '{"email": "test@example.com"}'

# 管理員認證測試
echo ""
echo "👑 管理員認證測試"
echo "--------------------"
test_api "/api/admin/auth/login" "管理員登錄" "POST" '{"username": "admin", "password": "admin123"}'

# 系統健康度測試
echo ""
echo "🏥 系統健康度測試"
echo "--------------------"
test_api "/actuator/health" "Spring Actuator健康檢查"
test_api "/actuator/health" "自定義健康檢查"

echo ""
echo "=========================================="
echo "測試完成！"
echo ""

# 生成測試摘要報告
echo "📊 測試摘要報告" > /tmp/api_test_summary.txt
echo "執行時間: $(date)" >> /tmp/api_test_summary.txt
echo "" >> /tmp/api_test_summary.txt

echo "測試摘要報告已生成: /tmp/api_test_summary.txt"