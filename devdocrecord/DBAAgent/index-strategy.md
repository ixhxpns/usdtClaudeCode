# USDT交易平台索引策略說明

## 索引設計原則

### 1. 性能優化原則
- **查詢頻率優先**: 為高頻查詢建立索引
- **選擇性優先**: 優先為高選擇性字段建立索引
- **複合索引優化**: 合理設計複合索引順序
- **避免過度索引**: 平衡查詢性能和寫入性能

### 2. 業務場景導向
- **用戶管理場景**: 快速定位用戶信息
- **交易查詢場景**: 快速檢索訂單和交易記錄
- **審計合規場景**: 快速查詢操作日誌
- **統計分析場景**: 支持數據統計和報表

## 主要索引設計

### 1. 用戶管理相關索引

#### users 表索引
```sql
-- 主鍵索引（自動創建）
PRIMARY KEY (id)

-- 唯一索引
UNIQUE KEY uk_email (email)

-- 普通索引
INDEX idx_phone (phone)
INDEX idx_status (status)
INDEX idx_role_id (role_id)
INDEX idx_created_at (created_at)

-- 複合索引（支持用戶管理後台查詢）
INDEX idx_status_role_created (status, role_id, created_at)
INDEX idx_email_status (email, status)
```

**索引使用場景**：
- `uk_email`: 用戶登錄、郵箱唯一性檢查
- `idx_phone`: 手機登錄、手機唯一性檢查
- `idx_status`: 按狀態篩選用戶
- `idx_status_role_created`: 管理後台用戶列表查詢
- `idx_email_status`: 快速驗證用戶郵箱和狀態

#### user_kyc 表索引
```sql
-- 主鍵索引
PRIMARY KEY (id)

-- 外鍵索引
UNIQUE KEY uk_user_id (user_id)

-- 業務索引
INDEX idx_status (status)
INDEX idx_verified_at (verified_at)

-- 複合索引
INDEX idx_status_verified (status, verified_at)
```

**索引使用場景**：
- `uk_user_id`: 一對一關係保證，快速查找用戶KYC信息
- `idx_status`: KYC審核工作台按狀態查詢
- `idx_status_verified`: KYC統計報表查詢

### 2. 錢包系統相關索引

#### wallets 表索引
```sql
-- 主鍵索引
PRIMARY KEY (id)

-- 外鍵索引
INDEX idx_user_id (user_id)

-- 業務索引
INDEX idx_currency (currency)
INDEX idx_address (address)
INDEX idx_is_active (is_active)

-- 複合索引
UNIQUE KEY uk_user_currency (user_id, currency)
INDEX idx_user_currency_active (user_id, currency, is_active)
```

**索引使用場景**：
- `uk_user_currency`: 保證用戶每種幣種只有一個錢包
- `idx_address`: 通過地址查找錢包（區塊鏈回調）
- `idx_user_currency_active`: 查詢用戶特定幣種的活躍錢包

#### wallet_transactions 表索引
```sql
-- 主鍵索引
PRIMARY KEY (id)

-- 外鍵索引
INDEX idx_wallet_id (wallet_id)

-- 業務索引
INDEX idx_transaction_hash (transaction_hash)
INDEX idx_type (type)
INDEX idx_status (status)
INDEX idx_block_number (block_number)
INDEX idx_created_at (created_at)

-- 複合索引（高頻查詢優化）
INDEX idx_wallet_type_created (wallet_id, type, created_at)
INDEX idx_wallet_status_created (wallet_id, status, created_at)
INDEX idx_hash_status (transaction_hash, status)
```

**索引使用場景**：
- `idx_wallet_type_created`: 按錢包查詢特定類型交易記錄
- `idx_transaction_hash`: 區塊鏈交易哈希查詢
- `idx_hash_status`: 區塊鏈回調時更新交易狀態

### 3. 交易系統相關索引

#### orders 表索引
```sql
-- 主鍵索引
PRIMARY KEY (id)

-- 外鍵索引
INDEX idx_user_id (user_id)

-- 唯一索引
UNIQUE KEY uk_order_no (order_no)

-- 業務索引
INDEX idx_type (type)
INDEX idx_status (status)
INDEX idx_created_at (created_at)
INDEX idx_payment_deadline (payment_deadline)

-- 複合索引（核心查詢優化）
INDEX idx_user_status_created (user_id, status, created_at)
INDEX idx_status_deadline (status, payment_deadline)
INDEX idx_type_status_created (type, status, created_at)
```

**索引使用場景**：
- `uk_order_no`: 訂單號唯一性和快速查找
- `idx_user_status_created`: 用戶訂單列表查詢（最常用）
- `idx_status_deadline`: 查詢即將過期的待支付訂單
- `idx_type_status_created`: 管理後台按類型和狀態統計

#### price_history 表索引
```sql
-- 主鍵索引
PRIMARY KEY (id)

-- 業務索引
INDEX idx_currency_pair (currency_pair)
INDEX idx_timestamp (timestamp)
INDEX idx_interval_type (interval_type)
INDEX idx_source (source)

-- 複合索引（K線圖查詢優化）
UNIQUE KEY uk_pair_interval_time (currency_pair, interval_type, timestamp)
INDEX idx_pair_interval_time_desc (currency_pair, interval_type, timestamp DESC)
```

**索引使用場景**：
- `uk_pair_interval_time`: 防止重複數據，確保唯一性
- `idx_pair_interval_time_desc`: K線圖數據查詢（降序）

### 4. 提款管理相關索引

#### withdrawals 表索引
```sql
-- 主鍵索引
PRIMARY KEY (id)

-- 外鍵索引
INDEX idx_user_id (user_id)
INDEX idx_wallet_id (wallet_id)
INDEX idx_reviewer_id (reviewer_id)

-- 唯一索引
UNIQUE KEY uk_withdrawal_no (withdrawal_no)

-- 業務索引
INDEX idx_status (status)
INDEX idx_review_level (review_level)
INDEX idx_created_at (created_at)

-- 複合索引（審核工作台優化）
INDEX idx_user_status_created (user_id, status, created_at)
INDEX idx_status_level_created (status, review_level, created_at)
INDEX idx_reviewer_status_reviewed (reviewer_id, status, reviewed_at)
```

**索引使用場景**：
- `idx_user_status_created`: 用戶提款記錄查詢
- `idx_status_level_created`: 審核工作台按級別查詢
- `idx_reviewer_status_reviewed`: 審核員工作記錄查詢

### 5. 系統管理相關索引

#### notifications 表索引
```sql
-- 主鍵索引
PRIMARY KEY (id)

-- 外鍵索引
INDEX idx_user_id (user_id)

-- 業務索引
INDEX idx_type (type)
INDEX idx_category (category)
INDEX idx_status (status)
INDEX idx_priority (priority)
INDEX idx_send_at (send_at)
INDEX idx_created_at (created_at)

-- 複合索引（用戶通知查詢優化）
INDEX idx_user_status_created (user_id, status, created_at)
INDEX idx_user_category_status (user_id, category, status)
INDEX idx_status_priority_send (status, priority, send_at)
```

**索引使用場景**：
- `idx_user_status_created`: 用戶通知列表查詢
- `idx_status_priority_send`: 通知發送隊列處理

#### audit_logs 表索引
```sql
-- 主鍵索引
PRIMARY KEY (id)

-- 外鍵索引
INDEX idx_user_id (user_id)

-- 業務索引
INDEX idx_action (action)
INDEX idx_resource (resource)
INDEX idx_resource_id (resource_id)
INDEX idx_result (result)
INDEX idx_ip_address (ip_address)
INDEX idx_created_at (created_at)

-- 複合索引（審計查詢優化）
INDEX idx_user_action_created (user_id, action, created_at)
INDEX idx_resource_resource_id_created (resource, resource_id, created_at)
INDEX idx_action_result_created (action, result, created_at)

-- 全文索引
FULLTEXT INDEX ft_description (description)
```

**索引使用場景**：
- `idx_user_action_created`: 查詢用戶操作記錄
- `idx_resource_resource_id_created`: 查詢特定資源的操作記錄
- `ft_description`: 操作描述全文搜索

### 6. 會話安全相關索引

#### user_sessions 表索引
```sql
-- 主鍵索引
PRIMARY KEY (id)

-- 外鍵索引
INDEX idx_user_id (user_id)

-- 唯一索引
UNIQUE KEY uk_session_id (session_id)

-- 業務索引
INDEX idx_token (token(100))
INDEX idx_is_active (is_active)
INDEX idx_expires_at (expires_at)
INDEX idx_last_activity (last_activity)

-- 複合索引（會話管理優化）
INDEX idx_user_active_expires (user_id, is_active, expires_at)
INDEX idx_active_expires_activity (is_active, expires_at, last_activity)
```

**索引使用場景**：
- `uk_session_id`: 會話ID快速查找
- `idx_token`: JWT Token驗證
- `idx_active_expires_activity`: 清理過期會話

## 索引維護策略

### 1. 索引監控
```sql
-- 查看索引使用情況
SELECT 
    TABLE_SCHEMA,
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    SUB_PART,
    NULLABLE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'usdt_trading_platform';

-- 查看未使用的索引
SELECT 
    object_schema,
    object_name,
    index_name
FROM performance_schema.table_io_waits_summary_by_index_usage 
WHERE index_name IS NOT NULL
    AND count_star = 0
    AND object_schema = 'usdt_trading_platform';
```

### 2. 定期優化
```sql
-- 重建索引統計信息
ANALYZE TABLE users, orders, wallets, wallet_transactions;

-- 檢查表碎片
SELECT 
    TABLE_NAME,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'DB Size in MB',
    ROUND((data_free / 1024 / 1024), 2) AS 'Free Space in MB'
FROM information_schema.TABLES 
WHERE table_schema = 'usdt_trading_platform'
    AND data_free > 0;

-- 優化表（減少碎片）
OPTIMIZE TABLE orders, wallet_transactions, audit_logs;
```

### 3. 分區策略

#### 大表分區設計
```sql
-- audit_logs 表按月分區（範圍分區）
ALTER TABLE audit_logs PARTITION BY RANGE (TO_DAYS(created_at)) (
    PARTITION p202508 VALUES LESS THAN (TO_DAYS('2025-09-01')),
    PARTITION p202509 VALUES LESS THAN (TO_DAYS('2025-10-01')),
    PARTITION p202510 VALUES LESS THAN (TO_DAYS('2025-11-01')),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);

-- price_history 表按時間戳分區（範圍分區）
ALTER TABLE price_history PARTITION BY RANGE (UNIX_TIMESTAMP(timestamp)) (
    PARTITION p202508 VALUES LESS THAN (UNIX_TIMESTAMP('2025-09-01')),
    PARTITION p202509 VALUES LESS THAN (UNIX_TIMESTAMP('2025-10-01')),
    PARTITION p202510 VALUES LESS THAN (UNIX_TIMESTAMP('2025-11-01')),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);

-- notifications 表按用戶ID哈希分區（哈希分區）
ALTER TABLE notifications PARTITION BY HASH(user_id) PARTITIONS 16;
```

## 性能調優建議

### 1. 查詢優化原則
- **避免SELECT \***: 只查詢需要的字段
- **使用LIMIT**: 分頁查詢必須使用LIMIT
- **合理使用索引**: 確保WHERE條件能命中索引
- **避免函數操作**: WHERE條件中避免對字段使用函數

### 2. 索引使用注意事項
- **最左前綴原則**: 複合索引必須遵循最左前綴原則
- **索引覆蓋**: 盡量使用覆蓋索引減少回表
- **避免索引失效**: 注意NULL值、類型轉換、模糊查詢等問題
- **監控索引效果**: 定期檢查慢查詢日誌

### 3. 寫入性能優化
- **批量插入**: 使用INSERT INTO ... VALUES (),(),()語法
- **事務控制**: 合理控制事務大小，避免長事務
- **索引影響**: 考慮索引對寫入性能的影響
- **鎖競爭**: 避免熱點數據的鎖競爭

## 監控指標

### 1. 關鍵性能指標
- **慢查詢數量**: 每小時慢查詢統計
- **索引命中率**: SELECT查詢的索引使用率
- **鎖等待時間**: 平均鎖等待時間
- **連接池使用率**: 數據庫連接池使用情況

### 2. 告警閾值設定
- **慢查詢**: 執行時間 > 2秒
- **鎖等待**: 等待時間 > 5秒
- **連接數**: 使用率 > 80%
- **磁盤使用**: 使用率 > 85%

### 3. 定期檢查任務
- **每日**: 檢查慢查詢日誌
- **每週**: 分析索引使用情況
- **每月**: 優化表結構和索引
- **每季度**: 容量規劃和性能評估