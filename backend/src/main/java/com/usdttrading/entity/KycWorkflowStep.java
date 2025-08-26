package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * KYC審核工作流步驟實體類
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kyc_workflow_steps")
public class KycWorkflowStep extends BaseEntity {

    /**
     * KYC ID
     */
    @NotNull(message = "KYC ID不能為空")
    private Long kycId;

    /**
     * 步驟編號 (1: 自動預審, 2: 初級審核, 3: 高級審核, 4: 風控終審)
     */
    @NotNull(message = "步驟編號不能為空")
    private Integer stepNumber;

    /**
     * 步驟名稱
     */
    @NotBlank(message = "步驟名稱不能為空")
    private String stepName;

    /**
     * 步驟狀態 (PENDING, IN_PROGRESS, COMPLETED, REJECTED, SKIPPED)
     */
    @NotBlank(message = "步驟狀態不能為空")
    private String status;

    /**
     * 分配給的審核員ID (自動審核時為null)
     */
    private Long assignedReviewerId;

    /**
     * 實際執行審核的人員ID
     */
    private Long actualReviewerId;

    /**
     * 審核結果 (APPROVED, REJECTED, REQUIRES_SUPPLEMENT)
     */
    private String result;

    /**
     * 審核意見
     */
    private String reviewComment;

    /**
     * 需要補充的材料說明
     */
    private String supplementRequirement;

    /**
     * 步驟開始時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    /**
     * 步驟完成時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    /**
     * 步驟處理時長 (分鐘)
     */
    private Integer processingTimeMinutes;

    /**
     * 是否需要人工介入
     */
    private Boolean requiresManualIntervention;

    /**
     * 審核品質評分 (1-5)
     */
    private Integer qualityScore;

    /**
     * 附加數據 (JSON格式)
     */
    private String additionalData;

    /**
     * 檢查步驟是否已完成
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    /**
     * 檢查步驟是否被拒絕
     */
    public boolean isRejected() {
        return "REJECTED".equals(status);
    }

    /**
     * 檢查步驟是否進行中
     */
    public boolean isInProgress() {
        return "IN_PROGRESS".equals(status);
    }

    /**
     * 檢查步驟是否待處理
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    /**
     * 檢查是否需要補充材料
     */
    public boolean requiresSupplement() {
        return "REQUIRES_SUPPLEMENT".equals(result);
    }

    /**
     * 計算處理時長
     */
    public void calculateProcessingTime() {
        if (startedAt != null && completedAt != null) {
            long minutes = java.time.Duration.between(startedAt, completedAt).toMinutes();
            this.processingTimeMinutes = (int) minutes;
        }
    }
}