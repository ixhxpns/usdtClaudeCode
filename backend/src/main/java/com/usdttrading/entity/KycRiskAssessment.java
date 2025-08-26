package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * KYC風險評估記錄實體類
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kyc_risk_assessments")
public class KycRiskAssessment extends BaseEntity {

    /**
     * KYC ID
     */
    @NotNull(message = "KYC ID不能為空")
    private Long kycId;

    /**
     * 用戶ID
     */
    @NotNull(message = "用戶ID不能為空")
    private Long userId;

    /**
     * 風險等級 (1-10, 1最低風險，10最高風險)
     */
    @Min(value = 1, message = "風險等級不能低於1")
    @Max(value = 10, message = "風險等級不能高於10")
    private Integer riskLevel;

    /**
     * 風險分數 (0-100)
     */
    @Min(value = 0, message = "風險分數不能低於0")
    @Max(value = 100, message = "風險分數不能高於100")
    private BigDecimal riskScore;

    /**
     * 年齡風險分數
     */
    private BigDecimal ageRiskScore;

    /**
     * 地域風險分數
     */
    private BigDecimal locationRiskScore;

    /**
     * 職業風險分數
     */
    private BigDecimal occupationRiskScore;

    /**
     * 收入來源風險分數
     */
    private BigDecimal incomeRiskScore;

    /**
     * 黑名單檢查結果
     */
    private Boolean blacklistCheck;

    /**
     * 重複申請檢查結果
     */
    private Boolean duplicateCheck;

    /**
     * AML (反洗錢) 檢查結果
     */
    private Boolean amlCheck;

    /**
     * 身份驗證自動檢查結果
     */
    private Boolean identityVerification;

    /**
     * OCR識別準確度
     */
    private BigDecimal ocrAccuracy;

    /**
     * 人臉匹配度
     */
    private BigDecimal faceMatchScore;

    /**
     * 風險評估詳細報告 (JSON格式)
     */
    private String assessmentDetails;

    /**
     * 評估建議
     */
    private String recommendation;

    /**
     * 是否需要人工審核
     */
    private Boolean requiresManualReview;

    /**
     * 評估完成時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime assessedAt;

    /**
     * 評估系統版本
     */
    private String assessmentVersion;

    /**
     * 判斷是否為高風險用戶
     */
    public boolean isHighRisk() {
        return riskLevel != null && riskLevel >= 7;
    }

    /**
     * 判斷是否為低風險用戶
     */
    public boolean isLowRisk() {
        return riskLevel != null && riskLevel <= 3;
    }

    /**
     * 判斷是否通過自動檢查
     */
    public boolean passesAutoCheck() {
        return Boolean.TRUE.equals(blacklistCheck) && 
               Boolean.TRUE.equals(duplicateCheck) && 
               Boolean.TRUE.equals(amlCheck) && 
               Boolean.TRUE.equals(identityVerification);
    }
}