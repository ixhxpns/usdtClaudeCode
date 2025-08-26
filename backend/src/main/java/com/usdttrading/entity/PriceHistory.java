package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 价格历史实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("price_history")
public class PriceHistory extends BaseEntity {

    /**
     * 交易对
     */
    private String currencyPair;

    /**
     * 价格
     */
    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    /**
     * 交易量
     */
    private BigDecimal volume;

    /**
     * 最高价
     */
    private BigDecimal high;

    /**
     * 最低价
     */
    private BigDecimal low;

    /**
     * 开盘价
     */
    private BigDecimal open;

    /**
     * 收盘价
     */
    private BigDecimal close;

    /**
     * 价格来源
     */
    private String source;

    /**
     * 时间间隔
     */
    private String intervalType;

    /**
     * 买入价格
     */
    private BigDecimal buyPrice;

    /**
     * 卖出价格
     */
    private BigDecimal sellPrice;

    /**
     * 价格变动原因
     */
    private String reason;

    /**
     * 价格时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "时间戳不能为空")
    private LocalDateTime timestamp;

    /**
     * 检查是否为USDT/TWD交易对
     */
    public boolean isUsdtTwdPair() {
        return "USDT/TWD".equals(currencyPair);
    }

    /**
     * 计算价格变化率
     */
    public BigDecimal getPriceChangeRate() {
        if (open == null || close == null || open.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return close.subtract(open).divide(open, 4, RoundingMode.HALF_UP);
    }

    /**
     * 检查是否为1分钟K线
     */
    public boolean is1MinuteInterval() {
        return "1m".equals(intervalType);
    }

    /**
     * 检查是否为日K线
     */
    public boolean isDailyInterval() {
        return "1d".equals(intervalType);
    }
}