# USDT交易平台資料庫ER圖設計

## ER圖概覽

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           USDT交易平台資料庫 ER 圖                                        │
└─────────────────────────────────────────────────────────────────────────────────────────┘

                                    ┌─────────────────┐
                                    │     users       │
                                    │ ──────────────  │
                                    │ id (PK)         │
                                    │ email           │
                                    │ phone           │
                                    │ password_hash   │
                                    │ salt            │
                                    │ google_auth_key │
                                    │ status          │
                                    │ role_id (FK)    │
                                    │ created_at      │
                                    │ updated_at      │
                                    └─────┬───────────┘
                                          │
                 ┌────────────────────────┼────────────────────────┐
                 │                        │                        │
                 │                        │                        │
     ┌───────────▼─────┐      ┌───────────▼─────┐      ┌───────────▼─────┐
     │   user_kyc      │      │    wallets      │      │  user_profiles  │
     │ ──────────────  │      │ ──────────────  │      │ ──────────────  │
     │ id (PK)         │      │ id (PK)         │      │ id (PK)         │
     │ user_id (FK)    │      │ user_id (FK)    │      │ user_id (FK)    │
     │ id_number       │      │ currency        │      │ first_name      │
     │ id_card_front   │      │ balance         │      │ last_name       │
     │ id_card_back    │      │ frozen_balance  │      │ birth_date      │
     │ second_doc      │      │ address         │      │ country         │
     │ bank_account    │      │ private_key     │      │ city            │
     │ bank_name       │      │ created_at      │      │ address         │
     │ status          │      │ updated_at      │      │ created_at      │
     │ verified_at     │      └─────┬───────────┘      │ updated_at      │
     │ created_at      │            │                  └─────────────────┘
     │ updated_at      │            │
     └─────────────────┘            │
                                    │
                          ┌─────────▼─────────┐
                          │ wallet_transactions│
                          │ ──────────────     │
                          │ id (PK)            │
                          │ wallet_id (FK)     │
                          │ transaction_hash   │
                          │ type               │
                          │ amount             │
                          │ fee                │
                          │ status             │
                          │ block_number       │
                          │ created_at         │
                          │ updated_at         │
                          └────────────────────┘

┌─────────────────┐           ┌─────────────────┐           ┌─────────────────┐
│    roles        │           │     orders      │           │  price_history  │
│ ──────────────  │           │ ──────────────  │           │ ──────────────  │
│ id (PK)         │           │ id (PK)         │           │ id (PK)         │
│ name            │           │ user_id (FK)    │           │ currency_pair   │
│ description     │           │ type            │           │ price           │
│ permissions     │           │ amount          │           │ volume          │
│ created_at      │           │ price           │           │ timestamp       │
│ updated_at      │           │ total_amount    │           │ source          │
└─────────────────┘           │ status          │           │ created_at      │
                              │ payment_info    │           └─────────────────┘
                              │ transaction_id  │
                              │ created_at      │
                              │ updated_at      │
                              │ completed_at    │
                              └─────┬───────────┘
                                    │
                          ┌─────────▼─────────┐
                          │ order_transactions │
                          │ ──────────────     │
                          │ id (PK)            │
                          │ order_id (FK)      │
                          │ transaction_hash   │
                          │ block_number       │
                          │ confirmations      │
                          │ status             │
                          │ created_at         │
                          │ updated_at         │
                          └────────────────────┘

┌─────────────────┐           ┌─────────────────┐           ┌─────────────────┐
│  withdrawals    │           │   system_config │           │  notifications  │
│ ──────────────  │           │ ──────────────  │           │ ──────────────  │
│ id (PK)         │           │ id (PK)         │           │ id (PK)         │
│ user_id (FK)    │           │ config_key      │           │ user_id (FK)    │
│ wallet_id (FK)  │           │ config_value    │           │ type            │
│ amount          │           │ description     │           │ title           │
│ fee             │           │ is_active       │           │ content         │
│ to_address      │           │ created_at      │           │ status          │
│ status          │           │ updated_at      │           │ sent_at         │
│ admin_id        │           └─────────────────┘           │ read_at         │
│ review_note     │                                         │ created_at      │
│ transaction_id  │           ┌─────────────────┐           │ updated_at      │
│ created_at      │           │  audit_logs     │           └─────────────────┘
│ updated_at      │           │ ──────────────  │
│ processed_at    │           │ id (PK)         │
└─────────────────┘           │ user_id (FK)    │
                              │ action          │
                              │ resource        │
                              │ resource_id     │
                              │ old_values      │
                              │ new_values      │
                              │ ip_address      │
                              │ user_agent      │
                              │ created_at      │
                              └─────────────────┘

┌─────────────────┐           ┌─────────────────┐
│   announcements │           │  kyc_reviews    │
│ ──────────────  │           │ ──────────────  │
│ id (PK)         │           │ id (PK)         │
│ title           │           │ kyc_id (FK)     │
│ content         │           │ reviewer_id (FK)│
│ type            │           │ status          │
│ priority        │           │ review_note     │
│ is_active       │           │ reviewed_at     │
│ publish_at      │           │ created_at      │
│ expire_at       │           │ updated_at      │
│ created_by      │           └─────────────────┘
│ created_at      │
│ updated_at      │
└─────────────────┘
```

## 表關係說明

### 主要實體關係
1. **users** 是核心表，與多個表有一對一或一對多關係
2. **wallets** 與 **wallet_transactions** 一對多關係
3. **orders** 與 **order_transactions** 一對多關係
4. **user_kyc** 與 **kyc_reviews** 一對多關係
5. **withdrawals** 連接 users 和 wallets
6. **roles** 與 users 一對多關係（角色權限）

### 業務流程關係
1. 用戶註冊 → 創建錢包 → KYC驗證
2. KYC通過 → 可進行交易 → 訂單生成
3. 訂單完成 → 區塊鏈交易 → 錢包餘額更新
4. 提款申請 → 管理員審核 → 區塊鏈轉賬

### 審計和日誌
1. **audit_logs** 記錄所有重要操作
2. **notifications** 處理系統通知
3. **system_config** 管理系統參數
4. **announcements** 處理系統公告

## 數據加密策略
1. **敏感字段加密**: password_hash, salt, private_key, id_number, bank_account
2. **傳輸加密**: 所有API使用HTTPS
3. **存儲加密**: 錢包私鑰使用AES-256加密
4. **備份加密**: 數據備份使用加密存儲