-- ============================================================================
-- USDT Trading Platform - 修復Admin登錄問題
-- ============================================================================
-- 解決方案：創建admins表並初始化admin用戶數據
-- 執行時間：2025-09-02
-- ============================================================================

USE usdt_trading_platform;

-- ============================================================================
-- 1. 創建admins表
-- ============================================================================
CREATE TABLE IF NOT EXISTS admins (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '管理員用戶名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '郵箱地址',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手機號碼',
    password VARCHAR(255) NOT NULL COMMENT '密碼哈希（BCrypt）',
    role VARCHAR(50) NOT NULL DEFAULT 'admin' COMMENT '角色',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '狀態',
    real_name VARCHAR(100) DEFAULT NULL COMMENT '真實姓名',
    permissions_json TEXT DEFAULT NULL COMMENT '權限JSON',
    last_login_at TIMESTAMP NULL DEFAULT NULL COMMENT '最後登錄時間',
    last_login_ip VARCHAR(45) DEFAULT NULL COMMENT '最後登錄IP',
    login_attempts INT DEFAULT 0 COMMENT '登錄失敗次數',
    locked_until TIMESTAMP NULL DEFAULT NULL COMMENT '鎖定到期時間',
    mfa_enabled BOOLEAN DEFAULT FALSE COMMENT '是否啟用MFA',
    mfa_secret VARCHAR(255) DEFAULT NULL COMMENT 'MFA密鑰',
    remarks TEXT DEFAULT NULL COMMENT '備註',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    deleted BOOLEAN DEFAULT FALSE COMMENT '軟刪除標記',
    version INT DEFAULT 0 COMMENT '樂觀鎖版本',
    
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_role (role),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理員表';

-- ============================================================================
-- 2. 插入admin用戶數據
-- ============================================================================
-- 密碼: Admin123! (BCrypt加密)
INSERT INTO admins (
    username,
    email,
    phone,
    password,
    role,
    status,
    real_name,
    permissions_json,
    created_at,
    updated_at
) VALUES (
    'admin',
    'admin@usdttrading.com',
    '+886987654321',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- Admin123!
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
    NOW()
) ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    status = VALUES(status),
    updated_at = NOW();

-- ============================================================================
-- 3. 插入超級管理員用戶數據
-- ============================================================================
-- 密碼: SuperAdmin123! (BCrypt加密)
INSERT INTO admins (
    username,
    email,
    phone,
    password,
    role,
    status,
    real_name,
    permissions_json,
    created_at,
    updated_at
) VALUES (
    'superadmin',
    'superadmin@usdttrading.com',
    '+886900000000',
    '$2a$10$E4iABBAM2uDzFUqiJy13UOA1RFUQ4T/Kh9gEHdpx/6uy/6TeRESxi', -- SuperAdmin123!
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
    NOW()
) ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    status = VALUES(status),
    updated_at = NOW();

-- ============================================================================
-- 4. 驗證數據創建結果
-- ============================================================================
SELECT 'Admin用戶創建結果:' as Info;
SELECT 
    id,
    username,
    email,
    role,
    status,
    real_name,
    created_at
FROM admins;

-- ============================================================================
-- 完成
-- ============================================================================
/*
修復完成後的登錄信息：

1. 管理員帳號
   - 用戶名: admin
   - 郵箱: admin@usdttrading.com
   - 密碼: Admin123!
   - 角色: admin

2. 超級管理員帳號
   - 用戶名: superadmin
   - 郵箱: superadmin@usdttrading.com
   - 密碼: SuperAdmin123!
   - 角色: super_admin

注意：所有帳號狀態均為 active，可以直接登錄使用。
*/