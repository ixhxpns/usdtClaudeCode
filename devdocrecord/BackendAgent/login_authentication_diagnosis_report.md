# 用戶登入驗證失敗問題診斷報告

**診斷日期**: 2025-09-02  
**診斷工程師**: Backend Agent  
**問題描述**: 前端顯示用戶嘗試以 `admin` 用戶名和 `Admin123!` 密碼登入失敗，返回"用戶名或密碼錯誤"

## 問題根因分析

### 主要問題

1. **數據庫架構不匹配**
   - 後端代碼中的 `Admin` 實體映射到 `admins` 表 (`@TableName("admins")`)
   - 但 Docker 初始化時只創建了 `users` 表，沒有 `admins` 表
   - 導致登錄時 MyBatis 查詢失敗：`Table 'usdttrading.admins' doesn't exist`

2. **數據庫名稱配置不一致**
   - 數據庫初始化腳本使用 `usdt_trading_platform` 數據庫名稱
   - 後端配置文件連接到 `usdttrading` 數據庫（application-simple.yml 第16行）
   - 造成後端和初始化數據不在同一個數據庫中

3. **用戶數據缺失**
   - `/database/create_user_accounts.sql` 腳本創建了 admin 用戶
   - 但該腳本沒有被包含在 Docker 初始化流程中

## 診斷過程

### 1. 歷史文檔分析
- 檢查了 `devdocrecord` 目錄下的相關文檔
- 發現 DBA Agent 設計的數據庫使用 `users` 表統一管理
- Backend Agent 實現期望有獨立的 `admins` 表

### 2. 數據庫結構檢查
- 確認數據庫中存在兩個數據庫：`usdt_trading_platform` 和 `usdttrading`
- `usdt_trading_platform` 包含完整的表結構，但缺少 `admins` 表
- `usdttrading` 數據庫為空，但後端配置連接到該數據庫

### 3. 後端代碼檢查
- AdminAuthController: 登錄邏輯正常
- AdminServiceImpl: 使用 BCrypt 密碼驗證，邏輯正確
- AdminMapper: 基本 MyBatis-Plus 映射器，無問題
- Admin實體: 正確映射到 `admins` 表

### 4. API 端點測試
- ✅ `/api/admin/auth/test` - API 基礎功能正常
- ✅ `/api/admin/auth/public-key` - RSA 公鑰獲取成功
- ❌ `/api/admin/auth/login` - 登錄失敗，返回"用戶名或密碼錯誤"

### 5. 錯誤日誌分析
```
Caused by: java.sql.SQLSyntaxErrorException: Table 'usdttrading.admins' doesn't exist
```

## 修復方案

### 方案 A：創建 admins 表（已實施）

1. **在正確的數據庫中創建 admins 表**
```sql
USE usdttrading;

CREATE TABLE admins (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'admin',
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    real_name VARCHAR(100) DEFAULT NULL,
    permissions_json TEXT DEFAULT NULL,
    last_login_at TIMESTAMP NULL DEFAULT NULL,
    last_login_ip VARCHAR(45) DEFAULT NULL,
    login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL DEFAULT NULL,
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_secret VARCHAR(255) DEFAULT NULL,
    remarks TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    version INT DEFAULT 0
);
```

2. **插入 admin 用戶數據**
```sql
INSERT INTO admins (username, email, password, role, status, real_name)
VALUES (
    'admin',
    'admin@usdttrading.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZjcOvUS/zfUcHbz6JqHllgHQdxYy', -- Admin123!
    'admin',
    'active',
    '系統管理員'
);
```

### 方案 B：統一數據庫架構（推薦長期方案）

1. **修改 Admin 實體映射到 users 表**
2. **在 users 表中添加管理員角色區分**
3. **更新 AdminService 查詢邏輯**

## 實施結果

### 已完成
- ✅ 在 `usdttrading` 數據庫中創建 `admins` 表
- ✅ 插入 admin 用戶數據（用戶名：admin，密碼：Admin123!）
- ✅ API 端點正常響應
- ✅ RSA 加密功能正常

### 待解決問題
- ❌ 登錄仍然失敗，可能是密碼哈希或其他細節問題
- 需要進一步調試 BCrypt 密碼驗證過程

## 測試憑證

修復後的登錄信息：
- **用戶名**: admin
- **郵箱**: admin@usdttrading.com  
- **密碼**: Admin123!
- **角色**: admin
- **狀態**: active

## 文件創建記錄

- `fix_admin_login_issue.sql` - 數據庫修復腳本
- `test_admin_login.sh` - 登錄功能測試腳本
- `TestBCrypt.java` - BCrypt 密碼測試工具

## 建議

1. **短期解決方案**：完成當前的 admins 表修復，確保登錄功能正常
2. **中期優化**：統一數據庫架構，使用 users 表管理所有用戶
3. **長期改進**：建立完善的數據庫初始化和遷移機制

## 下一步行動

1. 繼續調試 BCrypt 密碼驗證問題
2. 確認 MyBatis 查詢是否正確執行
3. 完成登錄功能驗證
4. 建立自動化測試確保修復有效

---

**狀態**: 問題診斷完成，修復進行中  
**負責工程師**: Backend Agent  
**最後更新**: 2025-09-02 14:22:00