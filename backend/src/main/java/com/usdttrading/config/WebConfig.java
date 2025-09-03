package com.usdttrading.config;

import com.usdttrading.interceptor.LoggingInterceptor;
import com.usdttrading.interceptor.RequestTraceInterceptor;
import com.usdttrading.interceptor.RateLimitInterceptor;
import com.usdttrading.interceptor.SecurityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置類
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RequestTraceInterceptor requestTraceInterceptor;

    @Autowired
    private LoggingInterceptor loggingInterceptor;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Autowired
    private SecurityInterceptor securityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 請求追蹤攔截器 - 最高優先級，為每個請求生成追蹤ID
        registry.addInterceptor(requestTraceInterceptor)
                .addPathPatterns("/**")
                .order(1);

        // 2. 安全攔截器 - 檢測並阻止惡意請求
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health", "/api/actuator/**")
                .order(2);

        // 3. 頻率限制攔截器 - 防止API被頻繁調用
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health", "/api/actuator/**", "/api/swagger-ui/**", "/api/v3/api-docs/**")
                .order(3);

        // 4. 日誌攔截器 - 記錄請求和響應日誌
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health", "/api/actuator/**", "/api/auth/captcha")
                .order(4);
    }
}