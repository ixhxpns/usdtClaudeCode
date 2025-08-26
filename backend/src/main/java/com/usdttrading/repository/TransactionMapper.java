package com.usdttrading.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usdttrading.entity.Transaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易記錄Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-21
 */
@Mapper
public interface TransactionMapper extends BaseMapper<Transaction> {

    /**
     * 根據用戶ID和時間範圍查詢交易記錄
     */
    @Select("SELECT * FROM transactions WHERE user_id = #{userId} " +
            "AND created_at BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY created_at DESC")
    List<Transaction> selectByUserIdAndTimeRange(
            @Param("userId") Long userId, 
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根據用戶ID和狀態查詢交易記錄
     */
    @Select("SELECT * FROM transactions WHERE user_id = #{userId} " +
            "AND status = #{status} ORDER BY created_at DESC")
    List<Transaction> selectByUserIdAndStatus(
            @Param("userId") Long userId, 
            @Param("status") String status);

    /**
     * 根據交易號查詢
     */
    @Select("SELECT * FROM transactions WHERE transaction_number = #{transactionNumber}")
    Transaction selectByTransactionNumber(@Param("transactionNumber") String transactionNumber);
}