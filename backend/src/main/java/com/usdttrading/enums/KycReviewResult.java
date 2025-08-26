package com.usdttrading.enums;

/**
 * KYC審核結果枚舉
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
public enum KycReviewResult {
    APPROVED("approved", "通過"),
    REJECTED("rejected", "拒絕"),
    REQUIRES_SUPPLEMENT("requires_supplement", "需要補充材料"),
    PENDING_HIGHER_REVIEW("pending_higher_review", "需要更高級別審核"),
    AUTO_APPROVED("auto_approved", "自動通過"),
    AUTO_REJECTED("auto_rejected", "自動拒絕");

    private final String code;
    private final String description;

    KycReviewResult(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static KycReviewResult fromCode(String code) {
        for (KycReviewResult result : values()) {
            if (result.code.equals(code)) {
                return result;
            }
        }
        return null;
    }

    /**
     * 檢查是否為通過結果
     */
    public boolean isApproved() {
        return this == APPROVED || this == AUTO_APPROVED;
    }

    /**
     * 檢查是否為拒絕結果
     */
    public boolean isRejected() {
        return this == REJECTED || this == AUTO_REJECTED;
    }

    /**
     * 檢查是否需要進一步處理
     */
    public boolean requiresFurtherAction() {
        return this == REQUIRES_SUPPLEMENT || this == PENDING_HIGHER_REVIEW;
    }

    /**
     * 檢查是否為自動處理結果
     */
    public boolean isAutomated() {
        return this == AUTO_APPROVED || this == AUTO_REJECTED;
    }
}