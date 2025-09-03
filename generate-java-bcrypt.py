#!/usr/bin/env python3
"""
使用Java Spring Security兼容的BCrypt版本生成密碼哈希
"""

import bcrypt

# 密碼
password = "Admin123!"

# 生成$2a$版本的BCrypt哈希（與Java Spring Security兼容）
salt = bcrypt.gensalt(rounds=10, prefix=b"2a")
hash_2a = bcrypt.hashpw(password.encode('utf-8'), salt)

print(f"密碼: {password}")
print(f"$2a$ 版本哈希: {hash_2a.decode('utf-8')}")

# 驗證新生成的哈希
result = bcrypt.checkpw(password.encode('utf-8'), hash_2a)
print(f"驗證結果: {result}")

# 測試原來的$2b$哈希
original_hash = "$2b$10$0flRzvgRQBOAx.F.uuM8N.jEOa8P5/mXK23tJWsYnBdmVFJt6paDa"
result_2b = bcrypt.checkpw(password.encode('utf-8'), original_hash.encode('utf-8'))
print(f"原$2b$哈希驗證結果: {result_2b}")