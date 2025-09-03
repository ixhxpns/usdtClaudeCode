#!/bin/bash

# ============================================================================
# USDT Trading Platform - 測試Admin登錄功能
# ============================================================================
# 用途：測試修復後的admin登錄功能
# 執行時間：2025-09-02
# ============================================================================

echo "=========================================="
echo "USDT Trading Platform - Admin登錄測試"
echo "=========================================="

BASE_URL="http://localhost:8090/api/admin/auth"

# 步驟1：獲取RSA公鑰
echo "步驟1：獲取RSA公鑰..."
RSA_RESPONSE=$(curl -s -X GET "${BASE_URL}/public-key")
echo "RSA公鑰響應: ${RSA_RESPONSE}"

# 步驟2：測試登錄（使用明文密碼，讓後端降級處理）
echo ""
echo "步驟2：測試登錄..."
LOGIN_DATA='{
  "username": "admin",
  "password": "Admin123!",
  "rememberMe": false
}'

echo "登錄數據: ${LOGIN_DATA}"

LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/login" \
  -H "Content-Type: application/json" \
  -d "${LOGIN_DATA}")

echo "登錄響應: ${LOGIN_RESPONSE}"

# 步驟3：檢查響應
if echo "${LOGIN_RESPONSE}" | grep -q "success.*true"; then
    echo ""
    echo "✅ 登錄成功！"
    
    # 提取token
    TOKEN=$(echo "${LOGIN_RESPONSE}" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
    echo "Access Token: ${TOKEN}"
    
    # 測試驗證會話
    echo ""
    echo "步驟3：測試會話驗證..."
    SESSION_RESPONSE=$(curl -s -X GET "${BASE_URL}/session/validate" \
      -H "Content-Type: application/json" \
      -H "satoken: ${TOKEN}")
    
    echo "會話驗證響應: ${SESSION_RESPONSE}"
    
else
    echo ""
    echo "❌ 登錄失敗！"
    echo "錯誤詳情: ${LOGIN_RESPONSE}"
fi

echo ""
echo "=========================================="
echo "測試完成"
echo "=========================================="