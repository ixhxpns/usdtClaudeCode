# USDT交易平台數據庫兼容性問題分析報告

**分析日期**: 2025-08-23  
**分析師**: DBA Agent  
**優先級**: 高  

## 發現的關鍵問題

### 1. 實體類與數據庫表字段不匹配

#### 1.1 User 實體問題
- **問題**: User.java 包含 `username` 字段，但數據庫表 `users` 沒有此字段
- **影響**: 可能導致查詢錯誤
- **解決方案**: 添加 `username` 字段到數據庫表或從實體類移除

#### 1.2 Wallet 實體問題
- **問題**: Wallet.java 包含以下冗余字段：
  - `frozenUsdt`: 與 `frozenBalance` 重複
  - `usdtBalance`: 與 `balance` 重複
  - `twdBalance`: 數據庫表不存在此字段
- **影響**: 字段映射混亂，可能導致數據不一致
- **解決方案**: 清理冗余字段或更新數據庫結構

### 2. MyBatis Plus 配置問題

#### 2.1 BaseEntity 字段映射
- **問題**: BaseEntity 使用 `deleted` 和 `version` 字段，但部分表缺少這些字段
- **影響**: 樂觀鎖和邏輯刪除可能無法正常工作
- **解決方案**: 統一添加這些字段到所有表

#### 2.2 自動填充字段
- **問題**: 配置了 `MybatisMetaObjectHandler` 但某些表的字段名可能不匹配
- **影響**: 創建時間和更新時間可能無法自動填充

### 3. 枚舉值映射問題

#### 3.1 Order 實體枚舉字段
- **問題**: 
  - 數據庫使用 `type` 字段，實體使用 `orderType`
  - 數據庫使用 `status` 字段，實體使用 `orderStatus`
- **影響**: 字段映射需要手動處理
- **解決方案**: 統一字段命名或配置映射規則

### 4. 數據類型兼容性

#### 4.1 JSON 字段處理
- **問題**: 某些表使用 JSON 類型但實體類可能沒有正確的序列化配置
- **字段**: `permissions`, `payment_info`, `metadata`
- **解決方案**: 確保 JSON 序列化配置正確

## 修復方案

### 方案一：修改數據庫結構（推薦）

```sql
-- 1. 為 users 表添加 username 字段
ALTER TABLE users ADD COLUMN username VARCHAR(50) DEFAULT NULL COMMENT '用戶名' AFTER id;
ALTER TABLE users ADD INDEX idx_username (username);

-- 2. 統一添加 BaseEntity 所需字段到缺失的表
ALTER TABLE roles ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '邏輯刪除標記';
ALTER TABLE roles ADD COLUMN version INT DEFAULT 1 COMMENT '樂觀鎖版本號';

-- 3. 修復字段命名不一致問題
ALTER TABLE orders CHANGE COLUMN type order_type ENUM('buy', 'sell') NOT NULL COMMENT '訂單類型';
ALTER TABLE orders CHANGE COLUMN status order_status ENUM('pending', 'paid', 'processing', 'completed', 'cancelled', 'expired', 'failed') DEFAULT 'pending' COMMENT '訂單狀態';

-- 4. 為 wallets 表添加缺失字段
ALTER TABLE wallets ADD COLUMN twd_balance DECIMAL(20,2) DEFAULT 0.00 COMMENT 'TWD餘額' AFTER frozen_balance;
```

### 方案二：修改實體類（備選）

```java
// 移除 User 實體中的 username 字段，或添加 @TableField(exist = false) 註解
// 清理 Wallet 實體中的冗余字段
// 統一枚舉字段的命名規範
```

## 建議執行順序

1. **立即執行**: 修復數據庫結構缺陷
2. **測試驗證**: 確保所有實體類字段映射正確
3. **部署前檢查**: 驗證 MyBatis 映射文件與數據庫表匹配
4. **性能測試**: 確保修改不會影響查詢性能

## 風險評估

- **低風險**: 添加新字段
- **中風險**: 修改現有字段名稱
- **高風險**: 修改字段類型或刪除字段

建議在測試環境先驗證所有修改，確保無誤後再部署到生產環境。