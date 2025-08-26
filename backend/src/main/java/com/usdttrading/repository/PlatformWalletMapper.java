package com.usdttrading.repository;

import com.usdttrading.entity.PlatformWallet;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 平台錢包池Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface PlatformWalletMapper extends BaseMapper<PlatformWallet> {

    /**
     * 根據幣種查詢錢包
     */
    @Select("SELECT * FROM platform_wallets WHERE currency = #{currency} AND is_active = 1 ORDER BY balance DESC")
    List<PlatformWallet> selectByCurrency(@Param("currency") String currency);

    /**
     * 查詢可用的錢包
     */
    @Select("SELECT * FROM platform_wallets WHERE is_active = 1 AND balance > #{minBalance} ORDER BY balance DESC")
    List<PlatformWallet> selectAvailableWallets(@Param("minBalance") BigDecimal minBalance);

    /**
     * 根據地址查詢錢包
     */
    @Select("SELECT * FROM platform_wallets WHERE address = #{address}")
    PlatformWallet selectByAddress(@Param("address") String address);

    /**
     * 查詢主錢包
     */
    @Select("SELECT * FROM platform_wallets WHERE currency = #{currency} AND type = 'main' AND is_active = 1")
    PlatformWallet selectMainWallet(@Param("currency") String currency);

    /**
     * 查詢熱錢包
     */
    @Select("SELECT * FROM platform_wallets WHERE currency = #{currency} AND type = 'hot' AND is_active = 1")
    List<PlatformWallet> selectHotWallets(@Param("currency") String currency);

    /**
     * 查詢冷錢包
     */
    @Select("SELECT * FROM platform_wallets WHERE currency = #{currency} AND type = 'cold' AND is_active = 1")
    List<PlatformWallet> selectColdWallets(@Param("currency") String currency);

    /**
     * 計算幣種總余額
     */
    @Select("SELECT COALESCE(SUM(balance), 0) FROM platform_wallets WHERE currency = #{currency} AND is_active = 1")
    BigDecimal calculateTotalBalance(@Param("currency") String currency);

    /**
     * 查詢餘額不足的錢包
     */
    @Select("SELECT * FROM platform_wallets WHERE balance < minimum_balance AND is_active = 1")
    List<PlatformWallet> selectLowBalanceWallets();
}