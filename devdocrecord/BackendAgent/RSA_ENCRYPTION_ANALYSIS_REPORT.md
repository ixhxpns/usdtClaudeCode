# RSA 加密架構問題分析報告

## 報告概述
**生成時間**: 2025-08-30  
**問題類型**: RSA加密失敗錯誤  
**影響範圍**: 管理員前端登錄功能  
**分析者**: Backend Agent  

## 問題描述

用戶報告管理員前端出現RSA加密失敗錯誤：
```
敏感数据加密失败: Error: RSA加密失败
at N0 (http://localhost:3000/js/index-CqXGDfew.js:46:190)
管理员登录失败: Error: RSA加密失败
```

## 架構分析

### 1. 後端RSA配置分析

#### 1.1 RSA工具類 (`/Users/jason/Projects/usdtClaudeCode/backend/src/main/java/com/usdttrading/security/RSAUtil.java`)

**配置狀態**: ✅ 正確實現
- 使用 HuTool RSA 加密庫
- 支持 2048-bit RSA 密鑰
- 實現了完整的加密/解密功能
- 密鑰從配置文件讀取

**核心方法**:
```java
@Value("${business.security.rsa.public-key:}")
private String publicKeyStr;

@Value("${business.security.rsa.private-key:}")
private String privateKeyStr;

public String encryptWithPublicKey(String data) // 公鑰加密
public String decryptWithPrivateKey(String encryptedData) // 私鑰解密
public String getPublicKeyString() // 獲取公鑰字符串
```

#### 1.2 配置文件分析

**主配置文件** (`application.yml`):
```yaml
business:
  security:
    rsa:
      public-key: ${RSA_PUBLIC_KEY:}
      private-key: ${RSA_PRIVATE_KEY:}
```

**環境變量配置** (`.env`):
```env
RSA_PUBLIC_KEY=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1pO7oCE5UOA...
RSA_PRIVATE_KEY=MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDWk7ug...
```

**狀態**: ✅ RSA密鑰已正確配置

### 2. 登錄API端點分析

#### 2.1 AuthController 登錄處理

**位置**: `/Users/jason/Projects/usdtClaudeCode/backend/src/main/java/com/usdttrading/controller/AuthController.java`

**登錄流程**:
```java
@PostMapping("/login")
public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    try {
        // 嘗試RSA解密登錄
        user = userService.loginWithRSADecryption(request.getEmail(), request.getPassword(), clientIp, userAgent);
    } catch (Exception rsaException) {
        // RSA解密失敗，嘗試普通登錄（兼容性）
        user = userService.login(request.getEmail(), request.getPassword(), clientIp, userAgent);
    }
}
```

**狀態**: ✅ 實現了RSA解密登錄與普通登錄的兼容性

#### 2.2 UserService RSA解密實現

**位置**: `/Users/jason/Projects/usdtClaudeCode/backend/src/main/java/com/usdttrading/service/UserService.java`

```java
@Transactional
public User loginWithRSADecryption(String email, String encryptedPassword, String clientIp, String userAgent) {
    try {
        // RSA解密密码
        String decryptedPassword = rsaUtil.decryptWithPrivateKey(encryptedPassword);
        // 调用普通登录方法
        return login(email, decryptedPassword, clientIp, userAgent);
    } catch (Exception e) {
        throw new BusinessException("DECRYPT_FAILED", "密碼解密失敗，請重試");
    }
}
```

**狀態**: ✅ RSA解密處理邏輯正確

#### 2.3 公鑰獲取API

**端點**: `GET /api/auth/public-key`
```java
@GetMapping("/public-key")
public ApiResponse<Map<String, Object>> getPublicKey() {
    try {
        String publicKey = rsaUtil.getPublicKeyString();
        Map<String, Object> result = new HashMap<>();
        result.put("publicKey", publicKey);
        result.put("keyType", "RSA");
        result.put("keySize", "2048");
        result.put("algorithm", "RSA/ECB/PKCS1Padding");
        return ApiResponse.success(result);
    } catch (Exception e) {
        return ApiResponse.error("獲取公钥失敗，請聯繫系統管理員");
    }
}
```

**狀態**: ✅ 公鑰API正確實現

### 3. 前端RSA加密分析

#### 3.1 管理員登錄前端實現

**位置**: `/Users/jason/Projects/usdtClaudeCode/frontend/admin/src/views/auth/LoginView.vue`

**登錄流程**:
```typescript
const handleLogin = async () => {
  await loginFormRef.value.validate()
  
  await authStore.login({
    username: loginForm.username,
    password: loginForm.password,
    mfa_code: loginForm.mfa_code || undefined
  })
}
```

#### 3.2 認證Store實現

**位置**: `/Users/jason/Projects/usdtClaudeCode/frontend/admin/src/stores/auth.ts`

**關鍵加密邏輯**:
```typescript
const login = async (credentials: AdminLoginRequest) => {
  // 加密密碼
  const encryptedPassword = await encryptSensitiveData(credentials.password)
  const encryptedCredentials = {
    ...credentials,
    password: encryptedPassword
  }

  const response = await AdminHttpClient.post<AdminLoginResponse>('/admin/auth/login', encryptedCredentials)
}
```

#### 3.3 加密工具類分析

**位置**: `/Users/jason/Projects/usdtClaudeCode/frontend/admin/src/utils/crypto.ts`

**核心加密函數**:
```typescript
async function fetchPublicKey(): Promise<string> {
  const response = await fetch('/api/auth/public-key')
  const data = await response.json()
  
  const publicKey = data.data.publicKey
  const pemKey = `-----BEGIN PUBLIC KEY-----\n${publicKey}\n-----END PUBLIC KEY-----`
  return pemKey
}

export async function rsaEncryptData(data: string): Promise<string> {
  const encryptInstance = await initRSAEncrypt()
  const encrypted = encryptInstance.encrypt(data)
  
  if (!encrypted) {
    throw new Error('RSA加密失败，请重试')
  }
  
  return encrypted
}
```

## 問題根因分析

### 🔍 關鍵問題發現

1. **API端點不一致問題**:
   - 管理員登錄調用: `/admin/auth/login`
   - 但RSA公鑰API: `/api/auth/public-key`
   - **普通用戶登錄端點**: `/api/auth/login` ✅ 存在並支持RSA
   - **管理員登錄端點**: `/admin/auth/login` ❌ 缺失

2. **管理員認證控制器缺失**:
   - 只有 `AuthController` 處理普通用戶認證
   - 缺少 `AdminController` 或管理員專用認證端點

3. **前後端API路由不匹配**:
   - 前端請求: `/admin/auth/login`
   - 後端實現: `/api/auth/login` (僅普通用戶)

### 🎯 具體錯誤原因

管理員前端嘗試調用不存在的API端點 `/admin/auth/login`，導致：
1. HTTP 404 錯誤或網路連接失敗
2. 前端加密函數無法正常處理伺服器響應
3. 導致"RSA加密失敗"錯誤

## 解決方案

### 方案一：創建管理員專用控制器（推薦）

創建 `AdminAuthController` 來處理管理員認證：

```java
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    
    private final UserService userService;
    private final RSAUtil rsaUtil;
    
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        // 管理員RSA登錄邏輯
        // 驗證管理員權限
        // 返回管理員專用Token
    }
    
    @GetMapping("/public-key")
    public ApiResponse<Map<String, Object>> getPublicKey() {
        // 復用RSAUtil獲取公鑰
        return rsaUtil.getPublicKeyString();
    }
}
```

### 方案二：統一認證端點（簡單快速）

修改前端管理員登錄調用統一的用戶認證API：

```typescript
// 修改 AdminHttpClient 基礎URL或直接調用
const response = await AdminHttpClient.post<AdminLoginResponse>('/api/auth/login', encryptedCredentials)
```

### 方案三：路由代理處理

在前端配置中添加API代理：

```javascript
// vite.config.js 或 vue.config.js
server: {
  proxy: {
    '/admin/auth': {
      target: 'http://localhost:8080/api/auth',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/admin\/auth/, '/auth')
    }
  }
}
```

## 推薦實施步驟

### 步驟1：創建管理員控制器
1. 創建 `AdminAuthController.java`
2. 實現管理員專用登錄邏輯
3. 添加權限驗證和審計日誌

### 步驟2：數據庫角色配置
1. 確保管理員用戶在 `users` 表中存在
2. 設置正確的 `role_id` (1=超級管理員, 2=管理員)
3. 驗證管理員賬戶狀態為 `ACTIVE`

### 步驟3：測試驗證
1. 測試RSA公鑰獲取API
2. 測試管理員RSA加密登錄
3. 驗證普通用戶登錄不受影響

### 步驟4：安全加固
1. 添加管理員專用的登錄頻率限制
2. 實施管理員會話管理
3. 添加管理員操作審計日誌

## 安全建議

1. **密鑰管理**:
   - 定期輪換RSA密鑰對
   - 使用更強的4096-bit RSA密鑰
   - 考慮使用HSM進行密鑰存儲

2. **認證增強**:
   - 實施雙因子認證(2FA)
   - 添加設備指紋識別
   - 實施IP白名單限制

3. **會話安全**:
   - 縮短管理員Token有效期
   - 實施並發會話控制
   - 添加會話異地檢測

## 監控建議

1. **加密監控**:
   - 監控RSA加密/解密成功率
   - 記錄加密失敗事件
   - 設置加密異常告警

2. **認證監控**:
   - 監控管理員登錄成功/失敗率
   - 記錄異常登錄嘗試
   - 設置暴力破解檢測

## 結論

問題根源是管理員前端調用了不存在的後端API端點。建議優先實施**方案一**，創建專用的管理員認證控制器，以提供更好的安全性和功能分離。同時建議加強RSA密鑰管理和認證安全機制。

---
**文檔版本**: v1.0  
**最後更新**: 2025-08-30  
**負責人**: Backend Agent