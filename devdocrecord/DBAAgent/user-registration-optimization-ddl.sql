-- ==========================================
-- USDT交易平台用户注册系统数据库优化DDL
-- ==========================================
-- 版本: v2.1
-- 创建日期: 2025-08-27  
-- 作者: DBA Agent
-- 目的: 优化用户注册流程的数据库性能和完整性
-- 执行环境: MySQL 8.0+
-- 预计执行时间: 15-20分钟
-- ==========================================

-- 设置会话参数
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO';

USE usdt_trading_platform;

-- ==========================================
-- 1. 数据备份（安全措施）
-- ==========================================

-- 备份users表（执行前必须）
CREATE TABLE users_backup_20250827 AS SELECT * FROM users;

-- 记录当前统计数据（用于验证）
SELECT 
    COUNT(*) as total_users,
    COUNT(DISTINCT email) as unique_emails,
    COUNT(DISTINCT username) as unique_usernames_not_null
FROM users 
WHERE deleted = 0;

-- ==========================================
-- 2. 约束优化 - 用户名唯一性
-- ==========================================

-- 检查是否存在重复的username（清理数据）
SELECT username, COUNT(*) as count 
FROM users 
WHERE username IS NOT NULL 
  AND username != '' 
  AND deleted = 0
GROUP BY username 
HAVING COUNT(*) > 1;

-- 如果存在重复数据，需要手动处理后再执行以下约束
-- 为username添加条件唯一约束（允许NULL，但不允许重复值）
CREATE UNIQUE INDEX uk_users_username_not_null ON users (username) 
WHERE username IS NOT NULL AND username != '' AND deleted = 0;

-- ==========================================
-- 3. 索引优化 - 注册流程专用索引
-- ==========================================

-- 用户名和邮箱可用性检查专用索引
CREATE INDEX idx_users_availability_check ON users (email, username, status, deleted);

-- 邮箱验证状态查询优化索引  
CREATE INDEX idx_users_email_verification ON users (email, email_verified, status, deleted);

-- 注册时间和状态复合索引
CREATE INDEX idx_users_registration_stats ON users (created_at, status, deleted);

-- 用户登录查询优化索引（支持邮箱登录）
CREATE INDEX idx_users_login_email ON users (email, status, email_verified, deleted);

-- 用户登录查询优化索引（支持用户名登录）
CREATE INDEX idx_users_login_username ON users (username, status, email_verified, deleted) 
WHERE username IS NOT NULL AND username != '';

-- ==========================================
-- 4. 性能优化索引
-- ==========================================

-- 用户统计和分页查询优化
CREATE INDEX idx_users_admin_list ON users (status, role_id, created_at DESC, deleted);

-- 活跃用户查询优化
CREATE INDEX idx_users_activity ON users (last_login_at, status, deleted);

-- 优化角色关联查询
CREATE INDEX idx_users_role_query ON users (role_id, status, deleted);

-- ==========================================
-- 5. 数据完整性约束增强
-- ==========================================

-- 确保email格式验证（MySQL 8.0+ 支持）
ALTER TABLE users ADD CONSTRAINT chk_users_email_format 
CHECK (email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$');

-- 确保username格式验证（字母、数字、下划线，4-20字符）
ALTER TABLE users ADD CONSTRAINT chk_users_username_format 
CHECK (username IS NULL OR (username REGEXP '^[A-Za-z0-9_]{4,20}$'));

-- 确保密码哈希不为空
ALTER TABLE users ADD CONSTRAINT chk_users_password_hash_not_empty 
CHECK (password_hash IS NOT NULL AND password_hash != '');

-- 确保盐值不为空
ALTER TABLE users ADD CONSTRAINT chk_users_salt_not_empty 
CHECK (salt IS NOT NULL AND salt != '');

-- ==========================================
-- 6. 数据一致性触发器
-- ==========================================

DELIMITER //

-- 删除已存在的触发器（如果有）
DROP TRIGGER IF EXISTS tr_users_before_insert//
DROP TRIGGER IF EXISTS tr_users_before_update//

-- 用户创建时自动设置默认值
CREATE TRIGGER tr_users_before_insert
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
    -- 如果username为空字符串，设为NULL
    IF NEW.username = '' THEN
        SET NEW.username = NULL;
    END IF;
    
    -- 确保邮箱小写存储
    SET NEW.email = LOWER(TRIM(NEW.email));
    
    -- 设置默认角色（如果未指定）
    IF NEW.role_id IS NULL THEN
        SET NEW.role_id = 3; -- 普通用户角色
    END IF;
    
    -- 设置默认状态
    IF NEW.status IS NULL THEN
        SET NEW.status = 'INACTIVE'; -- 注册后需要验证邮箱
    END IF;
END//

-- 用户更新时的数据一致性检查
CREATE TRIGGER tr_users_before_update
BEFORE UPDATE ON users
FOR EACH ROW
BEGIN
    -- 如果username被设为空字符串，设为NULL
    IF NEW.username = '' THEN
        SET NEW.username = NULL;
    END IF;
    
    -- 邮箱变更时确保小写并重置验证状态
    IF NEW.email != OLD.email THEN
        SET NEW.email = LOWER(TRIM(NEW.email));
        -- 邮箱变更时重置验证状态
        SET NEW.email_verified = FALSE;
    END IF;
    
    -- 用户名变更时的处理
    IF (OLD.username IS NULL AND NEW.username IS NOT NULL) OR 
       (OLD.username IS NOT NULL AND NEW.username IS NULL) OR
       (OLD.username != NEW.username) THEN
        -- 记录用户名变更（可以在这里添加日志逻辑）
        SET NEW.updated_at = CURRENT_TIMESTAMP;
    END IF;
    
    -- 更新版本号（乐观锁）
    SET NEW.version = OLD.version + 1;
END//

DELIMITER ;

-- ==========================================
-- 7. 性能优化视图
-- ==========================================

-- 用户基础信息查询视图（读库优化）
CREATE OR REPLACE VIEW v_users_basic_info AS
SELECT 
    u.id,
    u.username,
    u.email,
    u.status,
    u.email_verified,
    u.phone_verified,
    u.google_auth_enabled,
    u.role_id,
    r.name as role_name,
    u.created_at,
    u.last_login_at,
    u.login_attempts,
    u.locked_until
FROM users u
LEFT JOIN roles r ON u.role_id = r.id
WHERE u.deleted = 0;

-- 用户可用性检查视图（高频查询优化）
CREATE OR REPLACE VIEW v_users_availability AS
SELECT 
    id,
    username,
    email,
    status,
    deleted,
    created_at
FROM users
WHERE deleted = 0;

-- 用户统计视图
CREATE OR REPLACE VIEW v_users_statistics AS
SELECT 
    DATE(created_at) as reg_date,
    status,
    COUNT(*) as user_count,
    COUNT(CASE WHEN email_verified = TRUE THEN 1 END) as verified_count,
    COUNT(CASE WHEN username IS NOT NULL THEN 1 END) as with_username_count
FROM users
WHERE deleted = 0
GROUP BY DATE(created_at), status;

-- ==========================================
-- 8. 存储过程 - 用户可用性检查
-- ==========================================

DELIMITER //

-- 删除已存在的存储过程
DROP PROCEDURE IF EXISTS sp_check_username_availability//
DROP PROCEDURE IF EXISTS sp_check_email_availability//

-- 检查用户名可用性
CREATE PROCEDURE sp_check_username_availability(
    IN p_username VARCHAR(50),
    OUT p_available BOOLEAN
)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE user_count INT DEFAULT 0;
    
    -- 检查用户名是否已存在
    SELECT COUNT(*) INTO user_count
    FROM users 
    WHERE username = p_username 
      AND deleted = 0;
    
    -- 设置可用性结果
    SET p_available = (user_count = 0);
END//

-- 检查邮箱可用性
CREATE PROCEDURE sp_check_email_availability(
    IN p_email VARCHAR(100),
    OUT p_available BOOLEAN
)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE user_count INT DEFAULT 0;
    
    -- 检查邮箱是否已存在
    SELECT COUNT(*) INTO user_count
    FROM users 
    WHERE email = LOWER(TRIM(p_email))
      AND deleted = 0;
    
    -- 设置可用性结果
    SET p_available = (user_count = 0);
END//

DELIMITER ;

-- ==========================================
-- 9. 性能基准测试函数
-- ==========================================

DELIMITER //

-- 创建性能测试存储过程
DROP PROCEDURE IF EXISTS sp_performance_test_user_queries//

CREATE PROCEDURE sp_performance_test_user_queries()
READS SQL DATA
BEGIN
    DECLARE start_time, end_time BIGINT;
    DECLARE test_email VARCHAR(100) DEFAULT 'test@example.com';
    DECLARE test_username VARCHAR(50) DEFAULT 'testuser123';
    
    -- 测试邮箱查询性能
    SET start_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    SELECT COUNT(*) FROM users WHERE email = test_email AND deleted = 0;
    SET end_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    SELECT CONCAT('Email query time: ', (end_time - start_time), ' microseconds') as result;
    
    -- 测试用户名查询性能
    SET start_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    SELECT COUNT(*) FROM users WHERE username = test_username AND deleted = 0;
    SET end_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    SELECT CONCAT('Username query time: ', (end_time - start_time), ' microseconds') as result;
    
    -- 测试用户列表查询性能
    SET start_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    SELECT * FROM v_users_basic_info WHERE status = 'ACTIVE' LIMIT 20;
    SET end_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    SELECT CONCAT('User list query time: ', (end_time - start_time), ' microseconds') as result;
END//

DELIMITER ;

-- ==========================================
-- 10. 数据验证和完整性检查
-- ==========================================

-- 检查约束是否成功创建
SELECT 
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE,
    TABLE_NAME
FROM information_schema.TABLE_CONSTRAINTS 
WHERE TABLE_SCHEMA = 'usdt_trading_platform' 
  AND TABLE_NAME = 'users'
  AND CONSTRAINT_NAME LIKE 'chk_users_%';

-- 检查索引是否成功创建
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    NON_UNIQUE,
    INDEX_TYPE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'usdt_trading_platform' 
  AND TABLE_NAME = 'users'
  AND INDEX_NAME LIKE 'idx_users_%'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- 检查触发器是否成功创建
SELECT 
    TRIGGER_NAME,
    EVENT_MANIPULATION,
    ACTION_TIMING
FROM information_schema.TRIGGERS 
WHERE TRIGGER_SCHEMA = 'usdt_trading_platform' 
  AND TRIGGER_NAME LIKE 'tr_users_%';

-- 检查视图是否成功创建
SELECT 
    TABLE_NAME,
    VIEW_DEFINITION
FROM information_schema.VIEWS 
WHERE TABLE_SCHEMA = 'usdt_trading_platform' 
  AND TABLE_NAME LIKE 'v_users_%';

-- ==========================================
-- 11. 性能验证查询
-- ==========================================

-- 验证关键查询的执行计划
EXPLAIN FORMAT=JSON 
SELECT * FROM users 
WHERE email = 'test@example.com' 
  AND deleted = 0;

EXPLAIN FORMAT=JSON 
SELECT * FROM users 
WHERE username = 'testuser' 
  AND deleted = 0;

EXPLAIN FORMAT=JSON 
SELECT COUNT(*) FROM users 
WHERE status = 'ACTIVE' 
  AND deleted = 0;

-- 验证复合查询性能
EXPLAIN FORMAT=JSON 
SELECT u.*, r.name as role_name 
FROM users u 
LEFT JOIN roles r ON u.role_id = r.id 
WHERE u.status = 'ACTIVE' 
  AND u.deleted = 0 
ORDER BY u.created_at DESC 
LIMIT 20;

-- ==========================================
-- 12. 清理和设置
-- ==========================================

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 更新表统计信息
ANALYZE TABLE users;
ANALYZE TABLE roles;
ANALYZE TABLE user_profiles;

-- 刷新查询缓存
RESET QUERY CACHE;

-- ==========================================
-- 13. 完成验证
-- ==========================================

-- 统计优化后的数据
SELECT 
    'Optimization completed!' as message,
    NOW() as completion_time,
    (
        SELECT COUNT(*) 
        FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = 'usdt_trading_platform' 
          AND TABLE_NAME = 'users'
          AND INDEX_NAME LIKE 'idx_users_%'
    ) as new_indexes_count,
    (
        SELECT COUNT(*) 
        FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = 'usdt_trading_platform' 
          AND TABLE_NAME = 'users'
          AND CONSTRAINT_NAME LIKE 'chk_users_%'
    ) as new_constraints_count,
    (
        SELECT COUNT(*) 
        FROM information_schema.TRIGGERS 
        WHERE TRIGGER_SCHEMA = 'usdt_trading_platform' 
          AND TRIGGER_NAME LIKE 'tr_users_%'
    ) as new_triggers_count;

-- 记录优化完成
INSERT INTO system_config (config_key, config_value, data_type, category, description, is_public) 
VALUES (
    'user_registration_optimization_version', 
    '2.1', 
    'string', 
    'system', 
    '用户注册系统优化版本', 
    FALSE
) ON DUPLICATE KEY UPDATE 
    config_value = '2.1',
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO system_config (config_key, config_value, data_type, category, description, is_public) 
VALUES (
    'user_registration_optimization_time', 
    NOW(), 
    'string', 
    'system', 
    '用户注册系统优化时间', 
    FALSE
) ON DUPLICATE KEY UPDATE 
    config_value = NOW(),
    updated_at = CURRENT_TIMESTAMP;

-- ==========================================
-- 优化完成提示
-- ==========================================

SELECT 
    '用户注册系统数据库优化完成!' as message,
    'Please run performance tests to verify improvements' as next_step,
    '建议执行: CALL sp_performance_test_user_queries();' as test_command;