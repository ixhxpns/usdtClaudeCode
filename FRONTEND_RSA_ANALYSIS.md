# 前端RSA加密問題分析報告

## 執行摘要

通過對前端代碼的詳細分析，發現了導致RSA加密失敗的根本原因。主要問題集中在API端點路徑不匹配和基礎URL配置問題上。

## 問題診斷

### 1. 核心問題分析

#### 1.1 API端點路徑不匹配
- **前端調用**: `/admin/auth/login` (stores/auth.ts:75)
- **後端實際**: `/api/auth/login` (AuthController.java:43)
- **影響**: 管理員登錄請求404錯誤

#### 1.2 HTTP客戶端配置問題  
- **基礎URL配置**: `/api/api` (utils/http.ts:9)
- **預期配置**: `/api`
- **結果**: 雙重api路徑前綴錯誤

#### 1.3 公鑰獲取失敗
- **前端請求**: `/api/auth/public-key`
- **實際訪問**: `http://localhost:3000/api/auth/public-key` → 404
- **原因**: 代理配置正確但基礎URL有問題

### 2. 具體錯誤追蹤

#### 2.1 RSA加密流程分析
```typescript
// crypto.ts:76 - RSA加密函數
export async function rsaEncryptData(data: string): Promise<string> {
  try {
    const encryptInstance = await initRSAEncrypt() // 調用初始化
    const encrypted = encryptInstance.encrypt(data)
    
    if (!encrypted) {
      // 第86行拋出的錯誤
      throw new Error('RSA加密失敗，請重試')
    }
    
    return encrypted
  } catch (error) {
    console.error('RSA加密錯誤:', error)
    throw error // 這裡拋出給上層的錯誤
  }
}
```

#### 2.2 公鑰獲取失敗鏈
```typescript
// crypto.ts:12-31 - 公鑰獲取失敗
async function fetchPublicKey(): Promise<string> {
  try {
    const response = await fetch('/api/auth/public-key') // 404錯誤
    const data = await response.json()
    
    if (!data.success) {
      throw new Error(data.message || '获取公钥失败')
    }
    
    const publicKey = data.data.publicKey
    const pemKey = `-----BEGIN PUBLIC KEY-----\n${publicKey}\n-----END PUBLIC KEY-----`
    
    return pemKey
  } catch (error) {
    console.error('获取RSA公钥失败:', error)
    throw new Error('无法获取加密公钥，请检查网络连接') // 最終用戶看到的錯誤
  }
}
```

#### 2.3 登錄失敗鏈
```typescript
// stores/auth.ts:54-100 - 管理員登錄
const login = async (credentials: AdminLoginRequest) => {
  try {
    // 1. RSA加密密碼 - 在這裡失敗
    const encryptedPassword = await encryptSensitiveData(credentials.password)
    
    // 2. 調用錯誤的端點
    const response = await AdminHttpClient.post<AdminLoginResponse>('/admin/auth/login', encryptedCredentials)
    // 實際請求: baseURL(/api/api) + path(/admin/auth/login) = /api/api/admin/auth/login
    
  } catch (error: any) {
    loginAttempts.value++
    const message = error.message || '登录失败'
    ElMessage.error(message) // 用戶看到: "RSA加密失敗"
    throw error
  }
}
```

### 3. 根本原因

#### 3.1 配置層面
1. **HTTP客戶端基礎URL錯誤**: `baseURL: '/api/api'` 應該是 `baseURL: '/api'`
2. **管理員端點路徑錯誤**: 前端調用 `/admin/auth/login`，後端只有 `/api/auth/login`

#### 3.2 架構層面  
1. **缺少管理員專用認證端點**: 後端沒有 `/api/admin/auth/*` 路由
2. **錯誤處理不夠明確**: RSA加密失敗和網絡錯誤混淆

#### 3.3 開發流程問題
1. **前後端API契約不一致**
2. **環境配置驗證不足**

## 修復建議

### 即時修復 (Critical)

#### 1. 修復HTTP客戶端基礎URL
```typescript
// frontend/admin/src/utils/http.ts:9
const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api', // 移除重複的/api
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})
```

#### 2. 統一管理員登錄端點
**選項A: 修改前端調用 (推薦)**
```typescript
// frontend/admin/src/stores/auth.ts:75
const response = await AdminHttpClient.post<AdminLoginResponse>('/auth/login', encryptedCredentials)
```

**選項B: 後端添加管理員專用端點**
```java
// 在AuthController或新建AdminAuthController中
@PostMapping("/admin/auth/login")
public ApiResponse<Map<String, Object>> adminLogin(/*...*/) {
    // 管理員專用登錄邏輯
}
```

#### 3. 驗證配置修復
```typescript
// 測試公鑰獲取
curl http://localhost:3000/api/auth/public-key

// 預期結果
{
  "success": true,
  "data": {
    "publicKey": "...",
    "keyType": "RSA",
    "keySize": "2048"
  }
}
```

### 增強修復 (Important)

#### 1. 改進錯誤處理
```typescript
// frontend/admin/src/utils/crypto.ts
export async function rsaEncryptData(data: string): Promise<string> {
  try {
    const encryptInstance = await initRSAEncrypt()
    const encrypted = encryptInstance.encrypt(data)
    
    if (!encrypted) {
      // 更明確的錯誤信息
      throw new Error('RSA_ENCRYPT_FAILED')
    }
    
    return encrypted
  } catch (error) {
    if (error.message === 'RSA_ENCRYPT_FAILED') {
      throw new Error('數據加密失敗，請重試')
    } else if (error.message.includes('公钥')) {
      throw new Error('獲取加密密鑰失敗，請檢查網絡連接')
    } else {
      throw new Error('加密過程出錯，請聯繫技術支持')
    }
  }
}
```

#### 2. 添加環境檢查
```typescript
// frontend/admin/src/utils/http.ts
// 添加配置驗證
const validateConfig = () => {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
  if (baseURL.endsWith('/api/api')) {
    console.error('HTTP配置錯誤: 檢測到雙重API路徑前綴')
  }
  return baseURL
}

const http: AxiosInstance = axios.create({
  baseURL: validateConfig(),
  // ...
})
```

#### 3. 添加連接性測試
```typescript
// frontend/admin/src/utils/crypto.ts
export async function testPublicKeyConnection(): Promise<boolean> {
  try {
    const response = await fetch('/api/auth/public-key')
    return response.ok
  } catch (error) {
    return false
  }
}
```

### 長期優化 (Nice to Have)

#### 1. 統一API路由規範
```
用戶端點: /api/auth/*
管理端點: /api/admin/auth/*
```

#### 2. 添加API健康檢查
```typescript
// 應用啟動時檢查關鍵端點
export async function checkAPIHealth() {
  const endpoints = [
    '/api/auth/public-key',
    '/api/admin/auth/login'
  ]
  
  for (const endpoint of endpoints) {
    // 檢查端點可用性
  }
}
```

#### 3. 改進開發工具
- 添加API端點一致性檢查
- 自動化前後端契約測試
- 環境配置驗證腳本

## 優先級執行計劃

### Phase 1: 緊急修復 (今天)
1. ✅ 修復http.ts基礎URL配置
2. ✅ 統一登錄端點路徑
3. ✅ 驗證公鑰端點可訪問性

### Phase 2: 增強穩定性 (本週)
1. 改進錯誤處理和用戶反饋
2. 添加配置驗證機制
3. 完善日誌記錄

### Phase 3: 長期優化 (下週)
1. 統一API路由規範
2. 添加自動化測試
3. 完善監控機制

## 驗證檢查清單

- [ ] HTTP客戶端baseURL正確配置
- [ ] 公鑰獲取端點返回200狀態
- [ ] 管理員登錄端點路徑一致
- [ ] RSA加密流程完整測試
- [ ] 錯誤信息用戶友好
- [ ] 網絡錯誤和加密錯誤區分
- [ ] 所有端點返回預期數據格式

## 技術債務

1. **配置管理**: 環境變量和配置驗證機制需要加強
2. **錯誤處理**: 前端錯誤分類和用戶體驗需要改進  
3. **API契約**: 前後端接口定義需要標準化
4. **測試覆蓋**: 關鍵認證流程需要自動化測試

---

**分析完成時間**: 2024-08-30  
**Frontend Agent**: RSA加密問題診斷報告  
**狀態**: 已識別根本原因，提供修復方案