package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * 操作日志实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_logs")
public class AuditLog extends BaseEntity {

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作动作
     */
    @NotBlank(message = "操作动作不能为空")
    private String action;

    /**
     * 操作资源
     */
    @NotBlank(message = "操作资源不能为空")
    private String resource;

    /**
     * 资源ID
     */
    private String resourceId;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 操作前数据
     */
    private String oldValues;

    /**
     * 操作后数据
     */
    private String newValues;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 操作结果
     */
    private String result;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行时间(毫秒)
     */
    private Integer executionTime;

    /**
     * 检查操作是否成功
     */
    public boolean isSuccess() {
        return "success".equals(result);
    }

    /**
     * 检查操作是否失败
     */
    public boolean isFailure() {
        return "failure".equals(result) || "error".equals(result);
    }

    /**
     * 检查是否为用户操作
     */
    public boolean isUserOperation() {
        return userId != null;
    }

    /**
     * 检查是否为系统操作
     */
    public boolean isSystemOperation() {
        return userId == null;
    }

    /**
     * 检查是否为登录操作
     */
    public boolean isLoginAction() {
        return "login".equals(action);
    }

    /**
     * 检查是否为创建操作
     */
    public boolean isCreateAction() {
        return "create".equals(action);
    }

    /**
     * 检查是否为更新操作
     */
    public boolean isUpdateAction() {
        return "update".equals(action);
    }

    /**
     * 检查是否为删除操作
     */
    public boolean isDeleteAction() {
        return "delete".equals(action);
    }

    /**
     * 检查是否为敏感操作
     */
    public boolean isSensitiveAction() {
        return "delete".equals(action) || 
               "withdraw".equals(action) || 
               "transfer".equals(action) ||
               "admin_login".equals(action);
    }

    /**
     * 检查执行时间是否过长
     */
    public boolean isSlowExecution(int thresholdMs) {
        return executionTime != null && executionTime > thresholdMs;
    }
}