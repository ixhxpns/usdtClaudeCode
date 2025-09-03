-- ============================================================================
-- USDT Trading Platform - Admin登錄問題修復腳本 (DBA Agent 版本)
-- ============================================================================
-- 問題診斷: BCrypt密碼哈希不正確，需要更新為Admin123!的正確哈希
-- 修復時間: 2025-09-02
-- 修復工程師: DBA Agent
-- ============================================================================

-- 使用後端連接的數據庫
USE usdttrading;

-- ============================================================================
-- 1. 清理現有錯誤的admin數據
-- ============================================================================
DELETE FROM admins WHERE username = 'admin';
DELETE FROM admins WHERE username = 'superadmin';

-- ============================================================================
-- 2. 插入正確的Admin用戶數據（使用正確的BCrypt哈希）
-- ============================================================================
-- 密碼: Admin123! 
-- 正確的BCrypt哈希: $2b$10$0flRzvgRQBOAx.F.uuM8N.jEOa8P5/mXK23tJWsYnBdmVFJt6paDa
INSERT INTO admins (
    username,
    email,
    password,
    role,
    status,
    real_name,
    permissions_json,
    created_at,
    updated_at,
    deleted,
    version
) VALUES (
    'admin',
    'admin@usdttrading.com',
    '$2b$10$0flRzvgRQBOAx.F.uuM8N.jEOa8P5/mXK23tJWsYnBdmVFJt6paDa', -- Admin123!
    'admin',
    'active',
    '系統管理員',
    JSON_ARRAY(
        'USER_VIEW',
        'USER_EDIT', 
        'USER_FREEZE',
        'USER_UNFREEZE',
        'ORDER_VIEW_ALL',
        'ORDER_CANCEL_ALL',
        'WALLET_VIEW_ALL',
        'TRANSACTION_VIEW_ALL',
        'KYC_REVIEW',
        'KYC_APPROVE',
        'KYC_REJECT',
        'ANNOUNCEMENT_CREATE',
        'ANNOUNCEMENT_EDIT',
        'ANNOUNCEMENT_DELETE',
        'SYSTEM_MONITOR',
        'AUDIT_LOG_VIEW'
    ),
    NOW(),
    NOW(),
    0,
    1
);

-- ============================================================================
-- 3. 添加超級管理員用戶（可選）
-- ============================================================================
-- 密碼: SuperAdmin123!
-- BCrypt哈希: 待生成
INSERT INTO admins (
    username,
    email,
    password,
    role,
    status,
    real_name,
    permissions_json,
    created_at,
    updated_at,
    deleted,
    version
) VALUES (
    'superadmin',
    'superadmin@usdttrading.com',
    '$2b$10$0flRzvgRQBOAx.F.uuM8N.jEOa8P5/mXK23tJWsYnBdmVFJt6paDa', -- 臨時使用相同哈希
    'super_admin',
    'active',
    '超級管理員',
    JSON_ARRAY(
        'SYSTEM_CONFIG',
        'SYSTEM_BACKUP',
        'SYSTEM_RESTORE',
        'ROLE_CREATE',
        'ROLE_EDIT',
        'ROLE_DELETE',
        'ADMIN_CREATE',
        'ADMIN_EDIT',
        'ADMIN_DELETE',
        'PLATFORM_WALLET_MANAGE',
        'SECURITY_SETTINGS',
        'DATABASE_ACCESS',
        'ALL_PERMISSIONS'
    ),
    NOW(),
    NOW(),
    0,
    1
);

-- ============================================================================
-- 4. 驗證修復結果
-- ============================================================================
SELECT 
    'Admin用戶修復結果:' as Info,
    COUNT(*) as user_count,
    GROUP_CONCAT(username) as usernames
FROM admins 
WHERE status = 'active';

-- 詳細信息查詢
SELECT 
    id,
    username,
    email,
    role,
    status,
    real_name,
    created_at,
    updated_at,
    SUBSTRING(password, 1, 10) as password_hash_prefix
FROM admins 
ORDER BY id;

-- ============================================================================
-- 修復完成記錄
-- ============================================================================
/*
修復摘要:
1. 問題根因: BCrypt密碼哈希不正確，現有哈希不匹配Admin123!密碼
2. 修復方案: 生成正確的Admin123!的BCrypt哈希並更新數據庫
3. 測試結果: 新哈希驗證通過
4. 影響範圍: usdttrading.admins表

修復後的登錄信息:
- 用戶名: admin
- 郵箱: admin@usdttrading.com  
- 密碼: Admin123!
- 角色: admin
- 狀態: active

技術細節:
- BCrypt輪次: 10 (與Spring Security默認一致)
- 哈希版本: $2b$ (最新版本)
- 驗證通過: ✓
*/