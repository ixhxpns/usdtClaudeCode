package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.usdttrading.enums.OrderType;
import com.usdttrading.enums.OrderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 订单实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class Order extends BaseEntity {

    /**
     * 订单号
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 订单编号 (与orderNo同义，为了兼容性)
     */
    public String getOrderNumber() {
        return this.orderNo;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNo = orderNumber;
    }

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 订单类型
     */
    @NotNull(message = "订单类型不能为空")
    private OrderType orderType;

    /**
     * 获取订单类型字符串
     */
    public String getType() {
        return orderType != null ? orderType.name().toLowerCase() : null;
    }

    /**
     * 设置订单类型字符串
     */
    public void setType(String type) {
        if (type != null) {
            this.orderType = OrderType.valueOf(type.toUpperCase());
        }
    }

    /**
     * 交易对
     */
    private String currencyPair;

    /**
     * USDT数量
     */
    @NotNull(message = "USDT数量不能为空")
    private BigDecimal amount;

    /**
     * 获取USDT数量 (与amount同义，为了兼容性)
     */
    public BigDecimal getUsdtAmount() {
        return this.amount;
    }

    public void setUsdtAmount(BigDecimal usdtAmount) {
        this.amount = usdtAmount;
    }

    /**
     * 单价
     */
    @NotNull(message = "单价不能为空")
    private BigDecimal price;

    /**
     * 总金额(TWD)
     */
    @NotNull(message = "总金额不能为空")
    private BigDecimal totalAmount;

    /**
     * 已成交数量
     */
    private BigDecimal filledAmount;

    /**
     * 订单状态
     */
    private OrderStatus orderStatus;

    /**
     * 获取订单状态字符串
     */
    public String getStatus() {
        return orderStatus != null ? orderStatus.name().toLowerCase() : null;
    }

    /**
     * 设置订单状态字符串
     */
    public void setStatus(String status) {
        if (status != null) {
            this.orderStatus = OrderStatus.valueOf(status.toUpperCase());
        }
    }

    /**
     * 获取订单状态枚举
     */
    public OrderStatus getOrderStatus() {
        return this.orderStatus;
    }

    /**
     * 设置订单状态枚举
     */
    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 支付信息
     */
    private String paymentInfo;

    /**
     * 收款银行账号
     */
    private String bankAccount;

    /**
     * 支付截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentDeadline;

    /**
     * 管理员备注
     */
    private String adminNote;
    
    /**
     * 客服备注
     */
    private String customerServiceNotes;
    
    /**
     * 客户端IP地址
     */
    private String clientIp;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 收款账户
     */
    private String receivingAccount;
    
    /**
     * 收款银行
     */
    private String receivingBank;
    
    /**
     * 支付凭证
     */
    private String paymentProof;
    
    /**
     * 支付确认时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentConfirmTime;
    
    /**
     * 取消原因
     */
    private String cancelReason;
    
    /**
     * 取消时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;

    /**
     * 获取管理员备注 (与adminNote同义，为了兼容性)
     */
    public String getAdminNotes() {
        return this.adminNote;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNote = adminNotes;
    }

    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    /**
     * 检查是否为买单
     */
    public boolean isBuyOrder() {
        return orderType == OrderType.BUY;
    }

    /**
     * 检查是否为卖单
     */
    public boolean isSellOrder() {
        return orderType == OrderType.SELL;
    }

    /**
     * 检查订单是否完成
     */
    public boolean isCompleted() {
        return orderStatus == OrderStatus.COMPLETED;
    }

    /**
     * 检查订单是否取消
     */
    public boolean isCancelled() {
        return orderStatus == OrderStatus.CANCELLED;
    }

    /**
     * 检查订单是否待处理
     */
    public boolean isPending() {
        return orderStatus == OrderStatus.PENDING;
    }

    /**
     * 检查订单是否已支付
     */
    public boolean isPaid() {
        return orderStatus == OrderStatus.PAID;
    }

    /**
     * 检查订单是否过期
     */
    public boolean isExpired() {
        return orderStatus == OrderStatus.EXPIRED || 
               (paymentDeadline != null && paymentDeadline.isBefore(LocalDateTime.now()));
    }

    /**
     * 计算剩余数量
     */
    public BigDecimal getRemainingAmount() {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal filled = filledAmount != null ? filledAmount : BigDecimal.ZERO;
        return amount.subtract(filled);
    }

    /**
     * 计算完成百分比
     */
    public BigDecimal getFilledPercentage() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal filled = filledAmount != null ? filledAmount : BigDecimal.ZERO;
        return filled.divide(amount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }
}