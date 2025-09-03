#!/bin/bash

# 測試不同的MySQL連接方法
echo "=== 測試MySQL連接方法 ==="

# 方法1: 使用環境變量中的密碼
echo "1. 嘗試使用Docker環境變量密碼"
docker exec usdt-mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "SELECT VERSION();"' 2>&1

# 方法2: 嘗試無密碼連接(初始化階段可能沒設密碼)
echo -e "\n2. 嘗試無密碼連接"
docker exec usdt-mysql mysql -u root -e "SELECT VERSION();" 2>&1

# 方法3: 使用MySQL安全模式重設密碼
echo -e "\n3. 檢查MySQL進程"
docker exec usdt-mysql ps aux | grep mysql

echo -e "\n4. 檢查用戶表"
docker exec usdt-mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "USE mysql; SELECT user,host,authentication_string FROM mysql.user WHERE user=\"root\";"' 2>&1

echo -e "\n=== 測試完成 ==="