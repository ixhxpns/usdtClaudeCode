-- USDT交易平台資料庫建表腳本
-- 資料庫版本：MySQL 8.0+
-- 字符集：utf8mb4
-- 排序規則：utf8mb4_unicode_ci
-- 創建時間：2025-08-18

-- 創建數據庫
CREATE DATABASE IF NOT EXISTS usdt_trading_platform 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE usdt_trading_platform;

-- ==========================================
-- 1. 權限角色管理表
-- ==========================================

-- 角色表
CREATE TABLE roles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名稱',
    description VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    permissions JSON DEFAULT NULL COMMENT '權限列表(JSON格式)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否啟用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_name (name),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶角色表';

-- 初始化角色數據
INSERT INTO roles (name, description, permissions) VALUES 
('admin', '超級管理員', '["*"]'),
('manager', '管理員', '["user:read", "user:update", "order:read", "order:update", "withdrawal:review"]'),
('user', '普通用戶', '["profile:read", "profile:update", "order:create", "order:read", "wallet:read"]');

-- ==========================================
-- 2. 用戶管理相關表
-- ==========================================

-- 用戶主表
CREATE TABLE users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '郵箱地址',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手機號碼',
    password_hash VARCHAR(255) NOT NULL COMMENT '密碼哈希',
    salt VARCHAR(32) NOT NULL COMMENT '密碼鹽值',
    google_auth_key VARCHAR(32) DEFAULT NULL COMMENT 'Google驗證器密鑰',
    status ENUM('active', 'inactive', 'frozen', 'deleted') DEFAULT 'inactive' COMMENT '賬戶狀態',
    email_verified BOOLEAN DEFAULT FALSE COMMENT '郵箱是否驗證',
    phone_verified BOOLEAN DEFAULT FALSE COMMENT '手機是否驗證',
    google_auth_enabled BOOLEAN DEFAULT FALSE COMMENT '是否啟用Google驗證',
    role_id BIGINT UNSIGNED NOT NULL DEFAULT 3 COMMENT '角色ID',
    last_login_at TIMESTAMP NULL DEFAULT NULL COMMENT '最後登入時間',
    last_login_ip VARCHAR(45) DEFAULT NULL COMMENT '最後登入IP',
    login_attempts INT DEFAULT 0 COMMENT '登入嘗試次數',
    locked_until TIMESTAMP NULL DEFAULT NULL COMMENT '鎖定到期時間',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    
    FOREIGN KEY (role_id) REFERENCES roles(id),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_status (status),
    INDEX idx_role_id (role_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶主表';

-- 用戶個人資料表
CREATE TABLE user_profiles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL UNIQUE,
    first_name VARCHAR(50) DEFAULT NULL COMMENT '名',
    last_name VARCHAR(50) DEFAULT NULL COMMENT '姓',
    birth_date DATE DEFAULT NULL COMMENT '出生日期',
    gender ENUM('male', 'female', 'other') DEFAULT NULL COMMENT '性別',
    country VARCHAR(50) DEFAULT NULL COMMENT '國家',
    state VARCHAR(50) DEFAULT NULL COMMENT '省/州',
    city VARCHAR(50) DEFAULT NULL COMMENT '城市',
    address TEXT DEFAULT NULL COMMENT '詳細地址',
    postal_code VARCHAR(20) DEFAULT NULL COMMENT '郵政編碼',
    avatar_url VARCHAR(255) DEFAULT NULL COMMENT '頭像URL',
    timezone VARCHAR(50) DEFAULT 'UTC' COMMENT '時區',
    language VARCHAR(10) DEFAULT 'en' COMMENT '語言偏好',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_country (country)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶個人資料表';

-- KYC驗證表
CREATE TABLE user_kyc (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL UNIQUE,
    id_type ENUM('id_card', 'passport', 'driving_license') NOT NULL COMMENT '證件類型',
    id_number VARCHAR(100) NOT NULL COMMENT '證件號碼(加密存儲)',
    id_card_front VARCHAR(255) DEFAULT NULL COMMENT '身份證正面照片URL',
    id_card_back VARCHAR(255) DEFAULT NULL COMMENT '身份證反面照片URL',
    selfie_photo VARCHAR(255) DEFAULT NULL COMMENT '手持身份證自拍照URL',
    second_doc_type VARCHAR(50) DEFAULT NULL COMMENT '第二證件類型',
    second_doc_url VARCHAR(255) DEFAULT NULL COMMENT '第二證件照片URL',
    bank_account VARCHAR(255) DEFAULT NULL COMMENT '銀行賬號(加密存儲)',
    bank_name VARCHAR(100) DEFAULT NULL COMMENT '銀行名稱',
    bank_branch VARCHAR(100) DEFAULT NULL COMMENT '支行名稱',
    account_holder_name VARCHAR(100) DEFAULT NULL COMMENT '賬戶持有人姓名',
    status ENUM('pending', 'processing', 'approved', 'rejected', 'expired') DEFAULT 'pending' COMMENT 'KYC狀態',
    rejection_reason TEXT DEFAULT NULL COMMENT '拒絕原因',
    verified_at TIMESTAMP NULL DEFAULT NULL COMMENT '驗證通過時間',
    expires_at TIMESTAMP NULL DEFAULT NULL COMMENT '驗證到期時間',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_verified_at (verified_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='KYC驗證表';

-- KYC審核記錄表
CREATE TABLE kyc_reviews (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    kyc_id BIGINT UNSIGNED NOT NULL,
    reviewer_id BIGINT UNSIGNED NOT NULL COMMENT '審核人員ID',
    status ENUM('approved', 'rejected', 'requires_resubmit') NOT NULL COMMENT '審核結果',
    review_note TEXT DEFAULT NULL COMMENT '審核備註',
    reviewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '審核時間',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    
    FOREIGN KEY (kyc_id) REFERENCES user_kyc(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewer_id) REFERENCES users(id),
    INDEX idx_kyc_id (kyc_id),
    INDEX idx_reviewer_id (reviewer_id),
    INDEX idx_status (status),
    INDEX idx_reviewed_at (reviewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='KYC審核記錄表';

-- ==========================================
-- 3. 錢包系統相關表
-- ==========================================

-- 錢包表
CREATE TABLE wallets (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    currency ENUM('TRX', 'USDT') NOT NULL COMMENT '幣種',
    balance DECIMAL(20,8) DEFAULT 0.00000000 COMMENT '可用餘額',
    frozen_balance DECIMAL(20,8) DEFAULT 0.00000000 COMMENT '凍結餘額',
    address VARCHAR(100) NOT NULL COMMENT '錢包地址',
    private_key TEXT DEFAULT NULL COMMENT '私鑰(加密存儲)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否啟用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_currency (user_id, currency),
    INDEX idx_user_id (user_id),
    INDEX idx_currency (currency),
    INDEX idx_address (address),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶錢包表';

-- 錢包交易記錄表
CREATE TABLE wallet_transactions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT UNSIGNED NOT NULL,
    transaction_hash VARCHAR(100) DEFAULT NULL COMMENT '區塊鏈交易哈希',
    type ENUM('deposit', 'withdrawal', 'transfer_in', 'transfer_out', 'fee', 'reward') NOT NULL COMMENT '交易類型',
    amount DECIMAL(20,8) NOT NULL COMMENT '交易金額',
    fee DECIMAL(20,8) DEFAULT 0.00000000 COMMENT '手續費',
    balance_before DECIMAL(20,8) NOT NULL COMMENT '交易前餘額',
    balance_after DECIMAL(20,8) NOT NULL COMMENT '交易後餘額',
    status ENUM('pending', 'confirming', 'completed', 'failed', 'cancelled') DEFAULT 'pending' COMMENT '交易狀態',
    block_number BIGINT UNSIGNED DEFAULT NULL COMMENT '區塊號',
    confirmations INT DEFAULT 0 COMMENT '確認數',
    from_address VARCHAR(100) DEFAULT NULL COMMENT '發送地址',
    to_address VARCHAR(100) DEFAULT NULL COMMENT '接收地址',
    memo VARCHAR(255) DEFAULT NULL COMMENT '備註',
    error_message TEXT DEFAULT NULL COMMENT '錯誤信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    
    FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
    INDEX idx_wallet_id (wallet_id),
    INDEX idx_transaction_hash (transaction_hash),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_block_number (block_number),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='錢包交易記錄表';

-- ==========================================
-- 4. 交易系統相關表
-- ==========================================

-- 價格歷史表
CREATE TABLE price_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    currency_pair VARCHAR(20) NOT NULL DEFAULT 'USDT/TWD' COMMENT '交易對',
    price DECIMAL(10,4) NOT NULL COMMENT '價格',
    volume DECIMAL(20,8) DEFAULT 0.00000000 COMMENT '交易量',
    high DECIMAL(10,4) DEFAULT NULL COMMENT '最高價',
    low DECIMAL(10,4) DEFAULT NULL COMMENT '最低價',
    open DECIMAL(10,4) DEFAULT NULL COMMENT '開盤價',
    close DECIMAL(10,4) DEFAULT NULL COMMENT '收盤價',
    source VARCHAR(50) DEFAULT 'system' COMMENT '價格來源',
    interval_type ENUM('1m', '5m', '15m', '30m', '1h', '4h', '1d') DEFAULT '1m' COMMENT '時間間隔',
    timestamp TIMESTAMP NOT NULL COMMENT '價格時間戳',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    
    INDEX idx_currency_pair (currency_pair),
    INDEX idx_timestamp (timestamp),
    INDEX idx_interval_type (interval_type),
    INDEX idx_source (source),
    UNIQUE KEY uk_pair_interval_time (currency_pair, interval_type, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='價格歷史表';

-- 訂單表
CREATE TABLE orders (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '訂單號',
    user_id BIGINT UNSIGNED NOT NULL,
    type ENUM('buy', 'sell') NOT NULL COMMENT '訂單類型',
    currency_pair VARCHAR(20) NOT NULL DEFAULT 'USDT/TWD' COMMENT '交易對',
    amount DECIMAL(20,8) NOT NULL COMMENT 'USDT數量',
    price DECIMAL(10,4) NOT NULL COMMENT '單價',
    total_amount DECIMAL(20,2) NOT NULL COMMENT '總金額(TWD)',
    filled_amount DECIMAL(20,8) DEFAULT 0.00000000 COMMENT '已成交數量',
    status ENUM('pending', 'paid', 'processing', 'completed', 'cancelled', 'expired', 'failed') DEFAULT 'pending' COMMENT '訂單狀態',
    payment_method VARCHAR(50) DEFAULT NULL COMMENT '支付方式',
    payment_info JSON DEFAULT NULL COMMENT '支付信息',
    bank_account VARCHAR(255) DEFAULT NULL COMMENT '收款銀行賬號',
    payment_deadline TIMESTAMP NULL DEFAULT NULL COMMENT '支付截止時間',
    admin_note TEXT DEFAULT NULL COMMENT '管理員備註',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    completed_at TIMESTAMP NULL DEFAULT NULL COMMENT '完成時間',
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_payment_deadline (payment_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易訂單表';

-- 訂單區塊鏈交易記錄表
CREATE TABLE order_transactions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    transaction_hash VARCHAR(100) NOT NULL COMMENT '區塊鏈交易哈希',
    block_number BIGINT UNSIGNED DEFAULT NULL COMMENT '區塊號',
    confirmations INT DEFAULT 0 COMMENT '確認數',
    gas_used DECIMAL(20,8) DEFAULT NULL COMMENT '使用的Gas',
    gas_price DECIMAL(20,8) DEFAULT NULL COMMENT 'Gas價格',
    status ENUM('pending', 'confirmed', 'failed') DEFAULT 'pending' COMMENT '交易狀態',
    error_message TEXT DEFAULT NULL COMMENT '錯誤信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_transaction_hash (transaction_hash),
    INDEX idx_block_number (block_number),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='訂單區塊鏈交易記錄表';

-- ==========================================
-- 5. 提款管理相關表
-- ==========================================

-- 提款申請表
CREATE TABLE withdrawals (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    withdrawal_no VARCHAR(32) NOT NULL UNIQUE COMMENT '提款單號',
    user_id BIGINT UNSIGNED NOT NULL,
    wallet_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(20,8) NOT NULL COMMENT '提款金額',
    fee DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '手續費',
    actual_amount DECIMAL(20,8) NOT NULL COMMENT '實際到賬金額',
    to_address VARCHAR(100) NOT NULL COMMENT '提款地址',
    status ENUM('pending', 'reviewing', 'approved', 'processing', 'completed', 'rejected', 'cancelled') DEFAULT 'pending' COMMENT '提款狀態',
    review_level ENUM('auto', 'manual', 'senior') DEFAULT 'auto' COMMENT '審核級別',
    reviewer_id BIGINT UNSIGNED DEFAULT NULL COMMENT '審核人員ID',
    review_note TEXT DEFAULT NULL COMMENT '審核備註',
    rejection_reason TEXT DEFAULT NULL COMMENT '拒絕原因',
    transaction_hash VARCHAR(100) DEFAULT NULL COMMENT '區塊鏈交易哈希',
    block_number BIGINT UNSIGNED DEFAULT NULL COMMENT '區塊號',
    risk_score INT DEFAULT 0 COMMENT '風險評分(0-100)',
    ip_address VARCHAR(45) DEFAULT NULL COMMENT '申請IP地址',
    user_agent TEXT DEFAULT NULL COMMENT '用戶代理',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    reviewed_at TIMESTAMP NULL DEFAULT NULL COMMENT '審核時間',
    processed_at TIMESTAMP NULL DEFAULT NULL COMMENT '處理時間',
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (wallet_id) REFERENCES wallets(id),
    FOREIGN KEY (reviewer_id) REFERENCES users(id),
    INDEX idx_user_id (user_id),
    INDEX idx_wallet_id (wallet_id),
    INDEX idx_withdrawal_no (withdrawal_no),
    INDEX idx_status (status),
    INDEX idx_review_level (review_level),
    INDEX idx_reviewer_id (reviewer_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提款申請表';

-- ==========================================
-- 6. 系統管理相關表
-- ==========================================

-- 系統配置表
CREATE TABLE system_config (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置鍵',
    config_value TEXT DEFAULT NULL COMMENT '配置值',
    data_type ENUM('string', 'number', 'boolean', 'json') DEFAULT 'string' COMMENT '數據類型',
    category VARCHAR(50) DEFAULT 'general' COMMENT '配置分類',
    description VARCHAR(255) DEFAULT NULL COMMENT '配置描述',
    is_public BOOLEAN DEFAULT FALSE COMMENT '是否為公開配置(前端可見)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否啟用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    
    INDEX idx_config_key (config_key),
    INDEX idx_category (category),
    INDEX idx_is_public (is_public),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系統配置表';

-- 初始化系統配置
INSERT INTO system_config (config_key, config_value, data_type, category, description, is_public) VALUES
('usdt_buy_price', '31.50', 'number', 'trading', 'USDT買入價格', true),
('usdt_sell_price', '31.30', 'number', 'trading', 'USDT賣出價格', true),
('min_trade_amount', '100.00', 'number', 'trading', '最小交易金額(USDT)', true),
('max_trade_amount', '50000.00', 'number', 'trading', '最大交易金額(USDT)', true),
('withdrawal_fee', '5.00', 'number', 'trading', 'USDT提款手續費', true),
('withdrawal_min_amount', '50.00', 'number', 'trading', '最小提款金額(USDT)', true),
('withdrawal_daily_limit', '10000.00', 'number', 'trading', '每日提款限額(USDT)', false),
('auto_approval_limit', '1000.00', 'number', 'system', '自動審核限額(USDT)', false),
('kyc_required_limit', '1000.00', 'number', 'system', 'KYC必須限額(USDT)', false),
('maintenance_mode', 'false', 'boolean', 'system', '維護模式', false),
('registration_enabled', 'true', 'boolean', 'system', '是否允許註冊', true);

-- 系統公告表
CREATE TABLE announcements (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '公告標題',
    content TEXT NOT NULL COMMENT '公告內容',
    type ENUM('info', 'warning', 'error', 'maintenance', 'update') DEFAULT 'info' COMMENT '公告類型',
    priority ENUM('low', 'normal', 'high', 'urgent') DEFAULT 'normal' COMMENT '優先級',
    target_audience ENUM('all', 'users', 'vip', 'admins') DEFAULT 'all' COMMENT '目標受眾',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否啟用',
    is_popup BOOLEAN DEFAULT FALSE COMMENT '是否彈窗顯示',
    publish_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '發布時間',
    expire_at TIMESTAMP NULL DEFAULT NULL COMMENT '到期時間',
    created_by BIGINT UNSIGNED NOT NULL COMMENT '創建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_type (type),
    INDEX idx_priority (priority),
    INDEX idx_target_audience (target_audience),
    INDEX idx_is_active (is_active),
    INDEX idx_publish_at (publish_at),
    INDEX idx_expire_at (expire_at),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系統公告表';

-- 通知表
CREATE TABLE notifications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '用戶ID(NULL表示系統通知)',
    type ENUM('email', 'sms', 'push', 'system') NOT NULL COMMENT '通知類型',
    category ENUM('order', 'payment', 'kyc', 'security', 'system', 'promotion') NOT NULL COMMENT '通知分類',
    title VARCHAR(200) NOT NULL COMMENT '通知標題',
    content TEXT NOT NULL COMMENT '通知內容',
    data JSON DEFAULT NULL COMMENT '額外數據',
    status ENUM('pending', 'sent', 'delivered', 'read', 'failed') DEFAULT 'pending' COMMENT '通知狀態',
    priority ENUM('low', 'normal', 'high', 'urgent') DEFAULT 'normal' COMMENT '優先級',
    send_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '發送時間',
    delivered_at TIMESTAMP NULL DEFAULT NULL COMMENT '送達時間',
    read_at TIMESTAMP NULL DEFAULT NULL COMMENT '閱讀時間',
    error_message TEXT DEFAULT NULL COMMENT '錯誤信息',
    retry_count INT DEFAULT 0 COMMENT '重試次數',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_send_at (send_at),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- 操作日誌表
CREATE TABLE audit_logs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作用戶ID',
    action VARCHAR(100) NOT NULL COMMENT '操作動作',
    resource VARCHAR(100) NOT NULL COMMENT '操作資源',
    resource_id VARCHAR(100) DEFAULT NULL COMMENT '資源ID',
    description TEXT DEFAULT NULL COMMENT '操作描述',
    old_values JSON DEFAULT NULL COMMENT '操作前數據',
    new_values JSON DEFAULT NULL COMMENT '操作後數據',
    ip_address VARCHAR(45) DEFAULT NULL COMMENT 'IP地址',
    user_agent TEXT DEFAULT NULL COMMENT '用戶代理',
    request_id VARCHAR(100) DEFAULT NULL COMMENT '請求ID',
    session_id VARCHAR(100) DEFAULT NULL COMMENT '會話ID',
    result ENUM('success', 'failure', 'error') DEFAULT 'success' COMMENT '操作結果',
    error_message TEXT DEFAULT NULL COMMENT '錯誤信息',
    execution_time INT DEFAULT NULL COMMENT '執行時間(毫秒)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_resource (resource),
    INDEX idx_resource_id (resource_id),
    INDEX idx_result (result),
    INDEX idx_ip_address (ip_address),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日誌表';

-- ==========================================
-- 7. 會話和安全相關表
-- ==========================================

-- 用戶會話表
CREATE TABLE user_sessions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    session_id VARCHAR(128) NOT NULL UNIQUE COMMENT '會話ID',
    token VARCHAR(500) NOT NULL COMMENT 'JWT Token',
    refresh_token VARCHAR(128) DEFAULT NULL COMMENT '刷新令牌',
    device_type ENUM('web', 'mobile', 'tablet', 'desktop') DEFAULT 'web' COMMENT '設備類型',
    device_info JSON DEFAULT NULL COMMENT '設備信息',
    ip_address VARCHAR(45) NOT NULL COMMENT 'IP地址',
    user_agent TEXT DEFAULT NULL COMMENT '用戶代理',
    location VARCHAR(100) DEFAULT NULL COMMENT '登錄位置',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否活躍',
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '最後活動時間',
    expires_at TIMESTAMP NOT NULL COMMENT '過期時間',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_token (token(100)),
    INDEX idx_is_active (is_active),
    INDEX idx_expires_at (expires_at),
    INDEX idx_last_activity (last_activity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶會話表';

-- 安全事件表
CREATE TABLE security_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '相關用戶ID',
    event_type ENUM('login_success', 'login_failed', 'password_change', 'email_change', 'phone_change', 
                   'kyc_submit', 'large_withdrawal', 'suspicious_activity', 'account_locked', 
                   'account_unlocked', 'device_change') NOT NULL COMMENT '事件類型',
    severity ENUM('low', 'medium', 'high', 'critical') DEFAULT 'low' COMMENT '嚴重程度',
    description TEXT NOT NULL COMMENT '事件描述',
    metadata JSON DEFAULT NULL COMMENT '事件元數據',
    ip_address VARCHAR(45) DEFAULT NULL COMMENT 'IP地址',
    user_agent TEXT DEFAULT NULL COMMENT '用戶代理',
    is_resolved BOOLEAN DEFAULT FALSE COMMENT '是否已處理',
    resolved_by BIGINT UNSIGNED DEFAULT NULL COMMENT '處理人員ID',
    resolved_at TIMESTAMP NULL DEFAULT NULL COMMENT '處理時間',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (resolved_by) REFERENCES users(id),
    INDEX idx_user_id (user_id),
    INDEX idx_event_type (event_type),
    INDEX idx_severity (severity),
    INDEX idx_is_resolved (is_resolved),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全事件表';

-- ==========================================
-- 8. 平台錢包池管理表
-- ==========================================

-- 平台錢包池表
CREATE TABLE platform_wallets (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '錢包名稱',
    currency ENUM('TRX', 'USDT') NOT NULL COMMENT '幣種',
    address VARCHAR(100) NOT NULL UNIQUE COMMENT '錢包地址',
    private_key TEXT NOT NULL COMMENT '私鑰(加密存儲)',
    balance DECIMAL(20,8) DEFAULT 0.00000000 COMMENT '當前餘額',
    reserved_balance DECIMAL(20,8) DEFAULT 0.00000000 COMMENT '預留餘額',
    wallet_type ENUM('hot', 'cold', 'fee') DEFAULT 'hot' COMMENT '錢包類型',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否啟用',
    auto_collect BOOLEAN DEFAULT FALSE COMMENT '是否自動收集',
    collect_threshold DECIMAL(20,8) DEFAULT NULL COMMENT '收集閾值',
    description TEXT DEFAULT NULL COMMENT '錢包描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    
    INDEX idx_currency (currency),
    INDEX idx_address (address),
    INDEX idx_wallet_type (wallet_type),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台錢包池表';

-- ==========================================
-- 9. 創建視圖和存儲過程
-- ==========================================

-- 用戶綜合信息視圖
CREATE VIEW user_summary_view AS
SELECT 
    u.id,
    u.email,
    u.phone,
    u.status,
    u.email_verified,
    u.phone_verified,
    u.google_auth_enabled,
    r.name as role_name,
    p.first_name,
    p.last_name,
    p.country,
    kyc.status as kyc_status,
    kyc.verified_at as kyc_verified_at,
    w_trx.balance as trx_balance,
    w_usdt.balance as usdt_balance,
    u.created_at,
    u.last_login_at
FROM users u
LEFT JOIN roles r ON u.role_id = r.id
LEFT JOIN user_profiles p ON u.id = p.user_id
LEFT JOIN user_kyc kyc ON u.id = kyc.user_id
LEFT JOIN wallets w_trx ON u.id = w_trx.user_id AND w_trx.currency = 'TRX'
LEFT JOIN wallets w_usdt ON u.id = w_usdt.user_id AND w_usdt.currency = 'USDT';

-- 交易統計視圖
CREATE VIEW trading_stats_view AS
SELECT 
    DATE(created_at) as trade_date,
    type as trade_type,
    COUNT(*) as order_count,
    SUM(amount) as total_amount,
    SUM(total_amount) as total_value,
    AVG(price) as avg_price,
    COUNT(CASE WHEN status = 'completed' THEN 1 END) as completed_orders,
    COUNT(CASE WHEN status = 'cancelled' THEN 1 END) as cancelled_orders
FROM orders
WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY DATE(created_at), type;

-- ==========================================
-- 10. 創建必要的觸發器
-- ==========================================

-- 錢包餘額更新觸發器
DELIMITER //

CREATE TRIGGER update_wallet_balance_after_transaction
AFTER INSERT ON wallet_transactions
FOR EACH ROW
BEGIN
    UPDATE wallets 
    SET balance = NEW.balance_after,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.wallet_id;
END//

-- 用戶登錄時更新最後登錄信息
CREATE TRIGGER update_user_last_login
AFTER INSERT ON user_sessions
FOR EACH ROW
BEGIN
    UPDATE users 
    SET last_login_at = NEW.created_at,
        last_login_ip = NEW.ip_address,
        login_attempts = 0
    WHERE id = NEW.user_id;
END//

-- KYC狀態變更時創建安全事件
CREATE TRIGGER create_security_event_kyc_change
AFTER UPDATE ON user_kyc
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO security_events (user_id, event_type, severity, description, metadata)
        VALUES (NEW.user_id, 'kyc_submit', 'medium', 
                CONCAT('KYC status changed from ', OLD.status, ' to ', NEW.status),
                JSON_OBJECT('old_status', OLD.status, 'new_status', NEW.status));
    END IF;
END//

DELIMITER ;

-- ==========================================
-- 11. 創建索引優化查詢
-- ==========================================

-- 複合索引優化常用查詢
CREATE INDEX idx_orders_user_status_created ON orders(user_id, status, created_at);
CREATE INDEX idx_wallet_transactions_wallet_type_created ON wallet_transactions(wallet_id, type, created_at);
CREATE INDEX idx_withdrawals_user_status_created ON withdrawals(user_id, status, created_at);
CREATE INDEX idx_notifications_user_status_created ON notifications(user_id, status, created_at);
CREATE INDEX idx_audit_logs_user_action_created ON audit_logs(user_id, action, created_at);

-- 全文索引用於搜索
ALTER TABLE announcements ADD FULLTEXT(title, content);
ALTER TABLE audit_logs ADD FULLTEXT(description);

-- ==========================================
-- 完成資料庫初始化
-- ==========================================

-- 設置默認字符集和時區
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- 啟用事件調度器（用於定時任務）
SET GLOBAL event_scheduler = ON;