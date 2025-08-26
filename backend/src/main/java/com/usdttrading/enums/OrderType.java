package com.usdttrading.enums;

/**
 * 订单类型枚举
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
public enum OrderType {
    BUY("buy", "买入"),
    SELL("sell", "卖出");

    private final String code;
    private final String description;

    OrderType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static OrderType fromCode(String code) {
        for (OrderType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}