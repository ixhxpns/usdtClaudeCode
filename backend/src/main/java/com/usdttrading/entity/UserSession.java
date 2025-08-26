package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 用户会话实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_sessions")
public class UserSession extends BaseEntity {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * JWT Token
     */
    @JsonIgnore
    @NotBlank(message = "Token不能为空")
    private String token;

    /**
     * 刷新令牌
     */
    @JsonIgnore
    private String refreshToken;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 设备信息
     */
    private String deviceInfo;

    /**
     * IP地址
     */
    @NotBlank(message = "IP地址不能为空")
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 登录位置
     */
    private String location;

    /**
     * 是否活跃
     */
    private Boolean isActive;

    /**
     * 最后活动时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActivity;

    /**
     * 过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "过期时间不能为空")
    private LocalDateTime expiresAt;

    /**
     * 检查会话是否活跃
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * 检查会话是否过期
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * 检查是否为移动设备
     */
    public boolean isMobileDevice() {
        return "mobile".equals(deviceType);
    }

    /**
     * 检查是否为Web设备
     */
    public boolean isWebDevice() {
        return "web".equals(deviceType);
    }

    /**
     * 检查是否需要刷新
     */
    public boolean needsRefresh(int refreshThresholdMinutes) {
        if (expiresAt == null) {
            return false;
        }
        LocalDateTime refreshTime = LocalDateTime.now().plusMinutes(refreshThresholdMinutes);
        return expiresAt.isBefore(refreshTime);
    }

    /**
     * 检查会话是否长时间未活动
     */
    public boolean isInactive(int inactiveThresholdMinutes) {
        if (lastActivity == null) {
            return false;
        }
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(inactiveThresholdMinutes);
        return lastActivity.isBefore(threshold);
    }

    /**
     * 更新最后活动时间
     */
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * 使会话失效
     */
    public void invalidate() {
        this.isActive = false;
        this.expiresAt = LocalDateTime.now();
    }
}