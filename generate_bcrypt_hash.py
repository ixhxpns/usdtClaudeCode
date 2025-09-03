#!/usr/bin/env python3
"""
生成BCrypt密碼哈希的Python腳本
用於驗證Admin123!密碼的正確哈希值
"""

import bcrypt

# 密碼
password = "Admin123!"
password_bytes = password.encode('utf-8')

# 生成BCrypt哈希（使用輪次10，與Java Spring Security默認一致）
salt = bcrypt.gensalt(rounds=10)
hashed = bcrypt.hashpw(password_bytes, salt)

print(f"密碼: {password}")
print(f"BCrypt 哈希: {hashed.decode('utf-8')}")

# 驗證現有哈希
existing_hashes = [
    "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZjcOvUS/zfUcHbz6JqHllgHQdxYy",  # usdt_trading_platform
    "$2a$10$VDXKXCqKCZBGjZyNsKbZUOleFZ7qEJx5FKOB7F5fKRSkPrv.QYcfK",  # usdttrading
    "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi"   # Backend Agent腳本中的
]

for i, existing_hash in enumerate(existing_hashes, 1):
    is_valid = bcrypt.checkpw(password_bytes, existing_hash.encode('utf-8'))
    print(f"哈希 {i}: {existing_hash} - 驗證結果: {is_valid}")