package com.usdttrading.enums;

/**
 * 通知狀態枚舉
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
public enum NotificationStatus {
    UNREAD("unread", "未讀"),
    READ("read", "已讀"),
    ARCHIVED("archived", "已歸檔");

    private final String code;
    private final String description;

    NotificationStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static NotificationStatus fromCode(String code) {
        for (NotificationStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}