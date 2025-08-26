package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 平台钱包池实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("platform_wallets")
public class PlatformWallet extends BaseEntity {

    /**
     * 钱包名称
     */
    @NotBlank(message = "钱包名称不能为空")
    private String name;

    /**
     * 币种
     */
    @NotBlank(message = "币种不能为空")
    private String currency;

    /**
     * 钱包地址
     */
    @NotBlank(message = "钱包地址不能为空")
    private String address;

    /**
     * 私钥(加密存储)
     */
    @JsonIgnore
    @NotBlank(message = "私钥不能为空")
    private String privateKey;

    /**
     * 当前余额
     */
    private BigDecimal balance;

    /**
     * 预留余额
     */
    private BigDecimal reservedBalance;

    /**
     * 钱包类型
     */
    private String walletType;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 是否自动收集
     */
    private Boolean autoCollect;

    /**
     * 收集阈值
     */
    private BigDecimal collectThreshold;

    /**
     * 钱包描述
     */
    private String description;

    /**
     * 检查是否启用
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * 检查是否自动收集
     */
    public boolean isAutoCollect() {
        return Boolean.TRUE.equals(autoCollect);
    }

    /**
     * 检查是否为热钱包
     */
    public boolean isHotWallet() {
        return "hot".equals(walletType);
    }

    /**
     * 检查是否为冷钱包
     */
    public boolean isColdWallet() {
        return "cold".equals(walletType);
    }

    /**
     * 检查是否为手续费钱包
     */
    public boolean isFeeWallet() {
        return "fee".equals(walletType);
    }

    /**
     * 检查是否为TRX钱包
     */
    public boolean isTrxWallet() {
        return "TRX".equals(currency);
    }

    /**
     * 检查是否为USDT钱包
     */
    public boolean isUsdtWallet() {
        return "USDT".equals(currency);
    }

    /**
     * 获取可用余额
     */
    public BigDecimal getAvailableBalance() {
        BigDecimal currentBalance = balance != null ? balance : BigDecimal.ZERO;
        BigDecimal reserved = reservedBalance != null ? reservedBalance : BigDecimal.ZERO;
        return currentBalance.subtract(reserved);
    }

    /**
     * 检查是否有足够的可用余额
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        return getAvailableBalance().compareTo(amount) >= 0;
    }

    /**
     * 检查是否需要收集
     */
    public boolean needsCollection() {
        return isAutoCollect() && collectThreshold != null && 
               balance != null && balance.compareTo(collectThreshold) >= 0;
    }

    /**
     * 检查余额是否不足
     */
    public boolean isLowBalance(BigDecimal lowBalanceThreshold) {
        if (lowBalanceThreshold == null || balance == null) {
            return false;
        }
        return balance.compareTo(lowBalanceThreshold) < 0;
    }
}