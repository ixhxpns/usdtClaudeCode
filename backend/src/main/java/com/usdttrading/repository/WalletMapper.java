package com.usdttrading.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usdttrading.entity.Wallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 钱包Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface WalletMapper extends BaseMapper<Wallet> {

    /**
     * 根据用户ID和币种查找钱包
     */
    @Select("SELECT * FROM wallets WHERE user_id = #{userId} AND currency = #{currency} AND deleted = 0")
    Wallet findByUserIdAndCurrency(@Param("userId") Long userId, @Param("currency") String currency);

    /**
     * 根据地址查找钱包
     */
    @Select("SELECT * FROM wallets WHERE address = #{address} AND deleted = 0")
    Wallet findByAddress(@Param("address") String address);

    /**
     * 根据用户ID查找所有钱包
     */
    @Select("SELECT * FROM wallets WHERE user_id = #{userId} AND deleted = 0 ORDER BY created_at DESC")
    List<Wallet> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查找主钱包 (与findByUserId相同功能，为了兼容性)
     */
    @Select("SELECT * FROM wallets WHERE user_id = #{userId} AND deleted = 0 LIMIT 1")
    Wallet selectByUserId(@Param("userId") Long userId);

    /**
     * 根据币种查找所有钱包
     */
    @Select("SELECT * FROM wallets WHERE currency = #{currency} AND is_active = 1 AND deleted = 0")
    List<Wallet> findByCurrency(@Param("currency") String currency);

    /**
     * 获取用户总资产
     */
    @Select("SELECT COALESCE(SUM(balance + frozen_balance), 0) FROM wallets WHERE user_id = #{userId} AND currency = #{currency} AND deleted = 0")
    BigDecimal getTotalBalance(@Param("userId") Long userId, @Param("currency") String currency);

    /**
     * 查找余额大于指定金额的钱包
     */
    List<Wallet> findWalletsWithBalance(@Param("currency") String currency, @Param("minBalance") BigDecimal minBalance);

    /**
     * 更新钱包余额
     */
    int updateBalance(@Param("id") Long id, @Param("balance") BigDecimal balance, @Param("frozenBalance") BigDecimal frozenBalance);
}