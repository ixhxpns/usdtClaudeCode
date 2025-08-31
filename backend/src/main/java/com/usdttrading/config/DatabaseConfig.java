package com.usdttrading.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * 数据库配置类 - 强化版
 * 包含连接池配置、健康检查、重试机制
 * 
 * @author ArchitectAgent
 * @version 2.0.0
 * @since 2025-08-18
 */
@Slf4j
@Configuration
@EnableRetry
@EnableAsync
@EnableScheduling
public class DatabaseConfig {

    /**
     * MyBatis Plus 拦截器配置
     */
    @Bean
    @Primary
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        paginationInterceptor.setDbType(DbType.MYSQL);
        paginationInterceptor.setOverflow(false);
        paginationInterceptor.setMaxLimit(1000L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        return interceptor;
    }

    /**
     * 数据库健康检查器
     */
    @Component
    @ConditionalOnProperty(name = "management.health.db.enabled", havingValue = "true", matchIfMissing = true)
    public static class DatabaseHealthIndicator implements HealthIndicator {
        
        private final DataSource dataSource;
        
        public DatabaseHealthIndicator(@Qualifier("dataSource") DataSource dataSource) {
            this.dataSource = dataSource;
        }
        
        @Override
        public Health health() {
            try {
                return checkDatabaseHealth();
            } catch (Exception e) {
                log.error("数据库健康检查失败", e);
                return Health.down(e)
                    .withDetail("error", e.getMessage())
                    .withDetail("database", "MySQL")
                    .build();
            }
        }
        
        @Retryable(value = {SQLException.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
        private Health checkDatabaseHealth() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(5)) {
                    DruidDataSource druidDataSource = (DruidDataSource) dataSource;
                    return Health.up()
                        .withDetail("database", "MySQL")
                        .withDetail("activeConnections", druidDataSource.getActiveCount())
                        .withDetail("poolingConnections", druidDataSource.getPoolingCount())
                        .withDetail("maxActive", druidDataSource.getMaxActive())
                        .withDetail("connectionURL", connection.getMetaData().getURL())
                        .build();
                } else {
                    throw new SQLException("数据库连接验证失败");
                }
            }
        }
    }
    
    /**
     * 数据库连接监控和自动恢复
     */
    @Component
    public static class DatabaseConnectionMonitor {
        
        private final DataSource dataSource;
        private volatile boolean databaseAvailable = false;
        
        public DatabaseConnectionMonitor(@Qualifier("dataSource") DataSource dataSource) {
            this.dataSource = dataSource;
        }
        
        /**
         * 应用启动后初始化数据库连接
         */
        @EventListener(ApplicationReadyEvent.class)
        @Async
        public void initializeDatabaseConnection() {
            log.info("开始初始化数据库连接...");
            waitForDatabaseAvailability();
        }
        
        /**
         * 等待数据库可用
         */
        @Retryable(
            value = {SQLException.class, RuntimeException.class},
            maxAttempts = 10,
            backoff = @Backoff(delay = 5000, maxDelay = 30000, multiplier = 1.5)
        )
        public void waitForDatabaseAvailability() {
            int attempts = 0;
            int maxAttempts = 12; // 最多尝试2分钟
            
            while (attempts < maxAttempts) {
                try {
                    testDatabaseConnection();
                    databaseAvailable = true;
                    log.info("数据库连接成功建立，尝试次数: {}", attempts + 1);
                    return;
                } catch (Exception e) {
                    attempts++;
                    log.warn("数据库连接尝试 {}/{} 失败: {}", attempts, maxAttempts, e.getMessage());
                    
                    if (attempts >= maxAttempts) {
                        log.error("数据库连接失败，已达到最大重试次数", e);
                        throw new RuntimeException("数据库连接失败，无法启动应用", e);
                    }
                    
                    try {
                        TimeUnit.SECONDS.sleep(10); // 等待10秒后重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("等待数据库连接时被中断", ie);
                    }
                }
            }
        }
        
        /**
         * 定期检查数据库连接状态
         */
        @Scheduled(fixedRate = 30000) // 每30秒检查一次
        public void monitorDatabaseConnection() {
            try {
                testDatabaseConnection();
                if (!databaseAvailable) {
                    log.info("数据库连接已恢复");
                    databaseAvailable = true;
                }
            } catch (Exception e) {
                if (databaseAvailable) {
                    log.error("数据库连接丢失: {}", e.getMessage());
                    databaseAvailable = false;
                }
            }
        }
        
        /**
         * 测试数据库连接
         */
        private void testDatabaseConnection() throws SQLException {
            try (Connection connection = dataSource.getConnection()) {
                if (!connection.isValid(5)) {
                    throw new SQLException("数据库连接验证失败");
                }
            }
        }
        
        /**
         * 获取数据库可用状态
         */
        public boolean isDatabaseAvailable() {
            return databaseAvailable;
        }
    }
}