package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * KYC审核记录实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kyc_reviews")
public class KycReview extends BaseEntity {

    /**
     * KYC ID
     */
    private Long kycId;

    /**
     * 审核人员ID
     */
    private Long reviewerId;

    /**
     * 审核结果
     */
    @NotBlank(message = "审核结果不能为空")
    private String status;

    /**
     * 审核备注
     */
    private String reviewNote;

    /**
     * 审核时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewedAt;

    /**
     * 检查是否通过审核
     */
    public boolean isApproved() {
        return "approved".equals(status);
    }

    /**
     * 检查是否拒绝审核
     */
    public boolean isRejected() {
        return "rejected".equals(status);
    }

    /**
     * 检查是否需要重新提交
     */
    public boolean requiresResubmit() {
        return "requires_resubmit".equals(status);
    }
}