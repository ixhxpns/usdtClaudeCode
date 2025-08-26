package com.usdttrading.repository;

import com.usdttrading.entity.WalletTransaction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 錢包交易記錄Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface WalletTransactionMapper extends BaseMapper<WalletTransaction> {

    /**
     * 根據錢包ID分頁查詢交易記錄
     */
    @Select("SELECT * FROM wallet_transactions WHERE wallet_id = #{walletId} ORDER BY created_at DESC")
    Page<WalletTransaction> selectPageByWalletId(Page<WalletTransaction> page, @Param("walletId") Long walletId);

    /**
     * 根據交易哈希查詢交易
     */
    @Select("SELECT * FROM wallet_transactions WHERE transaction_hash = #{txHash}")
    WalletTransaction selectByTransactionHash(@Param("txHash") String transactionHash);

    /**
     * 計算錢包總收入
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM wallet_transactions WHERE wallet_id = #{walletId} AND type IN ('deposit', 'transfer_in', 'reward') AND status = 'completed'")
    BigDecimal calculateTotalIncome(@Param("walletId") Long walletId);

    /**
     * 計算錢包總支出
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM wallet_transactions WHERE wallet_id = #{walletId} AND type IN ('withdrawal', 'transfer_out', 'fee') AND status = 'completed'")
    BigDecimal calculateTotalExpense(@Param("walletId") Long walletId);

    /**
     * 查詢指定時間範圍內的交易記錄
     */
    @Select("SELECT * FROM wallet_transactions WHERE wallet_id = #{walletId} AND created_at BETWEEN #{startTime} AND #{endTime} ORDER BY created_at DESC")
    List<WalletTransaction> selectByWalletIdAndTimeRange(
        @Param("walletId") Long walletId, 
        @Param("startTime") LocalDateTime startTime, 
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查詢待確認的交易
     */
    @Select("SELECT * FROM wallet_transactions WHERE status = 'confirming' AND confirmations < 19")
    List<WalletTransaction> selectPendingConfirmations();
}