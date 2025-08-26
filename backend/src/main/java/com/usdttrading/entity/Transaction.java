package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.usdttrading.enums.TransactionStatus;
import com.usdttrading.enums.TransactionType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 通用交易記錄實體類
 * 包括充值、提現、交易等所有類型的交易記錄
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("transactions")
public class Transaction extends BaseEntity {

    /**
     * 用戶ID
     */
    @NotNull(message = "用戶ID不能為空")
    private Long userId;

    /**
     * 交易號
     */
    @NotNull(message = "交易號不能為空")
    private String transactionNumber;

    /**
     * 交易類型
     */
    @NotNull(message = "交易類型不能為空")
    private TransactionType type;

    /**
     * 交易金額
     */
    @NotNull(message = "交易金額不能為空")
    private BigDecimal amount;

    /**
     * 手續費
     */
    private BigDecimal fee;

    /**
     * 幣種
     */
    private String currency;

    /**
     * 交易狀態
     */
    @NotNull(message = "交易狀態不能為空")
    private TransactionStatus status;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 銀行帳號
     */
    private String bankAccount;

    /**
     * 銀行名稱
     */
    private String bankName;

    /**
     * 支付憑證
     */
    private String paymentProof;

    /**
     * 區塊鏈交易哈希
     */
    private String transactionHash;

    /**
     * 發送地址
     */
    private String fromAddress;

    /**
     * 接收地址
     */
    private String toAddress;

    /**
     * 網絡類型 (TRC20, ERC20, etc.)
     */
    private String network;

    /**
     * 區塊號
     */
    private Long blockNumber;

    /**
     * 確認數
     */
    private Integer confirmations;

    /**
     * 管理員備註
     */
    private String adminNotes;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 完成時間
     */
    private LocalDateTime completedAt;

    /**
     * 取消時間
     */
    private LocalDateTime cancelTime;

    /**
     * 客戶端IP
     */
    private String clientIp;

    /**
     * 用戶代理
     */
    private String userAgent;

    /**
     * 檢查是否為充值交易
     */
    public boolean isDeposit() {
        return type == TransactionType.DEPOSIT;
    }

    /**
     * 檢查是否為提現交易
     */
    public boolean isWithdrawal() {
        return type == TransactionType.WITHDRAWAL;
    }

    /**
     * 檢查是否為買入交易
     */
    public boolean isBuy() {
        return type == TransactionType.BUY;
    }

    /**
     * 檢查是否為賣出交易
     */
    public boolean isSell() {
        return type == TransactionType.SELL;
    }

    /**
     * 檢查交易是否完成
     */
    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }

    /**
     * 檢查交易是否失敗
     */
    public boolean isFailed() {
        return status == TransactionStatus.FAILED;
    }

    /**
     * 檢查交易是否待處理
     */
    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }

    /**
     * 檢查交易是否已取消
     */
    public boolean isCancelled() {
        return status == TransactionStatus.CANCELLED;
    }
}