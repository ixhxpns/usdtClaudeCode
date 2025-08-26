package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.usdttrading.enums.UserStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 
 * @author ArchitectAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    /**
     * 用户名 (可选，如果为空则使用邮箱)
     */
    private String username;

    /**
     * 邮箱地址
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 密码哈希
     */
    @JsonIgnore
    private String passwordHash;

    /**
     * 密码盐值
     */
    @JsonIgnore
    private String salt;

    /**
     * Google验证器密钥
     */
    @JsonIgnore
    private String googleAuthKey;

    /**
     * 账户状态
     */
    private UserStatus status;

    /**
     * 邮箱是否验证
     */
    private Boolean emailVerified;

    /**
     * 手机是否验证
     */
    private Boolean phoneVerified;

    /**
     * 是否启用Google验证
     */
    private Boolean googleAuthEnabled;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 最后登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 登录尝试次数
     */
    private Integer loginAttempts;

    /**
     * 锁定到期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lockedUntil;

    /**
     * 角色名称（非数据库字段）
     */
    @TableField(exist = false)
    private String roleName;

    /**
     * 用户权限列表（非数据库字段）
     */
    @TableField(exist = false)
    private java.util.List<String> permissions;

    /**
     * 检查账户是否被锁定
     */
    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * 检查账户是否激活
     */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(status);
    }

    /**
     * 检查邮箱是否已验证
     */
    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified);
    }

    /**
     * 检查手机是否已验证
     */
    public boolean isPhoneVerified() {
        return Boolean.TRUE.equals(phoneVerified);
    }

    /**
     * 检查是否启用双因子认证
     */
    public boolean isTwoFactorEnabled() {
        return Boolean.TRUE.equals(googleAuthEnabled) && googleAuthKey != null;
    }

    /**
     * 获取用户名 (如果没有设置username则返回email)
     */
    public String getUsername() {
        return username != null && !username.trim().isEmpty() ? username : email;
    }
}