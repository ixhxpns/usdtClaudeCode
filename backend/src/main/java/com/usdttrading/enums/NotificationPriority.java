package com.usdttrading.enums;

/**
 * 通知優先級枚舉
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
public enum NotificationPriority {
    LOW("low", "低優先級"),
    NORMAL("normal", "普通"),
    HIGH("high", "高優先級"),
    URGENT("urgent", "緊急");

    private final String code;
    private final String description;

    NotificationPriority(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static NotificationPriority fromCode(String code) {
        for (NotificationPriority priority : values()) {
            if (priority.code.equals(code)) {
                return priority;
            }
        }
        return null;
    }
}