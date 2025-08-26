package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 系统公告实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("announcements")
public class Announcement extends BaseEntity {

    /**
     * 公告标题
     */
    @NotBlank(message = "公告标题不能为空")
    private String title;

    /**
     * 公告内容
     */
    @NotBlank(message = "公告内容不能为空")
    private String content;

    /**
     * 公告类型
     */
    private String type;

    /**
     * 优先级
     */
    private String priority;

    /**
     * 目标受众
     */
    private String targetAudience;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 是否弹窗显示
     */
    private Boolean isPopup;

    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishAt;

    /**
     * 到期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireAt;

    /**
     * 创建人ID
     */
    @NotNull(message = "创建人ID不能为空")
    private Long createdBy;

    /**
     * 检查是否启用
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * 检查是否弹窗显示
     */
    public boolean isPopup() {
        return Boolean.TRUE.equals(isPopup);
    }

    /**
     * 检查是否已发布
     */
    public boolean isPublished() {
        return publishAt != null && publishAt.isBefore(LocalDateTime.now());
    }

    /**
     * 检查是否已过期
     */
    public boolean isExpired() {
        return expireAt != null && expireAt.isBefore(LocalDateTime.now());
    }

    /**
     * 检查是否为紧急公告
     */
    public boolean isUrgent() {
        return "urgent".equals(priority);
    }

    /**
     * 检查是否为高优先级公告
     */
    public boolean isHighPriority() {
        return "high".equals(priority) || "urgent".equals(priority);
    }

    /**
     * 检查是否为维护公告
     */
    public boolean isMaintenanceAnnouncement() {
        return "maintenance".equals(type);
    }

    /**
     * 检查是否为错误公告
     */
    public boolean isErrorAnnouncement() {
        return "error".equals(type);
    }

    /**
     * 检查是否针对所有用户
     */
    public boolean isForAllUsers() {
        return "all".equals(targetAudience);
    }

    /**
     * 检查是否针对管理员
     */
    public boolean isForAdmins() {
        return "admins".equals(targetAudience);
    }
}