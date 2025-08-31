package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 * 用于记录系统操作的审计信息
 * 
 * @author ArchitectAgent
 * @since 2025-08-31
 */
@Data
@TableName("audit_log")
public class AuditLog {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 操作动作
     */
    private String action;
    
    /**
     * 操作资源
     */
    private String resource;
    
    /**
     * 资源ID
     */
    private String resourceId;
    
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
     * 操作是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 操作描述
     */
    private String description;
    
    /**
     * 操作前的值
     */
    private String oldValues;
    
    /**
     * 操作后的值
     */
    private String newValues;
    
    /**
     * 操作结果 (success/failure)
     */
    private String result;
    
    /**
     * 操作详情
     */
    private String details;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}