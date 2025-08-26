package com.usdttrading.enums;

/**
 * 通知類型枚舉
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
public enum NotificationType {
    EMAIL("email", "郵件通知"),
    SMS("sms", "簡訊通知"),
    PUSH("push", "推送通知"),
    SYSTEM("system", "系統內部通知"),
    
    // KYC相關通知
    KYC_SUBMITTED("kyc_submitted", "KYC已提交"),
    KYC_APPROVED("kyc_approved", "KYC審核通過"),
    KYC_REJECTED("kyc_rejected", "KYC審核拒絕"),
    KYC_SUPPLEMENT_REQUIRED("kyc_supplement_required", "KYC需要補充材料"),
    KYC_RESUBMITTED("kyc_resubmitted", "KYC重新提交"),
    
    // 交易相關通知
    ORDER_CREATED("order_created", "訂單創建"),
    ORDER_COMPLETED("order_completed", "訂單完成"),
    ORDER_CANCELLED("order_cancelled", "訂單取消"),
    ORDER_STATUS_CHANGED("order_status_changed", "訂單狀態變更"),
    TRANSACTION_UPDATE("transaction_update", "交易更新"),
    
    // 安全相關通知
    LOGIN_SUCCESS("login_success", "登錄成功"),
    LOGIN_FAILED("login_failed", "登錄失敗"),
    PASSWORD_CHANGED("password_changed", "密碼修改"),
    ACCOUNT_LOCKED("account_locked", "賬戶鎖定");

    private final String code;
    private final String description;

    NotificationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static NotificationType fromCode(String code) {
        for (NotificationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}