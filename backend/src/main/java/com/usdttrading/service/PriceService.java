package com.usdttrading.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.controller.PriceController.PriceAlertRequest;
import com.usdttrading.controller.PriceController.PriceStrategyRequest;
import com.usdttrading.dto.ApiResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 價格服務接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-20
 */
public interface PriceService {

    /**
     * 獲取當前USDT價格
     */
    ApiResponse<Map<String, Object>> getCurrentPrice();

    /**
     * 獲取實時價格詳情
     */
    ApiResponse<Map<String, Object>> getRealtimePriceDetail();

    /**
     * 獲取價格歷史記錄
     */
    ApiResponse<List<Map<String, Object>>> getPriceHistory(String period, String interval);

    /**
     * 獲取價格統計信息
     */
    ApiResponse<Map<String, Object>> getPriceStatistics(String period);

    /**
     * 手動更新價格（管理員）
     */
    ApiResponse<String> updatePrice(BigDecimal buyPrice, BigDecimal sellPrice, String reason);

    /**
     * 設置價格策略（管理員）
     */
    ApiResponse<String> updatePriceStrategy(PriceStrategyRequest request);

    /**
     * 獲取價格配置（管理員）
     */
    ApiResponse<Map<String, Object>> getPriceConfig();

    /**
     * 獲取價格更新日誌（管理員）
     */
    ApiResponse<Page<Map<String, Object>>> getPriceUpdateLogs(int pageNum, int pageSize, 
            String startDate, String endDate);

    /**
     * 強制刷新外部價格數據（管理員）
     */
    ApiResponse<Map<String, Object>> refreshExternalPrice();

    /**
     * 設置價格預警規則（管理員）
     */
    ApiResponse<String> setPriceAlerts(PriceAlertRequest request);
}