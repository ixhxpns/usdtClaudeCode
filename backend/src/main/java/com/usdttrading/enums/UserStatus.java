package com.usdttrading.enums;

/**
 * 用户状态枚举
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
public enum UserStatus {
    ACTIVE("active", "激活"),
    INACTIVE("inactive", "未激活"),
    FROZEN("frozen", "冻结"),
    DELETED("deleted", "已删除");

    private final String code;
    private final String description;

    UserStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserStatus fromCode(String code) {
        for (UserStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}