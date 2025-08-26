# USDT交易平台架構實現細節

## 項目結構總覽

### 後端項目結構 (Spring Boot)

```
backend/
├── src/main/java/com/usdttrading/
│   ├── UsdtTradingApplication.java          # 主啟動類
│   ├── config/                              # 配置類
│   │   ├── BlockchainConfig.java           # 區塊鏈配置
│   │   ├── CorsConfig.java                 # 跨域配置
│   │   ├── DatabaseConfig.java             # 數據庫配置
│   │   ├── MybatisMetaObjectHandler.java   # MyBatis自動填充
│   │   ├── RedisConfig.java                # Redis配置
│   │   ├── SecurityConfig.java             # 安全配置
│   │   └── SwaggerConfig.java              # API文檔配置
│   ├── controller/                         # 控制器層
│   ├── service/                           # 服務層
│   ├── repository/                        # 數據訪問層
│   │   ├── BaseMapper.java                # 基礎Mapper
│   │   ├── UserMapper.java                # 用戶Mapper
│   │   └── ...                           # 其他Mapper
│   ├── entity/                           # 實體類
│   │   ├── BaseEntity.java               # 基礎實體
│   │   ├── User.java                     # 用戶實體
│   │   └── ...                          # 其他實體
│   ├── dto/                             # 數據傳輸對象
│   │   ├── ApiResponse.java             # 統一響應格式
│   │   └── ...                         # 其他DTO
│   ├── security/                       # 安全模組
│   │   ├── JwtUtil.java               # JWT工具類
│   │   ├── PasswordEncoder.java       # 密碼編碼器
│   │   ├── RSAUtil.java               # RSA加密工具
│   │   └── StpInterfaceImpl.java      # Sa-Token權限實現
│   ├── blockchain/                    # 區塊鏈整合
│   ├── notification/                  # 通知服務
│   ├── utils/                        # 工具類
│   ├── exception/                    # 異常處理
│   └── enums/                        # 枚舉類
├── src/main/resources/
│   ├── application.yml               # 主配置文件
│   ├── application-dev.yml           # 開發環境配置
│   ├── application-prod.yml          # 生產環境配置
│   ├── mapper/                       # MyBatis映射文件
│   │   └── UserMapper.xml           # 用戶映射文件
│   ├── static/                      # 靜態資源
│   └── templates/                   # 模板文件
└── pom.xml                          # Maven依賴配置
```

### 前端項目結構 (Vue 3)

```
frontend/
├── admin/                           # 管理後台
│   ├── src/
│   │   ├── components/             # 公共組件
│   │   │   ├── common/            # 通用組件
│   │   │   ├── layout/            # 佈局組件
│   │   │   └── charts/            # 圖表組件
│   │   ├── views/                 # 頁面視圖
│   │   │   ├── dashboard/         # 儀表盤
│   │   │   ├── users/             # 用戶管理
│   │   │   ├── orders/            # 訂單管理
│   │   │   ├── wallets/           # 錢包管理
│   │   │   ├── withdrawals/       # 提款管理
│   │   │   ├── kyc/               # KYC審核
│   │   │   ├── settings/          # 系統設置
│   │   │   └── announcements/     # 公告管理
│   │   ├── router/                # 路由配置
│   │   ├── stores/                # 狀態管理
│   │   ├── api/                   # API接口
│   │   ├── utils/                 # 工具函數
│   │   ├── types/                 # TypeScript類型
│   │   ├── composables/           # 組合式函數
│   │   └── assets/                # 靜態資源
│   ├── package.json               # 項目配置
│   ├── vite.config.ts             # Vite配置
│   ├── Dockerfile                 # Docker配置
│   └── nginx.conf                 # Nginx配置
└── user/                          # 用戶端
    ├── src/
    │   ├── components/            # 公共組件
    │   ├── views/                 # 頁面視圖
    │   │   ├── dashboard/         # 用戶儀表盤
    │   │   ├── trading/           # 交易頁面
    │   │   ├── wallet/            # 錢包頁面
    │   │   ├── profile/           # 個人中心
    │   │   └── kyc/               # KYC驗證
    │   └── ...                    # 其他配置同admin
    ├── package.json               # 項目配置
    ├── vite.config.ts             # Vite配置
    ├── Dockerfile                 # Docker配置
    └── nginx.conf                 # Nginx配置
```

## 技術實現詳情

### 1. 數據庫連接與配置

#### MyBatis Plus配置
- **版本**: 3.5.4.1
- **特性**: 
  - 自動分頁插件
  - 樂觀鎖插件
  - 自動填充處理器
  - 邏輯刪除支持

#### 數據庫連接池
- **連接池**: Alibaba Druid
- **配置**: 
  - 初始連接數: 10
  - 最大連接數: 100
  - 監控頁面: `/druid/*`

### 2. 安全架構實現

#### JWT認證機制
- **框架**: Sa-Token 1.37.0
- **特性**:
  - Token自動續簽
  - 多設備登錄控制
  - 權限註解支持
  - 踢人下線功能

#### RSA加密實現
- **密鑰長度**: 2048位
- **用途**: 
  - 敏感數據加密存儲
  - 前端密碼傳輸加密
  - API簽名驗證

#### 密碼安全
- **算法**: BCrypt
- **特性**: 
  - 自適應哈希
  - 密碼強度檢查
  - 隨機鹽值生成

### 3. Redis緩存配置

#### 緩存策略
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      lettuce:
        pool:
          max-active: 200
          max-idle: 20
          min-idle: 5
```

#### 使用場景
- 用戶會話存儲
- API響應缓存
- 分布式鎖
- 消息隊列

### 4. 區塊鏈整合架構

#### TRON網絡配置
```yaml
blockchain:
  tron:
    use-testnet: true
    mainnet-url: https://api.trongrid.io
    testnet-url: https://api.shasta.trongrid.io
    usdt-contract-address: TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t
    confirmation-count: 19
```

#### 關鍵特性
- USDT (TRC20) 支持
- 自動Gas費管理
- 交易確認監控
- 錢包地址生成

### 5. 前端架構實現

#### Vue 3 + TypeScript
- **構建工具**: Vite 5.0.10
- **UI框架**: Element Plus 2.4.4
- **狀態管理**: Pinia 2.1.7
- **路由**: Vue Router 4.2.5

#### 開發體驗優化
- **自動導入**: unplugin-auto-import
- **組件自動註冊**: unplugin-vue-components
- **熱重載**: Vite HMR
- **TypeScript**: 完整類型支持

### 6. 容器化部署

#### Docker配置
- **後端**: OpenJDK 17 + Spring Boot
- **前端**: Node.js 18 + Nginx
- **數據庫**: MySQL 8.0
- **緩存**: Redis 7

#### Docker Compose服務
```yaml
services:
  - mysql: 數據庫服務
  - redis: 緩存服務  
  - backend: 後端API服務
  - admin-frontend: 管理後台
  - user-frontend: 用戶端
  - nginx: 反向代理
```

### 7. Nginx負載均衡

#### 配置特性
- **HTTPS**: 強制SSL加密
- **HSTS**: 安全傳輸
- **壓縮**: Gzip壓縮
- **緩存**: 靜態資源緩存
- **限流**: API請求限制

#### 路由規則
- `/api/*`: 後端API代理
- `/admin/*`: 管理後台靜態資源
- `/user/*`: 用戶端靜態資源
- `/uploads/*`: 文件上傳訪問

## 開發環境配置

### 本地開發啟動順序

1. **啟動基礎服務**
   ```bash
   # 啟動MySQL和Redis
   docker-compose up mysql redis -d
   ```

2. **啟動後端服務**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

3. **啟動前端服務**
   ```bash
   # 管理後台
   cd frontend/admin
   npm run dev

   # 用戶端
   cd frontend/user  
   npm run dev
   ```

### 生產環境部署

```bash
# 一鍵部署所有服務
docker-compose up -d

# 查看服務狀態
docker-compose ps

# 查看日誌
docker-compose logs -f backend
```

## 性能優化策略

### 1. 數據庫優化
- **索引策略**: 覆盖查詢常用字段
- **分頁查詢**: MyBatis Plus分頁插件
- **連接池**: Druid監控和調優
- **讀寫分離**: 主從數據庫配置

### 2. 緩存策略
- **多級緩存**: Redis + 本地緩存
- **緩存預熱**: 系統啟動時預載熱數據
- **緩存更新**: 寫入時同步更新
- **緩存雪崩**: 隨機過期時間

### 3. 前端優化
- **代碼分割**: 動態導入組件
- **資源壓縮**: Gzip + Brotli
- **CDN加速**: 靜態資源分發
- **緩存策略**: 長期緩存 + 版本控制

## 安全防護措施

### 1. 身份認證
- **多因子認證**: Google Authenticator
- **登錄限制**: IP白名單 + 設備指紋
- **會話管理**: 自動過期 + 強制下線
- **密碼策略**: 復雜度檢查 + 定期更換

### 2. API安全
- **請求簽名**: HMAC-SHA256簽名
- **頻率限制**: 分級限流策略
- **輸入驗證**: 參數校驗 + SQL注入防護
- **輸出過濾**: XSS防護 + 敏感信息過濾

### 3. 數據安全
- **加密存儲**: 敏感數據RSA加密
- **傳輸加密**: 全站HTTPS
- **數據備份**: 定期自動備份
- **審計日誌**: 完整操作記錄

## 監控與運維

### 1. 應用監控
- **健康檢查**: Spring Boot Actuator
- **性能指標**: JVM + 業務指標
- **鏈路追蹤**: 分布式追蹤
- **告警機制**: 多渠道通知

### 2. 日誌管理
- **日誌級別**: 分環境配置
- **日誌格式**: 結構化日誌
- **日誌收集**: 集中式存儲
- **日誌分析**: ELK Stack

### 3. 備份策略
- **數據備份**: 每日增量 + 每週全量
- **配置備份**: 版本控制
- **災難恢復**: RTO < 30分鐘
- **備份測試**: 月度恢復測試

這個架構實現為USDT交易平台提供了完整的技術解決方案，涵蓋了從開發到部署的全流程，確保系統的安全性、可靠性和可維護性。