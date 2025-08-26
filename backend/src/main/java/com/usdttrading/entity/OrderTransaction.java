package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 订单区块链交易记录实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_transactions")
public class OrderTransaction extends BaseEntity {

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**
     * 区块链交易哈希
     */
    @NotBlank(message = "交易哈希不能为空")
    private String transactionHash;

    /**
     * 区块号
     */
    private Long blockNumber;

    /**
     * 确认数
     */
    private Integer confirmations;

    /**
     * 使用的Gas
     */
    private BigDecimal gasUsed;

    /**
     * Gas价格
     */
    private BigDecimal gasPrice;

    /**
     * 交易状态
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 检查交易是否确认
     */
    public boolean isConfirmed() {
        return "confirmed".equals(status);
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
        return "pending".equals(status);
    }

    /**
     * 计算Gas费用
     */
    public BigDecimal getGasFee() {
        if (gasUsed == null || gasPrice == null) {
            return BigDecimal.ZERO;
        }
        return gasUsed.multiply(gasPrice);
    }

    /**
     * 检查是否有足够的确认数
     */
    public boolean hasEnoughConfirmations(int requiredConfirmations) {
        return confirmations != null && confirmations >= requiredConfirmations;
    }
}