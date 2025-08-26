# USDT交易平台資料字典

## 資料表概覽

| 資料表名稱 | 中文說明 | 主要用途 | 記錄數預估 |
|-----------|---------|---------|-----------|
| roles | 角色表 | 用戶權限管理 | < 100 |
| users | 用戶主表 | 用戶基礎信息 | 100,000+ |
| user_profiles | 用戶資料表 | 用戶詳細資料 | 100,000+ |
| user_kyc | KYC驗證表 | 身份認證資料 | 50,000+ |
| kyc_reviews | KYC審核記錄 | 審核流程記錄 | 100,000+ |
| wallets | 錢包表 | 用戶錢包管理 | 200,000+ |
| wallet_transactions | 錢包交易記錄 | 充提記錄 | 1,000,000+ |
| orders | 訂單表 | 交易訂單 | 1,000,000+ |
| order_transactions | 訂單區塊鏈交易 | 區塊鏈交易記錄 | 1,000,000+ |
| price_history | 價格歷史 | 價格數據 | 10,000,000+ |
| withdrawals | 提款申請表 | 提款管理 | 500,000+ |
| system_config | 系統配置 | 系統參數 | < 1,000 |
| announcements | 系統公告 | 平台公告 | < 10,000 |
| notifications | 通知表 | 用戶通知 | 10,000,000+ |
| audit_logs | 操作日誌 | 審計追蹤 | 100,000,000+ |
| user_sessions | 用戶會話 | 登錄會話 | 1,000,000+ |
| security_events | 安全事件 | 安全監控 | 10,000,000+ |
| platform_wallets | 平台錢包池 | 平台資金管理 | < 100 |

## 詳細字段說明

### 1. 用戶管理模組

#### users（用戶主表）
| 字段名 | 類型 | 長度 | 說明 | 索引 | 備註 |
|--------|------|------|------|------|------|
| id | BIGINT | - | 用戶ID | PK | 自增主鍵 |
| email | VARCHAR | 100 | 郵箱地址 | UK | 唯一，登錄用 |
| phone | VARCHAR | 20 | 手機號碼 | IDX | 可選登錄方式 |
| password_hash | VARCHAR | 255 | 密碼哈希 | - | BCrypt加密 |
| salt | VARCHAR | 32 | 密碼鹽值 | - | 隨機生成 |
| google_auth_key | VARCHAR | 32 | Google驗證器密鑰 | - | Base32編碼 |
| status | ENUM | - | 賬戶狀態 | IDX | active,inactive,frozen,deleted |
| email_verified | BOOLEAN | - | 郵箱驗證狀態 | - | 預設false |
| phone_verified | BOOLEAN | - | 手機驗證狀態 | - | 預設false |
| google_auth_enabled | BOOLEAN | - | Google驗證啟用 | - | 預設false |
| role_id | BIGINT | - | 角色ID | FK+IDX | 關聯roles表 |
| last_login_at | TIMESTAMP | - | 最後登錄時間 | - | 可為空 |
| last_login_ip | VARCHAR | 45 | 最後登錄IP | - | 支持IPv6 |
| login_attempts | INT | - | 登錄嘗試次數 | - | 防暴力破解 |
| locked_until | TIMESTAMP | - | 鎖定到期時間 | - | 可為空 |

#### user_profiles（用戶資料表）
| 字段名 | 類型 | 長度 | 說明 | 索引 | 備註 |
|--------|------|------|------|------|------|
| id | BIGINT | - | 資料ID | PK | 自增主鍵 |
| user_id | BIGINT | - | 用戶ID | FK+UK | 一對一關係 |
| first_name | VARCHAR | 50 | 名 | - | 可選 |
| last_name | VARCHAR | 50 | 姓 | - | 可選 |
| birth_date | DATE | - | 出生日期 | - | KYC必需 |
| gender | ENUM | - | 性別 | - | male,female,other |
| country | VARCHAR | 50 | 國家 | IDX | 統計用 |
| state | VARCHAR | 50 | 省/州 | - | - |
| city | VARCHAR | 50 | 城市 | - | - |
| address | TEXT | - | 詳細地址 | - | - |
| postal_code | VARCHAR | 20 | 郵政編碼 | - | - |
| avatar_url | VARCHAR | 255 | 頭像URL | - | - |
| timezone | VARCHAR | 50 | 時區 | - | 預設UTC |
| language | VARCHAR | 10 | 語言偏好 | - | 預設en |

#### user_kyc（KYC驗證表）
| 字段名 | 類型 | 長度 | 說明 | 索引 | 備註 |
|--------|------|------|------|------|------|
| id | BIGINT | - | KYC ID | PK | 自增主鍵 |
| user_id | BIGINT | - | 用戶ID | FK+UK | 一對一關係 |
| id_type | ENUM | - | 證件類型 | - | id_card,passport,driving_license |
| id_number | VARCHAR | 100 | 證件號碼 | - | 加密存儲 |
| id_card_front | VARCHAR | 255 | 身份證正面 | - | 圖片URL |
| id_card_back | VARCHAR | 255 | 身份證反面 | - | 圖片URL |
| selfie_photo | VARCHAR | 255 | 手持自拍照 | - | 圖片URL |
| second_doc_type | VARCHAR | 50 | 第二證件類型 | - | 可選 |
| second_doc_url | VARCHAR | 255 | 第二證件照片 | - | 圖片URL |
| bank_account | VARCHAR | 255 | 銀行賬號 | - | 加密存儲 |
| bank_name | VARCHAR | 100 | 銀行名稱 | - | - |
| bank_branch | VARCHAR | 100 | 支行名稱 | - | - |
| account_holder_name | VARCHAR | 100 | 賬戶持有人姓名 | - | - |
| status | ENUM | - | KYC狀態 | IDX | pending,processing,approved,rejected,expired |
| rejection_reason | TEXT | - | 拒絕原因 | - | 拒絕時必填 |
| verified_at | TIMESTAMP | - | 驗證通過時間 | IDX | 可為空 |
| expires_at | TIMESTAMP | - | 驗證到期時間 | - | 可為空 |

### 2. 錢包系統模組

#### wallets（錢包表）
| 字段名 | 類型 | 長度 | 說明 | 索引 | 備註 |
|--------|------|------|------|------|------|
| id | BIGINT | - | 錢包ID | PK | 自增主鍵 |
| user_id | BIGINT | - | 用戶ID | FK | 關聯users表 |
| currency | ENUM | - | 幣種 | IDX | TRX,USDT |
| balance | DECIMAL | 20,8 | 可用餘額 | - | 精度8位小數 |
| frozen_balance | DECIMAL | 20,8 | 凍結餘額 | - | 交易凍結 |
| address | VARCHAR | 100 | 錢包地址 | IDX | 區塊鏈地址 |
| private_key | TEXT | - | 私鑰 | - | AES加密存儲 |
| is_active | BOOLEAN | - | 是否啟用 | IDX | 預設true |

#### wallet_transactions（錢包交易記錄）
| 字段名 | 類型 | 長度 | 說明 | 索引 | 備註 |
|--------|------|------|------|------|------|
| id | BIGINT | - | 交易ID | PK | 自增主鍵 |
| wallet_id | BIGINT | - | 錢包ID | FK+IDX | 關聯wallets表 |
| transaction_hash | VARCHAR | 100 | 區塊鏈交易哈希 | IDX | 可為空 |
| type | ENUM | - | 交易類型 | IDX | deposit,withdrawal,transfer_in,transfer_out,fee,reward |
| amount | DECIMAL | 20,8 | 交易金額 | - | 可為負數 |
| fee | DECIMAL | 20,8 | 手續費 | - | 預設0 |
| balance_before | DECIMAL | 20,8 | 交易前餘額 | - | 記錄快照 |
| balance_after | DECIMAL | 20,8 | 交易後餘額 | - | 記錄快照 |
| status | ENUM | - | 交易狀態 | IDX | pending,confirming,completed,failed,cancelled |
| block_number | BIGINT | - | 區塊號 | IDX | 可為空 |
| confirmations | INT | - | 確認數 | - | 預設0 |
| from_address | VARCHAR | 100 | 發送地址 | - | 可為空 |
| to_address | VARCHAR | 100 | 接收地址 | - | 可為空 |
| memo | VARCHAR | 255 | 備註 | - | 可選 |
| error_message | TEXT | - | 錯誤信息 | - | 失敗時填寫 |

### 3. 交易系統模組

#### orders（訂單表）
| 字段名 | 類型 | 長度 | 說明 | 索引 | 備註 |
|--------|------|------|------|------|------|
| id | BIGINT | - | 訂單ID | PK | 自增主鍵 |
| order_no | VARCHAR | 32 | 訂單號 | UK+IDX | 唯一標識 |
| user_id | BIGINT | - | 用戶ID | FK+IDX | 關聯users表 |
| type | ENUM | - | 訂單類型 | IDX | buy,sell |
| currency_pair | VARCHAR | 20 | 交易對 | - | 預設USDT/TWD |
| amount | DECIMAL | 20,8 | USDT數量 | - | 交易數量 |
| price | DECIMAL | 10,4 | 單價 | - | TWD單價 |
| total_amount | DECIMAL | 20,2 | 總金額 | - | TWD總額 |
| filled_amount | DECIMAL | 20,8 | 已成交數量 | - | 預設0 |
| status | ENUM | - | 訂單狀態 | IDX | pending,paid,processing,completed,cancelled,expired,failed |
| payment_method | VARCHAR | 50 | 支付方式 | - | 可選 |
| payment_info | JSON | - | 支付信息 | - | 結構化數據 |
| bank_account | VARCHAR | 255 | 收款銀行賬號 | - | 賣單用 |
| payment_deadline | TIMESTAMP | - | 支付截止時間 | IDX | 買單必需 |
| admin_note | TEXT | - | 管理員備註 | - | 可選 |
| completed_at | TIMESTAMP | - | 完成時間 | - | 可為空 |

#### price_history（價格歷史表）
| 字段名 | 類型 | 長度 | 說明 | 索引 | 備註 |
|--------|------|------|------|------|------|
| id | BIGINT | - | 價格ID | PK | 自增主鍵 |
| currency_pair | VARCHAR | 20 | 交易對 | IDX | 預設USDT/TWD |
| price | DECIMAL | 10,4 | 價格 | - | 4位小數 |
| volume | DECIMAL | 20,8 | 交易量 | - | 可選 |
| high | DECIMAL | 10,4 | 最高價 | - | K線用 |
| low | DECIMAL | 10,4 | 最低價 | - | K線用 |
| open | DECIMAL | 10,4 | 開盤價 | - | K線用 |
| close | DECIMAL | 10,4 | 收盤價 | - | K線用 |
| source | VARCHAR | 50 | 價格來源 | IDX | system,coinbase,binance等 |
| interval_type | ENUM | - | 時間間隔 | IDX | 1m,5m,15m,30m,1h,4h,1d |
| timestamp | TIMESTAMP | - | 價格時間戳 | IDX | 重要時間軸 |

### 4. 提款管理模組

#### withdrawals（提款申請表）
| 字段名 | 類型 | 長度 | 說明 | 索引 | 備註 |
|--------|------|------|------|------|------|
| id | BIGINT | - | 提款ID | PK | 自增主鍵 |
| withdrawal_no | VARCHAR | 32 | 提款單號 | UK+IDX | 唯一標識 |
| user_id | BIGINT | - | 用戶ID | FK+IDX | 關聯users表 |
| wallet_id | BIGINT | - | 錢包ID | FK+IDX | 關聯wallets表 |
| amount | DECIMAL | 20,8 | 提款金額 | - | 申請金額 |
| fee | DECIMAL | 20,8 | 手續費 | - | 預設0 |
| actual_amount | DECIMAL | 20,8 | 實際到賬金額 | - | amount - fee |
| to_address | VARCHAR | 100 | 提款地址 | - | 目標地址 |
| status | ENUM | - | 提款狀態 | IDX | pending,reviewing,approved,processing,completed,rejected,cancelled |
| review_level | ENUM | - | 審核級別 | IDX | auto,manual,senior |
| reviewer_id | BIGINT | - | 審核人員ID | FK+IDX | 可為空 |
| review_note | TEXT | - | 審核備註 | - | 可選 |
| rejection_reason | TEXT | - | 拒絕原因 | - | 拒絕時必填 |
| transaction_hash | VARCHAR | 100 | 區塊鏈交易哈希 | - | 完成後填入 |
| block_number | BIGINT | - | 區塊號 | - | 可為空 |
| risk_score | INT | - | 風險評分 | - | 0-100 |
| ip_address | VARCHAR | 45 | 申請IP地址 | - | 風控用 |
| user_agent | TEXT | - | 用戶代理 | - | 風控用 |
| reviewed_at | TIMESTAMP | - | 審核時間 | - | 可為空 |
| processed_at | TIMESTAMP | - | 處理時間 | - | 可為空 |

### 5. 系統管理模組

#### system_config（系統配置表）
| 字段名 | 類型 | 長度 | 說明 | 索引 | 備註 |
|--------|------|------|------|------|------|
| id | BIGINT | - | 配置ID | PK | 自增主鍵 |
| config_key | VARCHAR | 100 | 配置鍵 | UK+IDX | 唯一標識 |
| config_value | TEXT | - | 配置值 | - | 支持長文本 |
| data_type | ENUM | - | 數據類型 | - | string,number,boolean,json |
| category | VARCHAR | 50 | 配置分類 | IDX | general,trading,system等 |
| description | VARCHAR | 255 | 配置描述 | - | 可選 |
| is_public | BOOLEAN | - | 是否公開配置 | IDX | 前端可見 |
| is_active | BOOLEAN | - | 是否啟用 | IDX | 預設true |

#### audit_logs（操作日誌表）
| 字段名 | 類型 | 長度 | 說明 | 索引 | 備註 |
|--------|------|------|------|------|------|
| id | BIGINT | - | 日誌ID | PK | 自增主鍵 |
| user_id | BIGINT | - | 操作用戶ID | FK+IDX | 可為空 |
| action | VARCHAR | 100 | 操作動作 | IDX | create,update,delete等 |
| resource | VARCHAR | 100 | 操作資源 | IDX | user,order,withdrawal等 |
| resource_id | VARCHAR | 100 | 資源ID | IDX | 相關記錄ID |
| description | TEXT | - | 操作描述 | FULLTEXT | 全文搜索 |
| old_values | JSON | - | 操作前數據 | - | 變更前快照 |
| new_values | JSON | - | 操作後數據 | - | 變更後快照 |
| ip_address | VARCHAR | 45 | IP地址 | IDX | 審計用 |
| user_agent | TEXT | - | 用戶代理 | - | 客戶端信息 |
| request_id | VARCHAR | 100 | 請求ID | - | 鏈路追蹤 |
| session_id | VARCHAR | 100 | 會話ID | - | 會話關聯 |
| result | ENUM | - | 操作結果 | IDX | success,failure,error |
| error_message | TEXT | - | 錯誤信息 | - | 失敗時記錄 |
| execution_time | INT | - | 執行時間 | - | 毫秒單位 |

## 枚舉值定義

### 用戶狀態（users.status）
- `active`: 正常活躍用戶
- `inactive`: 未激活用戶（新註冊）
- `frozen`: 凍結用戶（暫停使用）
- `deleted`: 已刪除用戶（軟刪除）

### KYC狀態（user_kyc.status）
- `pending`: 等待提交
- `processing`: 審核中
- `approved`: 審核通過
- `rejected`: 審核拒絕
- `expired`: 驗證過期

### 訂單狀態（orders.status）
- `pending`: 待支付
- `paid`: 已支付（待處理）
- `processing`: 處理中
- `completed`: 已完成
- `cancelled`: 已取消
- `expired`: 已過期
- `failed`: 處理失敗

### 提款狀態（withdrawals.status）
- `pending`: 待審核
- `reviewing`: 審核中
- `approved`: 審核通過
- `processing`: 處理中
- `completed`: 已完成
- `rejected`: 審核拒絕
- `cancelled`: 已取消

## 數據加密規範

### 敏感字段加密
1. **用戶密碼**: 使用BCrypt進行哈希加密
2. **錢包私鑰**: 使用AES-256加密存儲
3. **身份證號碼**: 使用AES-256加密存儲
4. **銀行賬號**: 使用AES-256加密存儲

### 加密密鑰管理
- 主加密密鑰存儲在環境變量中
- 使用密鑰輪換機制
- 定期更新加密密鑰
- 備份加密密鑰到安全位置

## 數據保留策略

### 日誌數據
- **audit_logs**: 保留5年，按月分表
- **security_events**: 保留3年，按月分表
- **notifications**: 保留1年，按月清理

### 交易數據
- **orders**: 永久保留
- **wallet_transactions**: 永久保留
- **withdrawals**: 永久保留
- **price_history**: 按時間間隔保留（1m:1月，5m:3月，1h:1年，1d:永久）

### 會話數據
- **user_sessions**: 保留3個月，定期清理過期會話
- **notifications**: 已讀通知30天後清理

## 備份策略

### 完整備份
- 每日凌晨2點進行全量備份
- 備份保留30天
- 異地備份保留1年

### 增量備份
- 每小時進行增量備份
- 二進制日誌實時同步
- 重要交易表實時備份

### 恢復測試
- 每月進行備份恢復測試
- 災難恢復預案演練
- RTO目標：4小時內恢復服務