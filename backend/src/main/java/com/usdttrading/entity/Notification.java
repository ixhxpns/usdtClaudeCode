package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.usdttrading.enums.NotificationStatus;
import com.usdttrading.enums.NotificationType;
import com.usdttrading.enums.NotificationCategory;
import com.usdttrading.enums.NotificationPriority;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 通知实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notifications")
public class Notification extends BaseEntity {

    /**
     * 用户ID(NULL表示系统通知)
     */
    private Long userId;

    /**
     * 通知类型
     */
    private NotificationType type;

    /**
     * 通知分类
     */
    private NotificationCategory category;

    /**
     * 通知标题
     */
    @NotBlank(message = "通知标题不能为空")
    private String title;

    /**
     * 通知内容
     */
    @NotBlank(message = "通知内容不能为空")
    private String content;

    /**
     * 额外数据（元數據）
     */
    private String metadata;

    /**
     * 通知状态
     */
    private NotificationStatus status;

    /**
     * 优先级
     */
    private NotificationPriority priority;

    /**
     * 发送时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendAt;

    /**
     * 送达时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliveredAt;

    /**
     * 阅读时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readAt;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 检查是否为系统通知
     */
    public boolean isSystemNotification() {
        return userId == null;
    }

    /**
     * 检查是否待发送
     */
    public boolean isPending() {
        return "pending".equals(status);
    }

    /**
     * 检查是否已发送
     */
    public boolean isSent() {
        return "sent".equals(status);
    }

    /**
     * 检查是否已送达
     */
    public boolean isDelivered() {
        return "delivered".equals(status);
    }

    /**
     * 检查是否已阅读
     */
    public boolean isRead() {
        return "read".equals(status);
    }

    /**
     * 检查是否发送失败
     */
    public boolean isFailed() {
        return "failed".equals(status);
    }

    /**
     * 检查是否为邮件通知
     */
    public boolean isEmailNotification() {
        return "email".equals(type);
    }

    /**
     * 检查是否为短信通知
     */
    public boolean isSmsNotification() {
        return "sms".equals(type);
    }

    /**
     * 检查是否为推送通知
     */
    public boolean isPushNotification() {
        return "push".equals(type);
    }

    /**
     * 检查是否为紧急通知
     */
    public boolean isUrgent() {
        return "urgent".equals(priority);
    }

    /**
     * 检查是否为订单相关通知
     */
    public boolean isOrderNotification() {
        return "order".equals(category);
    }

    /**
     * 检查是否为安全相关通知
     */
    public boolean isSecurityNotification() {
        return "security".equals(category);
    }

    /**
     * 检查是否需要重试
     */
    public boolean needsRetry() {
        return isFailed() && (retryCount == null || retryCount < 3);
    }
}