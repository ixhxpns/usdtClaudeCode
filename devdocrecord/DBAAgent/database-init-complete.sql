-- ==========================================
-- USDT交易平台完整數據庫初始化腳本
-- ==========================================
-- 版本: 2.0
-- 創建日期: 2025-08-23
-- 更新說明: 修復實體類與數據庫表的兼容性問題
-- 執行環境: MySQL 8.0+
-- 字符集: utf8mb4
-- ==========================================

-- 設置會話參數
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO';

-- 創建數據庫
CREATE DATABASE IF NOT EXISTS usdt_trading_platform 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE usdt_trading_platform;

-- ==========================================
-- 1. 角色權限管理表
-- ==========================================

-- 角色表
DROP TABLE IF EXISTS roles;
CREATE TABLE roles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名稱',
    description VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    permissions JSON DEFAULT NULL COMMENT '權限列表(JSON格式)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否啟用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    INDEX idx_name (name),
    INDEX idx_is_active (is_active),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶角色表';

-- ==========================================
-- 2. 用戶管理相關表
-- ==========================================

-- 用戶主表
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) DEFAULT NULL COMMENT '用戶名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '郵箱地址',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手機號碼',
    password_hash VARCHAR(255) NOT NULL COMMENT '密碼哈希',
    salt VARCHAR(32) NOT NULL COMMENT '密碼鹽值',
    google_auth_key VARCHAR(32) DEFAULT NULL COMMENT 'Google驗證器密鑰',
    status ENUM('ACTIVE', 'INACTIVE', 'FROZEN', 'DELETED') DEFAULT 'INACTIVE' COMMENT '賬戶狀態',
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
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (role_id) REFERENCES roles(id),
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_status (status),
    INDEX idx_role_id (role_id),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶主表';

-- 用戶個人資料表
DROP TABLE IF EXISTS user_profiles;
CREATE TABLE user_profiles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL UNIQUE,
    first_name VARCHAR(50) DEFAULT NULL COMMENT '名',
    last_name VARCHAR(50) DEFAULT NULL COMMENT '姓',
    birth_date DATE DEFAULT NULL COMMENT '出生日期',
    gender ENUM('MALE', 'FEMALE', 'OTHER') DEFAULT NULL COMMENT '性別',
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
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_country (country),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶個人資料表';

-- KYC驗證表
DROP TABLE IF EXISTS user_kyc;
CREATE TABLE user_kyc (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL UNIQUE,
    id_type ENUM('ID_CARD', 'PASSPORT', 'DRIVING_LICENSE') NOT NULL COMMENT '證件類型',
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
    status ENUM('PENDING', 'PROCESSING', 'APPROVED', 'REJECTED', 'EXPIRED') DEFAULT 'PENDING' COMMENT 'KYC狀態',
    rejection_reason TEXT DEFAULT NULL COMMENT '拒絕原因',
    verified_at TIMESTAMP NULL DEFAULT NULL COMMENT '驗證通過時間',
    expires_at TIMESTAMP NULL DEFAULT NULL COMMENT '驗證到期時間',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_verified_at (verified_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='KYC驗證表';

-- ==========================================
-- 3. 錢包系統相關表
-- ==========================================

-- 錢包表
DROP TABLE IF EXISTS wallets;
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
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_currency (user_id, currency),
    INDEX idx_user_id (user_id),
    INDEX idx_currency (currency),
    INDEX idx_address (address),
    INDEX idx_is_active (is_active),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶錢包表';

-- 錢包交易記錄表
DROP TABLE IF EXISTS wallet_transactions;
CREATE TABLE wallet_transactions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT UNSIGNED NOT NULL,
    transaction_hash VARCHAR(100) DEFAULT NULL COMMENT '區塊鏈交易哈希',
    type ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT', 'FEE', 'REWARD') NOT NULL COMMENT '交易類型',
    amount DECIMAL(20,8) NOT NULL COMMENT '交易金額',
    fee DECIMAL(20,8) DEFAULT 0.00000000 COMMENT '手續費',
    balance_before DECIMAL(20,8) NOT NULL COMMENT '交易前餘額',
    balance_after DECIMAL(20,8) NOT NULL COMMENT '交易後餘額',
    status ENUM('PENDING', 'CONFIRMING', 'COMPLETED', 'FAILED', 'CANCELLED') DEFAULT 'PENDING' COMMENT '交易狀態',
    block_number BIGINT UNSIGNED DEFAULT NULL COMMENT '區塊號',
    confirmations INT DEFAULT 0 COMMENT '確認數',
    from_address VARCHAR(100) DEFAULT NULL COMMENT '發送地址',
    to_address VARCHAR(100) DEFAULT NULL COMMENT '接收地址',
    memo VARCHAR(255) DEFAULT NULL COMMENT '備註',
    error_message TEXT DEFAULT NULL COMMENT '錯誤信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
    INDEX idx_wallet_id (wallet_id),
    INDEX idx_transaction_hash (transaction_hash),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_block_number (block_number),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='錢包交易記錄表';

-- ==========================================
-- 4. 交易系統相關表
-- ==========================================

-- 價格歷史表
DROP TABLE IF EXISTS price_history;
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
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    INDEX idx_currency_pair (currency_pair),
    INDEX idx_timestamp (timestamp),
    INDEX idx_interval_type (interval_type),
    INDEX idx_source (source),
    INDEX idx_deleted (deleted),
    UNIQUE KEY uk_pair_interval_time (currency_pair, interval_type, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='價格歷史表';

-- 訂單表
DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '訂單號',
    user_id BIGINT UNSIGNED NOT NULL,
    order_type ENUM('BUY', 'SELL') NOT NULL COMMENT '訂單類型',
    currency_pair VARCHAR(20) NOT NULL DEFAULT 'USDT/TWD' COMMENT '交易對',
    amount DECIMAL(20,8) NOT NULL COMMENT 'USDT數量',
    price DECIMAL(10,4) NOT NULL COMMENT '單價',
    total_amount DECIMAL(20,2) NOT NULL COMMENT '總金額(TWD)',
    filled_amount DECIMAL(20,8) DEFAULT 0.00000000 COMMENT '已成交數量',
    order_status ENUM('PENDING', 'PAID', 'PROCESSING', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'FAILED') DEFAULT 'PENDING' COMMENT '訂單狀態',
    payment_method VARCHAR(50) DEFAULT NULL COMMENT '支付方式',
    payment_info JSON DEFAULT NULL COMMENT '支付信息',
    bank_account VARCHAR(255) DEFAULT NULL COMMENT '收款銀行賬號',
    payment_deadline TIMESTAMP NULL DEFAULT NULL COMMENT '支付截止時間',
    admin_note TEXT DEFAULT NULL COMMENT '管理員備註',
    completed_at TIMESTAMP NULL DEFAULT NULL COMMENT '完成時間',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_order_type (order_type),
    INDEX idx_order_status (order_status),
    INDEX idx_created_at (created_at),
    INDEX idx_payment_deadline (payment_deadline),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易訂單表';

-- 訂單區塊鏈交易記錄表
DROP TABLE IF EXISTS order_transactions;
CREATE TABLE order_transactions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    transaction_hash VARCHAR(100) NOT NULL COMMENT '區塊鏈交易哈希',
    block_number BIGINT UNSIGNED DEFAULT NULL COMMENT '區塊號',
    confirmations INT DEFAULT 0 COMMENT '確認數',
    gas_used DECIMAL(20,8) DEFAULT NULL COMMENT '使用的Gas',
    gas_price DECIMAL(20,8) DEFAULT NULL COMMENT 'Gas價格',
    status ENUM('PENDING', 'CONFIRMED', 'FAILED') DEFAULT 'PENDING' COMMENT '交易狀態',
    error_message TEXT DEFAULT NULL COMMENT '錯誤信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_transaction_hash (transaction_hash),
    INDEX idx_block_number (block_number),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='訂單區塊鏈交易記錄表';

-- ==========================================
-- 5. 提款管理相關表
-- ==========================================

-- 提款申請表
DROP TABLE IF EXISTS withdrawals;
CREATE TABLE withdrawals (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    withdrawal_no VARCHAR(32) NOT NULL UNIQUE COMMENT '提款單號',
    user_id BIGINT UNSIGNED NOT NULL,
    wallet_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(20,8) NOT NULL COMMENT '提款金額',
    fee DECIMAL(20,8) NOT NULL DEFAULT 0.00000000 COMMENT '手續費',
    actual_amount DECIMAL(20,8) NOT NULL COMMENT '實際到賬金額',
    to_address VARCHAR(100) NOT NULL COMMENT '提款地址',
    status ENUM('PENDING', 'REVIEWING', 'APPROVED', 'PROCESSING', 'COMPLETED', 'REJECTED', 'CANCELLED') DEFAULT 'PENDING' COMMENT '提款狀態',
    review_level ENUM('AUTO', 'MANUAL', 'SENIOR') DEFAULT 'AUTO' COMMENT '審核級別',
    reviewer_id BIGINT UNSIGNED DEFAULT NULL COMMENT '審核人員ID',
    review_note TEXT DEFAULT NULL COMMENT '審核備註',
    rejection_reason TEXT DEFAULT NULL COMMENT '拒絕原因',
    transaction_hash VARCHAR(100) DEFAULT NULL COMMENT '區塊鏈交易哈希',
    block_number BIGINT UNSIGNED DEFAULT NULL COMMENT '區塊號',
    risk_score INT DEFAULT 0 COMMENT '風險評分(0-100)',
    ip_address VARCHAR(45) DEFAULT NULL COMMENT '申請IP地址',
    user_agent TEXT DEFAULT NULL COMMENT '用戶代理',
    reviewed_at TIMESTAMP NULL DEFAULT NULL COMMENT '審核時間',
    processed_at TIMESTAMP NULL DEFAULT NULL COMMENT '處理時間',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (wallet_id) REFERENCES wallets(id),
    FOREIGN KEY (reviewer_id) REFERENCES users(id),
    INDEX idx_user_id (user_id),
    INDEX idx_wallet_id (wallet_id),
    INDEX idx_withdrawal_no (withdrawal_no),
    INDEX idx_status (status),
    INDEX idx_review_level (review_level),
    INDEX idx_reviewer_id (reviewer_id),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提款申請表';

-- ==========================================
-- 6. 系統管理相關表
-- ==========================================

-- 系統配置表
DROP TABLE IF EXISTS system_config;
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
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    INDEX idx_config_key (config_key),
    INDEX idx_category (category),
    INDEX idx_is_public (is_public),
    INDEX idx_is_active (is_active),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系統配置表';

-- 系統公告表
DROP TABLE IF EXISTS announcements;
CREATE TABLE announcements (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '公告標題',
    content TEXT NOT NULL COMMENT '公告內容',
    type ENUM('INFO', 'WARNING', 'ERROR', 'MAINTENANCE', 'UPDATE') DEFAULT 'INFO' COMMENT '公告類型',
    priority ENUM('LOW', 'NORMAL', 'HIGH', 'URGENT') DEFAULT 'NORMAL' COMMENT '優先級',
    target_audience ENUM('ALL', 'USERS', 'VIP', 'ADMINS') DEFAULT 'ALL' COMMENT '目標受眾',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否啟用',
    is_popup BOOLEAN DEFAULT FALSE COMMENT '是否彈窗顯示',
    publish_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '發布時間',
    expire_at TIMESTAMP NULL DEFAULT NULL COMMENT '到期時間',
    created_by BIGINT UNSIGNED NOT NULL COMMENT '創建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_type (type),
    INDEX idx_priority (priority),
    INDEX idx_target_audience (target_audience),
    INDEX idx_is_active (is_active),
    INDEX idx_publish_at (publish_at),
    INDEX idx_expire_at (expire_at),
    INDEX idx_created_by (created_by),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系統公告表';

-- 通知表
DROP TABLE IF EXISTS notifications;
CREATE TABLE notifications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '用戶ID(NULL表示系統通知)',
    type ENUM('EMAIL', 'SMS', 'PUSH', 'SYSTEM') NOT NULL COMMENT '通知類型',
    category ENUM('ORDER', 'PAYMENT', 'KYC', 'SECURITY', 'SYSTEM', 'PROMOTION') NOT NULL COMMENT '通知分類',
    title VARCHAR(200) NOT NULL COMMENT '通知標題',
    content TEXT NOT NULL COMMENT '通知內容',
    data JSON DEFAULT NULL COMMENT '額外數據',
    status ENUM('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED') DEFAULT 'PENDING' COMMENT '通知狀態',
    priority ENUM('LOW', 'NORMAL', 'HIGH', 'URGENT') DEFAULT 'NORMAL' COMMENT '優先級',
    send_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '發送時間',
    delivered_at TIMESTAMP NULL DEFAULT NULL COMMENT '送達時間',
    read_at TIMESTAMP NULL DEFAULT NULL COMMENT '閱讀時間',
    error_message TEXT DEFAULT NULL COMMENT '錯誤信息',
    retry_count INT DEFAULT 0 COMMENT '重試次數',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_send_at (send_at),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- 操作日誌表
DROP TABLE IF EXISTS audit_logs;
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
    result ENUM('SUCCESS', 'FAILURE', 'ERROR') DEFAULT 'SUCCESS' COMMENT '操作結果',
    error_message TEXT DEFAULT NULL COMMENT '錯誤信息',
    execution_time INT DEFAULT NULL COMMENT '執行時間(毫秒)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_resource (resource),
    INDEX idx_resource_id (resource_id),
    INDEX idx_result (result),
    INDEX idx_ip_address (ip_address),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日誌表';

-- ==========================================
-- 7. 會話和安全相關表
-- ==========================================

-- 用戶會話表
DROP TABLE IF EXISTS user_sessions;
CREATE TABLE user_sessions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    session_id VARCHAR(128) NOT NULL UNIQUE COMMENT '會話ID',
    token VARCHAR(500) NOT NULL COMMENT 'JWT Token',
    refresh_token VARCHAR(128) DEFAULT NULL COMMENT '刷新令牌',
    device_type ENUM('WEB', 'MOBILE', 'TABLET', 'DESKTOP') DEFAULT 'WEB' COMMENT '設備類型',
    device_info JSON DEFAULT NULL COMMENT '設備信息',
    ip_address VARCHAR(45) NOT NULL COMMENT 'IP地址',
    user_agent TEXT DEFAULT NULL COMMENT '用戶代理',
    location VARCHAR(100) DEFAULT NULL COMMENT '登錄位置',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否活躍',
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '最後活動時間',
    expires_at TIMESTAMP NOT NULL COMMENT '過期時間',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_token (token(100)),
    INDEX idx_is_active (is_active),
    INDEX idx_expires_at (expires_at),
    INDEX idx_last_activity (last_activity),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶會話表';

-- 安全事件表
DROP TABLE IF EXISTS security_events;
CREATE TABLE security_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '相關用戶ID',
    event_type ENUM('LOGIN_SUCCESS', 'LOGIN_FAILED', 'PASSWORD_CHANGE', 'EMAIL_CHANGE', 'PHONE_CHANGE', 
                   'KYC_SUBMIT', 'LARGE_WITHDRAWAL', 'SUSPICIOUS_ACTIVITY', 'ACCOUNT_LOCKED', 
                   'ACCOUNT_UNLOCKED', 'DEVICE_CHANGE') NOT NULL COMMENT '事件類型',
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'LOW' COMMENT '嚴重程度',
    description TEXT NOT NULL COMMENT '事件描述',
    metadata JSON DEFAULT NULL COMMENT '事件元數據',
    ip_address VARCHAR(45) DEFAULT NULL COMMENT 'IP地址',
    user_agent TEXT DEFAULT NULL COMMENT '用戶代理',
    is_resolved BOOLEAN DEFAULT FALSE COMMENT '是否已處理',
    resolved_by BIGINT UNSIGNED DEFAULT NULL COMMENT '處理人員ID',
    resolved_at TIMESTAMP NULL DEFAULT NULL COMMENT '處理時間',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (resolved_by) REFERENCES users(id),
    INDEX idx_user_id (user_id),
    INDEX idx_event_type (event_type),
    INDEX idx_severity (severity),
    INDEX idx_is_resolved (is_resolved),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全事件表';

-- ==========================================
-- 8. 平台錢包池管理表
-- ==========================================

-- 平台錢包池表
DROP TABLE IF EXISTS platform_wallets;
CREATE TABLE platform_wallets (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '錢包名稱',
    currency ENUM('TRX', 'USDT') NOT NULL COMMENT '幣種',
    address VARCHAR(100) NOT NULL UNIQUE COMMENT '錢包地址',
    private_key TEXT NOT NULL COMMENT '私鑰(加密存儲)',
    balance DECIMAL(20,8) DEFAULT 0.00000000 COMMENT '當前餘額',
    reserved_balance DECIMAL(20,8) DEFAULT 0.00000000 COMMENT '預留餘額',
    wallet_type ENUM('HOT', 'COLD', 'FEE') DEFAULT 'HOT' COMMENT '錢包類型',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否啟用',
    auto_collect BOOLEAN DEFAULT FALSE COMMENT '是否自動收集',
    collect_threshold DECIMAL(20,8) DEFAULT NULL COMMENT '收集閾值',
    description TEXT DEFAULT NULL COMMENT '錢包描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    INDEX idx_currency (currency),
    INDEX idx_address (address),
    INDEX idx_wallet_type (wallet_type),
    INDEX idx_is_active (is_active),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台錢包池表';

-- ==========================================
-- 9. 額外必要表
-- ==========================================

-- KYC審核記錄表
DROP TABLE IF EXISTS kyc_reviews;
CREATE TABLE kyc_reviews (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    kyc_id BIGINT UNSIGNED NOT NULL,
    reviewer_id BIGINT UNSIGNED NOT NULL COMMENT '審核人員ID',
    status ENUM('APPROVED', 'REJECTED', 'REQUIRES_RESUBMIT') NOT NULL COMMENT '審核結果',
    review_note TEXT DEFAULT NULL COMMENT '審核備註',
    reviewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '審核時間',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (kyc_id) REFERENCES user_kyc(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewer_id) REFERENCES users(id),
    INDEX idx_kyc_id (kyc_id),
    INDEX idx_reviewer_id (reviewer_id),
    INDEX idx_status (status),
    INDEX idx_reviewed_at (reviewed_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='KYC審核記錄表';

-- KYC文檔表
DROP TABLE IF EXISTS kyc_documents;
CREATE TABLE kyc_documents (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    document_type ENUM('ID_FRONT', 'ID_BACK', 'SELFIE', 'UTILITY_BILL', 'BANK_STATEMENT', 'OTHER') NOT NULL COMMENT '文檔類型',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名稱',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路徑',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小(bytes)',
    mime_type VARCHAR(100) DEFAULT NULL COMMENT 'MIME類型',
    upload_status ENUM('PENDING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING' COMMENT '上傳狀態',
    verification_status ENUM('PENDING', 'VERIFIED', 'REJECTED') DEFAULT 'PENDING' COMMENT '驗證狀態',
    rejection_reason TEXT DEFAULT NULL COMMENT '拒絕原因',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記',
    version INT DEFAULT 1 COMMENT '樂觀鎖版本號',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_document_type (document_type),
    INDEX idx_upload_status (upload_status),
    INDEX idx_verification_status (verification_status),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='KYC文檔表';

-- ==========================================
-- 10. 初始化基礎數據
-- ==========================================

-- 初始化角色數據
INSERT INTO roles (id, name, description, permissions, is_active) VALUES 
(1, 'admin', '超級管理員', JSON_ARRAY('*'), TRUE),
(2, 'manager', '管理員', JSON_ARRAY('user:read', 'user:update', 'order:read', 'order:update', 'withdrawal:review'), TRUE),
(3, 'user', '普通用戶', JSON_ARRAY('profile:read', 'profile:update', 'order:create', 'order:read', 'wallet:read'), TRUE);

-- 初始化系統配置
INSERT INTO system_config (config_key, config_value, data_type, category, description, is_public) VALUES
('usdt_buy_price', '31.50', 'number', 'trading', 'USDT買入價格', TRUE),
('usdt_sell_price', '31.30', 'number', 'trading', 'USDT賣出價格', TRUE),
('min_trade_amount', '100.00', 'number', 'trading', '最小交易金額(USDT)', TRUE),
('max_trade_amount', '50000.00', 'number', 'trading', '最大交易金額(USDT)', TRUE),
('withdrawal_fee', '5.00', 'number', 'trading', 'USDT提款手續費', TRUE),
('withdrawal_min_amount', '50.00', 'number', 'trading', '最小提款金額(USDT)', TRUE),
('withdrawal_daily_limit', '10000.00', 'number', 'trading', '每日提款限額(USDT)', FALSE),
('auto_approval_limit', '1000.00', 'number', 'system', '自動審核限額(USDT)', FALSE),
('kyc_required_limit', '1000.00', 'number', 'system', 'KYC必須限額(USDT)', FALSE),
('maintenance_mode', 'false', 'boolean', 'system', '維護模式', FALSE),
('registration_enabled', 'true', 'boolean', 'system', '是否允許註冊', TRUE);

-- 創建管理員用戶 (密碼: admin123)
INSERT INTO users (id, username, email, password_hash, salt, status, email_verified, role_id) VALUES 
(1, 'admin', 'admin@usdttrading.com', 'hashed_password_here', 'salt_here', 'ACTIVE', TRUE, 1);

-- 創建用戶個人資料
INSERT INTO user_profiles (user_id, first_name, last_name, country, language) VALUES 
(1, 'System', 'Administrator', 'Taiwan', 'zh-TW');

-- ==========================================
-- 11. 創建視圖
-- ==========================================

-- 用戶綜合信息視圖
CREATE OR REPLACE VIEW user_summary_view AS
SELECT 
    u.id,
    u.username,
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
LEFT JOIN wallets w_usdt ON u.id = w_usdt.user_id AND w_usdt.currency = 'USDT'
WHERE u.deleted = 0;

-- 交易統計視圖
CREATE OR REPLACE VIEW trading_stats_view AS
SELECT 
    DATE(created_at) as trade_date,
    order_type as trade_type,
    COUNT(*) as order_count,
    SUM(amount) as total_amount,
    SUM(total_amount) as total_value,
    AVG(price) as avg_price,
    COUNT(CASE WHEN order_status = 'COMPLETED' THEN 1 END) as completed_orders,
    COUNT(CASE WHEN order_status = 'CANCELLED' THEN 1 END) as cancelled_orders
FROM orders
WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) AND deleted = 0
GROUP BY DATE(created_at), order_type;

-- ==========================================
-- 12. 創建觸發器
-- ==========================================

DELIMITER //

-- 錢包餘額更新觸發器
DROP TRIGGER IF EXISTS update_wallet_balance_after_transaction//
CREATE TRIGGER update_wallet_balance_after_transaction
AFTER INSERT ON wallet_transactions
FOR EACH ROW
BEGIN
    UPDATE wallets 
    SET balance = NEW.balance_after,
        updated_at = CURRENT_TIMESTAMP,
        version = version + 1
    WHERE id = NEW.wallet_id;
END//

-- 用戶登錄時更新最後登錄信息
DROP TRIGGER IF EXISTS update_user_last_login//
CREATE TRIGGER update_user_last_login
AFTER INSERT ON user_sessions
FOR EACH ROW
BEGIN
    UPDATE users 
    SET last_login_at = NEW.created_at,
        last_login_ip = NEW.ip_address,
        login_attempts = 0,
        locked_until = NULL,
        updated_at = CURRENT_TIMESTAMP,
        version = version + 1
    WHERE id = NEW.user_id;
END//

-- KYC狀態變更時創建安全事件
DROP TRIGGER IF EXISTS create_security_event_kyc_change//
CREATE TRIGGER create_security_event_kyc_change
AFTER UPDATE ON user_kyc
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO security_events (user_id, event_type, severity, description, metadata)
        VALUES (NEW.user_id, 'KYC_SUBMIT', 'MEDIUM', 
                CONCAT('KYC status changed from ', OLD.status, ' to ', NEW.status),
                JSON_OBJECT('old_status', OLD.status, 'new_status', NEW.status));
    END IF;
END//

DELIMITER ;

-- ==========================================
-- 13. 創建性能優化索引
-- ==========================================

-- 複合索引優化常用查詢
CREATE INDEX idx_orders_user_status_created ON orders(user_id, order_status, created_at);
CREATE INDEX idx_wallet_transactions_wallet_type_created ON wallet_transactions(wallet_id, type, created_at);
CREATE INDEX idx_withdrawals_user_status_created ON withdrawals(user_id, status, created_at);
CREATE INDEX idx_notifications_user_status_created ON notifications(user_id, status, created_at);
CREATE INDEX idx_audit_logs_user_action_created ON audit_logs(user_id, action, created_at);

-- 用戶登錄查詢優化
CREATE INDEX idx_users_email_status_verified ON users(email, status, email_verified);

-- KYC審核工作台優化
CREATE INDEX idx_kyc_reviews_status_created_reviewer ON kyc_reviews(status, created_at, reviewer_id);

-- 價格數據查詢優化（K線圖）
CREATE INDEX idx_price_pair_interval_time_desc ON price_history(currency_pair, interval_type, timestamp DESC);

-- 全文索引用於搜索
ALTER TABLE announcements ADD FULLTEXT(title, content);
ALTER TABLE audit_logs ADD FULLTEXT(description);

-- ==========================================
-- 14. 設置數據庫參數
-- ==========================================

-- 恢復外鍵檢查
SET FOREIGN_KEY_CHECKS = 1;

-- 設置時區
SET time_zone = '+00:00';

-- 啟用事件調度器
SET GLOBAL event_scheduler = ON;

-- ==========================================
-- 完成初始化
-- ==========================================

-- 記錄初始化完成
INSERT INTO system_config (config_key, config_value, data_type, category, description, is_public) 
VALUES ('database_version', '2.0', 'string', 'system', '數據庫版本', FALSE);

INSERT INTO system_config (config_key, config_value, data_type, category, description, is_public) 
VALUES ('last_init_time', NOW(), 'string', 'system', '最後初始化時間', FALSE);

-- 初始化完成提示
SELECT 'USDT交易平台數據庫初始化完成!' as message,
       NOW() as completion_time,
       '2.0' as database_version;