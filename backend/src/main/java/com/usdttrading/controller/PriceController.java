package com.usdttrading.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.service.PriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 價格管理控制器
 * 提供USDT價格查詢和管理相關API端點
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-20
 */
@Slf4j
@RestController
@RequestMapping("/api/price")
@RequiredArgsConstructor
@Validated
@Tag(name = "價格管理", description = "USDT價格相關API")
public class PriceController {

    private final PriceService priceService;

    /**
     * 獲取當前USDT價格（公開接口）
     */
    @GetMapping("/current")
    @Operation(summary = "獲取當前價格", description = "獲取USDT當前市場價格（無需登錄）")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "系統錯誤")
    })
    public ApiResponse<Map<String, Object>> getCurrentPrice() {
        log.info("查詢當前USDT價格");
        return priceService.getCurrentPrice();
    }

    /**
     * 獲取實時價格詳情
     */
    @GetMapping("/realtime")
    @SaCheckLogin
    @Operation(summary = "獲取實時價格詳情", description = "獲取包含買賣價差的實時價格信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登錄")
    })
    public ApiResponse<Map<String, Object>> getRealtimePrice() {
        log.info("用戶查詢實時價格詳情");
        return priceService.getRealtimePriceDetail();
    }

    /**
     * 獲取價格歷史記錄
     */
    @GetMapping("/history")
    @SaCheckLogin
    @Operation(summary = "獲取價格歷史", description = "查詢USDT價格歷史記錄")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登錄")
    })
    public ApiResponse<List<Map<String, Object>>> getPriceHistory(
            @Parameter(description = "時間範圍", example = "24h")
            @RequestParam(value = "period", defaultValue = "24h") String period,
            
            @Parameter(description = "數據間隔", example = "1h")
            @RequestParam(value = "interval", defaultValue = "1h") String interval) {
        
        log.info("用戶查詢價格歷史: period={}, interval={}", period, interval);
        return priceService.getPriceHistory(period, interval);
    }

    /**
     * 獲取價格統計信息
     */
    @GetMapping("/statistics")
    @SaCheckLogin
    @Operation(summary = "獲取價格統計", description = "獲取價格統計數據如漲跌幅、成交量等")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤")
    })
    public ApiResponse<Map<String, Object>> getPriceStatistics(
            @Parameter(description = "統計週期", example = "24h")
            @RequestParam(value = "period", defaultValue = "24h") String period) {
        
        log.info("用戶查詢價格統計: period={}", period);
        return priceService.getPriceStatistics(period);
    }

    // ==================== 管理端接口 ====================

    /**
     * 手動更新USDT價格（管理員）
     */
    @PostMapping("/admin/update")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @Operation(summary = "手動更新價格", description = "管理員手動設置USDT價格")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "價格參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<String> updatePrice(@Valid @RequestBody UpdatePriceRequest request) {
        log.info("管理員手動更新價格: buyPrice={}, sellPrice={}", request.getBuyPrice(), request.getSellPrice());
        return priceService.updatePrice(request.getBuyPrice(), request.getSellPrice(), request.getReason());
    }

    /**
     * 設置價格策略（管理員）
     */
    @PostMapping("/admin/strategy")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @Operation(summary = "設置價格策略", description = "配置自動價格更新策略")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "設置成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "策略參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<String> updatePriceStrategy(@Valid @RequestBody PriceStrategyRequest request) {
        log.info("管理員設置價格策略: autoUpdate={}, spreadPercent={}", 
                request.getAutoUpdate(), request.getSpreadPercent());
        return priceService.updatePriceStrategy(request);
    }

    /**
     * 獲取價格管理配置（管理員）
     */
    @GetMapping("/admin/config")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @Operation(summary = "獲取價格配置", description = "獲取當前價格管理配置")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<Map<String, Object>> getPriceConfig() {
        log.info("管理員查詢價格配置");
        return priceService.getPriceConfig();
    }

    /**
     * 獲取價格更新日誌（管理員）
     */
    @GetMapping("/admin/logs")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @Operation(summary = "獲取價格更新日誌", description = "查詢價格更新的操作日誌")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<Page<Map<String, Object>>> getPriceUpdateLogs(
            @Parameter(description = "頁碼", example = "1")
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            
            @Parameter(description = "頁大小", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            
            @Parameter(description = "開始時間", example = "2025-01-01")
            @RequestParam(value = "startDate", required = false) String startDate,
            
            @Parameter(description = "結束時間", example = "2025-01-31")
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        log.info("管理員查詢價格更新日誌: pageNum={}, pageSize={}", pageNum, pageSize);
        return priceService.getPriceUpdateLogs(pageNum, pageSize, startDate, endDate);
    }

    /**
     * 強制刷新外部價格數據（管理員）
     */
    @PostMapping("/admin/refresh-external")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @Operation(summary = "刷新外部價格", description = "強制從外部API刷新價格數據")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "刷新成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "刷新失敗"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<Map<String, Object>> refreshExternalPrice() {
        log.info("管理員強制刷新外部價格數據");
        return priceService.refreshExternalPrice();
    }

    /**
     * 設置價格預警規則（管理員）
     */
    @PostMapping("/admin/alerts")
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @Operation(summary = "設置價格預警", description = "設置價格異常波動預警規則")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "設置成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<String> setPriceAlerts(@Valid @RequestBody PriceAlertRequest request) {
        log.info("管理員設置價格預警: enabled={}, threshold={}", 
                request.getEnabled(), request.getThreshold());
        return priceService.setPriceAlerts(request);
    }

    // DTO classes
    public static class UpdatePriceRequest {
        @NotNull(message = "買入價格不能為空")
        @DecimalMin(value = "0.1", message = "買入價格必須大於0.1")
        private BigDecimal buyPrice;

        @NotNull(message = "賣出價格不能為空")
        @DecimalMin(value = "0.1", message = "賣出價格必須大於0.1")
        private BigDecimal sellPrice;

        private String reason;

        // getters and setters
        public BigDecimal getBuyPrice() { return buyPrice; }
        public void setBuyPrice(BigDecimal buyPrice) { this.buyPrice = buyPrice; }
        public BigDecimal getSellPrice() { return sellPrice; }
        public void setSellPrice(BigDecimal sellPrice) { this.sellPrice = sellPrice; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class PriceStrategyRequest {
        @NotNull(message = "自動更新設置不能為空")
        private Boolean autoUpdate;

        @NotNull(message = "價差百分比不能為空")
        @DecimalMin(value = "0.1", message = "價差百分比必須大於0.1%")
        private BigDecimal spreadPercent;

        private String externalApiUrl;
        private Integer updateIntervalMinutes;
        private BigDecimal maxChangePercent;

        // getters and setters
        public Boolean getAutoUpdate() { return autoUpdate; }
        public void setAutoUpdate(Boolean autoUpdate) { this.autoUpdate = autoUpdate; }
        public BigDecimal getSpreadPercent() { return spreadPercent; }
        public void setSpreadPercent(BigDecimal spreadPercent) { this.spreadPercent = spreadPercent; }
        public String getExternalApiUrl() { return externalApiUrl; }
        public void setExternalApiUrl(String externalApiUrl) { this.externalApiUrl = externalApiUrl; }
        public Integer getUpdateIntervalMinutes() { return updateIntervalMinutes; }
        public void setUpdateIntervalMinutes(Integer updateIntervalMinutes) { this.updateIntervalMinutes = updateIntervalMinutes; }
        public BigDecimal getMaxChangePercent() { return maxChangePercent; }
        public void setMaxChangePercent(BigDecimal maxChangePercent) { this.maxChangePercent = maxChangePercent; }
    }

    public static class PriceAlertRequest {
        @NotNull(message = "預警開關設置不能為空")
        private Boolean enabled;

        @NotNull(message = "預警閾值不能為空")
        @DecimalMin(value = "1.0", message = "預警閾值必須大於1%")
        private BigDecimal threshold;

        private String notificationEmail;
        private Boolean smsAlert;
        private Integer checkIntervalMinutes;

        // getters and setters
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public BigDecimal getThreshold() { return threshold; }
        public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
        public String getNotificationEmail() { return notificationEmail; }
        public void setNotificationEmail(String notificationEmail) { this.notificationEmail = notificationEmail; }
        public Boolean getSmsAlert() { return smsAlert; }
        public void setSmsAlert(Boolean smsAlert) { this.smsAlert = smsAlert; }
        public Integer getCheckIntervalMinutes() { return checkIntervalMinutes; }
        public void setCheckIntervalMinutes(Integer checkIntervalMinutes) { this.checkIntervalMinutes = checkIntervalMinutes; }
    }
}