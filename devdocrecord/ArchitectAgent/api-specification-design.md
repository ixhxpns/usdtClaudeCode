# 用戶註冊系統 API 規範設計

## 概述

本文檔定義了用戶註冊系統的完整API規範，確保前後端數據交互的一致性和標準化。

## 基礎規範

### 請求格式
- **Content-Type**: `application/json`
- **字符編碼**: UTF-8
- **請求方法**: 按照RESTful設計原則
- **Base URL**: `/api/auth`

### 響應格式標準

#### 成功響應
```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {}, // 響應數據
  "timestamp": "2025-08-27T10:00:00Z",
  "trace_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### 錯誤響應
```json
{
  "success": false,
  "code": 400,
  "message": "請求參數錯誤",
  "errors": [
    {
      "field": "email",
      "code": "INVALID_FORMAT", 
      "message": "郵箱格式不正確"
    }
  ],
  "timestamp": "2025-08-27T10:00:00Z",
  "trace_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

### HTTP 狀態碼規範

| 狀態碼 | 含義 | 使用場景 |
|--------|------|----------|
| 200 | OK | 請求成功 |
| 201 | Created | 資源創建成功（註冊成功） |
| 400 | Bad Request | 請求參數錯誤 |
| 401 | Unauthorized | 未授權訪問 |
| 403 | Forbidden | 訪問被禁止 |
| 409 | Conflict | 資源衝突（用戶已存在） |
| 422 | Unprocessable Entity | 驗證失敗 |
| 429 | Too Many Requests | 請求頻率超限 |
| 500 | Internal Server Error | 服務器內部錯誤 |

### 錯誤碼標準

#### 認證相關錯誤碼
```json
{
  "AUTH_001": "無效的認證憑證",
  "AUTH_002": "認證令牌已過期",
  "AUTH_003": "認證令牌格式錯誤",
  "AUTH_004": "用戶賬戶已被鎖定",
  "AUTH_005": "用戶賬戶已被禁用",
  "AUTH_006": "郵箱未驗證",
  "AUTH_007": "MFA驗證失敗"
}
```

#### 註冊相關錯誤碼
```json
{
  "REG_001": "用戶名已存在",
  "REG_002": "郵箱已被註冊",
  "REG_003": "手機號已被註冊",
  "REG_004": "密碼強度不足",
  "REG_005": "驗證碼錯誤或已過期",
  "REG_006": "註冊頻率超限",
  "REG_007": "用戶名格式不正確",
  "REG_008": "郵箱格式不正確",
  "REG_009": "必須同意服務條款"
}
```

## 核心API端點規範

### 1. 用戶名可用性檢查

#### 端點信息
- **方法**: `GET`
- **路徑**: `/api/auth/check-username`
- **描述**: 檢查用戶名是否可用

#### 請求參數
```json
{
  "username": "john_doe" // Query parameter
}
```

#### 參數驗證規則
- `username`: 必填，4-20字符，只允許字母、數字、下劃線

#### 響應示例

##### 用戶名可用
```json
{
  "success": true,
  "code": 200,
  "message": "用戶名可用",
  "data": {
    "username": "john_doe",
    "available": true,
    "suggestions": []
  }
}
```

##### 用戶名不可用
```json
{
  "success": true,
  "code": 200, 
  "message": "用戶名已被使用",
  "data": {
    "username": "john_doe",
    "available": false,
    "suggestions": [
      "john_doe1",
      "john_doe2",
      "john_doe_2025"
    ]
  }
}
```

##### 頻率超限
```json
{
  "success": false,
  "code": 429,
  "message": "請求過於頻繁",
  "data": {
    "retry_after": 60
  }
}
```

#### cURL 示例
```bash
curl -X GET "https://api.example.com/api/auth/check-username?username=john_doe" \
  -H "Content-Type: application/json"
```

### 2. 郵箱可用性檢查

#### 端點信息
- **方法**: `GET`
- **路徑**: `/api/auth/check-email`
- **描述**: 檢查郵箱是否可用

#### 請求參數
```json
{
  "email": "user@example.com" // Query parameter
}
```

#### 參數驗證規則
- `email`: 必填，有效的郵箱格式

#### 響應示例

##### 郵箱可用
```json
{
  "success": true,
  "code": 200,
  "message": "郵箱可用",
  "data": {
    "email": "user@example.com",
    "available": true,
    "domain_valid": true,
    "mx_record_exists": true
  }
}
```

##### 郵箱已註冊
```json
{
  "success": true,
  "code": 200,
  "message": "郵箱已被註冊",
  "data": {
    "email": "user@example.com", 
    "available": false,
    "registered_at": "2024-01-15T08:30:00Z"
  }
}
```

#### cURL 示例
```bash
curl -X GET "https://api.example.com/api/auth/check-email?email=user@example.com" \
  -H "Content-Type: application/json"
```

### 3. 發送郵箱驗證碼

#### 端點信息
- **方法**: `POST`
- **路徑**: `/api/auth/send-verification-code`
- **描述**: 發送郵箱驗證碼

#### 請求體
```json
{
  "email": "user@example.com",
  "type": "registration", // registration, password_reset, email_change
  "language": "zh-CN" // 可選，默認為zh-CN
}
```

#### 參數驗證規則
- `email`: 必填，有效的郵箱格式
- `type`: 必填，枚舉值：registration, password_reset, email_change
- `language`: 可選，支持的語言代碼

#### 響應示例

##### 發送成功
```json
{
  "success": true,
  "code": 200,
  "message": "驗證碼已發送",
  "data": {
    "email": "user@example.com",
    "expires_in": 300,
    "can_resend_after": 60,
    "attempt_count": 1,
    "max_attempts": 3
  }
}
```

##### 頻率超限
```json
{
  "success": false,
  "code": 429,
  "message": "發送過於頻繁",
  "data": {
    "retry_after": 45,
    "daily_limit_exceeded": false
  }
}
```

#### cURL 示例
```bash
curl -X POST "https://api.example.com/api/auth/send-verification-code" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "type": "registration"
  }'
```

### 4. 用戶註冊

#### 端點信息
- **方法**: `POST`
- **路徑**: `/api/auth/register`
- **描述**: 用戶註冊

#### 請求體
```json
{
  "username": "john_doe",
  "email": "user@example.com",
  "password": "SecurePass123!",
  "phone": "+1234567890",
  "verification_code": "123456",
  "agree_terms": true,
  "marketing_consent": false,
  "referral_code": "ABC123",
  "preferred_language": "zh-CN",
  "timezone": "Asia/Shanghai"
}
```

#### 參數驗證規則
- `username`: 必填，4-20字符，只允許字母、數字、下劃線
- `email`: 必填，有效的郵箱格式
- `password`: 必填，8-128字符，必須包含大小寫字母、數字和特殊字符
- `phone`: 可選，有效的手機號格式
- `verification_code`: 必填，6位數字
- `agree_terms`: 必填，必須為true
- `marketing_consent`: 可選，布爾值
- `referral_code`: 可選，推薦碼
- `preferred_language`: 可選，語言偏好
- `timezone`: 可選，時區設置

#### 響應示例

##### 註冊成功
```json
{
  "success": true,
  "code": 201,
  "message": "註冊成功",
  "data": {
    "user": {
      "id": 123,
      "username": "john_doe",
      "email": "user@example.com",
      "phone": "+1234567890",
      "status": "active",
      "email_verified": true,
      "phone_verified": false,
      "mfa_enabled": false,
      "created_at": "2025-08-27T10:00:00Z",
      "profile": {
        "preferred_language": "zh-CN",
        "timezone": "Asia/Shanghai",
        "marketing_consent": false
      }
    },
    "auth": {
      "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "expires_in": 3600,
      "token_type": "Bearer"
    },
    "next_steps": [
      {
        "action": "verify_phone",
        "description": "驗證手機號碼",
        "optional": true
      },
      {
        "action": "setup_mfa", 
        "description": "設置雙因子認證",
        "optional": true
      }
    ]
  }
}
```

##### 驗證錯誤
```json
{
  "success": false,
  "code": 422,
  "message": "註冊資料驗證失敗",
  "errors": [
    {
      "field": "username",
      "code": "REG_001",
      "message": "用戶名已存在"
    },
    {
      "field": "password", 
      "code": "REG_004",
      "message": "密碼必須包含至少一個大寫字母"
    },
    {
      "field": "verification_code",
      "code": "REG_005", 
      "message": "驗證碼錯誤或已過期"
    }
  ]
}
```

#### cURL 示例
```bash
curl -X POST "https://api.example.com/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "user@example.com", 
    "password": "SecurePass123!",
    "phone": "+1234567890",
    "verification_code": "123456",
    "agree_terms": true,
    "marketing_consent": false
  }'
```

### 5. 用戶登錄

#### 端點信息
- **方法**: `POST`
- **路徑**: `/api/auth/login`
- **描述**: 用戶登錄

#### 請求體
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "mfa_code": "123456",
  "remember_me": true,
  "device_info": {
    "device_type": "web",
    "os": "Windows 10",
    "browser": "Chrome 118.0",
    "screen_resolution": "1920x1080"
  }
}
```

#### 參數驗證規則
- `email`: 必填，有效的郵箱格式
- `password`: 必填
- `mfa_code`: 條件必填（當用戶啟用MFA時）
- `remember_me`: 可選，布爾值
- `device_info`: 可選，設備信息

#### 響應示例

##### 登錄成功
```json
{
  "success": true,
  "code": 200,
  "message": "登錄成功",
  "data": {
    "user": {
      "id": 123,
      "username": "john_doe",
      "email": "user@example.com",
      "status": "active",
      "email_verified": true,
      "phone_verified": true,
      "mfa_enabled": true,
      "last_login_at": "2025-08-27T10:00:00Z"
    },
    "auth": {
      "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "expires_in": 3600,
      "token_type": "Bearer"
    },
    "session": {
      "session_id": "sess_1234567890",
      "device_registered": true,
      "trusted_device": false
    }
  }
}
```

##### 需要MFA驗證
```json
{
  "success": false,
  "code": 202,
  "message": "需要多因子認證",
  "data": {
    "require_mfa": true,
    "mfa_methods": ["totp", "sms"],
    "temp_token": "temp_eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

#### cURL 示例
```bash
curl -X POST "https://api.example.com/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "remember_me": true
  }'
```

## 數據模型規範

### User 模型
```typescript
interface User {
  id: number
  username: string
  email: string
  phone?: string
  status: 'inactive' | 'active' | 'suspended' | 'locked'
  email_verified: boolean
  phone_verified: boolean
  mfa_enabled: boolean
  last_login_at?: string // ISO 8601 format
  created_at: string      // ISO 8601 format
  updated_at: string      // ISO 8601 format
  profile?: UserProfile
}
```

### UserProfile 模型
```typescript
interface UserProfile {
  id: number
  user_id: number
  first_name?: string
  last_name?: string
  nickname?: string
  avatar?: string
  birth_date?: string    // YYYY-MM-DD format
  gender?: 'male' | 'female' | 'other'
  country?: string       // ISO 3166-1 alpha-2 country code
  city?: string
  address?: string
  postal_code?: string
  preferred_language: string  // ISO 639-1 language code
  timezone: string           // IANA timezone identifier
  marketing_consent: boolean
  created_at: string         // ISO 8601 format
  updated_at: string         // ISO 8601 format
}
```

### AuthToken 模型
```typescript
interface AuthTokens {
  access_token: string
  refresh_token: string
  expires_in: number     // Seconds until access_token expires
  token_type: 'Bearer'
}
```

### ValidationError 模型
```typescript
interface ValidationError {
  field: string          // The field that failed validation
  code: string           // Error code for programmatic handling
  message: string        // Human-readable error message
  value?: any           // The value that failed validation (optional)
}
```

## 安全考慮

### 請求安全
1. **HTTPS強制**: 所有API請求必須使用HTTPS
2. **CSRF保護**: 使用CSRF令牌保護狀態改變操作
3. **CORS配置**: 正確配置跨域訪問策略

### 認證安全
1. **JWT安全**: 使用強隨機密鑰簽名JWT，設置合理過期時間
2. **令牌刷新**: 實施安全的令牌刷新機制
3. **會話管理**: 支持會話撤銷和設備管理

### 輸入驗證
1. **服務端驗證**: 所有輸入必須在服務端進行驗證
2. **SQL注入防護**: 使用參數化查詢
3. **XSS防護**: 對輸出進行適當編碼

### 頻率限制
```yaml
rate_limits:
  username_check: 20/minute per IP
  email_check: 20/minute per IP
  send_verification: 1/minute per email, 10/hour per IP
  register: 3/hour per IP
  login: 10/minute per IP, 100/day per IP
```

## 測試規範

### 單元測試示例
```java
@Test
public void testUsernameAvailability_Success() {
    // Given
    String username = "test_user";
    when(userService.existsByUsername(username)).thenReturn(false);
    
    // When
    ResponseEntity<ApiResponse> response = authController.checkUsernameAvailability(username);
    
    // Then
    assertEquals(200, response.getStatusCodeValue());
    assertTrue(response.getBody().isSuccess());
    
    AvailabilityData data = (AvailabilityData) response.getBody().getData();
    assertTrue(data.isAvailable());
    assertEquals(username, data.getUsername());
}
```

### 集成測試示例
```java
@Test
@Transactional
public void testUserRegistration_FullFlow() {
    // 1. Check username availability
    // 2. Check email availability  
    // 3. Send verification code
    // 4. Complete registration
    // 5. Verify user created in database
    // 6. Verify email sent
    // 7. Verify JWT token issued
}
```

## 監控和日誌

### API監控指標
- 請求響應時間
- 成功/失敗率
- 頻率限制觸發次數
- 錯誤類型分佈

### 安全事件日誌
- 失敗的認證嘗試
- 頻率限制觸發
- 異常的註冊模式
- IP地址異常活動

## 版本控制

### API版本策略
- 使用語義版本控制：`v{major}.{minor}.{patch}`
- 在URL路徑中包含主版本號：`/api/v1/auth/register`
- 向後兼容性原則：次版本和補丁版本保持向後兼容

### 廢棄政策
- 提前6個月通知API廢棄
- 在響應頭中包含廢棄警告
- 提供遷移指南和時間表

---

**文檔版本**: 1.0.0  
**最後更新**: 2025-08-27  
**負責人**: Architect Agent