package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 安全事件实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("security_events")
public class SecurityEvent extends BaseEntity {

    /**
     * 相关用户ID
     */
    private Long userId;

    /**
     * 事件类型
     */
    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    /**
     * 严重程度
     */
    private String severity;

    /**
     * 事件描述
     */
    @NotBlank(message = "事件描述不能为空")
    private String description;

    /**
     * 事件元数据
     */
    private String metadata;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 請求ID
     */
    private String requestId;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 錯誤消息
     */
    private String errorMessage;

    /**
     * 是否已处理
     */
    private Boolean isResolved;

    /**
     * 处理人员ID
     */
    private Long resolvedBy;

    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;

    /**
     * 检查是否已处理
     */
    public boolean isResolved() {
        return Boolean.TRUE.equals(isResolved);
    }

    /**
     * 检查是否为登录相关事件
     */
    public boolean isLoginEvent() {
        return "login_success".equals(eventType) || "login_failed".equals(eventType);
    }

    /**
     * 检查是否为登录成功事件
     */
    public boolean isLoginSuccess() {
        return "login_success".equals(eventType);
    }

    /**
     * 检查是否为登录失败事件
     */
    public boolean isLoginFailure() {
        return "login_failed".equals(eventType);
    }

    /**
     * 检查是否为密码相关事件
     */
    public boolean isPasswordEvent() {
        return "password_change".equals(eventType);
    }

    /**
     * 检查是否为KYC相关事件
     */
    public boolean isKycEvent() {
        return "kyc_submit".equals(eventType);
    }

    /**
     * 检查是否为大额提款事件
     */
    public boolean isLargeWithdrawal() {
        return "large_withdrawal".equals(eventType);
    }

    /**
     * 检查是否为可疑活动
     */
    public boolean isSuspiciousActivity() {
        return "suspicious_activity".equals(eventType);
    }

    /**
     * 检查是否为账户锁定事件
     */
    public boolean isAccountLocked() {
        return "account_locked".equals(eventType);
    }

    /**
     * 检查是否为设备变更事件
     */
    public boolean isDeviceChange() {
        return "device_change".equals(eventType);
    }

    /**
     * 检查严重程度是否为低
     */
    public boolean isLowSeverity() {
        return "low".equals(severity);
    }

    /**
     * 检查严重程度是否为中等
     */
    public boolean isMediumSeverity() {
        return "medium".equals(severity);
    }

    /**
     * 检查严重程度是否为高
     */
    public boolean isHighSeverity() {
        return "high".equals(severity);
    }

    /**
     * 检查严重程度是否为致命
     */
    public boolean isCriticalSeverity() {
        return "critical".equals(severity);
    }

    /**
     * 检查是否需要立即处理
     */
    public boolean requiresImmediateAction() {
        return isCriticalSeverity() || isHighSeverity();
    }

    /**
     * 标记为已处理
     */
    public void markResolved(Long resolvedBy) {
        this.isResolved = true;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = LocalDateTime.now();
    }
}