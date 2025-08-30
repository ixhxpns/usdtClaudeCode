-- ============================================================================
-- USDT Trading Platform - Create User Accounts
-- ============================================================================
-- 建立三種不同權限的用戶帳號：一般用戶、管理者、超級管理者
-- 創建日期: 2024-12-28
-- ============================================================================

USE usdt_trading_platform;

-- ============================================================================
-- 1. 建立角色 (如果不存在)
-- ============================================================================

-- 一般用戶角色
INSERT IGNORE INTO roles (name, description, permissions, is_active, created_at, updated_at) 
VALUES (
    'USER',
    '一般用戶 - 基本交易功能',
    JSON_ARRAY(
        'TRADE_VIEW',
        'TRADE_BUY',
        'TRADE_SELL',
        'WALLET_VIEW',
        'WALLET_DEPOSIT',
        'WALLET_WITHDRAW',
        'PROFILE_VIEW',
        'PROFILE_EDIT',
        'ORDER_VIEW',
        'ORDER_CANCEL'
    ),
    1,
    NOW(),
    NOW()
);

-- 管理者角色
INSERT IGNORE INTO roles (name, description, permissions, is_active, created_at, updated_at) 
VALUES (
    'ADMIN',
    '管理者 - 用戶管理和系統監控',
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
    1,
    NOW(),
    NOW()
);

-- 超級管理者角色
INSERT IGNORE INTO roles (name, description, permissions, is_active, created_at, updated_at) 
VALUES (
    'SUPER_ADMIN',
    '超級管理者 - 完整系統控制權限',
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
    1,
    NOW(),
    NOW()
);

-- ============================================================================
-- 2. 建立用戶帳號
-- ============================================================================

-- 一般用戶帳號
-- 密碼: User123! (已加密)
INSERT IGNORE INTO users (
    username, 
    email, 
    phone, 
    password_hash, 
    salt, 
    status, 
    email_verified, 
    phone_verified, 
    role_id, 
    created_at, 
    updated_at
) VALUES (
    'testuser',
    'user@usdttrading.com',
    '+886912345678',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjZjcOvUS/zfUcHbz6JqHllgHQdxYy', -- User123!
    SUBSTRING(MD5(RAND()), 1, 32),
    'ACTIVE',
    1,
    1,
    (SELECT id FROM roles WHERE name = 'USER' LIMIT 1),
    NOW(),
    NOW()
);

-- 管理者帳號
-- 密碼: Admin123! (已加密)
INSERT IGNORE INTO users (
    username, 
    email, 
    phone, 
    password_hash, 
    salt, 
    status, 
    email_verified, 
    phone_verified, 
    role_id, 
    created_at, 
    updated_at
) VALUES (
    'admin',
    'admin@usdttrading.com',
    '+886987654321',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- Admin123!
    SUBSTRING(MD5(RAND()), 1, 32),
    'ACTIVE',
    1,
    1,
    (SELECT id FROM roles WHERE name = 'ADMIN' LIMIT 1),
    NOW(),
    NOW()
);

-- 超級管理者帳號
-- 密碼: SuperAdmin123! (已加密)
INSERT IGNORE INTO users (
    username, 
    email, 
    phone, 
    password_hash, 
    salt, 
    status, 
    email_verified, 
    phone_verified, 
    role_id, 
    created_at, 
    updated_at
) VALUES (
    'superadmin',
    'superadmin@usdttrading.com',
    '+886900000000',
    '$2a$10$E4iABBAM2uDzFUqiJy13UOA1RFUQ4T/Kh9gEHdpx/6uy/6TeRESxi', -- SuperAdmin123!
    SUBSTRING(MD5(RAND()), 1, 32),
    'ACTIVE',
    1,
    1,
    (SELECT id FROM roles WHERE name = 'SUPER_ADMIN' LIMIT 1),
    NOW(),
    NOW()
);

-- ============================================================================
-- 3. 為每個用戶建立基本資料
-- ============================================================================

-- 一般用戶資料
INSERT IGNORE INTO user_profiles (
    user_id,
    first_name,
    last_name,
    birth_date,
    gender,
    country,
    city,
    address,
    postal_code,
    timezone,
    language,
    created_at,
    updated_at
) VALUES (
    (SELECT id FROM users WHERE username = 'testuser' LIMIT 1),
    '測試',
    '用戶',
    '1990-01-01',
    'OTHER',
    'Taiwan',
    'Taipei',
    '台北市信義區信義路五段7號',
    '110',
    'Asia/Taipei',
    'zh-TW',
    NOW(),
    NOW()
);

-- 管理者資料
INSERT IGNORE INTO user_profiles (
    user_id,
    first_name,
    last_name,
    birth_date,
    gender,
    country,
    city,
    address,
    postal_code,
    timezone,
    language,
    created_at,
    updated_at
) VALUES (
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1),
    '系統',
    '管理員',
    '1985-01-01',
    'OTHER',
    'Taiwan',
    'Taipei',
    '台北市中正區重慶南路一段122號',
    '100',
    'Asia/Taipei',
    'zh-TW',
    NOW(),
    NOW()
);

-- 超級管理者資料
INSERT IGNORE INTO user_profiles (
    user_id,
    first_name,
    last_name,
    birth_date,
    gender,
    country,
    city,
    address,
    postal_code,
    timezone,
    language,
    created_at,
    updated_at
) VALUES (
    (SELECT id FROM users WHERE username = 'superadmin' LIMIT 1),
    '超級',
    '管理員',
    '1980-01-01',
    'OTHER',
    'Taiwan',
    'Taipei',
    '台北市松山區南京東路四段2號',
    '105',
    'Asia/Taipei',
    'zh-TW',
    NOW(),
    NOW()
);

-- ============================================================================
-- 4. 為每個用戶建立錢包
-- ============================================================================

-- 一般用戶錢包 (USDT)
INSERT IGNORE INTO wallets (
    user_id,
    currency,
    balance,
    frozen_balance,
    address,
    is_active,
    created_at,
    updated_at
) VALUES (
    (SELECT id FROM users WHERE username = 'testuser' LIMIT 1),
    'USDT',
    1000.00000000,
    0.00000000,
    CONCAT('T', UPPER(SUBSTRING(MD5(CONCAT('testuser', NOW())), 1, 33))),
    1,
    NOW(),
    NOW()
);

-- 管理者錢包 (USDT)
INSERT IGNORE INTO wallets (
    user_id,
    currency,
    balance,
    frozen_balance,
    address,
    is_active,
    created_at,
    updated_at
) VALUES (
    (SELECT id FROM users WHERE username = 'admin' LIMIT 1),
    'USDT',
    5000.00000000,
    0.00000000,
    CONCAT('T', UPPER(SUBSTRING(MD5(CONCAT('admin', NOW())), 1, 33))),
    1,
    NOW(),
    NOW()
);

-- 超級管理者錢包 (USDT)
INSERT IGNORE INTO wallets (
    user_id,
    currency,
    balance,
    frozen_balance,
    address,
    is_active,
    created_at,
    updated_at
) VALUES (
    (SELECT id FROM users WHERE username = 'superadmin' LIMIT 1),
    'USDT',
    10000.00000000,
    0.00000000,
    CONCAT('T', UPPER(SUBSTRING(MD5(CONCAT('superadmin', NOW())), 1, 33))),
    1,
    NOW(),
    NOW()
);

-- ============================================================================
-- 5. 驗證建立結果
-- ============================================================================

-- 顯示建立的角色
SELECT 'Created Roles:' as Info;
SELECT id, name, description, is_active FROM roles WHERE name IN ('USER', 'ADMIN', 'SUPER_ADMIN');

-- 顯示建立的用戶
SELECT 'Created Users:' as Info;
SELECT 
    u.id,
    u.username,
    u.email,
    u.status,
    r.name as role_name,
    u.created_at
FROM users u
LEFT JOIN roles r ON u.role_id = r.id
WHERE u.username IN ('testuser', 'admin', 'superadmin');

-- 顯示用戶錢包
SELECT 'User Wallets:' as Info;
SELECT 
    u.username,
    w.currency,
    w.balance,
    w.address,
    w.is_active
FROM wallets w
JOIN users u ON w.user_id = u.id
WHERE u.username IN ('testuser', 'admin', 'superadmin');

-- ============================================================================
-- 帳號資訊摘要
-- ============================================================================
/*
建立的帳號資訊：

1. 一般用戶帳號
   - 用戶名: testuser
   - 郵箱: user@usdttrading.com
   - 密碼: User123!
   - 角色: USER
   - 初始餘額: 1000 USDT

2. 管理者帳號
   - 用戶名: admin
   - 郵箱: admin@usdttrading.com
   - 密碼: Admin123!
   - 角色: ADMIN
   - 初始餘額: 5000 USDT

3. 超級管理者帳號
   - 用戶名: superadmin
   - 郵箱: superadmin@usdttrading.com
   - 密碼: SuperAdmin123!
   - 角色: SUPER_ADMIN
   - 初始餘額: 10000 USDT

所有帳號狀態均為 ACTIVE，郵箱和手機已驗證。
*/