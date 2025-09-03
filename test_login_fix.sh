#!/bin/bash

echo "==========================================="
echo "USDT Trading Platform - 測試登入修復"
echo "==========================================="

# 測試1：使用正確轉義的JSON
echo "測試1：直接登入API（轉義JSON）"
response=$(curl -s -X POST "http://localhost:8090/api/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"Admin123!\",\"rememberMe\":false}")
echo "響應: $response"

echo ""
echo "測試2：不使用感嘆號的密碼"
response2=$(curl -s -X POST "http://localhost:8090/api/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123","rememberMe":false}')
echo "響應: $response2"

echo ""
echo "測試3：檢查數據庫中的實際數據"
docker exec -i usdt-mysql mysql -u root -pUsdtTrading123! -e "
USE usdttrading; 
SELECT username, password, status FROM admins WHERE username = 'admin';
" 2>/dev/null

echo ""
echo "==========================================="
echo "測試完成"
echo "==========================================="