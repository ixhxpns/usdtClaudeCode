package com.usdttrading.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 日誌記錄攔截器
 * 記錄API調用日誌
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME_KEY = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 記錄請求開始時間
        request.setAttribute(START_TIME_KEY, System.currentTimeMillis());

        // 記錄請求基本信息
        logRequest(request);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 計算請求處理時間
        Long startTime = (Long) request.getAttribute(START_TIME_KEY);
        long executionTime = startTime != null ? System.currentTimeMillis() - startTime : 0;

        // 記錄響應信息
        logResponse(request, response, executionTime, ex);
    }

    /**
     * 記錄請求信息
     */
    private void logRequest(HttpServletRequest request) {
        try {
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // 獲取當前登錄用戶ID
            Long userId = null;
            try {
                if (StpUtil.isLogin()) {
                    userId = StpUtil.getLoginIdAsLong();
                }
            } catch (Exception e) {
                // 忽略未登錄的情況
            }

            // 構建請求參數
            Map<String, String> parameters = new HashMap<>();
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                // 敏感參數不記錄
                if (!isSensitiveParameter(paramName)) {
                    parameters.put(paramName, request.getParameter(paramName));
                }
            }

            log.info("請求開始 - {} {} {} - 用戶: {} - IP: {} - 參數: {} - UA: {}",
                    method, uri, queryString != null ? "?" + queryString : "",
                    userId, clientIp, JSONUtil.toJsonStr(parameters), userAgent);

        } catch (Exception e) {
            log.warn("記錄請求信息失敗", e);
        }
    }

    /**
     * 記錄響應信息
     */
    private void logResponse(HttpServletRequest request, HttpServletResponse response, long executionTime, Exception ex) {
        try {
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();

            if (ex != null) {
                log.error("請求異常 - {} {} - 狀態: {} - 耗時: {}ms - 異常: {}",
                        method, uri, status, executionTime, ex.getMessage());
            } else if (status >= 400) {
                log.warn("請求失敗 - {} {} - 狀態: {} - 耗時: {}ms",
                        method, uri, status, executionTime);
            } else {
                log.info("請求完成 - {} {} - 狀態: {} - 耗時: {}ms",
                        method, uri, status, executionTime);
            }

        } catch (Exception e) {
            log.warn("記錄響應信息失敗", e);
        }
    }

    /**
     * 獲取客戶端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 取第一個IP
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 判斷是否為敏感參數
     */
    private boolean isSensitiveParameter(String paramName) {
        String lowerParamName = paramName.toLowerCase();
        return lowerParamName.contains("password") ||
               lowerParamName.contains("token") ||
               lowerParamName.contains("secret") ||
               lowerParamName.contains("key");
    }
}