package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.usdttrading.enums.WithdrawalStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提款申请实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("withdrawals")
public class Withdrawal extends BaseEntity {

    /**
     * 提款单号
     */
    @NotBlank(message = "提款单号不能为空")
    private String withdrawalNo;

    /**
     * 获取提款单号 (别名方法，用于兼容性)
     */
    public String getWithdrawalNumber() {
        return this.withdrawalNo;
    }

    /**
     * 设置提款单号 (别名方法，用于兼容性)
     */
    public void setWithdrawalNumber(String withdrawalNumber) {
        this.withdrawalNo = withdrawalNumber;
    }

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 钱包ID
     */
    @NotNull(message = "钱包ID不能为空")
    private Long walletId;

    /**
     * 提款金额
     */
    @NotNull(message = "提款金额不能为空")
    private BigDecimal amount;

    /**
     * 手续费
     */
    private BigDecimal fee;

    /**
     * 实际到账金额
     */
    @NotNull(message = "实际到账金额不能为空")
    private BigDecimal actualAmount;

    /**
     * 提款地址
     */
    @NotBlank(message = "提款地址不能为空")
    private String toAddress;
    
    /**
     * 网络类型
     */
    private String network;
    
    /**
     * 客户端IP地址
     */
    private String clientIp;

    /**
     * 提款状态
     */
    private WithdrawalStatus status;

    /**
     * 审核级别
     */
    private String reviewLevel;

    /**
     * 审核人员ID
     */
    private Long reviewerId;

    /**
     * 审核备注
     */
    private String reviewNote;

    /**
     * 拒绝原因
     */
    private String rejectionReason;

    /**
     * 区块链交易哈希
     */
    private String transactionHash;

    /**
     * 区块号
     */
    private Long blockNumber;

    /**
     * 风险评分(0-100)
     */
    private Integer riskScore;

    /**
     * 申请IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 审核时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewedAt;

    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedAt;

    /**
     * 检查是否待审核
     */
    public boolean isPending() {
        return WithdrawalStatus.PENDING.equals(status);
    }

    /**
     * 检查是否审核中
     */
    public boolean isReviewing() {
        return WithdrawalStatus.REVIEWING.equals(status);
    }

    /**
     * 检查是否已批准
     */
    public boolean isApproved() {
        return WithdrawalStatus.APPROVED.equals(status);
    }

    /**
     * 检查是否处理中
     */
    public boolean isProcessing() {
        return WithdrawalStatus.PROCESSING.equals(status);
    }

    /**
     * 检查是否已完成
     */
    public boolean isCompleted() {
        return WithdrawalStatus.COMPLETED.equals(status);
    }

    /**
     * 检查是否被拒绝
     */
    public boolean isRejected() {
        return WithdrawalStatus.REJECTED.equals(status);
    }

    /**
     * 检查是否已取消
     */
    public boolean isCancelled() {
        return WithdrawalStatus.CANCELLED.equals(status);
    }

    /**
     * 检查是否为自动审核
     */
    public boolean isAutoReview() {
        return "auto".equals(reviewLevel);
    }

    /**
     * 检查是否需要人工审核
     */
    public boolean needsManualReview() {
        return "manual".equals(reviewLevel) || "senior".equals(reviewLevel);
    }

    /**
     * 检查是否为高风险提款
     */
    public boolean isHighRisk() {
        return riskScore != null && riskScore >= 70;
    }

    /**
     * 计算手续费率
     */
    public BigDecimal getFeeRate() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0 || fee == null) {
            return BigDecimal.ZERO;
        }
        return fee.divide(amount, 4, BigDecimal.ROUND_HALF_UP);
    }
}