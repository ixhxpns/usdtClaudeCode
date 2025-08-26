package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * KYC審核配置實體類
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kyc_review_configs")
public class KycReviewConfig extends BaseEntity {

    /**
     * 配置名稱
     */
    @NotBlank(message = "配置名稱不能為空")
    private String configName;

    /**
     * 配置類型 (RISK_THRESHOLD, AUTO_APPROVAL, WORKFLOW_RULE)
     */
    @NotBlank(message = "配置類型不能為空")
    private String configType;

    /**
     * 最小年齡限制
     */
    @Min(value = 16, message = "最小年齡不能低於16歲")
    @Max(value = 25, message = "最小年齡不能高於25歲")
    private Integer minAge;

    /**
     * 最大年齡限制
     */
    @Max(value = 100, message = "最大年齡不能高於100歲")
    private Integer maxAge;

    /**
     * 自動通過風險分數閾值
     */
    @Min(value = 0, message = "風險分數不能低於0")
    @Max(value = 100, message = "風險分數不能高於100")
    private BigDecimal autoApprovalRiskThreshold;

    /**
     * 自動拒絕風險分數閾值
     */
    @Min(value = 0, message = "風險分數不能低於0")
    @Max(value = 100, message = "風險分數不能高於100")
    private BigDecimal autoRejectionRiskThreshold;

    /**
     * OCR最低準確度要求
     */
    @Min(value = 0, message = "OCR準確度不能低於0")
    @Max(value = 100, message = "OCR準確度不能高於100")
    private BigDecimal minOcrAccuracy;

    /**
     * 人臉匹配最低分數要求
     */
    @Min(value = 0, message = "人臉匹配分數不能低於0")
    @Max(value = 100, message = "人臉匹配分數不能高於100")
    private BigDecimal minFaceMatchScore;

    /**
     * 是否啟用自動審核
     */
    @NotNull(message = "自動審核設置不能為空")
    private Boolean autoReviewEnabled;

    /**
     * 是否啟用黑名單檢查
     */
    @NotNull(message = "黑名單檢查設置不能為空")
    private Boolean blacklistCheckEnabled;

    /**
     * 是否啟用AML檢查
     */
    @NotNull(message = "AML檢查設置不能為空")
    private Boolean amlCheckEnabled;

    /**
     * 是否啟用重複申請檢查
     */
    @NotNull(message = "重複申請檢查設置不能為空")
    private Boolean duplicateCheckEnabled;

    /**
     * 審核超時時間 (小時)
     */
    @Min(value = 1, message = "審核超時時間不能少於1小時")
    @Max(value = 72, message = "審核超時時間不能超過72小時")
    private Integer reviewTimeoutHours;

    /**
     * 每日最大處理數量
     */
    @Min(value = 1, message = "每日最大處理數量不能少於1")
    private Integer dailyMaxProcessing;

    /**
     * 單個審核員每日最大處理數量
     */
    @Min(value = 1, message = "單個審核員每日最大處理數量不能少於1")
    private Integer reviewerDailyMaxProcessing;

    /**
     * 是否需要雙重審核
     */
    private Boolean requiresDualReview;

    /**
     * 高風險用戶需要額外審核級別
     */
    private Boolean highRiskRequiresExtraReview;

    /**
     * 工作流步驟配置 (JSON格式)
     */
    private String workflowStepsConfig;

    /**
     * 通知配置 (JSON格式)
     */
    private String notificationConfig;

    /**
     * 風險權重配置 (JSON格式)
     */
    private String riskWeightConfig;

    /**
     * 地域風險配置 (JSON格式)
     */
    private String geographicRiskConfig;

    /**
     * 職業風險配置 (JSON格式)
     */
    private String occupationRiskConfig;

    /**
     * 是否啟用配置
     */
    @NotNull(message = "配置啟用狀態不能為空")
    private Boolean enabled;

    /**
     * 配置版本
     */
    private String configVersion;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 檢查風險分數是否需要自動通過
     */
    public boolean shouldAutoApprove(BigDecimal riskScore) {
        return autoApprovalRiskThreshold != null && 
               riskScore != null && 
               riskScore.compareTo(autoApprovalRiskThreshold) <= 0;
    }

    /**
     * 檢查風險分數是否需要自動拒絕
     */
    public boolean shouldAutoReject(BigDecimal riskScore) {
        return autoRejectionRiskThreshold != null && 
               riskScore != null && 
               riskScore.compareTo(autoRejectionRiskThreshold) >= 0;
    }

    /**
     * 檢查OCR準確度是否滿足要求
     */
    public boolean meetsOcrRequirement(BigDecimal ocrAccuracy) {
        return minOcrAccuracy == null || 
               (ocrAccuracy != null && ocrAccuracy.compareTo(minOcrAccuracy) >= 0);
    }

    /**
     * 檢查人臉匹配分數是否滿足要求
     */
    public boolean meetsFaceMatchRequirement(BigDecimal faceMatchScore) {
        return minFaceMatchScore == null || 
               (faceMatchScore != null && faceMatchScore.compareTo(minFaceMatchScore) >= 0);
    }
}