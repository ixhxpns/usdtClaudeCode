#!/usr/bin/env python3
"""
BCrypt密碼驗證測試工具
驗證Admin123!密碼是否與數據庫中的哈希匹配
"""

import bcrypt

# 數據庫中的哈希
db_hash = "$2b$10$0flRzvgRQBOAx.F.uuM8N.jEOa8P5/mXK23tJWsYnBdmVFJt6paDa"
password = "Admin123!"

print(f"數據庫哈希: {db_hash}")
print(f"待驗證密碼: {password}")

# 驗證密碼
try:
    result = bcrypt.checkpw(password.encode('utf-8'), db_hash.encode('utf-8'))
    print(f"BCrypt驗證結果: {result}")
    
    if result:
        print("✅ 密碼匹配成功！")
    else:
        print("❌ 密碼不匹配")
        
    # 測試生成新的哈希
    print("\n--- 生成新哈希測試 ---")
    new_hash = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt(rounds=10))
    print(f"新生成的哈希: {new_hash.decode('utf-8')}")
    
    # 驗證新哈希
    new_result = bcrypt.checkpw(password.encode('utf-8'), new_hash)
    print(f"新哈希驗證結果: {new_result}")
    
except Exception as e:
    print(f"驗證過程出錯: {e}")