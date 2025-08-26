package com.usdttrading.enums;

/**
 * KYC審核狀態枚舉
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
public enum KycStatus {
    PENDING("pending", "待審核"),
    APPROVED("approved", "已通過"),
    REJECTED("rejected", "已拒絕"),
    UNDER_REVIEW("under_review", "審核中"),
    REQUIRES_RESUBMIT("requires_resubmit", "需要重新提交");

    private final String code;
    private final String description;

    KycStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static KycStatus fromCode(String code) {
        for (KycStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}