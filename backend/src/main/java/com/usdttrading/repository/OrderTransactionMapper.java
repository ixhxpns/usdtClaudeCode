package com.usdttrading.repository;

import com.usdttrading.entity.OrderTransaction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 訂單區塊鏈交易記錄Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface OrderTransactionMapper extends BaseMapper<OrderTransaction> {

    /**
     * 根據訂單ID查詢交易記錄
     */
    @Select("SELECT * FROM order_transactions WHERE order_id = #{orderId} ORDER BY created_at DESC")
    List<OrderTransaction> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 根據交易哈希查詢記錄
     */
    @Select("SELECT * FROM order_transactions WHERE transaction_hash = #{txHash}")
    OrderTransaction selectByTransactionHash(@Param("txHash") String transactionHash);

    /**
     * 查詢待確認的交易
     */
    @Select("SELECT * FROM order_transactions WHERE status = 'pending' AND confirmations < 19")
    List<OrderTransaction> selectPendingConfirmations();

    /**
     * 查詢失敗的交易
     */
    @Select("SELECT * FROM order_transactions WHERE status = 'failed' ORDER BY created_at DESC")
    List<OrderTransaction> selectFailedTransactions();
}