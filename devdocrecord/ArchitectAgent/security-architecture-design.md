# 安全架構設計

## 安全架構總覽

### 多層安全防護體系

```
┌─────────────────────────────────────────────────────────────────┐
│                        安全防護層級                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                 1. 網路安全層                                │ │
│  │                                                             │ │
│  │  CDN + DDoS防護 → WAF → SSL/TLS終端 → 負載均衡器            │ │
│  │                                                             │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                 2. 應用安全層                                │ │
│  │                                                             │ │
│  │  API網關 → 認證授權 → 輸入驗證 → 業務邏輯安全               │ │
│  │                                                             │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                 3. 數據安全層                                │ │
│  │                                                             │ │
│  │  傳輸加密 → 存儲加密 → 敏感數據處理 → 備份加密               │ │
│  │                                                             │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                 4. 運維安全層                                │ │
│  │                                                             │ │
│  │  訪問控制 → 審計日誌 → 監控告警 → 安全備份                   │ │
│  │                                                             │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 1. 身份認證與授權架構

### 1.1 JWT認證機制

#### JWT Token結構設計
```json
{
  "header": {
    "typ": "JWT",
    "alg": "HS256",
    "kid": "key-id-001"
  },
  "payload": {
    "sub": "user_id",
    "iss": "https://api.usdt-platform.com",
    "aud": ["usdt-platform-web", "usdt-platform-mobile"],
    "exp": 1641110400,
    "iat": 1641024000,
    "nbf": 1641024000,
    "jti": "token-uuid",
    "email": "user@example.com",
    "roles": ["user", "kyc_verified"],
    "permissions": ["trade", "withdraw"],
    "session_id": "session-uuid",
    "ip": "192.168.1.100",
    "device_id": "device-fingerprint"
  },
  "signature": "signed-hash"
}
```

#### Token生命周期管理
```yaml
Token類型配置:
  Access Token:
    過期時間: 2小時
    用途: API訪問
    存儲位置: 內存/SessionStorage
    
  Refresh Token:
    過期時間: 7天  
    用途: 刷新Access Token
    存儲位置: HttpOnly Cookie
    
  Device Token:
    過期時間: 30天
    用途: 設備記住功能
    存儲位置: 本地加密存儲

安全策略:
  - Token輪轉: 每次使用Refresh Token都會生成新的
  - 異常檢測: IP變更、設備變更自動失效
  - 並發限制: 同用戶最多5個活躍Token
  - 黑名單機制: 可疑Token立即加入黑名單
```

#### 多因子認證(MFA)實現
```typescript
// MFA認證流程
interface MFAConfig {
  email: {
    required: boolean;
    expiry: number; // 5分鐘
  };
  sms: {
    required: boolean;
    expiry: number; // 5分鐘
  };
  totp: {
    required: boolean;
    algorithm: 'SHA1' | 'SHA256';
    digits: 6;
    period: 30;
  };
  backup_codes: {
    enabled: boolean;
    count: 10;
    length: 8;
  };
}

// 認證等級
enum SecurityLevel {
  BASIC = 1,     // 用戶名密碼
  ENHANCED = 2,  // + 郵箱/簡訊驗證
  MAXIMUM = 3    // + TOTP
}
```

### 1.2 角色權限控制(RBAC)

#### 權限模型設計
```sql
-- 角色表
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(100),
    description TEXT,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 權限表
CREATE TABLE permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) UNIQUE NOT NULL,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 角色權限關聯表
CREATE TABLE role_permissions (
    role_id BIGINT,
    permission_id BIGINT,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id),
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

-- 用戶角色關聯表
CREATE TABLE user_roles (
    user_id BIGINT,
    role_id BIGINT,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT,
    expires_at TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES auth_users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);
```

#### 預定義角色權限
```yaml
系統角色:
  super_admin:
    description: 超級管理員
    permissions:
      - "*:*"  # 所有權限
      
  admin:
    description: 管理員
    permissions:
      - "user:read,update,freeze,unfreeze"
      - "trade:read,cancel"
      - "wallet:read"
      - "price:read,update"
      - "system:read,config"
      
  compliance_officer:
    description: 合規官
    permissions:
      - "kyc:read,approve,reject"
      - "withdrawal:read,approve,reject"
      - "user:read"
      - "audit:read"

用戶角色:
  verified_user:
    description: 已驗證用戶
    permissions:
      - "trade:create,read,cancel"
      - "wallet:read,deposit,withdraw"
      - "profile:read,update"
      - "kyc:submit"
      
  basic_user:
    description: 基礎用戶
    permissions:
      - "profile:read,update"
      - "kyc:submit"
      - "wallet:read,deposit"
      - "trade:read"  # 限制交易功能

  guest:
    description: 訪客
    permissions:
      - "price:read"
      - "market:read"
```

## 2. 數據加密安全

### 2.1 RSA非對稱加密

#### 密鑰管理策略
```yaml
密鑰規格:
  算法: RSA
  長度: 2048位 (最小), 4096位 (推薦)
  填充: OAEP (Optimal Asymmetric Encryption Padding)
  哈希: SHA-256

密鑰分類:
  系統密鑰對:
    用途: 系統級數據加密
    生成: 服務器端生成
    存儲: HSM硬件安全模組
    輪轉: 每年輪轉
    
  用戶密鑰對:
    用途: 用戶敏感數據加密
    生成: 客戶端生成
    存儲: 私鑰客戶端存儲，公鑰服務器存儲
    輪轉: 用戶主動更換

密鑰存儲:
  開發環境: 配置文件 (測試用)
  測試環境: 環境變量
  生產環境: HSM/KMS (Key Management Service)
```

#### 加密實施方案
```typescript
// 前端加密實現
class RSAEncryption {
  private publicKey: string;
  private privateKey: string;

  constructor() {
    this.initializeKeys();
  }

  // 敏感數據加密 (密碼、私人信息)
  encryptSensitiveData(data: string): string {
    const encrypted = JSEncrypt.encrypt(data, this.publicKey);
    return Base64.encode(encrypted);
  }

  // 表單數據加密
  encryptFormData(formData: any): EncryptedFormData {
    const sensitiveFields = ['password', 'privateKey', 'bankAccount'];
    const result = { ...formData };
    
    sensitiveFields.forEach(field => {
      if (result[field]) {
        result[field] = this.encryptSensitiveData(result[field]);
      }
    });
    
    return result;
  }
}

// 後端解密實現
@Service
public class RSADecryptionService {
    
    @Value("${rsa.private.key}")
    private String privateKeyString;
    
    public String decryptSensitiveData(String encryptedData) {
        try {
            byte[] encrypted = Base64.decode(encryptedData);
            RSAPrivateKey privateKey = loadPrivateKey(privateKeyString);
            
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SecurityException("解密失敗", e);
        }
    }
}
```

### 2.2 AES對稱加密

#### 數據庫敏感字段加密
```yaml
加密配置:
  算法: AES-256-GCM
  密鑰管理: 定期輪轉 (每季度)
  初始向量: 隨機生成，與密文一起存儲
  
加密字段:
  用戶信息:
    - password_hash (BCrypt + Salt)
    - phone_number (AES)
    - id_card_number (AES)
    - bank_account (AES)
    
  交易信息:
    - wallet_private_key (AES + 用戶密鑰)
    - withdrawal_address (AES)
    
  KYC信息:
    - document_number (AES)
    - personal_details (AES)
```

#### 實現示例
```java
@Component
public class AESEncryptionService {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    @Autowired
    private KeyManagementService keyService;
    
    public String encrypt(String plaintext, String keyId) {
        try {
            SecretKey secretKey = keyService.getKey(keyId);
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // IV + 密文一起存儲
            byte[] encryptedWithIv = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedWithIv, iv.length, ciphertext.length);
            
            return Base64.getEncoder().encodeToString(encryptedWithIv);
        } catch (Exception e) {
            throw new SecurityException("加密失敗", e);
        }
    }
}
```

## 3. HTTPS/TLS安全配置

### 3.1 SSL/TLS配置標準
```nginx
# Nginx SSL配置
server {
    listen 443 ssl http2;
    server_name api.usdt-platform.com;

    # SSL證書配置
    ssl_certificate /path/to/fullchain.pem;
    ssl_certificate_key /path/to/private.key;
    
    # SSL協議和加密套件
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    
    # SSL會話配置
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;
    ssl_session_tickets off;
    
    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;
    ssl_trusted_certificate /path/to/chain.pem;
    
    # 安全頭部
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';" always;
}
```

### 3.2 證書管理
```yaml
證書策略:
  類型: EV SSL證書 (Extended Validation)
  CA機構: Let's Encrypt / DigiCert
  算法: RSA-2048 或 ECDSA P-256
  有效期: 3個月 (自動續期)
  
域名配置:
  主域名:
    - api.usdt-platform.com
    - admin.usdt-platform.com
    - www.usdt-platform.com
    
  通配符證書:
    - *.usdt-platform.com
    
自動續期:
  工具: Certbot
  頻率: 每月檢查
  監控: 過期前30天告警
```

## 4. API安全防護

### 4.1 輸入驗證與過濾
```java
@RestController
@Validated
public class ApiController {
    
    // 參數驗證註解
    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(
        @Valid @RequestBody TransferRequest request,
        @Pattern(regexp = "^[0-9]+$") @RequestParam String userId) {
        
        // 業務邏輯
    }
}

// 請求DTO驗證
public class TransferRequest {
    @NotNull(message = "收款地址不能為空")
    @Pattern(regexp = "^T[A-Za-z0-9]{33}$", message = "TRON地址格式不正確")
    private String toAddress;
    
    @NotNull(message = "轉帳金額不能為空")
    @DecimalMin(value = "0.01", message = "最小轉帳金額0.01")
    @DecimalMax(value = "1000000", message = "最大轉帳金額1,000,000")
    @Digits(integer = 10, fraction = 8)
    private BigDecimal amount;
    
    @Length(max = 100, message = "備註長度不能超過100字符")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9\\s]*$", message = "備註只能包含中英文、數字和空格")
    private String memo;
}
```

### 4.2 SQL注入防護
```java
// 使用MyBatis Plus防護
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    // 正確：使用參數化查詢
    @Select("SELECT * FROM users WHERE email = #{email} AND status = 'active'")
    User findByEmail(@Param("email") String email);
    
    // 動態查詢使用QueryWrapper
    default List<User> findUsers(UserSearchCriteria criteria) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        
        if (StringUtils.hasText(criteria.getEmail())) {
            wrapper.eq("email", criteria.getEmail());
        }
        
        if (criteria.getStatus() != null) {
            wrapper.eq("status", criteria.getStatus());
        }
        
        return selectList(wrapper);
    }
}
```

### 4.3 XSS防護
```typescript
// 前端XSS防護
class SecurityHelper {
    // HTML內容清理
    static sanitizeHtml(html: string): string {
        const div = document.createElement('div');
        div.textContent = html;
        return div.innerHTML;
    }
    
    // 輸入過濾
    static filterInput(input: string): string {
        return input
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#x27;')
            .replace(/\//g, '&#x2F;');
    }
}

// Vue.js中使用
export default {
    methods: {
        displayUserContent(content: string) {
            // 使用v-text而不是v-html
            return SecurityHelper.sanitizeHtml(content);
        }
    }
}
```

### 4.4 CSRF防護
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/v1/auth/login", "/api/v1/auth/register")
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            
        return http.build();
    }
}
```

## 5. 區塊鏈安全

### 5.1 私鑰管理
```yaml
私鑰安全策略:
  生成:
    - 使用加密強度的隨機數生成器
    - 客戶端生成，避免網路傳輸
    - 符合TRON網路標準
    
  存儲:
    - 用戶私鑰: 客戶端加密存儲
    - 熱錢包私鑰: HSM硬件安全模組
    - 冷錢包私鑰: 離線物理隔離
    
  使用:
    - 私鑰永不明文傳輸
    - 簽名在安全環境中進行
    - 使用後立即清理內存
```

### 5.2 交易安全驗證
```typescript
// 交易前安全檢查
interface TransactionSecurityCheck {
  // 地址有效性檢查
  validateAddress(address: string): boolean;
  
  // 餘額檢查
  checkSufficientBalance(amount: BigNumber): boolean;
  
  // 手續費檢查
  checkGasFee(): Promise<boolean>;
  
  // 黑名單檢查
  checkBlacklist(address: string): Promise<boolean>;
  
  // 風險評估
  riskAssessment(transaction: Transaction): RiskLevel;
}

// 多重簽名驗證
class MultiSigVerification {
  async verifyTransaction(
    transaction: Transaction,
    signatures: Signature[]
  ): Promise<boolean> {
    // 需要至少2/3簽名通過
    const requiredSignatures = Math.ceil(this.totalSigners * 2 / 3);
    
    let validSignatures = 0;
    for (const sig of signatures) {
      if (await this.verifySignature(transaction, sig)) {
        validSignatures++;
      }
    }
    
    return validSignatures >= requiredSignatures;
  }
}
```

## 6. 監控與審計

### 6.1 安全事件監控
```yaml
監控規則:
  登入安全:
    - 異常登入地點檢測
    - 暴力破解檢測  
    - 多設備登入告警
    
  交易安全:
    - 大額交易告警
    - 異常頻繁交易
    - 可疑地址交易
    
  系統安全:
    - API異常調用
    - 權限越權操作
    - 敏感數據訪問
```

### 6.2 審計日誌
```json
{
  "timestamp": "2023-01-01T00:00:00Z",
  "event_type": "SECURITY_EVENT",
  "severity": "HIGH",
  "user_id": "12345",
  "session_id": "session-uuid",
  "ip_address": "192.168.1.100",
  "user_agent": "Mozilla/5.0...",
  "action": "WITHDRAW_REQUEST",
  "resource": "wallet/withdraw",
  "amount": "1000.00",
  "currency": "USDT",
  "destination": "TXxx...xxx",
  "status": "SUCCESS",
  "risk_score": 0.3,
  "additional_info": {
    "verification_method": "EMAIL_SMS_TOTP",
    "processing_time_ms": 1500
  }
}
```

## 7. 合規與法規要求

### 7.1 KYC/AML合規
```yaml
合規要求:
  身份驗證:
    - 政府頒發的身份證件
    - 活體檢測和人臉識別
    - 地址證明文件
    
  交易監控:
    - 可疑交易模式識別
    - 大額交易報告
    - 制裁名單檢查
    
  記錄保存:
    - 交易記錄保存7年
    - KYC文件保存5年
    - 審計軌跡完整性
```

### 7.2 數據保護合規
```yaml
數據保護:
  個人數據處理:
    - 明確告知數據使用目的
    - 獲得用戶明確同意
    - 提供數據刪除權限
    
  跨境數據傳輸:
    - 數據本地化要求
    - 傳輸加密保護
    - 合規報告提交
    
  數據洩露響應:
    - 24小時內識別洩露
    - 72小時內報告當局
    - 用戶及時通知機制
```

這個安全架構設計確保了USDT交易平台在各個層面的安全防護，滿足金融級系統的安全要求，為用戶資金和數據提供全方位保護。