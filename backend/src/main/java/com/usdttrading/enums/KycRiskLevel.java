package com.usdttrading.enums;

/**
 * KYC風險等級枚舉
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
public enum KycRiskLevel {
    VERY_LOW(1, "極低風險"),
    LOW(2, "低風險"),
    MEDIUM_LOW(3, "中低風險"),
    MEDIUM(4, "中等風險"),
    MEDIUM_HIGH(5, "中高風險"),
    HIGH(6, "高風險"),
    VERY_HIGH(7, "極高風險"),
    CRITICAL(8, "關鍵風險");

    private final Integer level;
    private final String description;

    KycRiskLevel(Integer level, String description) {
        this.level = level;
        this.description = description;
    }

    public Integer getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public static KycRiskLevel fromLevel(Integer level) {
        for (KycRiskLevel riskLevel : values()) {
            if (riskLevel.level.equals(level)) {
                return riskLevel;
            }
        }
        return MEDIUM; // 預設為中等風險
    }

    /**
     * 檢查是否為低風險
     */
    public boolean isLowRisk() {
        return this.level <= 3;
    }

    /**
     * 檢查是否為高風險
     */
    public boolean isHighRisk() {
        return this.level >= 6;
    }

    /**
     * 檢查是否需要額外審核
     */
    public boolean requiresExtraReview() {
        return this.level >= 5;
    }

    /**
     * 檢查是否可以自動通過
     */
    public boolean canAutoApprove() {
        return this.level <= 2;
    }
}