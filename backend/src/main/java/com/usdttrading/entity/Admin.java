package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理員實體類
 * 
 * @author Master Agent
 * @version 1.0.0
 * @since 2025-08-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admins")
public class Admin extends BaseEntity {

    /**
     * 管理員用戶名
     */
    @NotBlank(message = "用戶名不能為空")
    private String username;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 手機號碼（數據庫中不存在此欄位）
     */
    @TableField(exist = false)
    private String phone;

    /**
     * 密碼哈希
     */
    @JsonIgnore
    private String password;

    /**
     * 管理員角色
     */
    private String role;

    /**
     * 帳戶狀態 (active, inactive, suspended)
     */
    private String status;

    /**
     * 真實姓名
     */
    private String realName;

    /**
     * 管理員權限JSON字符串
     */
    @JsonIgnore
    private String permissionsJson;

    /**
     * 最後登入時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    /**
     * 最後登入IP
     */
    private String lastLoginIp;

    /**
     * 登入失敗次數
     */
    private Integer loginAttempts;

    /**
     * 帳戶鎖定到期時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lockedUntil;

    /**
     * 是否啟用雙因子認證
     */
    private Boolean mfaEnabled;

    /**
     * 雙因子認證密鑰
     */
    @JsonIgnore
    private String mfaSecret;

    /**
     * 管理員備註
     */
    private String remarks;

    /**
     * 權限列表（非數據庫字段）
     */
    @TableField(exist = false)
    private List<String> permissions;

    /**
     * 檢查帳戶是否被鎖定
     */
    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * 檢查帳戶是否激活
     */
    public boolean isActive() {
        return "active".equals(status);
    }

    /**
     * 檢查是否為超級管理員
     */
    public boolean isSuperAdmin() {
        return "super_admin".equals(role);
    }

    /**
     * 檢查是否啟用雙因子認證
     */
    public boolean isMfaEnabled() {
        return Boolean.TRUE.equals(mfaEnabled) && mfaSecret != null;
    }
}