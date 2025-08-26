package com.usdttrading.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.dto.ApiResponse;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 交易服務接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-20
 */
public interface TradingService {

    /**
     * 獲取當前USDT買賣價格
     */
    ApiResponse<Map<String, Object>> getCurrentPrice();

    /**
     * 創建買入訂單
     */
    ApiResponse<Map<String, Object>> createBuyOrder(Long userId, BigDecimal amount, 
            String paymentMethod, String clientIp, String userAgent);

    /**
     * 創建賣出訂單
     */
    ApiResponse<Map<String, Object>> createSellOrder(Long userId, BigDecimal usdtAmount, 
            String receivingAccount, String receivingBank, String clientIp, String userAgent);

    /**
     * 確認訂單支付
     */
    ApiResponse<String> confirmPayment(Long orderId, Long userId, 
            Map<String, String> paymentProof, String clientIp);

    /**
     * 取消訂單
     */
    ApiResponse<String> cancelOrder(Long orderId, Long userId, String reason, String clientIp);

    /**
     * 獲取訂單詳情
     */
    ApiResponse<Map<String, Object>> getOrderDetail(Long orderId, Long userId);

    /**
     * 獲取用戶訂單列表
     */
    ApiResponse<Page<Map<String, Object>>> getUserOrders(Long userId, int pageNum, int pageSize, 
            String orderType, String status, String startDate, String endDate);

    /**
     * 獲取交易統計信息
     */
    ApiResponse<Map<String, Object>> getTradingStatistics(Long userId, String period);

    /**
     * 獲取支付方式配置
     */
    ApiResponse<Map<String, Object>> getPaymentMethods();

    /**
     * 獲取交易限額信息
     */
    ApiResponse<Map<String, Object>> getTradingLimits(Long userId);
}