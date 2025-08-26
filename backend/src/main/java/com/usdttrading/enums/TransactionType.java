package com.usdttrading.enums;

/**
 * 交易类型枚举
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
public enum TransactionType {
    DEPOSIT("deposit", "充值"),
    WITHDRAWAL("withdrawal", "提款"),
    TRANSFER_IN("transfer_in", "转入"),
    TRANSFER_OUT("transfer_out", "转出"),
    FEE("fee", "手续费"),
    REWARD("reward", "奖励"),
    BUY("buy", "買入"),
    SELL("sell", "賣出");

    private final String code;
    private final String description;

    TransactionType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TransactionType fromCode(String code) {
        for (TransactionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    public boolean isInbound() {
        return this == DEPOSIT || this == TRANSFER_IN || this == REWARD || this == SELL;
    }

    public boolean isOutbound() {
        return this == WITHDRAWAL || this == TRANSFER_OUT || this == FEE || this == BUY;
    }
}