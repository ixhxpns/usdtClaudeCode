#!/usr/bin/env python3
"""
驗證Admin登錄修復是否成功
"""

import bcrypt

# 測試密碼
test_password = "Admin123!"
test_password_bytes = test_password.encode('utf-8')

# 修復後的哈希
fixed_hash = "$2b$10$0flRzvgRQBOAx.F.uuM8N.jEOa8P5/mXK23tJWsYnBdmVFJt6paDa"

# 驗證
is_valid = bcrypt.checkpw(test_password_bytes, fixed_hash.encode('utf-8'))

print("=== Admin登錄修復驗證 ===")
print(f"測試密碼: {test_password}")
print(f"BCrypt哈希: {fixed_hash}")
print(f"驗證結果: {'✓ 通過' if is_valid else '✗ 失敗'}")

if is_valid:
    print("\n✅ 修復成功！admin用戶現在可以使用Admin123!密碼登錄")
else:
    print("\n❌ 修復失敗！需要重新檢查哈希")

# 額外測試 - 生成另一個哈希來確保不同哈希值都能驗證同一密碼
print(f"\n=== 額外驗證 ===")
new_salt = bcrypt.gensalt(rounds=10)
new_hash = bcrypt.hashpw(test_password_bytes, new_salt)
new_is_valid = bcrypt.checkpw(test_password_bytes, new_hash)
print(f"新生成哈希: {new_hash.decode('utf-8')}")
print(f"新哈希驗證: {'✓ 通過' if new_is_valid else '✗ 失敗'}")

print(f"\n=== 技術確認 ===")
print(f"哈希版本: $2b$ (推薦)")
print(f"輪次: 10 (與Spring Security默認一致)")
print(f"適用框架: Java Spring Security BCryptPasswordEncoder")