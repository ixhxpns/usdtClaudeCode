package com.usdttrading.enums;

import lombok.Getter;

/**
 * 交易狀態枚舉
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-21
 */
@Getter
public enum TransactionStatus {
    
    /**
     * 待處理
     */
    PENDING("pending", "待處理"),
    
    /**
     * 處理中
     */
    PROCESSING("processing", "處理中"),
    
    /**
     * 已完成
     */
    COMPLETED("completed", "已完成"),
    
    /**
     * 已取消
     */
    CANCELLED("cancelled", "已取消"),
    
    /**
     * 失敗
     */
    FAILED("failed", "失敗"),
    
    /**
     * 確認中
     */
    CONFIRMING("confirming", "確認中"),
    
    /**
     * 審核中
     */
    REVIEWING("reviewing", "審核中"),
    
    /**
     * 審核拒絕
     */
    REJECTED("rejected", "審核拒絕"),
    
    /**
     * 部分完成
     */
    PARTIAL("partial", "部分完成");

    private final String code;
    private final String description;

    TransactionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根據代碼獲取枚舉
     */
    public static TransactionStatus fromCode(String code) {
        for (TransactionStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown transaction status code: " + code);
    }

    /**
     * 檢查是否為最終狀態（不可再變更）
     */
    public boolean isFinalStatus() {
        return this == COMPLETED || this == CANCELLED || this == FAILED || this == REJECTED;
    }

    /**
     * 檢查是否為處理中狀態
     */
    public boolean isProcessing() {
        return this == PROCESSING || this == CONFIRMING || this == REVIEWING;
    }

    /**
     * 檢查是否為成功狀態
     */
    public boolean isSuccessful() {
        return this == COMPLETED || this == PARTIAL;
    }
}