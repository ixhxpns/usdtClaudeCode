package com.usdttrading.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.entity.AuditLog;
import com.usdttrading.interceptor.RequestTraceInterceptor;
import com.usdttrading.repository.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.usdttrading.entity.SecurityEvent;
import com.usdttrading.repository.SecurityEventMapper;
import com.usdttrading.utils.RequestUtils;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 審計日誌服務類
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final SecurityEventMapper securityEventMapper;

    /**
     * 記錄用戶操作日誌 (简化版本)
     */
    public void logUserAction(Long userId, String action, String resource, String resourceId, String description) {
        logUserAction(userId, action, resource, resourceId, description, null, null);
    }
    
    /**
     * 記錄用戶操作日誌
     */
    public void logUserAction(Long userId, String action, String resource, String resourceId,
                             String description, Object oldValues, Object newValues) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setResource(resource);
            auditLog.setResourceId(resourceId);
            auditLog.setDescription(description);
            
            if (oldValues != null) {
                auditLog.setOldValues(oldValues.toString());
            }
            
            if (newValues != null) {
                auditLog.setNewValues(newValues.toString());
            }
            
            // 從請求中獲取額外信息
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    auditLog.setIpAddress(RequestUtils.getClientIp(attributes.getRequest()));
                    auditLog.setUserAgent(attributes.getRequest().getHeader("User-Agent"));
                    auditLog.setRequestId(RequestTraceInterceptor.getCurrentTraceId());
                }
            } catch (Exception e) {
                // 忽略請求上下文獲取失敗的情況
            }
            
            auditLog.setResult("success");
            auditLogMapper.insert(auditLog);
            
        } catch (Exception e) {
            log.error("記錄審計日誌失敗", e);
        }
    }

    /**
     * 記錄操作失敗日誌
     */
    public void logFailedAction(Long userId, String action, String resource, String resourceId,
                               String description, String errorMessage) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setResource(resource);
            auditLog.setResourceId(resourceId);
            auditLog.setDescription(description);
            auditLog.setErrorMessage(errorMessage);
            auditLog.setResult("failure");
            
            // 從請求中獲取額外信息
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    auditLog.setIpAddress(RequestUtils.getClientIp(attributes.getRequest()));
                    auditLog.setUserAgent(attributes.getRequest().getHeader("User-Agent"));
                    auditLog.setRequestId(RequestTraceInterceptor.getCurrentTraceId());
                }
            } catch (Exception e) {
                // 忽略請求上下文獲取失敗的情況
            }
            
            auditLogMapper.insert(auditLog);
            
        } catch (Exception e) {
            log.error("記錄失敗審計日誌失敗", e);
        }
    }

    /**
     * 分頁查詢審計日誌
     */
    public Page<AuditLog> getAuditLogs(int page, int size, Long userId, String action, String resource) {
        Page<AuditLog> pageObj = new Page<>(page, size);
        QueryWrapper<AuditLog> wrapper = new QueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        
        if (action != null && !action.trim().isEmpty()) {
            wrapper.eq("action", action);
        }
        
        if (resource != null && !resource.trim().isEmpty()) {
            wrapper.eq("resource", resource);
        }
        
        wrapper.orderByDesc("created_at");
        return auditLogMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 根據資源查詢操作記錄
     */
    public Page<AuditLog> getResourceLogs(String resource, String resourceId, int page, int size) {
        Page<AuditLog> pageObj = new Page<>(page, size);
        QueryWrapper<AuditLog> wrapper = new QueryWrapper<>();
        wrapper.eq("resource", resource);
        wrapper.eq("resource_id", resourceId);
        wrapper.orderByDesc("created_at");
        
        return auditLogMapper.selectPage(pageObj, wrapper);
    }


    /**
     * 記錄安全事件
     */
    public void logSecurityEvent(Long userId, String eventType, String description, 
                                String ipAddress, String userAgent, boolean success, String errorMessage) {
        try {
            SecurityEvent securityEvent = new SecurityEvent();
            securityEvent.setUserId(userId);
            securityEvent.setEventType(eventType);
            securityEvent.setDescription(description);
            securityEvent.setIpAddress(ipAddress);
            securityEvent.setUserAgent(userAgent);
            securityEvent.setSuccess(success);
            securityEvent.setErrorMessage(errorMessage);
            
            try {
                securityEvent.setRequestId(getCurrentTraceId());
            } catch (Exception e) {
                // 忽略請求上下文獲取失敗的情況
            }
            
            securityEventMapper.insert(securityEvent);
            
        } catch (Exception e) {
            log.error("記錄安全事件失敗", e);
        }
    }

    /**
     * 記錄管理員操作日誌
     */
    public void logAdminAction(Long adminId, String action, String resource, String resourceId, String description) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(adminId);
            auditLog.setAction(action);
            auditLog.setResource(resource);
            auditLog.setResourceId(resourceId);
            auditLog.setDescription(description);
            auditLog.setResult("success");
            
            // 從請求中獲取額外信息
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    auditLog.setIpAddress(RequestUtils.getClientIp(attributes.getRequest()));
                    auditLog.setUserAgent(attributes.getRequest().getHeader("User-Agent"));
                    auditLog.setRequestId(RequestTraceInterceptor.getCurrentTraceId());
                }
            } catch (Exception e) {
                // 忽略請求上下文獲取失敗的情況
            }
            
            auditLogMapper.insert(auditLog);
            
        } catch (Exception e) {
            log.error("記錄管理員審計日誌失敗", e);
        }
    }

    /**
     * 記錄郵件發送事件
     */
    public void logEmailEvent(Long userId, String emailType, String recipient, 
                             String subject, boolean success, String errorMessage) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction("email_send");
            auditLog.setResource("email");
            auditLog.setResourceId(recipient);
            auditLog.setDescription("郵件發送: " + emailType + " - " + subject);
            auditLog.setResult(success ? "success" : "failure");
            auditLog.setErrorMessage(errorMessage);
            
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    auditLog.setIpAddress(RequestUtils.getClientIp(attributes.getRequest()));
                    auditLog.setUserAgent(attributes.getRequest().getHeader("User-Agent"));
                    auditLog.setRequestId(RequestTraceInterceptor.getCurrentTraceId());
                }
            } catch (Exception e) {
                // 忽略請求上下文獲取失敗的情況
            }
            
            auditLogMapper.insert(auditLog);
            
        } catch (Exception e) {
            log.error("記錄郵件事件失敗", e);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // 多个代理的情况，第一个IP为客户端真实IP
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    /**
     * 获取当前请求的追踪ID
     */
    private String getCurrentTraceId() {
        try {
            return RequestTraceInterceptor.getCurrentTraceId();
        } catch (Exception e) {
            // 如果RequestTraceInterceptor不可用，生成一个简单的ID
            return "TRACE-" + System.currentTimeMillis();
        }
    }
}