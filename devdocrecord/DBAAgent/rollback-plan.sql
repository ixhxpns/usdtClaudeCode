-- ==========================================
-- USDT交易平台用户注册优化回滚脚本
-- ==========================================
-- 版本: v2.1 Rollback
-- 创建日期: 2025-08-27
-- 作者: DBA Agent
-- 目的: 紧急回滚用户注册系统数据库优化
-- 执行环境: MySQL 8.0+
-- 注意: 仅在优化后出现严重问题时使用
-- ==========================================

-- 设置会话参数
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO';

USE usdt_trading_platform;

-- ==========================================
-- 1. 备份当前状态（回滚前安全措施）
-- ==========================================

CREATE TABLE users_before_rollback_20250827 AS SELECT * FROM users;

-- ==========================================
-- 2. 删除新增的索引
-- ==========================================

-- 删除可用性检查索引
DROP INDEX IF EXISTS idx_users_availability_check ON users;

-- 删除邮箱验证索引
DROP INDEX IF EXISTS idx_users_email_verification ON users;

-- 删除注册统计索引
DROP INDEX IF EXISTS idx_users_registration_stats ON users;

-- 删除登录优化索引
DROP INDEX IF EXISTS idx_users_login_email ON users;
DROP INDEX IF EXISTS idx_users_login_username ON users;

-- 删除管理员列表索引
DROP INDEX IF EXISTS idx_users_admin_list ON users;

-- 删除活跃用户索引
DROP INDEX IF EXISTS idx_users_activity ON users;

-- 删除角色查询索引
DROP INDEX IF EXISTS idx_users_role_query ON users;

-- 删除用户名唯一索引
DROP INDEX IF EXISTS uk_users_username_not_null ON users;

-- ==========================================
-- 3. 删除新增的约束
-- ==========================================

-- 删除邮箱格式约束
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_email_format;

-- 删除用户名格式约束
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_username_format;

-- 删除密码哈希约束
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_password_hash_not_empty;

-- 删除盐值约束
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_salt_not_empty;

-- ==========================================
-- 4. 删除触发器
-- ==========================================

DROP TRIGGER IF EXISTS tr_users_before_insert;
DROP TRIGGER IF EXISTS tr_users_before_update;

-- ==========================================
-- 5. 删除视图
-- ==========================================

DROP VIEW IF EXISTS v_users_basic_info;
DROP VIEW IF EXISTS v_users_availability;
DROP VIEW IF EXISTS v_users_statistics;

-- ==========================================
-- 6. 删除存储过程
-- ==========================================

DROP PROCEDURE IF EXISTS sp_check_username_availability;
DROP PROCEDURE IF EXISTS sp_check_email_availability;
DROP PROCEDURE IF EXISTS sp_performance_test_user_queries;

-- ==========================================
-- 7. 恢复数据（可选 - 仅在数据损坏时使用）
-- ==========================================

-- 注意: 以下操作将覆盖当前users表数据，请慎重使用
-- 仅在确认数据损坏且备份可靠时执行

/*
-- 检查备份数据完整性
SELECT 
    COUNT(*) as backup_count,
    MIN(created_at) as earliest_user,
    MAX(created_at) as latest_user
FROM users_backup_20250827;

-- 如需恢复数据，请取消以下注释并执行
-- TRUNCATE TABLE users;
-- INSERT INTO users SELECT * FROM users_backup_20250827;
*/

-- ==========================================
-- 8. 清理配置记录
-- ==========================================

-- 删除优化版本记录
DELETE FROM system_config 
WHERE config_key IN (
    'user_registration_optimization_version',
    'user_registration_optimization_time'
);

-- ==========================================
-- 9. 恢复设置
-- ==========================================

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 更新表统计信息
ANALYZE TABLE users;

-- 刷新查询缓存
RESET QUERY CACHE;

-- ==========================================
-- 10. 验证回滚结果
-- ==========================================

-- 检查索引状态
SELECT 
    'Rollback verification - Indexes' as check_type,
    COUNT(*) as remaining_custom_indexes
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'usdt_trading_platform' 
  AND TABLE_NAME = 'users'
  AND INDEX_NAME LIKE 'idx_users_%';

-- 检查约束状态
SELECT 
    'Rollback verification - Constraints' as check_type,
    COUNT(*) as remaining_custom_constraints
FROM information_schema.TABLE_CONSTRAINTS 
WHERE TABLE_SCHEMA = 'usdt_trading_platform' 
  AND TABLE_NAME = 'users'
  AND CONSTRAINT_NAME LIKE 'chk_users_%';

-- 检查触发器状态
SELECT 
    'Rollback verification - Triggers' as check_type,
    COUNT(*) as remaining_custom_triggers
FROM information_schema.TRIGGERS 
WHERE TRIGGER_SCHEMA = 'usdt_trading_platform' 
  AND TRIGGER_NAME LIKE 'tr_users_%';

-- 检查视图状态
SELECT 
    'Rollback verification - Views' as check_type,
    COUNT(*) as remaining_custom_views
FROM information_schema.VIEWS 
WHERE TABLE_SCHEMA = 'usdt_trading_platform' 
  AND TABLE_NAME LIKE 'v_users_%';

-- 检查存储过程状态
SELECT 
    'Rollback verification - Procedures' as check_type,
    COUNT(*) as remaining_custom_procedures
FROM information_schema.ROUTINES 
WHERE ROUTINE_SCHEMA = 'usdt_trading_platform' 
  AND ROUTINE_NAME LIKE 'sp_check_%';

-- 检查数据完整性
SELECT 
    'Data integrity check' as check_type,
    COUNT(*) as total_users,
    COUNT(DISTINCT email) as unique_emails,
    COUNT(CASE WHEN username IS NOT NULL AND username != '' THEN 1 END) as users_with_username
FROM users 
WHERE deleted = 0;

-- ==========================================
-- 11. 记录回滚操作
-- ==========================================

-- 在审计日志中记录回滚操作
INSERT INTO audit_logs (
    user_id,
    action,
    resource,
    description,
    result,
    created_at
) VALUES (
    NULL,
    'DATABASE_ROLLBACK',
    'users_optimization',
    '用户注册系统数据库优化回滚',
    'SUCCESS',
    CURRENT_TIMESTAMP
);

-- 在系统配置中记录回滚
INSERT INTO system_config (
    config_key,
    config_value,
    data_type,
    category,
    description,
    is_public
) VALUES (
    'last_rollback_time',
    NOW(),
    'string',
    'system',
    '最后回滚时间',
    FALSE
) ON DUPLICATE KEY UPDATE 
    config_value = NOW(),
    updated_at = CURRENT_TIMESTAMP;

-- ==========================================
-- 12. 完成提示
-- ==========================================

SELECT 
    '用户注册系统数据库优化已回滚完成!' as message,
    NOW() as rollback_time,
    '请检查应用程序功能是否恢复正常' as next_step,
    '建议检查备份数据完整性' as recommendation;

-- ==========================================
-- 13. 后续检查建议
-- ==========================================

-- 建议执行以下查询验证系统状态:

-- 1. 检查基础查询性能
-- EXPLAIN SELECT * FROM users WHERE email = 'test@example.com' AND deleted = 0;

-- 2. 检查用户统计
-- SELECT status, COUNT(*) FROM users WHERE deleted = 0 GROUP BY status;

-- 3. 检查最近的用户注册
-- SELECT * FROM users WHERE deleted = 0 ORDER BY created_at DESC LIMIT 10;

-- 4. 验证登录功能
-- SELECT id, email, status, email_verified FROM users WHERE email = 'your_test_email' AND deleted = 0;

SELECT 
    '回滚脚本执行完成!' as status,
    '请手动验证以上查询以确保系统正常' as manual_check_required;