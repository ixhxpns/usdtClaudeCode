package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 钱包交易记录实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wallet_transactions")
public class WalletTransaction extends BaseEntity {

    /**
     * 钱包ID
     */
    @NotNull(message = "钱包ID不能为空")
    private Long walletId;

    /**
     * 区块链交易哈希
     */
    private String transactionHash;

    /**
     * 交易类型
     */
    @NotNull(message = "交易类型不能为空")
    private String type;

    /**
     * 交易金额
     */
    @NotNull(message = "交易金额不能为空")
    private BigDecimal amount;

    /**
     * 手续费
     */
    private BigDecimal fee;

    /**
     * 交易前余额
     */
    @NotNull(message = "交易前余额不能为空")
    private BigDecimal balanceBefore;

    /**
     * 交易后余额
     */
    @NotNull(message = "交易后余额不能为空")
    private BigDecimal balanceAfter;

    /**
     * 交易状态
     */
    private String status;

    /**
     * 区块号
     */
    private Long blockNumber;

    /**
     * 确认数
     */
    private Integer confirmations;

    /**
     * 发送地址
     */
    private String fromAddress;

    /**
     * 接收地址
     */
    private String toAddress;

    /**
     * 备注
     */
    private String memo;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 检查是否为充值交易
     */
    public boolean isDeposit() {
        return "deposit".equals(type);
    }

    /**
     * 检查是否为提款交易
     */
    public boolean isWithdrawal() {
        return "withdrawal".equals(type);
    }

    /**
     * 检查是否为转账交易
     */
    public boolean isTransfer() {
        return "transfer_in".equals(type) || "transfer_out".equals(type);
    }

    /**
     * 检查交易是否完成
     */
    public boolean isCompleted() {
        return "completed".equals(status);
    }

    /**
     * 检查交易是否失败
     */
    public boolean isFailed() {
        return "failed".equals(status);
    }

    /**
     * 检查交易是否待确认
     */
    public boolean isPending() {
        return "pending".equals(status) || "confirming".equals(status);
    }
}