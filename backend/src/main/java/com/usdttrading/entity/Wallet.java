package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 钱包实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wallets")
public class Wallet extends BaseEntity {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 币种
     */
    @NotBlank(message = "币种不能为空")
    private String currency;

    /**
     * 可用余额
     */
    private BigDecimal balance;

    /**
     * 冻结余额
     */
    private BigDecimal frozenBalance;

    /**
     * 钱包地址
     */
    @NotBlank(message = "钱包地址不能为空")
    private String address;

    /**
     * 私钥(加密存储)
     */
    @JsonIgnore
    private String privateKey;

    /**
     * 冻结的USDT余额
     */
    private BigDecimal frozenUsdt;

    /**
     * USDT余额
     */
    private BigDecimal usdtBalance;

    /**
     * TWD余额
     */
    private BigDecimal twdBalance;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 获取总余额
     */
    public BigDecimal getTotalBalance() {
        BigDecimal total = balance != null ? balance : BigDecimal.ZERO;
        BigDecimal frozen = frozenBalance != null ? frozenBalance : BigDecimal.ZERO;
        return total.add(frozen);
    }

    /**
     * 检查是否有足够的可用余额
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        if (amount == null || balance == null) {
            return false;
        }
        return balance.compareTo(amount) >= 0;
    }

    /**
     * 检查钱包是否活跃
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
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
}