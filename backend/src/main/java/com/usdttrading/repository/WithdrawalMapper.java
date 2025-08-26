package com.usdttrading.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.entity.Withdrawal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 提款申请Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface WithdrawalMapper extends BaseMapper<Withdrawal> {

    /**
     * 根据提款单号查找
     */
    @Select("SELECT * FROM withdrawals WHERE withdrawal_no = #{withdrawalNo} AND deleted = 0")
    Withdrawal findByWithdrawalNo(@Param("withdrawalNo") String withdrawalNo);

    /**
     * 根据用户ID分页查找提款记录
     */
    IPage<Withdrawal> findByUserId(Page<Withdrawal> page, @Param("userId") Long userId);

    /**
     * 根据状态查找提款记录
     */
    @Select("SELECT * FROM withdrawals WHERE status = #{status} AND deleted = 0 ORDER BY created_at ASC")
    List<Withdrawal> findByStatus(@Param("status") String status);

    /**
     * 获取待审核的提款记录
     */
    @Select("SELECT * FROM withdrawals WHERE status IN ('pending', 'reviewing') AND deleted = 0 ORDER BY created_at ASC")
    List<Withdrawal> findPendingWithdrawals();

    /**
     * 获取高风险提款记录
     */
    @Select("SELECT * FROM withdrawals WHERE risk_score >= #{minRiskScore} AND status = 'pending' AND deleted = 0")
    List<Withdrawal> findHighRiskWithdrawals(@Param("minRiskScore") int minRiskScore);

    /**
     * 统计用户今日提款金额
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM withdrawals WHERE user_id = #{userId} AND DATE(created_at) = CURDATE() AND status != 'cancelled' AND deleted = 0")
    BigDecimal getTodayWithdrawalAmount(@Param("userId") Long userId);

    /**
     * 获取需要自动审核的提款记录
     */
    List<Withdrawal> findAutoApprovalWithdrawals(@Param("maxAmount") BigDecimal maxAmount);
}