package com.usdttrading.enums;

/**
 * 提款狀態枚舉
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
public enum WithdrawalStatus {
    PENDING("pending", "待處理"),
    REVIEWING("reviewing", "審核中"),
    APPROVED("approved", "已批準"),
    PROCESSING("processing", "處理中"),
    COMPLETED("completed", "已完成"),
    REJECTED("rejected", "已拒絕"),
    CANCELLED("cancelled", "已取消"),
    FAILED("failed", "失敗");

    private final String code;
    private final String description;

    WithdrawalStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static WithdrawalStatus fromCode(String code) {
        for (WithdrawalStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}