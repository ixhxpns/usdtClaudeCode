# 用戶註冊系統數據庫優化設計方案

## 數據庫架構概覽

### 當前數據庫環境分析
- **數據庫版本**: MySQL 8.0
- **存儲引擎**: InnoDB
- **字符集**: utf8mb4
- **排序規則**: utf8mb4_unicode_ci
- **隔離級別**: REPEATABLE READ

### 優化目標
1. **性能目標**: 註冊響應時間 < 500ms，並發支持 1000+ TPS
2. **擴展性目標**: 支持100萬+用戶，日增長10,000用戶
3. **可用性目標**: 99.99%可用性，RPO < 15分鐘，RTO < 5分鐘
4. **安全目標**: 數據加密，訪問控制，審計合規

## 表結構設計與優化

### 1. 核心用戶表（users）優化

#### 優化後表結構
```sql
CREATE TABLE users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '用戶ID',
    uuid CHAR(36) NOT NULL UNIQUE COMMENT '用戶UUID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用戶名',
    email VARCHAR(255) NOT NULL UNIQUE COMMENT '郵箱地址',
    email_hash CHAR(64) NOT NULL UNIQUE COMMENT '郵箱哈希（用於快速查找）',
    phone VARCHAR(20) NULL UNIQUE COMMENT '手機號碼（加密存儲）',
    password_hash VARCHAR(255) NOT NULL COMMENT '密碼哈希（Argon2）',
    salt VARCHAR(32) NOT NULL COMMENT '密碼鹽值',
    
    -- 狀態字段
    status ENUM('inactive', 'active', 'suspended', 'locked', 'deleted') 
           NOT NULL DEFAULT 'active' COMMENT '用戶狀態',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT '郵箱驗證狀態',
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT '手機驗證狀態',
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'MFA啟用狀態',
    
    -- 安全字段
    role_id TINYINT UNSIGNED NOT NULL DEFAULT 3 COMMENT '角色ID',
    last_login_at TIMESTAMP NULL COMMENT '最後登錄時間',
    last_login_ip VARBINARY(16) NULL COMMENT '最後登錄IP（二進制存儲）',
    login_attempts TINYINT UNSIGNED DEFAULT 0 COMMENT '登錄嘗試次數',
    locked_until TIMESTAMP NULL COMMENT '鎖定到期時間',
    
    -- 審計字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    created_by BIGINT UNSIGNED NULL COMMENT '創建者ID',
    updated_by BIGINT UNSIGNED NULL COMMENT '更新者ID',
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '樂觀鎖版本號',
    
    -- 索引定義
    INDEX idx_uuid (uuid),
    INDEX idx_email_hash (email_hash),
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_status_created (status, created_at),
    INDEX idx_login_time (last_login_at DESC),
    INDEX idx_login_attempts (login_attempts, locked_until),
    INDEX idx_role_status (role_id, status),
    
    -- 複合索引優化查詢
    INDEX idx_auth_login (email_hash, status, locked_until),
    INDEX idx_active_users (status, email_verified, created_at),
    
    -- 全文索引（如需要用戶搜索功能）
    FULLTEXT KEY ft_search (username, email)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  ROW_FORMAT=DYNAMIC
  COMMENT='用戶基礎信息表';
```

#### 關鍵優化說明

1. **UUID字段**: 添加UUID作為外部引用標識，避免暴露自增ID
2. **郵箱哈希**: 使用SHA-256哈希值加速郵箱查找，同時隱藏原始郵箱
3. **IP地址優化**: 使用VARBINARY存儲IP地址，支持IPv4/IPv6，節省空間
4. **枚舉優化**: 使用ENUM限制狀態值，提高查詢效率
5. **索引策略**: 精心設計複合索引，覆蓋常用查詢模式

### 2. 用戶資料表（user_profiles）

```sql
CREATE TABLE user_profiles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用戶ID',
    user_uuid CHAR(36) NOT NULL COMMENT '用戶UUID（冗餘字段，減少關聯查詢）',
    
    -- 基本信息
    first_name VARCHAR(50) NULL COMMENT '名',
    last_name VARCHAR(50) NULL COMMENT '姓',
    display_name VARCHAR(100) NULL COMMENT '顯示名稱',
    nickname VARCHAR(50) NULL COMMENT '昵稱',
    avatar_url VARCHAR(500) NULL COMMENT '頭像URL',
    
    -- 個人信息
    birth_date DATE NULL COMMENT '出生日期',
    gender ENUM('male', 'female', 'other', 'prefer_not_to_say') NULL COMMENT '性別',
    
    -- 地址信息
    country_code CHAR(2) NULL COMMENT 'ISO 3166-1 alpha-2 國家代碼',
    state_province VARCHAR(100) NULL COMMENT '省/州',
    city VARCHAR(100) NULL COMMENT '城市',
    postal_code VARCHAR(20) NULL COMMENT '郵政編碼',
    address_line1 VARCHAR(255) NULL COMMENT '地址行1',
    address_line2 VARCHAR(255) NULL COMMENT '地址行2',
    
    -- 偏好設置
    preferred_language CHAR(5) NOT NULL DEFAULT 'zh-CN' COMMENT 'ISO 639-1 語言代碼',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT 'IANA 時區標識符',
    date_format VARCHAR(20) DEFAULT 'YYYY-MM-DD' COMMENT '日期格式偏好',
    currency_code CHAR(3) DEFAULT 'USD' COMMENT 'ISO 4217 貨幣代碼',
    
    -- 隱私設置
    profile_visibility ENUM('public', 'friends', 'private') DEFAULT 'private' COMMENT '資料可見性',
    marketing_consent BOOLEAN DEFAULT FALSE COMMENT '營銷郵件同意',
    analytics_consent BOOLEAN DEFAULT TRUE COMMENT '分析數據同意',
    
    -- 審計字段
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '樂觀鎖版本號',
    
    -- 外鍵約束
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- 索引定義
    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_user_uuid (user_uuid),
    INDEX idx_country (country_code),
    INDEX idx_language (preferred_language),
    INDEX idx_created (created_at),
    
    -- 全文索引（支持用戶搜索）
    FULLTEXT KEY ft_profile_search (first_name, last_name, display_name, nickname)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='用戶詳細資料表';
```

### 3. 安全事件表（security_events）

```sql
CREATE TABLE security_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    event_uuid CHAR(36) NOT NULL UNIQUE COMMENT '事件UUID',
    user_id BIGINT UNSIGNED NULL COMMENT '關聯用戶ID',
    user_uuid CHAR(36) NULL COMMENT '用戶UUID',
    
    -- 事件信息
    event_type ENUM(
        'LOGIN_SUCCESS', 'LOGIN_FAILURE', 'LOGIN_LOCKED',
        'REGISTRATION', 'PASSWORD_CHANGE', 'EMAIL_VERIFICATION',
        'MFA_ENABLED', 'MFA_DISABLED', 'ACCOUNT_SUSPENDED',
        'SUSPICIOUS_ACTIVITY', 'IP_CHANGE', 'DEVICE_CHANGE',
        'SQL_INJECTION_ATTEMPT', 'XSS_ATTEMPT', 'RATE_LIMIT_EXCEEDED',
        'UNAUTHORIZED_ACCESS', 'DATA_EXPORT', 'DATA_DELETION'
    ) NOT NULL COMMENT '事件類型',
    
    event_description TEXT NOT NULL COMMENT '事件描述',
    
    -- 風險評估
    risk_level ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL COMMENT '風險等級',
    threat_score TINYINT UNSIGNED DEFAULT 0 COMMENT '威脅評分(0-100)',
    
    -- 請求信息
    ip_address VARBINARY(16) NULL COMMENT 'IP地址',
    ip_country CHAR(2) NULL COMMENT 'IP所屬國家',
    user_agent TEXT NULL COMMENT '用戶代理字符串',
    device_fingerprint CHAR(64) NULL COMMENT '設備指紋',
    
    -- 請求詳情
    request_method VARCHAR(10) NULL COMMENT 'HTTP方法',
    request_path VARCHAR(500) NULL COMMENT '請求路徑',
    request_params JSON NULL COMMENT '請求參數',
    response_status SMALLINT UNSIGNED NULL COMMENT '響應狀態碼',
    
    -- 處置信息
    action_taken VARCHAR(500) NULL COMMENT '採取的措施',
    resolved BOOLEAN DEFAULT FALSE COMMENT '是否已處理',
    resolved_at TIMESTAMP NULL COMMENT '處理時間',
    resolved_by BIGINT UNSIGNED NULL COMMENT '處理人員ID',
    
    -- 審計信息
    session_id VARCHAR(128) NULL COMMENT '會話ID',
    trace_id VARCHAR(128) NULL COMMENT '追蹤ID',
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 索引定義
    INDEX idx_user_event (user_id, event_type, created_at),
    INDEX idx_ip_time (ip_address, created_at),
    INDEX idx_event_type_risk (event_type, risk_level, created_at),
    INDEX idx_resolved (resolved, created_at),
    INDEX idx_trace_id (trace_id),
    
    -- 分區鍵（按月分區）
    PARTITION BY RANGE (TO_DAYS(created_at)) (
        PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
        PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
        PARTITION p202503 VALUES LESS THAN (TO_DAYS('2025-04-01')),
        PARTITION p202504 VALUES LESS THAN (TO_DAYS('2025-05-01')),
        PARTITION p202505 VALUES LESS THAN (TO_DAYS('2025-06-01')),
        PARTITION p202506 VALUES LESS THAN (TO_DAYS('2025-07-01')),
        PARTITION p_future VALUES LESS THAN MAXVALUE
    )
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='安全事件日誌表';
```

## 性能優化策略

### 1. 索引優化策略

#### 索引設計原則
```sql
-- 1. 高頻查詢索引覆蓋
-- 用戶登錄查詢
EXPLAIN SELECT id, password_hash, salt, status, locked_until 
FROM users 
WHERE email_hash = SHA2('user@example.com', 256) 
AND status IN ('active', 'inactive');

-- 覆蓋索引：避免回表查詢
CREATE INDEX idx_login_cover 
ON users(email_hash, status, id, password_hash, salt, locked_until);

-- 2. 複合索引順序優化
-- 基於查詢頻率和選擇性排序
CREATE INDEX idx_user_search 
ON users(status, email_verified, created_at DESC);

-- 3. 前綴索引優化長字段
CREATE INDEX idx_email_prefix 
ON users(email(20)); -- 只索引前20個字符

-- 4. 函數索引（MySQL 8.0特性）
CREATE INDEX idx_upper_username 
ON users((UPPER(username)));
```

#### 索引監控與維護
```sql
-- 索引使用情況分析
SELECT 
    OBJECT_SCHEMA,
    OBJECT_NAME,
    INDEX_NAME,
    COUNT_FETCH,
    COUNT_INSERT,
    COUNT_UPDATE,
    COUNT_DELETE,
    SUM_TIMER_FETCH/1000000000 as FETCH_TIME_MS
FROM performance_schema.table_io_waits_summary_by_index_usage 
WHERE OBJECT_SCHEMA = 'usdt_trading_platform'
ORDER BY COUNT_FETCH DESC;

-- 重複索引檢查
SELECT 
    a.TABLE_SCHEMA,
    a.TABLE_NAME,
    a.COLUMN_NAME,
    GROUP_CONCAT(a.INDEX_NAME) as INDEXES
FROM information_schema.STATISTICS a
JOIN information_schema.STATISTICS b ON 
    a.TABLE_SCHEMA = b.TABLE_SCHEMA AND
    a.TABLE_NAME = b.TABLE_NAME AND 
    a.COLUMN_NAME = b.COLUMN_NAME AND
    a.INDEX_NAME != b.INDEX_NAME
WHERE a.TABLE_SCHEMA = 'usdt_trading_platform'
GROUP BY a.TABLE_SCHEMA, a.TABLE_NAME, a.COLUMN_NAME
HAVING COUNT(*) > 1;

-- 定期重建索引（避免碎片化）
ALTER TABLE users ENGINE=InnoDB;
OPTIMIZE TABLE users;
```

### 2. 查詢優化策略

#### 查詢重構案例
```sql
-- 優化前：低效的OR查詢
SELECT * FROM users 
WHERE username = 'john_doe' OR email = 'john@example.com';

-- 優化後：使用UNION ALL
SELECT * FROM users WHERE username = 'john_doe'
UNION ALL
SELECT * FROM users WHERE email = 'john@example.com' AND username != 'john_doe';

-- 優化前：子查詢
SELECT * FROM users u 
WHERE u.id IN (
    SELECT user_id FROM user_profiles 
    WHERE country_code = 'US'
);

-- 優化後：連接查詢
SELECT u.* FROM users u
JOIN user_profiles p ON u.id = p.user_id
WHERE p.country_code = 'US';

-- 優化前：LIKE模糊查詢
SELECT * FROM users 
WHERE email LIKE '%@gmail.com';

-- 優化後：使用反向索引或全文索引
-- 方案1：添加email_domain字段
ALTER TABLE users ADD email_domain VARCHAR(255) 
GENERATED ALWAYS AS (SUBSTRING_INDEX(email, '@', -1)) STORED;
CREATE INDEX idx_email_domain ON users(email_domain);

SELECT * FROM users WHERE email_domain = 'gmail.com';

-- 方案2：全文索引
SELECT * FROM users 
WHERE MATCH(email) AGAINST('gmail.com' IN BOOLEAN MODE);
```

### 3. 分區策略

#### 時間範圍分區
```sql
-- 安全事件表按月分區
ALTER TABLE security_events 
PARTITION BY RANGE (TO_DAYS(created_at)) (
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
    PARTITION p202503 VALUES LESS THAN (TO_DAYS('2025-04-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 自動分區維護存儲過程
DELIMITER //
CREATE PROCEDURE CreateMonthlyPartition()
BEGIN
    DECLARE partition_name VARCHAR(20);
    DECLARE partition_date DATE;
    DECLARE next_month_date DATE;
    
    SET partition_date = DATE_SUB(CURDATE(), INTERVAL DAY(CURDATE())-1 DAY);
    SET partition_date = DATE_ADD(partition_date, INTERVAL 2 MONTH);
    SET next_month_date = DATE_ADD(partition_date, INTERVAL 1 MONTH);
    SET partition_name = CONCAT('p', DATE_FORMAT(partition_date, '%Y%m'));
    
    SET @sql = CONCAT('ALTER TABLE security_events 
                      ADD PARTITION (PARTITION ', partition_name,
                      ' VALUES LESS THAN (TO_DAYS(''', next_month_date, ''')))');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END //
DELIMITER ;

-- 定期執行分區維護
CREATE EVENT ev_monthly_partition
ON SCHEDULE EVERY 1 MONTH STARTS '2025-01-01 02:00:00'
DO CALL CreateMonthlyPartition();
```

#### 哈希分區（用戶表）
```sql
-- 大用戶量情況下的哈希分區
ALTER TABLE users 
PARTITION BY HASH(id) 
PARTITIONS 16; -- 根據硬件資源調整分區數量
```

## 數據庫連接池優化

### HikariCP配置優化
```yaml
spring:
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      # 連接池大小
      minimum-idle: 10              # 最小空閒連接數
      maximum-pool-size: 50         # 最大連接數
      
      # 連接超時設置
      connection-timeout: 20000     # 連接超時時間(ms)
      idle-timeout: 300000          # 空閒超時時間(ms)
      max-lifetime: 1200000         # 連接最大生命週期(ms)
      
      # 連接測試
      connection-test-query: SELECT 1
      validation-timeout: 3000      # 驗證超時時間(ms)
      
      # 連接池名稱
      pool-name: USDTTradingCP
      
      # 性能相關
      auto-commit: false            # 關閉自動提交
      transaction-isolation: TRANSACTION_READ_COMMITTED
      
      # 連接屬性
      data-source-properties:
        cachePrepStmts: true        # 緩存PreparedStatement
        prepStmtCacheSize: 250      # PreparedStatement緩存大小
        prepStmtCacheSqlLimit: 2048 # PreparedStatement SQL長度限制
        useServerPrepStmts: true    # 使用服務器端PreparedStatement
        useLocalSessionState: true # 使用本地會話狀態
        rewriteBatchedStatements: true # 重寫批量語句
        cacheResultSetMetadata: true # 緩存結果集元數據
        cacheServerConfiguration: true # 緩存服務器配置
        elideSetAutoCommits: true   # 省略多餘的setAutoCommit調用
        maintainTimeStats: false    # 不維護時間統計
        
  # JPA配置優化
  jpa:
    show-sql: false
    open-in-view: false           # 關閉OSIV，避免懶載入問題
    hibernate:
      ddl-auto: none              # 生產環境關閉DDL自動執行
    properties:
      hibernate:
        # 查詢優化
        jdbc.batch_size: 25       # JDBC批量大小
        jdbc.fetch_size: 100      # JDBC獲取大小
        
        # 緩存配置
        cache.use_second_level_cache: true
        cache.use_query_cache: true
        cache.region.factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
        
        # 統計信息
        generate_statistics: false
        session.events.log.LOG_QUERIES_SLOWER_THAN_MS: 1000
```

## 讀寫分離架構

### ShardingSphere配置
```yaml
spring:
  shardingsphere:
    datasource:
      names: master,slave1,slave2
      
      master:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://mysql-master:3306/usdt_trading_platform?useSSL=true&serverTimezone=Asia/Shanghai
        username: ${MASTER_DB_USER}
        password: ${MASTER_DB_PASSWORD}
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
          
      slave1:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://mysql-slave1:3306/usdt_trading_platform?useSSL=true&serverTimezone=Asia/Shanghai
        username: ${SLAVE_DB_USER}
        password: ${SLAVE_DB_PASSWORD}
        hikari:
          maximum-pool-size: 30
          minimum-idle: 10
          
      slave2:
        type: com.zaxxer.hikari.HikariDataSource
        driver-class-name: com.mysql.cj.jdbc.Driver
        jdbc-url: jdbc:mysql://mysql-slave2:3306/usdt_trading_platform?useSSL=true&serverTimezone=Asia/Shanghai
        username: ${SLAVE_DB_USER}
        password: ${SLAVE_DB_PASSWORD}
        hikari:
          maximum-pool-size: 30
          minimum-idle: 10
    
    rules:
      readwrite-splitting:
        data-sources:
          userdb:
            write-data-source-name: master
            read-data-source-names: 
              - slave1
              - slave2
            load-balancer-name: round_robin
            
    props:
      sql-show: false
      check-table-metadata-enabled: false
```

### 數據庫負載均衡策略
```java
@Configuration
public class DatabaseLoadBalanceConfig {
    
    // 自定義負載均衡算法
    @Bean
    public LoadBalanceAlgorithm weightedRoundRobinLoadBalance() {
        return new WeightedRoundRobinLoadBalanceAlgorithm();
    }
}

public class WeightedRoundRobinLoadBalanceAlgorithm implements LoadBalanceAlgorithm {
    
    private final Map<String, Integer> weights = Map.of(
        "slave1", 6, // 高配置服務器，權重更高
        "slave2", 4  // 標準配置服務器
    );
    
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    
    @Override
    public String getDataSource(String name, List<String> availableTargetNames) {
        List<String> weightedList = buildWeightedList(availableTargetNames);
        int index = currentIndex.getAndIncrement() % weightedList.size();
        return weightedList.get(index);
    }
    
    private List<String> buildWeightedList(List<String> dataSources) {
        List<String> weightedList = new ArrayList<>();
        for (String dataSource : dataSources) {
            Integer weight = weights.getOrDefault(dataSource, 1);
            for (int i = 0; i < weight; i++) {
                weightedList.add(dataSource);
            }
        }
        return weightedList;
    }
}
```

## 緩存策略優化

### 多級緩存架構
```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    // L1: 本地緩存（Caffeine）
    @Bean("localCache")
    public CacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
    
    // L2: 分佈式緩存（Redis）
    @Bean("distributedCache") 
    public CacheManager redisCacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
            
        return RedisCacheManager.builder(redisConnectionFactory())
            .cacheDefaults(config)
            .build();
    }
    
    // 複合緩存管理器
    @Bean
    @Primary
    public CacheManager compositeCacheManager(
            @Qualifier("localCache") CacheManager localCache,
            @Qualifier("distributedCache") CacheManager distributedCache) {
        
        CompositeCacheManager cacheManager = new CompositeCacheManager();
        cacheManager.setCacheManagers(Arrays.asList(localCache, distributedCache));
        cacheManager.setFallbackToNoOpCache(false);
        return cacheManager;
    }
}

// 緩存使用策略
@Service
public class UserCacheService {
    
    // 用戶基本信息：短期本地緩存
    @Cacheable(value = "users", key = "#userId", cacheManager = "localCache")
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
    
    // 用戶權限信息：長期分佈式緩存
    @Cacheable(value = "userPermissions", key = "#userId", cacheManager = "distributedCache")
    public List<String> getUserPermissions(Long userId) {
        return userPermissionRepository.findPermissionsByUserId(userId);
    }
    
    // 緩存更新策略
    @CacheEvict(value = {"users", "userPermissions"}, key = "#userId", allEntries = false)
    public void evictUserCache(Long userId) {
        // 緩存失效
    }
    
    // 緩存預熱
    @PostConstruct
    @Async
    public void warmupCache() {
        // 預載入熱點用戶數據
        List<Long> hotUserIds = getHotUserIds();
        hotUserIds.parallelStream().forEach(this::getUserById);
    }
}
```

## 數據備份與恢復策略

### 備份策略設計
```bash
#!/bin/bash
# 數據庫備份腳本

# 配置變量
DB_HOST="mysql-master"
DB_USER="backup_user"
DB_PASS="${BACKUP_PASSWORD}"
DB_NAME="usdt_trading_platform"
BACKUP_DIR="/var/backups/mysql"
DATE=$(date +%Y%m%d_%H%M%S)

# 創建備份目錄
mkdir -p ${BACKUP_DIR}/{full,incremental,binlog}

# 全量備份（每日）
full_backup() {
    echo "開始全量備份: ${DATE}"
    
    # 使用mysqldump進行全量備份
    mysqldump --host=${DB_HOST} \
              --user=${DB_USER} \
              --password=${DB_PASS} \
              --single-transaction \
              --routines \
              --triggers \
              --events \
              --hex-blob \
              --master-data=2 \
              --flush-logs \
              ${DB_NAME} | gzip > ${BACKUP_DIR}/full/full_backup_${DATE}.sql.gz
    
    if [ $? -eq 0 ]; then
        echo "全量備份成功: full_backup_${DATE}.sql.gz"
        
        # 上傳到雲存儲
        aws s3 cp ${BACKUP_DIR}/full/full_backup_${DATE}.sql.gz \
                  s3://usdt-db-backups/full/
        
        # 清理7天前的本地備份
        find ${BACKUP_DIR}/full -name "full_backup_*.sql.gz" -mtime +7 -delete
    else
        echo "全量備份失敗"
        send_alert "數據庫全量備份失敗"
        exit 1
    fi
}

# 增量備份（binlog）
incremental_backup() {
    echo "開始增量備份: ${DATE}"
    
    # 刷新binlog
    mysql --host=${DB_HOST} --user=${DB_USER} --password=${DB_PASS} \
          -e "FLUSH LOGS;"
    
    # 複製binlog文件
    BINLOG_DIR="/var/lib/mysql"
    CURRENT_BINLOG=$(mysql --host=${DB_HOST} --user=${DB_USER} --password=${DB_PASS} \
                          -e "SHOW MASTER STATUS\G" | grep File | awk '{print $2}')
    
    # 複製除當前binlog外的所有binlog
    for binlog in $(ls ${BINLOG_DIR}/mysql-bin.* | grep -v ${CURRENT_BINLOG}); do
        if [ ! -f ${BACKUP_DIR}/binlog/$(basename ${binlog}) ]; then
            cp ${binlog} ${BACKUP_DIR}/binlog/
            
            # 上傳到雲存儲
            aws s3 cp ${binlog} s3://usdt-db-backups/binlog/
        fi
    done
    
    echo "增量備份完成"
}

# 主備份邏輯
case "$1" in
    "full")
        full_backup
        ;;
    "incremental")
        incremental_backup
        ;;
    *)
        echo "用法: $0 {full|incremental}"
        exit 1
        ;;
esac
```

### 恢復策略
```bash
#!/bin/bash
# 數據庫恢復腳本

restore_full_backup() {
    local backup_file=$1
    local target_db=$2
    
    echo "開始恢復全量備份: ${backup_file}"
    
    # 停止應用服務
    systemctl stop usdt-trading-backend
    
    # 創建恢復數據庫
    mysql --host=${DB_HOST} --user=${DB_USER} --password=${DB_PASS} \
          -e "DROP DATABASE IF EXISTS ${target_db}; CREATE DATABASE ${target_db};"
    
    # 恢復數據
    if [[ ${backup_file} == *.gz ]]; then
        zcat ${backup_file} | mysql --host=${DB_HOST} --user=${DB_USER} \
                                   --password=${DB_PASS} ${target_db}
    else
        mysql --host=${DB_HOST} --user=${DB_USER} --password=${DB_PASS} \
              ${target_db} < ${backup_file}
    fi
    
    if [ $? -eq 0 ]; then
        echo "全量備份恢復成功"
    else
        echo "全量備份恢復失敗"
        exit 1
    fi
}

restore_incremental() {
    local start_position=$1
    local end_position=$2
    local target_db=$3
    
    echo "開始恢復增量數據: ${start_position} 到 ${end_position}"
    
    # 應用binlog
    mysqlbinlog --start-position=${start_position} \
                --stop-position=${end_position} \
                ${BACKUP_DIR}/binlog/mysql-bin.* | \
    mysql --host=${DB_HOST} --user=${DB_USER} --password=${DB_PASS} ${target_db}
    
    if [ $? -eq 0 ]; then
        echo "增量數據恢復成功"
    else
        echo "增量數據恢復失敗"
        exit 1
    fi
}

# Point-in-Time Recovery
point_in_time_recovery() {
    local recovery_time=$1
    local target_db=$2
    
    echo "執行時間點恢復到: ${recovery_time}"
    
    # 找到最近的全量備份
    latest_full_backup=$(ls -t ${BACKUP_DIR}/full/full_backup_*.sql.gz | head -n1)
    
    # 恢復全量備份
    restore_full_backup ${latest_full_backup} ${target_db}
    
    # 應用binlog到指定時間點
    mysqlbinlog --stop-datetime="${recovery_time}" \
                ${BACKUP_DIR}/binlog/mysql-bin.* | \
    mysql --host=${DB_HOST} --user=${DB_USER} --password=${DB_PASS} ${target_db}
    
    echo "時間點恢復完成"
}
```

## 監控與性能調優

### 性能監控指標
```sql
-- 1. 慢查詢監控
SELECT 
    query_time,
    lock_time,
    rows_sent,
    rows_examined,
    sql_text
FROM mysql.slow_log 
WHERE start_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
ORDER BY query_time DESC 
LIMIT 10;

-- 2. 連接數監控
SELECT 
    VARIABLE_NAME,
    VARIABLE_VALUE
FROM INFORMATION_SCHEMA.GLOBAL_STATUS 
WHERE VARIABLE_NAME IN (
    'Connections',
    'Max_used_connections',
    'Threads_connected',
    'Threads_running'
);

-- 3. InnoDB狀態監控
SELECT 
    VARIABLE_NAME,
    VARIABLE_VALUE
FROM INFORMATION_SCHEMA.GLOBAL_STATUS 
WHERE VARIABLE_NAME LIKE 'Innodb%'
AND VARIABLE_NAME IN (
    'Innodb_buffer_pool_read_requests',
    'Innodb_buffer_pool_reads',
    'Innodb_buffer_pool_pages_free',
    'Innodb_buffer_pool_pages_total',
    'Innodb_log_waits',
    'Innodb_deadlocks'
);

-- 4. 表鎖監控
SELECT 
    r.trx_id AS waiting_trx_id,
    r.trx_mysql_thread_id AS waiting_thread,
    r.trx_query AS waiting_query,
    b.trx_id AS blocking_trx_id,
    b.trx_mysql_thread_id AS blocking_thread,
    b.trx_query AS blocking_query
FROM information_schema.innodb_lock_waits w
INNER JOIN information_schema.innodb_trx b ON b.trx_id = w.blocking_trx_id
INNER JOIN information_schema.innodb_trx r ON r.trx_id = w.requesting_trx_id;
```

### MySQL配置優化
```cnf
[mysqld]
# 基本配置
server-id = 1
port = 3306
bind-address = 0.0.0.0

# 字符集配置
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# InnoDB配置優化
innodb_buffer_pool_size = 8G              # 設置為物理內存的70-80%
innodb_buffer_pool_instances = 8          # 緩衝池實例數
innodb_log_file_size = 1G                 # 日誌文件大小
innodb_log_files_in_group = 2             # 日誌文件組數量
innodb_log_buffer_size = 64M              # 日誌緩衝區大小
innodb_flush_log_at_trx_commit = 1        # 事務提交時刷新日誌
innodb_flush_method = O_DIRECT            # 避免雙重緩衝
innodb_file_per_table = 1                 # 每個表獨立表空間
innodb_read_io_threads = 8                # 讀IO線程數
innodb_write_io_threads = 8               # 寫IO線程數
innodb_thread_concurrency = 16           # InnoDB併發線程數

# 連接配置
max_connections = 1000                    # 最大連接數
max_connect_errors = 100                  # 最大連接錯誤數
connect_timeout = 10                      # 連接超時時間
interactive_timeout = 3600                # 交互式連接超時
wait_timeout = 3600                       # 非交互式連接超時

# 查詢緩存（MySQL 8.0已移除）
# query_cache_type = 1
# query_cache_size = 256M

# 慢查詢日誌
slow_query_log = 1
slow_query_log_file = /var/log/mysql/mysql-slow.log
long_query_time = 1                       # 慢查詢閾值（秒）
log_queries_not_using_indexes = 1         # 記錄未使用索引的查詢

# 二進制日誌
log-bin = mysql-bin
binlog_format = ROW                       # 使用行格式
binlog_row_image = MINIMAL                # 最小行映像
sync_binlog = 1                          # 每次提交同步binlog
expire_logs_days = 7                     # binlog保留天數

# 表級配置
table_open_cache = 2000                  # 表緩存大小
table_definition_cache = 1000            # 表定義緩存

# 排序和分組
sort_buffer_size = 2M                    # 排序緩衝區大小
join_buffer_size = 2M                    # 連接緩衝區大小
tmp_table_size = 64M                     # 內存臨時表大小
max_heap_table_size = 64M                # 堆表最大大小

# 安全配置
ssl-ca = /etc/mysql/certs/ca.pem
ssl-cert = /etc/mysql/certs/server-cert.pem
ssl-key = /etc/mysql/certs/server-key.pem
require_secure_transport = ON            # 強制SSL連接

[mysql]
default-character-set = utf8mb4

[client]
default-character-set = utf8mb4
```

## 數據遷移策略

### 在線遷移方案
```java
@Component
public class OnlineDataMigrationService {
    
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 雙寫策略：同時寫入新舊表結構
    @Transactional
    public void migrateUserData(Long userId) {
        try {
            // 讀取舊結構數據
            User oldUser = userRepository.findById(userId).orElseThrow();
            
            // 轉換為新結構
            User newUser = convertToNewStructure(oldUser);
            
            // 寫入新表結構
            userRepository.saveToNewStructure(newUser);
            
            // 驗證數據一致性
            if (!verifyDataConsistency(oldUser, newUser)) {
                throw new DataMigrationException("數據不一致");
            }
            
            // 標記遷移完成
            markMigrationComplete(userId);
            
        } catch (Exception e) {
            log.error("用戶數據遷移失敗: userId={}", userId, e);
            rollbackMigration(userId);
            throw e;
        }
    }
    
    // 批量遷移
    @Async
    public void batchMigrateUsers(List<Long> userIds) {
        int batchSize = 100;
        int totalBatches = (int) Math.ceil((double) userIds.size() / batchSize);
        
        for (int i = 0; i < totalBatches; i++) {
            int start = i * batchSize;
            int end = Math.min(start + batchSize, userIds.size());
            List<Long> batch = userIds.subList(start, end);
            
            try {
                processMigrationBatch(batch);
                updateMigrationProgress(i + 1, totalBatches);
            } catch (Exception e) {
                log.error("批量遷移失敗: batch={}", i, e);
                handleMigrationError(batch, e);
            }
            
            // 限制遷移速度，避免影響在線服務
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private User convertToNewStructure(User oldUser) {
        User newUser = new User();
        
        // 基本字段映射
        newUser.setId(oldUser.getId());
        newUser.setUuid(UUID.randomUUID().toString());
        newUser.setUsername(oldUser.getUsername());
        newUser.setEmail(oldUser.getEmail());
        
        // 添加新字段：郵箱哈希
        newUser.setEmailHash(DigestUtils.sha256Hex(oldUser.getEmail().toLowerCase()));
        
        // IP地址轉換為二進制存儲
        if (oldUser.getLastLoginIp() != null) {
            newUser.setLastLoginIp(convertIpToBinary(oldUser.getLastLoginIp()));
        }
        
        return newUser;
    }
    
    private byte[] convertIpToBinary(String ipAddress) {
        try {
            return InetAddress.getByName(ipAddress).getAddress();
        } catch (UnknownHostException e) {
            log.warn("無效IP地址: {}", ipAddress);
            return null;
        }
    }
}
```

---

**文檔版本**: 1.0.0  
**最後更新**: 2025-08-27  
**負責人**: Architect Agent  
**審核狀態**: 待審核