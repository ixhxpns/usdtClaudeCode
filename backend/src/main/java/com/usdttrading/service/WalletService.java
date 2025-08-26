package com.usdttrading.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.dto.ApiResponse;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 錢包服務接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-20
 */
public interface WalletService {

    /**
     * 獲取錢包餘額
     */
    ApiResponse<Map<String, Object>> getWalletBalance(Long userId);

    /**
     * 獲取錢包地址
     */
    ApiResponse<Map<String, Object>> getWalletAddress(Long userId, String currency);

    /**
     * 創建提現申請
     */
    ApiResponse<Map<String, Object>> createWithdrawal(Long userId, BigDecimal amount, 
            String toAddress, String network, String authCode, String clientIp, String userAgent);

    /**
     * 獲取提現記錄
     */
    ApiResponse<Page<Map<String, Object>>> getWithdrawals(Long userId, int pageNum, int pageSize, 
            String status, String startDate, String endDate);

    /**
     * 取消提現申請
     */
    ApiResponse<String> cancelWithdrawal(Long withdrawalId, Long userId, String clientIp);

    /**
     * 獲取交易記錄
     */
    ApiResponse<Page<Map<String, Object>>> getTransactions(Long userId, int pageNum, int pageSize, 
            String transactionType, String currency, String startDate, String endDate);

    /**
     * 獲取充值記錄
     */
    ApiResponse<Page<Map<String, Object>>> getDeposits(Long userId, int pageNum, int pageSize, 
            String currency, String network, String startDate, String endDate);

    /**
     * 獲取提現信息
     */
    ApiResponse<Map<String, Object>> getWithdrawInfo(Long userId, String currency, String network);

    /**
     * 驗證錢包地址
     */
    ApiResponse<Map<String, Object>> validateAddress(String address, String network);

    /**
     * 獲取所有提現申請（管理員）
     */
    ApiResponse<Page<Map<String, Object>>> getAllWithdrawals(int pageNum, int pageSize, 
            Long userId, String status, String startDate, String endDate);

    /**
     * 審核提現申請（管理員）
     */
    ApiResponse<String> reviewWithdrawal(Long withdrawalId, Long reviewerId, Boolean approved, 
            String comment, String clientIp);

    /**
     * 處理提現（管理員）
     */
    ApiResponse<String> processWithdrawal(Long withdrawalId, Long adminId, 
            Map<String, String> transactionInfo, String clientIp);

    /**
     * 獲取錢包統計數據（管理員）
     */
    ApiResponse<Map<String, Object>> getWalletStatistics(String period);
}