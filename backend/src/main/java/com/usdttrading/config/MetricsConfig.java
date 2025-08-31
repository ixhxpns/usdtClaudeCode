package com.usdttrading.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Metrics 配置類
 * 解決 Spring Boot Actuator MeterRegistry 依賴注入問題
 * 
 * @author DebugAgent
 * @version 1.0.0
 * @since 2025-12-28
 */
@Configuration
public class MetricsConfig {

    /**
     * 提供 SimpleMeterRegistry Bean
     * 解決 webMvcMetricsFilter 依賴注入錯誤
     */
    @Bean
    @Primary
    public MeterRegistry simpleMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    /**
     * MeterRegistry 自定義配置
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "usdt-trading-platform");
    }
}