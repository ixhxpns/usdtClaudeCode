package com.usdttrading.interceptor;

import com.usdttrading.service.AuditLogService;
import com.usdttrading.utils.RequestUtils;
import com.usdttrading.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 安全攔截器
 * 檢測並阻止可疑的惡意請求，記錄安全事件
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityInterceptor implements HandlerInterceptor {

    private final AuditLogService auditLogService;
    private final ValidationUtils validationUtils;

    // 可疑User-Agent黑名單
    private static final List<String> SUSPICIOUS_USER_AGENTS = Arrays.asList(
            "bot", "crawler", "spider", "scraper", "scanner", "attack", "hack",
            "sqlmap", "nmap", "nikto", "burp", "dirb", "gobuster", "masscan"
    );

    // 危險請求頭
    private static final List<String> DANGEROUS_HEADERS = Arrays.asList(
            "x-forwarded-for", "x-real-ip", "x-originating-ip", "x-remote-ip"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String clientIp = RequestUtils.getClientIp(request);
        String userAgent = RequestUtils.getUserAgent(request);
        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // 1. 檢查User-Agent
        if (isSuspiciousUserAgent(userAgent)) {
            log.warn("檢測到可疑User-Agent - IP: {}, UA: {}, URI: {}", clientIp, userAgent, requestUri);
            auditLogService.logSecurityEvent(null, "SUSPICIOUS_USER_AGENT",
                    "檢測到可疑User-Agent: " + userAgent, clientIp, userAgent, false, "已阻止");
            return handleSecurityViolation(response, "請求被拒絕");
        }

        // 2. 檢查請求頭注入
        if (hasHeaderInjection(request)) {
            log.warn("檢測到請求頭注入 - IP: {}, URI: {}", clientIp, requestUri);
            auditLogService.logSecurityEvent(null, "HEADER_INJECTION",
                    "檢測到請求頭注入嘗試", clientIp, userAgent, false, "已阻止");
            return handleSecurityViolation(response, "請求格式錯誤");
        }

        // 3. 檢查SQL注入嘗試
        if (hasSqlInjectionAttempt(request)) {
            log.warn("檢測到SQL注入嘗試 - IP: {}, URI: {}", clientIp, requestUri);
            auditLogService.logSecurityEvent(null, "SQL_INJECTION_ATTEMPT",
                    "檢測到SQL注入嘗試", clientIp, userAgent, false, "已阻止");
            return handleSecurityViolation(response, "請求包含非法字符");
        }

        // 4. 檢查XSS嘗試
        if (hasXssAttempt(request)) {
            log.warn("檢測到XSS嘗試 - IP: {}, URI: {}", clientIp, requestUri);
            auditLogService.logSecurityEvent(null, "XSS_ATTEMPT",
                    "檢測到XSS嘗試", clientIp, userAgent, false, "已阻止");
            return handleSecurityViolation(response, "請求包含非法腳本");
        }

        // 5. 檢查路徑遍歷攻擊
        if (hasPathTraversalAttempt(requestUri)) {
            log.warn("檢測到路徑遍歷攻擊 - IP: {}, URI: {}", clientIp, requestUri);
            auditLogService.logSecurityEvent(null, "PATH_TRAVERSAL_ATTEMPT",
                    "檢測到路徑遍歷攻擊", clientIp, userAgent, false, "已阻止");
            return handleSecurityViolation(response, "無效的請求路徑");
        }

        // 6. 檢查異常大的請求
        if (hasAbnormalRequestSize(request)) {
            log.warn("檢測到異常大的請求 - IP: {}, Size: {}", clientIp, request.getContentLengthLong());
            auditLogService.logSecurityEvent(null, "ABNORMAL_REQUEST_SIZE",
                    "請求大小異常: " + request.getContentLengthLong() + " bytes", 
                    clientIp, userAgent, false, "已阻止");
            return handleSecurityViolation(response, "請求大小超出限制");
        }

        // 7. 檢查是否為自動化工具
        if (isAutomatedTool(request)) {
            log.warn("檢測到自動化工具 - IP: {}, UA: {}", clientIp, userAgent);
            auditLogService.logSecurityEvent(null, "AUTOMATED_TOOL_DETECTED",
                    "檢測到自動化工具", clientIp, userAgent, false, "已阻止");
            return handleSecurityViolation(response, "不允許自動化訪問");
        }

        return true;
    }

    /**
     * 檢查是否為可疑User-Agent
     */
    private boolean isSuspiciousUserAgent(String userAgent) {
        if (userAgent == null || userAgent.length() < 10) {
            return true;
        }

        String lowerUserAgent = userAgent.toLowerCase();
        return SUSPICIOUS_USER_AGENTS.stream()
                .anyMatch(lowerUserAgent::contains);
    }

    /**
     * 檢查請求頭注入
     */
    private boolean hasHeaderInjection(HttpServletRequest request) {
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            if (headerValue != null && (headerValue.contains("\n") || headerValue.contains("\r"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 檢查SQL注入嘗試
     */
    private boolean hasSqlInjectionAttempt(HttpServletRequest request) {
        // 檢查URL參數
        String queryString = request.getQueryString();
        if (queryString != null && validationUtils.containsSqlInjectionRisk(queryString)) {
            return true;
        }

        // 檢查請求參數
        return request.getParameterMap().values().stream()
                .flatMap(Arrays::stream)
                .anyMatch(validationUtils::containsSqlInjectionRisk);
    }

    /**
     * 檢查XSS嘗試
     */
    private boolean hasXssAttempt(HttpServletRequest request) {
        String[] xssPatterns = {
                "<script", "javascript:", "onload=", "onerror=", "onclick=",
                "eval(", "alert(", "document.cookie", "document.location"
        };

        // 檢查URL參數
        String queryString = request.getQueryString();
        if (queryString != null) {
            String lowerQuery = queryString.toLowerCase();
            for (String pattern : xssPatterns) {
                if (lowerQuery.contains(pattern)) {
                    return true;
                }
            }
        }

        // 檢查請求參數
        return request.getParameterMap().values().stream()
                .flatMap(Arrays::stream)
                .anyMatch(value -> {
                    String lowerValue = value.toLowerCase();
                    return Arrays.stream(xssPatterns)
                            .anyMatch(lowerValue::contains);
                });
    }

    /**
     * 檢查路徑遍歷攻擊
     */
    private boolean hasPathTraversalAttempt(String uri) {
        String[] pathTraversalPatterns = {
                "../", "..\\", "%2e%2e%2f", "%2e%2e\\",
                "....//", "....\\\\", "%252e%252e%252f"
        };

        String lowerUri = uri.toLowerCase();
        return Arrays.stream(pathTraversalPatterns)
                .anyMatch(lowerUri::contains);
    }

    /**
     * 檢查異常大的請求
     */
    private boolean hasAbnormalRequestSize(HttpServletRequest request) {
        long contentLength = request.getContentLengthLong();
        // 一般API請求不應該超過10MB
        return contentLength > 10 * 1024 * 1024;
    }

    /**
     * 檢查是否為自動化工具
     */
    private boolean isAutomatedTool(HttpServletRequest request) {
        String userAgent = RequestUtils.getUserAgent(request);
        
        // 檢查常見的自動化工具特徵
        String[] automatedToolSignatures = {
            "curl", "wget", "python-requests", "java/", "apache-httpclient",
            "postman", "insomnia", "httpie", "restsharp"
        };

        String lowerUserAgent = userAgent.toLowerCase();
        for (String signature : automatedToolSignatures) {
            if (lowerUserAgent.contains(signature)) {
                // 但允許測試環境使用
                String environment = System.getProperty("spring.profiles.active", "");
                if (environment.contains("dev") || environment.contains("test")) {
                    return false;
                }
                return true;
            }
        }

        // 檢查是否缺少常見瀏覽器頭信息
        return isMissingBrowserHeaders(request);
    }

    /**
     * 檢查是否缺少瀏覽器常見頭信息
     */
    private boolean isMissingBrowserHeaders(HttpServletRequest request) {
        // 真實瀏覽器通常會包含這些頭信息
        String accept = request.getHeader("Accept");
        String acceptLanguage = request.getHeader("Accept-Language");
        String acceptEncoding = request.getHeader("Accept-Encoding");
        
        // 如果這些頭信息都沒有，可能是自動化工具
        return accept == null && acceptLanguage == null && acceptEncoding == null;
    }

    /**
     * 處理安全違規
     */
    private boolean handleSecurityViolation(HttpServletResponse response, String message) {
        response.setStatus(403); // Forbidden
        response.setContentType("application/json;charset=UTF-8");
        
        String jsonResponse = String.format(
                "{\"success\":false,\"code\":403,\"message\":\"%s\",\"data\":null}",
                message
        );
        
        try {
            response.getWriter().write(jsonResponse);
        } catch (IOException e) {
            log.error("寫入安全違規響應失敗", e);
        }
        
        return false;
    }
}