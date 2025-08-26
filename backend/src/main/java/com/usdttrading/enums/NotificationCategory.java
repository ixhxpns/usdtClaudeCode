package com.usdttrading.enums;

/**
 * 通知分類枚舉
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
public enum NotificationCategory {
    SYSTEM("system", "系統通知"),
    ORDER("order", "訂單通知"),
    TRANSACTION("transaction", "交易通知"),
    SECURITY("security", "安全通知"),
    WALLET("wallet", "錢包通知"),
    KYC("kyc", "KYC通知"),
    MARKETING("marketing", "營銷通知"),
    MAINTENANCE("maintenance", "維護通知");

    private final String code;
    private final String description;

    NotificationCategory(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static NotificationCategory fromCode(String code) {
        for (NotificationCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return null;
    }
}