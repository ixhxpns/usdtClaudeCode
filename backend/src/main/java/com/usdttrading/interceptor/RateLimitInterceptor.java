package com.usdttrading.interceptor;

import com.usdttrading.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 頻率限制攔截器
 * 防止API被惡意頻繁調用，保護系統穩定性
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, Object> redisTemplate;

    // 不同API的限制配置
    private static final int DEFAULT_LIMIT = 100;           // 默認每分鐘限制
    private static final int LOGIN_LIMIT = 5;               // 登錄每分鐘限制
    private static final int REGISTER_LIMIT = 3;            // 註冊每5分鐘限制
    private static final int RESET_PASSWORD_LIMIT = 3;      // 密碼重設每小時限制
    private static final int VERIFY_EMAIL_LIMIT = 10;       // 郵箱驗證每分鐘限制

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        String clientIp = RequestUtils.getClientIp(request);
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        
        // 檢查是否為需要限制的API
        RateLimitConfig config = getRateLimitConfig(requestUri, method);
        if (config == null) {
            return true; // 不需要限制
        }
        
        String key = generateRateLimitKey(clientIp, requestUri, method);
        
        // 檢查當前請求次數
        Integer currentCount = (Integer) redisTemplate.opsForValue().get(key);
        if (currentCount == null) {
            currentCount = 0;
        }
        
        if (currentCount >= config.getLimit()) {
            log.warn("API頻率限制觸發 - IP: {}, URI: {}, Method: {}, Count: {}/{}", 
                    clientIp, requestUri, method, currentCount, config.getLimit());
            
            handleRateLimitExceeded(response, config);
            return false;
        }
        
        // 增加請求次數
        incrementRequestCount(key, config.getWindowSeconds());
        
        return true;
    }

    /**
     * 根據API路徑獲取限制配置
     */
    private RateLimitConfig getRateLimitConfig(String uri, String method) {
        if (!method.equals("POST")) {
            return null; // 只限制POST請求
        }
        
        if (uri.contains("/auth/login")) {
            return new RateLimitConfig(LOGIN_LIMIT, 60, "登錄"); // 每分鐘5次
        } else if (uri.contains("/auth/register")) {
            return new RateLimitConfig(REGISTER_LIMIT, 300, "註冊"); // 每5分鐘3次
        } else if (uri.contains("/auth/forgot-password")) {
            return new RateLimitConfig(RESET_PASSWORD_LIMIT, 3600, "密碼重設"); // 每小時3次
        } else if (uri.contains("/auth/verify-email") || uri.contains("/auth/resend-verification")) {
            return new RateLimitConfig(VERIFY_EMAIL_LIMIT, 60, "郵箱驗證"); // 每分鐘10次
        } else if (uri.contains("/auth/")) {
            return new RateLimitConfig(20, 60, "認證API"); // 其他認證API每分鐘20次
        }
        
        return null;
    }

    /**
     * 生成限制鍵
     */
    private String generateRateLimitKey(String ip, String uri, String method) {
        return String.format("rate_limit:%s:%s:%s", ip, method, uri);
    }

    /**
     * 增加請求次數
     */
    private void incrementRequestCount(String key, int windowSeconds) {
        try {
            redisTemplate.opsForValue().increment(key, 1);
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("增加請求次數失敗: {}", e.getMessage());
        }
    }

    /**
     * 處理限制超出情況
     */
    private void handleRateLimitExceeded(HttpServletResponse response, RateLimitConfig config) {
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json;charset=UTF-8");
        
        String jsonResponse = String.format(
                "{\"success\":false,\"code\":429,\"message\":\"%s請求過於頻繁，請稍後重試\",\"data\":null}",
                config.getDescription()
        );
        
        try {
            response.getWriter().write(jsonResponse);
        } catch (IOException e) {
            log.error("寫入限制響應失敗", e);
        }
    }

    /**
     * 限制配置類
     */
    private static class RateLimitConfig {
        private final int limit;
        private final int windowSeconds;
        private final String description;

        public RateLimitConfig(int limit, int windowSeconds, String description) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
            this.description = description;
        }

        public int getLimit() {
            return limit;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public String getDescription() {
            return description;
        }
    }
}