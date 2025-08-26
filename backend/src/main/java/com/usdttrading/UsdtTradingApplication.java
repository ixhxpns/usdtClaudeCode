package com.usdttrading;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * USDT交易平台主启动类
 * 
 * @author ArchitectAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@SpringBootApplication
@MapperScan("com.usdttrading.repository")
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
public class UsdtTradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsdtTradingApplication.class, args);
        System.out.println("""
            
            =====================================================
            USDT交易平台启动成功!
            
            📊 Swagger文档: http://localhost:8080/swagger-ui.html
            📈 Actuator监控: http://localhost:8080/actuator
            🔐 API文档: http://localhost:8080/v3/api-docs
            
            =====================================================
            """);
    }
}