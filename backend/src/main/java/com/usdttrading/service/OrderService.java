package com.usdttrading.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.dto.ApiResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 訂單服務接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-20
 */
public interface OrderService {

    /**
     * 獲取用戶訂單列表
     */
    ApiResponse<Page<Map<String, Object>>> getUserOrders(Long userId, int pageNum, int pageSize, 
            String orderType, String status, String startDate, String endDate);

    /**
     * 獲取訂單詳情
     */
    ApiResponse<Map<String, Object>> getOrderDetail(Long orderId, Long userId);

    /**
     * 獲取訂單交易記錄
     */
    ApiResponse<List<Map<String, Object>>> getOrderTransactions(Long orderId, Long userId);

    /**
     * 重新提交支付憑證
     */
    ApiResponse<String> resubmitPaymentProof(Long orderId, Long userId, 
            Map<String, String> paymentProof, String clientIp);

    /**
     * 申請訂單仲裁
     */
    ApiResponse<String> createDispute(Long orderId, Long userId, String reason, 
            String description, List<String> evidence, String clientIp);

    /**
     * 獲取所有訂單列表（管理員）
     */
    ApiResponse<Page<Map<String, Object>>> getAllOrders(int pageNum, int pageSize, 
            Long userId, String orderType, String status, String startDate, String endDate);

    /**
     * 審核支付憑證（管理員）
     */
    ApiResponse<String> reviewPayment(Long orderId, Long reviewerId, Boolean approved, 
            String comment, String clientIp);

    /**
     * 手動完成訂單（管理員）
     */
    ApiResponse<String> manualCompleteOrder(Long orderId, Long adminId, String reason, String clientIp);

    /**
     * 處理訂單爭議（管理員）
     */
    ApiResponse<String> resolveDispute(Long orderId, Long adminId, String resolution, 
            String comment, Map<String, Object> compensation, String clientIp);

    /**
     * 獲取訂單統計數據（管理員）
     */
    ApiResponse<Map<String, Object>> getOrderStatistics(String period, String type);
}