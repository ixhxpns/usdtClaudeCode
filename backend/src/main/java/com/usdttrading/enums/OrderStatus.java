package com.usdttrading.enums;

/**
 * 订单状态枚举
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
public enum OrderStatus {
    PENDING("pending", "待处理"),
    PAID("paid", "已支付"),
    PROCESSING("processing", "处理中"),
    COMPLETED("completed", "已完成"),
    CANCELLED("cancelled", "已取消"),
    REJECTED("rejected", "已拒绝"),
    EXPIRED("expired", "已过期"),
    FAILED("failed", "失败");

    private final String code;
    private final String description;

    OrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static OrderStatus fromCode(String code) {
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == REJECTED || this == EXPIRED || this == FAILED;
    }

    public boolean isActive() {
        return this == PENDING || this == PAID || this == PROCESSING;
    }
}