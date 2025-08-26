package com.usdttrading.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.enums.OrderType;
import com.usdttrading.service.TradingService;
import com.usdttrading.utils.RequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 交易控制器
 * 提供USDT買賣交易相關API端點
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-20
 */
@Slf4j
@RestController
@RequestMapping("/api/trading")
@RequiredArgsConstructor
@Validated
@Tag(name = "交易管理", description = "USDT交易相關API")
@SaCheckLogin
public class TradingController {

    private final TradingService tradingService;

    /**
     * 獲取當前USDT買賣價格
     */
    @GetMapping("/price")
    @Operation(summary = "獲取當前價格", description = "獲取USDT當前買入和賣出價格")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "系統錯誤")
    })
    public ApiResponse<Map<String, Object>> getCurrentPrice() {
        log.info("用戶查詢當前USDT價格");
        return tradingService.getCurrentPrice();
    }

    /**
     * 創建買入訂單
     */
    @PostMapping("/buy")
    @Operation(summary = "創建買入訂單", description = "用戶買入USDT")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "訂單創建成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤或餘額不足"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "用戶狀態異常"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "系統錯誤")
    })
    public ApiResponse<Map<String, Object>> createBuyOrder(
            @Valid @RequestBody BuyOrderRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);
        
        log.info("用戶創建買入訂單: userId={}, amount={}, paymentMethod={}", 
                userId, request.getAmount(), request.getPaymentMethod());
        
        return tradingService.createBuyOrder(userId, request.getAmount(), 
                request.getPaymentMethod(), clientIp, userAgent);
    }

    /**
     * 創建賣出訂單
     */
    @PostMapping("/sell")
    @Operation(summary = "創建賣出訂單", description = "用戶賣出USDT")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "訂單創建成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤或USDT餘額不足"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "用戶狀態異常"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "系統錯誤")
    })
    public ApiResponse<Map<String, Object>> createSellOrder(
            @Valid @RequestBody SellOrderRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);
        
        log.info("用戶創建賣出訂單: userId={}, usdtAmount={}, receivingAccount={}", 
                userId, request.getUsdtAmount(), request.getReceivingAccount());
        
        return tradingService.createSellOrder(userId, request.getUsdtAmount(), 
                request.getReceivingAccount(), request.getReceivingBank(), clientIp, userAgent);
    }

    /**
     * 確認訂單支付
     */
    @PostMapping("/{orderId}/confirm-payment")
    @Operation(summary = "確認訂單支付", description = "用戶確認已完成支付")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "確認成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "訂單狀態錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無權限操作此訂單")
    })
    public ApiResponse<String> confirmPayment(
            @Parameter(description = "訂單ID", required = true)
            @PathVariable("orderId") @NotNull Long orderId,
            @RequestBody(required = false) Map<String, String> paymentProof,
            HttpServletRequest httpRequest) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        
        log.info("用戶確認訂單支付: userId={}, orderId={}", userId, orderId);
        
        return tradingService.confirmPayment(orderId, userId, paymentProof, clientIp);
    }

    /**
     * 取消訂單
     */
    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "取消訂單", description = "用戶取消未完成的訂單")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取消成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "訂單狀態不允許取消"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無權限操作此訂單")
    })
    public ApiResponse<String> cancelOrder(
            @Parameter(description = "訂單ID", required = true)
            @PathVariable("orderId") @NotNull Long orderId,
            @RequestBody(required = false) Map<String, String> reason,
            HttpServletRequest httpRequest) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        String cancelReason = reason != null ? reason.get("reason") : "用戶主動取消";
        
        log.info("用戶取消訂單: userId={}, orderId={}, reason={}", userId, orderId, cancelReason);
        
        return tradingService.cancelOrder(orderId, userId, cancelReason, clientIp);
    }

    /**
     * 獲取訂單詳情
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "獲取訂單詳情", description = "查看特定訂單的詳細信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無權限查看此訂單")
    })
    public ApiResponse<Map<String, Object>> getOrderDetail(
            @Parameter(description = "訂單ID", required = true)
            @PathVariable("orderId") @NotNull Long orderId) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶查詢訂單詳情: userId={}, orderId={}", userId, orderId);
        
        return tradingService.getOrderDetail(orderId, userId);
    }

    /**
     * 獲取用戶訂單列表
     */
    @GetMapping("/orders")
    @Operation(summary = "獲取訂單列表", description = "分頁查詢用戶的交易訂單")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤")
    })
    public ApiResponse<Page<Map<String, Object>>> getUserOrders(
            @Parameter(description = "頁碼", example = "1")
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            
            @Parameter(description = "頁大小", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            
            @Parameter(description = "訂單類型過濾", example = "BUY")
            @RequestParam(value = "orderType", required = false) String orderType,
            
            @Parameter(description = "狀態過濾")
            @RequestParam(value = "status", required = false) String status,
            
            @Parameter(description = "開始時間", example = "2025-01-01")
            @RequestParam(value = "startDate", required = false) String startDate,
            
            @Parameter(description = "結束時間", example = "2025-01-31")
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶查詢訂單列表: userId={}, pageNum={}, pageSize={}, orderType={}, status={}", 
                userId, pageNum, pageSize, orderType, status);
        
        return tradingService.getUserOrders(userId, pageNum, pageSize, orderType, status, startDate, endDate);
    }

    /**
     * 獲取交易統計信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "獲取交易統計", description = "獲取用戶的交易統計數據")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功")
    })
    public ApiResponse<Map<String, Object>> getTradingStatistics(
            @Parameter(description = "統計時間範圍", example = "30d")
            @RequestParam(value = "period", defaultValue = "30d") String period) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶查詢交易統計: userId={}, period={}", userId, period);
        
        return tradingService.getTradingStatistics(userId, period);
    }

    /**
     * 獲取支付方式配置
     */
    @GetMapping("/payment-methods")
    @Operation(summary = "獲取支付方式", description = "獲取可用的支付方式配置")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功")
    })
    public ApiResponse<Map<String, Object>> getPaymentMethods() {
        log.info("用戶查詢支付方式配置");
        
        return tradingService.getPaymentMethods();
    }

    /**
     * 獲取交易限額信息
     */
    @GetMapping("/limits")
    @Operation(summary = "獲取交易限額", description = "根據用戶KYC等級獲取交易限額")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功")
    })
    public ApiResponse<Map<String, Object>> getTradingLimits() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶查詢交易限額: userId={}", userId);
        
        return tradingService.getTradingLimits(userId);
    }

    // DTO classes
    public static class BuyOrderRequest {
        @NotNull(message = "金額不能為空")
        @DecimalMin(value = "10.0", message = "最小買入金額為10元")
        private BigDecimal amount;

        @NotNull(message = "支付方式不能為空")
        private String paymentMethod;

        // getters and setters
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }

    public static class SellOrderRequest {
        @NotNull(message = "USDT數量不能為空")
        @DecimalMin(value = "1.0", message = "最小賣出數量為1 USDT")
        private BigDecimal usdtAmount;

        @NotNull(message = "收款賬戶不能為空")
        private String receivingAccount;

        @NotNull(message = "收款銀行不能為空")
        private String receivingBank;

        // getters and setters
        public BigDecimal getUsdtAmount() { return usdtAmount; }
        public void setUsdtAmount(BigDecimal usdtAmount) { this.usdtAmount = usdtAmount; }
        public String getReceivingAccount() { return receivingAccount; }
        public void setReceivingAccount(String receivingAccount) { this.receivingAccount = receivingAccount; }
        public String getReceivingBank() { return receivingBank; }
        public void setReceivingBank(String receivingBank) { this.receivingBank = receivingBank; }
    }
}