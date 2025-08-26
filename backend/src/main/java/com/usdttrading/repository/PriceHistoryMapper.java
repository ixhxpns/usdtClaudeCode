package com.usdttrading.repository;

import com.usdttrading.entity.PriceHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 價格歷史Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface PriceHistoryMapper extends BaseMapper<PriceHistory> {

    /**
     * 查詢最新價格
     */
    @Select("SELECT * FROM price_history WHERE currency_pair = #{currencyPair} ORDER BY timestamp DESC LIMIT 1")
    PriceHistory selectLatestPrice(@Param("currencyPair") String currencyPair);

    /**
     * 查詢指定時間範圍的價格歷史
     */
    @Select("SELECT * FROM price_history WHERE currency_pair = #{currencyPair} AND timestamp BETWEEN #{startTime} AND #{endTime} AND interval_type = #{intervalType} ORDER BY timestamp ASC")
    List<PriceHistory> selectPriceHistory(
        @Param("currencyPair") String currencyPair,
        @Param("intervalType") String intervalType,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 獲取24小時價格統計
     */
    @Select("SELECT " +
            "MAX(price) as high, " +
            "MIN(price) as low, " +
            "(SELECT price FROM price_history WHERE currency_pair = #{currencyPair} AND timestamp >= #{startTime} ORDER BY timestamp ASC LIMIT 1) as open, " +
            "(SELECT price FROM price_history WHERE currency_pair = #{currencyPair} AND timestamp >= #{startTime} ORDER BY timestamp DESC LIMIT 1) as close, " +
            "SUM(volume) as volume " +
            "FROM price_history " +
            "WHERE currency_pair = #{currencyPair} AND timestamp >= #{startTime}")
    PriceHistory select24HourStats(@Param("currencyPair") String currencyPair, @Param("startTime") LocalDateTime startTime);

    /**
     * 查詢平均價格
     */
    @Select("SELECT AVG(price) FROM price_history WHERE currency_pair = #{currencyPair} AND timestamp BETWEEN #{startTime} AND #{endTime}")
    BigDecimal selectAveragePrice(
        @Param("currencyPair") String currencyPair,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}