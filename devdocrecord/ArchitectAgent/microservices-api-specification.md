# 微服務架構與API接口規範

## 微服務架構設計

### 服務劃分原則
1. **業務邊界清晰**: 每個服務職責單一，邊界明確
2. **數據獨立**: 每個服務擁有自己的數據庫
3. **鬆耦合**: 服務間通過API通信，避免直接依賴
4. **高內聚**: 相關功能集中在同一服務內

### 微服務架構圖

```
┌─────────────────────────────────────────────────────────────────┐
│                     微服務架構圖                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                        API Gateway                             │
│              Nginx + Kong/Zuul + Rate Limiting                 │
│                    Load Balancer + SSL                         │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ Auth Service│  │ User Service│  │Admin Service│              │
│  │             │  │             │  │             │              │
│  │ JWT + OAuth │  │ Profile     │  │ Management  │              │
│  │ Multi-Auth  │  │ KYC         │  │ Config      │              │
│  │ Session     │  │ Permissions │  │ Monitoring  │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │Trade Service│  │Wallet Svc   │  │Price Service│              │
│  │             │  │             │  │             │              │
│  │ Orders      │  │ Balance     │  │ Market Data │              │
│  │ Matching    │  │ Deposit     │  │ History     │              │
│  │ History     │  │ Withdraw    │  │ Alerts      │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │Blockchain   │  │Notification │  │File Service │              │
│  │ Service     │  │ Service     │  │             │              │
│  │             │  │             │  │ KYC Docs    │              │
│  │ TRON/USDT   │  │ Email/SMS   │  │ Uploads     │              │
│  │ TRX Gas     │  │ Push        │  │ Images      │              │
│  │ Monitoring  │  │ Templates   │  │ Storage     │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│               Service Discovery & Config                        │
│                 Consul / Eureka + Config Server                │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                   Message Queue                                 │
│              RabbitMQ / Apache Kafka                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 核心微服務詳細設計

### 1. Authentication Service (認證服務)
**職責**: 用戶認證、授權、會話管理

#### API端點
```yaml
/api/v1/auth:
  POST /register:
    description: 用戶註冊
    request: 
      email: string
      password: string
      phone: string
      code: string
    response:
      token: string
      user: UserInfo
      
  POST /login:
    description: 用戶登入
    request:
      email: string
      password: string
      mfa_code?: string
    response:
      access_token: string
      refresh_token: string
      expires_in: number
      
  POST /logout:
    description: 用戶登出
    headers:
      Authorization: Bearer <token>
    response:
      message: string
      
  POST /refresh:
    description: 刷新Token
    request:
      refresh_token: string
    response:
      access_token: string
      expires_in: number
      
  POST /verify-email:
    description: 郵箱驗證
    request:
      token: string
    response:
      success: boolean
      
  POST /reset-password:
    description: 重設密碼
    request:
      email: string
    response:
      message: string
```

#### 數據模型
```sql
-- 用戶認證表
CREATE TABLE auth_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_secret VARCHAR(255),
    status ENUM('active', 'inactive', 'locked') DEFAULT 'inactive',
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 會話管理表
CREATE TABLE user_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    refresh_token_hash VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES auth_users(id)
);
```

### 2. User Service (用戶服務)
**職責**: 用戶資料管理、KYC處理、權限管理

#### API端點
```yaml
/api/v1/users:
  GET /profile:
    description: 獲取用戶資料
    headers:
      Authorization: Bearer <token>
    response:
      user: UserProfile
      kyc_status: string
      
  PUT /profile:
    description: 更新用戶資料
    headers:
      Authorization: Bearer <token>
    request:
      first_name: string
      last_name: string
      birth_date: string
      address: Address
    response:
      user: UserProfile
      
  POST /kyc/submit:
    description: 提交KYC資料
    headers:
      Authorization: Bearer <token>
    request:
      document_type: string
      document_number: string
      document_front: file
      document_back: file
      selfie: file
    response:
      kyc_id: string
      status: string
      
  GET /kyc/status:
    description: 查詢KYC狀態
    headers:
      Authorization: Bearer <token>
    response:
      status: string
      submitted_at: string
      reviewed_at?: string
      reject_reason?: string
```

### 3. Trading Service (交易服務)
**職責**: 交易撮合、訂單管理、交易記錄

#### API端點
```yaml
/api/v1/trading:
  POST /orders:
    description: 創建訂單
    headers:
      Authorization: Bearer <token>
    request:
      type: 'buy' | 'sell'
      amount: decimal
      price?: decimal
      order_type: 'market' | 'limit'
    response:
      order_id: string
      status: string
      
  GET /orders:
    description: 查詢訂單列表
    headers:
      Authorization: Bearer <token>
    params:
      status?: string
      type?: string
      page?: number
      size?: number
    response:
      orders: Order[]
      pagination: Pagination
      
  DELETE /orders/{orderId}:
    description: 取消訂單
    headers:
      Authorization: Bearer <token>
    response:
      success: boolean
      
  GET /trades:
    description: 交易記錄
    headers:
      Authorization: Bearer <token>
    params:
      start_date?: string
      end_date?: string
      page?: number
      size?: number
    response:
      trades: Trade[]
      pagination: Pagination
```

### 4. Wallet Service (錢包服務)
**職責**: 錢包管理、充值提現、餘額查詢

#### API端點
```yaml
/api/v1/wallet:
  GET /balance:
    description: 查詢錢包餘額
    headers:
      Authorization: Bearer <token>
    response:
      usdt_balance: decimal
      trx_balance: decimal
      frozen_balance: decimal
      
  POST /deposit:
    description: 生成充值地址
    headers:
      Authorization: Bearer <token>
    request:
      currency: 'USDT' | 'TRX'
    response:
      address: string
      qr_code: string
      
  POST /withdraw:
    description: 申請提現
    headers:
      Authorization: Bearer <token>
    request:
      currency: 'USDT' | 'TRX'
      amount: decimal
      to_address: string
      password: string
    response:
      withdraw_id: string
      status: string
      
  GET /transactions:
    description: 交易記錄
    headers:
      Authorization: Bearer <token>
    params:
      type?: 'deposit' | 'withdraw'
      status?: string
      page?: number
      size?: number
    response:
      transactions: Transaction[]
      pagination: Pagination
```

### 5. Price Service (價格服務)
**職責**: 價格管理、歷史數據、市場數據

#### API端點
```yaml
/api/v1/price:
  GET /current:
    description: 獲取當前價格
    response:
      usdt_cny: decimal
      usdt_usd: decimal
      updated_at: string
      
  GET /history:
    description: 歷史價格數據
    params:
      period: '1m' | '5m' | '1h' | '1d'
      start_time?: string
      end_time?: string
      limit?: number
    response:
      prices: PricePoint[]
      
  POST /alerts:
    description: 設置價格提醒
    headers:
      Authorization: Bearer <token>
    request:
      price: decimal
      condition: 'above' | 'below'
      enabled: boolean
    response:
      alert_id: string
```

### 6. Blockchain Service (區塊鏈服務)
**職責**: 區塊鏈交互、USDT/TRX處理、交易監控

#### API端點
```yaml
/api/v1/blockchain:
  POST /transfer:
    description: 發起鏈上轉帳
    request:
      from_address: string
      to_address: string
      amount: decimal
      currency: 'USDT' | 'TRX'
      private_key: string
    response:
      transaction_id: string
      status: string
      
  GET /transaction/{txId}:
    description: 查詢交易狀態
    response:
      transaction: BlockchainTransaction
      confirmations: number
      status: string
      
  GET /address/{address}/balance:
    description: 查詢地址餘額
    response:
      usdt_balance: decimal
      trx_balance: decimal
      
  POST /address/generate:
    description: 生成新地址
    response:
      address: string
      private_key: string
      public_key: string
```

## API設計規範

### 1. RESTful API設計原則
- **資源導向**: URL代表資源，HTTP方法代表操作
- **無狀態**: 每個請求包含完整信息，不依賴服務器狀態
- **統一接口**: 使用標準HTTP方法和狀態碼
- **分層系統**: 客戶端無需知道中間層結構

### 2. URL命名規範
```
基礎格式: /api/{version}/{resource}[/{resource-id}][/{sub-resource}]

示例:
GET /api/v1/users                    # 獲取用戶列表
GET /api/v1/users/123                # 獲取指定用戶
GET /api/v1/users/123/orders         # 獲取用戶的訂單
POST /api/v1/orders                  # 創建訂單
PUT /api/v1/orders/456               # 更新訂單
DELETE /api/v1/orders/456            # 刪除訂單
```

### 3. HTTP方法使用規範
| 方法 | 用途 | 冪等性 | 安全性 |
|------|------|--------|--------|
| GET | 查詢資源 | ✓ | ✓ |
| POST | 創建資源 | ✗ | ✗ |
| PUT | 更新資源(全量) | ✓ | ✗ |
| PATCH | 更新資源(部分) | ✗ | ✗ |
| DELETE | 刪除資源 | ✓ | ✗ |

### 4. HTTP狀態碼規範
```
成功響應:
200 OK - 請求成功
201 Created - 資源創建成功
202 Accepted - 請求已接受，異步處理
204 No Content - 成功但無返回內容

客戶端錯誤:
400 Bad Request - 請求參數錯誤
401 Unauthorized - 未授權
403 Forbidden - 權限不足
404 Not Found - 資源不存在
409 Conflict - 資源衝突
422 Unprocessable Entity - 參數驗證失敗
429 Too Many Requests - 請求頻率過高

服務器錯誤:
500 Internal Server Error - 服務器內部錯誤
502 Bad Gateway - 網關錯誤
503 Service Unavailable - 服務不可用
```

### 5. 請求/響應格式規範

#### 統一響應格式
```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {
    // 實際數據
  },
  "timestamp": "2023-01-01T00:00:00Z",
  "request_id": "uuid"
}
```

#### 錯誤響應格式
```json
{
  "success": false,
  "code": 400,
  "message": "請求參數錯誤",
  "errors": [
    {
      "field": "email",
      "message": "郵箱格式不正確"
    }
  ],
  "timestamp": "2023-01-01T00:00:00Z",
  "request_id": "uuid"
}
```

#### 分頁響應格式
```json
{
  "success": true,
  "code": 200,
  "message": "查詢成功",
  "data": [
    // 數據列表
  ],
  "pagination": {
    "page": 1,
    "size": 20,
    "total": 100,
    "total_pages": 5,
    "has_next": true,
    "has_prev": false
  },
  "timestamp": "2023-01-01T00:00:00Z",
  "request_id": "uuid"
}
```

### 6. 認證與授權規範

#### JWT Token格式
```json
{
  "header": {
    "typ": "JWT",
    "alg": "HS256"
  },
  "payload": {
    "sub": "user_id",
    "email": "user@example.com",
    "roles": ["user"],
    "permissions": ["read", "write"],
    "iat": 1641024000,
    "exp": 1641110400,
    "iss": "usdt-platform",
    "aud": "usdt-platform-users"
  }
}
```

#### 請求頭規範
```
Authorization: Bearer <access_token>
Content-Type: application/json
Accept: application/json
X-Request-ID: <uuid>
X-Client-Version: <version>
```

### 7. 參數驗證規範

#### 通用驗證規則
```yaml
email:
  type: string
  format: email
  required: true
  
password:
  type: string
  minLength: 8
  maxLength: 64
  pattern: "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]"
  
amount:
  type: decimal
  minimum: 0.01
  maximum: 1000000
  scale: 8
  
page:
  type: integer
  minimum: 1
  default: 1
  
size:
  type: integer
  minimum: 1
  maximum: 100
  default: 20
```

### 8. API版本管理

#### 版本策略
- **URL版本**: `/api/v1/`, `/api/v2/`
- **向後兼容**: 舊版本至少支持6個月
- **廢棄通知**: 提前3個月通知API變更
- **平滑遷移**: 提供遷移指南和工具

#### 版本變更類型
- **Major**: 不向後兼容的變更 (v1 -> v2)
- **Minor**: 向後兼容的功能新增 (v1.1 -> v1.2)  
- **Patch**: 向後兼容的Bug修復 (v1.1.1 -> v1.1.2)

## 服務間通信規範

### 1. 同步通信
- **HTTP/REST**: 服務間API調用
- **gRPC**: 高性能內部服務通信
- **負載均衡**: 客戶端負載均衡 + 服務發現

### 2. 異步通信
- **消息隊列**: RabbitMQ/Kafka
- **事件驅動**: 發布/訂閱模式
- **可靠性**: 消息持久化 + 確認機制

### 3. 數據一致性
- **強一致性**: 數據庫事務
- **最終一致性**: 分散式事務 + 補償機制
- **衝突解決**: 基於時間戳和業務規則

## API安全規範

### 1. 認證安全
- **Token過期**: Access Token 2小時，Refresh Token 7天
- **Token刷新**: 自動刷新機制
- **會話管理**: 異常登錄檢測

### 2. 授權安全
- **RBAC**: 基於角色的權限控制
- **資源級授權**: 細粒度權限控制
- **權限繼承**: 角色權限繼承機制

### 3. 數據安全
- **數據加密**: 敏感數據RSA加密
- **傳輸加密**: HTTPS/TLS1.3
- **輸入驗證**: 防止SQL注入、XSS攻擊

### 4. API限流
```yaml
限流策略:
  - 用戶級別: 1000 請求/分鐘
  - IP級別: 5000 請求/分鐘
  - API級別: 特定API獨立限流
  
限流算法:
  - 令牌桶算法
  - 滑動窗口計數器
  - 分佈式限流
```

## 監控與日誌

### 1. API監控指標
- **響應時間**: P50, P90, P95, P99
- **成功率**: 2xx響應比例
- **錯誤率**: 4xx, 5xx響應比例
- **QPS**: 每秒請求數

### 2. 日誌格式
```json
{
  "timestamp": "2023-01-01T00:00:00Z",
  "level": "INFO",
  "service": "auth-service",
  "trace_id": "uuid",
  "span_id": "uuid",
  "user_id": "12345",
  "method": "POST",
  "path": "/api/v1/auth/login",
  "status": 200,
  "duration": 150,
  "message": "User login successful"
}
```

### 3. 鏈路追蹤
- **分散式追蹤**: Jaeger/Zipkin
- **Trace ID**: 請求全鏈路追蹤
- **Span**: 服務內部操作追蹤

這個微服務架構設計確保了系統的可擴展性、可維護性和高可用性，為USDT交易平台提供了堅實的技術基礎。