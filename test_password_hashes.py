#!/usr/bin/env python3
"""
測試各種可能的密碼與現有哈希的匹配
"""

import bcrypt

# 可能的密碼列表
possible_passwords = [
    "Admin123!",
    "admin123",
    "password",
    "admin",
    "123456",
    "UsdtTrading123!",
    "admin123!",
    "Admin123",
    "ADMIN123!",
    "usdtadmin",
    "secret"
]

# 現有哈希
existing_hashes = [
    ("usdt_trading_platform", "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZjcOvUS/zfUcHbz6JqHllgHQdxYy"),
    ("usdttrading", "$2a$10$VDXKXCqKCZBGjZyNsKbZUOleFZ7qEJx5FKOB7F5fKRSkPrv.QYcfK"),
    ("Backend Agent腳本", "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi")
]

print("=== 測試密碼匹配 ===")
for hash_name, hash_value in existing_hashes:
    print(f"\n{hash_name}: {hash_value}")
    for password in possible_passwords:
        try:
            is_valid = bcrypt.checkpw(password.encode('utf-8'), hash_value.encode('utf-8'))
            if is_valid:
                print(f"  ✓ 匹配密碼: {password}")
            else:
                print(f"  ✗ 不匹配: {password}")
        except Exception as e:
            print(f"  錯誤: {password} - {e}")

# 生成一個新的正確哈希
print(f"\n=== 生成新的Admin123!哈希 ===")
password = "Admin123!"
salt = bcrypt.gensalt(rounds=10)
new_hash = bcrypt.hashpw(password.encode('utf-8'), salt)
print(f"新哈希: {new_hash.decode('utf-8')}")

# 驗證新哈希
is_valid = bcrypt.checkpw(password.encode('utf-8'), new_hash)
print(f"驗證結果: {is_valid}")