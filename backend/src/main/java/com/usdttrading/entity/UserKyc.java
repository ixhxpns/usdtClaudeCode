package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.usdttrading.enums.Gender;
import com.usdttrading.enums.IdType;
import com.usdttrading.enums.KycStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * KYC验证实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_kyc")
public class UserKyc extends BaseEntity {

    /**
     * 用户ID
     */
    @NotNull(message = "用戶ID不能為空")
    private Long userId;

    /**
     * 真實姓名 (加密存儲)
     */
    @NotBlank(message = "真實姓名不能為空")
    private String realName;

    /**
     * 英文姓名
     */
    private String englishName;

    /**
     * 性別
     */
    private Gender gender;

    /**
     * 出生日期
     */
    @Past(message = "出生日期必須是過去的日期")
    private LocalDate birthDate;

    /**
     * 國籍
     */
    @NotBlank(message = "國籍不能為空")
    private String nationality;

    /**
     * 居住地址 (加密存儲)
     */
    @NotBlank(message = "居住地址不能為空")
    private String address;

    /**
     * 城市
     */
    private String city;

    /**
     * 省份/州
     */
    private String state;

    /**
     * 郵遞區號
     */
    private String zipCode;

    /**
     * 國家
     */
    @NotBlank(message = "國家不能為空")
    private String country;

    /**
     * 職業
     */
    private String occupation;

    /**
     * 雇主名稱
     */
    private String employerName;

    /**
     * 年收入範圍
     */
    private String incomeRange;

    /**
     * 收入來源
     */
    private String incomeSource;

    /**
     * 資金來源說明
     */
    private String fundSource;

    /**
     * 聯繫電話 (加密存儲)
     */
    private String phoneNumber;

    /**
     * 電子郵箱
     */
    @Email(message = "電子郵箱格式不正確")
    private String email;

    /**
     * 证件类型
     */
    private IdType idType;

    /**
     * 证件号码(加密存储)
     */
    @NotBlank(message = "证件号码不能为空")
    private String idNumber;

    /**
     * 身份证正面照片URL
     */
    private String idCardFront;

    /**
     * 身份证反面照片URL
     */
    private String idCardBack;

    /**
     * 手持身份证自拍照URL
     */
    private String selfiePhoto;

    /**
     * 第二证件类型
     */
    private String secondDocType;

    /**
     * 第二证件照片URL
     */
    private String secondDocUrl;

    /**
     * 银行账号(加密存储)
     */
    private String bankAccount;

    /**
     * 银行名称
     */
    private String bankName;

    /**
     * 支行名称
     */
    private String bankBranch;

    /**
     * 账户持有人姓名
     */
    private String accountHolderName;

    /**
     * KYC状态
     */
    private KycStatus status;

    /**
     * 拒绝原因
     */
    private String rejectionReason;

    /**
     * 验证通过时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime verifiedAt;

    /**
     * 验证到期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    /**
     * KYC等級 (1: 基礎認證, 2: 進階認證, 3: 專業認證)
     */
    private Integer kycLevel;

    /**
     * 當前審核步驟
     */
    private Integer currentStep;

    /**
     * 總審核步驟數
     */
    private Integer totalSteps;

    /**
     * 風險評估分數
     */
    private BigDecimal riskScore;

    /**
     * 風險等級 (1-8)
     */
    private Integer riskLevel;

    /**
     * 最後審核人ID
     */
    private Long lastReviewerId;

    /**
     * 提交次數
     */
    private Integer submissionCount;

    /**
     * 拒絕次數
     */
    private Integer rejectionCount;

    /**
     * 是否需要補充材料
     */
    private Boolean requiresSupplement;

    /**
     * 補充材料說明
     */
    private String supplementRequirement;

    /**
     * 最後提交時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSubmittedAt;

    /**
     * 審核開始時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewStartedAt;

    /**
     * 自動審核通過
     */
    private Boolean autoApproved;

    /**
     * IP地址記錄
     */
    private String ipAddress;

    /**
     * 用戶代理記錄
     */
    private String userAgent;

    /**
     * 地理位置信息
     */
    private String geoLocation;

    /**
     * 检查是否已通过验证
     */
    public boolean isApproved() {
        return KycStatus.APPROVED.equals(status);
    }

    /**
     * 检查是否被拒绝
     */
    public boolean isRejected() {
        return KycStatus.REJECTED.equals(status);
    }

    /**
     * 检查是否待审核
     */
    public boolean isPending() {
        return KycStatus.PENDING.equals(status);
    }

    /**
     * 检查是否已过期
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * 檢查是否需要補充材料
     */
    public boolean requiresSupplementary() {
        return KycStatus.REQUIRES_RESUBMIT.equals(status) || Boolean.TRUE.equals(requiresSupplement);
    }

    /**
     * 檢查是否為審核中狀態
     */
    public boolean isUnderReview() {
        return KycStatus.UNDER_REVIEW.equals(status);
    }

    /**
     * 檢查是否為高風險用戶
     */
    public boolean isHighRisk() {
        return riskLevel != null && riskLevel >= 6;
    }

    /**
     * 獲取審核進度百分比
     */
    public int getProgressPercentage() {
        if (totalSteps == null || currentStep == null || totalSteps == 0) {
            return 0;
        }
        return (int) ((currentStep * 100.0) / totalSteps);
    }

    /**
     * 檢查是否超過最大提交次數
     */
    public boolean exceedsMaxSubmissions(int maxSubmissions) {
        return submissionCount != null && submissionCount >= maxSubmissions;
    }

    /**
     * 檢查是否為自動審核通過
     */
    public boolean isAutoApproved() {
        return Boolean.TRUE.equals(autoApproved);
    }

    /**
     * 增加提交次數
     */
    public void incrementSubmissionCount() {
        if (submissionCount == null) {
            submissionCount = 1;
        } else {
            submissionCount++;
        }
    }

    /**
     * 增加拒絕次數
     */
    public void incrementRejectionCount() {
        if (rejectionCount == null) {
            rejectionCount = 1;
        } else {
            rejectionCount++;
        }
    }
    
    /**
     * 获取KYC等级
     */
    public Integer getLevel() {
        return this.kycLevel;
    }
    
    /**
     * 设置KYC等级
     */
    public void setLevel(Integer level) {
        this.kycLevel = level;
    }
}