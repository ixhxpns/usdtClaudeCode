package com.usdttrading.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.service.WalletService;
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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 錢包管理控制器
 * 提供錢包餘額、充值、提現等相關API端點
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-20
 */
@Slf4j
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Validated
@Tag(name = "錢包管理", description = "錢包餘額和交易相關API")
@SaCheckLogin
public class WalletController {

    private final WalletService walletService;

    /**
     * 獲取錢包餘額
     */
    @GetMapping("/balance")
    @Operation(summary = "獲取錢包餘額", description = "查看用戶各種幣種的錢包餘額")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登錄"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "錢包不存在")
    })
    public ApiResponse<Map<String, Object>> getWalletBalance() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶查詢錢包餘額: userId={}", userId);
        
        return walletService.getWalletBalance(userId);
    }

    /**
     * 獲取錢包地址
     */
    @GetMapping("/address")
    @Operation(summary = "獲取錢包地址", description = "獲取用戶的USDT錢包充值地址")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登錄"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "地址生成失敗")
    })
    public ApiResponse<Map<String, Object>> getWalletAddress(
            @Parameter(description = "幣種類型", example = "USDT")
            @RequestParam(value = "currency", defaultValue = "USDT") String currency) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶獲取錢包地址: userId={}, currency={}", userId, currency);
        
        return walletService.getWalletAddress(userId, currency);
    }

    /**
     * 創建提現申請
     */
    @PostMapping("/withdraw")
    @Operation(summary = "申請提現", description = "用戶申請USDT提現到外部錢包")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "申請成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤或餘額不足"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "提現權限不足或KYC未完成"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "提現頻率限制")
    })
    public ApiResponse<Map<String, Object>> createWithdrawal(
            @Valid @RequestBody WithdrawRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);
        
        log.info("用戶申請提現: userId={}, amount={}, toAddress={}, network={}", 
                userId, request.getAmount(), request.getToAddress(), request.getNetwork());
        
        return walletService.createWithdrawal(userId, request.getAmount(), 
                request.getToAddress(), request.getNetwork(), request.getAuthCode(), clientIp, userAgent);
    }

    /**
     * 獲取提現記錄
     */
    @GetMapping("/withdrawals")
    @Operation(summary = "獲取提現記錄", description = "分頁查詢用戶的提現申請記錄")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登錄")
    })
    public ApiResponse<Page<Map<String, Object>>> getWithdrawals(
            @Parameter(description = "頁碼", example = "1")
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            
            @Parameter(description = "頁大小", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            
            @Parameter(description = "狀態過濾")
            @RequestParam(value = "status", required = false) String status,
            
            @Parameter(description = "開始時間", example = "2025-01-01")
            @RequestParam(value = "startDate", required = false) String startDate,
            
            @Parameter(description = "結束時間", example = "2025-01-31")
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶查詢提現記錄: userId={}, pageNum={}, pageSize={}, status={}", 
                userId, pageNum, pageSize, status);
        
        return walletService.getWithdrawals(userId, pageNum, pageSize, status, startDate, endDate);
    }

    /**
     * 取消提現申請
     */
    @PostMapping("/withdrawals/{withdrawalId}/cancel")
    @Operation(summary = "取消提現申請", description = "取消待審核的提現申請")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取消成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "提現狀態不允許取消"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "提現記錄不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無權限操作")
    })
    public ApiResponse<String> cancelWithdrawal(
            @Parameter(description = "提現記錄ID", required = true)
            @PathVariable("withdrawalId") @NotNull Long withdrawalId,
            HttpServletRequest httpRequest) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        
        log.info("用戶取消提現申請: userId={}, withdrawalId={}", userId, withdrawalId);
        
        return walletService.cancelWithdrawal(withdrawalId, userId, clientIp);
    }

    /**
     * 獲取交易記錄
     */
    @GetMapping("/transactions")
    @Operation(summary = "獲取交易記錄", description = "分頁查詢錢包交易流水記錄")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登錄")
    })
    public ApiResponse<Page<Map<String, Object>>> getTransactions(
            @Parameter(description = "頁碼", example = "1")
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            
            @Parameter(description = "頁大小", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            
            @Parameter(description = "交易類型過濾")
            @RequestParam(value = "transactionType", required = false) String transactionType,
            
            @Parameter(description = "幣種過濾")
            @RequestParam(value = "currency", required = false) String currency,
            
            @Parameter(description = "開始時間", example = "2025-01-01")
            @RequestParam(value = "startDate", required = false) String startDate,
            
            @Parameter(description = "結束時間", example = "2025-01-31")
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶查詢交易記錄: userId={}, pageNum={}, pageSize={}, transactionType={}", 
                userId, pageNum, pageSize, transactionType);
        
        return walletService.getTransactions(userId, pageNum, pageSize, transactionType, currency, startDate, endDate);
    }

    /**
     * 獲取充值記錄
     */
    @GetMapping("/deposits")
    @Operation(summary = "獲取充值記錄", description = "分頁查詢用戶的充值記錄")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登錄")
    })
    public ApiResponse<Page<Map<String, Object>>> getDeposits(
            @Parameter(description = "頁碼", example = "1")
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            
            @Parameter(description = "頁大小", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            
            @Parameter(description = "幣種過濾")
            @RequestParam(value = "currency", required = false) String currency,
            
            @Parameter(description = "網絡過濾")
            @RequestParam(value = "network", required = false) String network,
            
            @Parameter(description = "開始時間", example = "2025-01-01")
            @RequestParam(value = "startDate", required = false) String startDate,
            
            @Parameter(description = "結束時間", example = "2025-01-31")
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶查詢充值記錄: userId={}, pageNum={}, pageSize={}, currency={}", 
                userId, pageNum, pageSize, currency);
        
        return walletService.getDeposits(userId, pageNum, pageSize, currency, network, startDate, endDate);
    }

    /**
     * 獲取提現限額和手續費
     */
    @GetMapping("/withdraw-info")
    @Operation(summary = "獲取提現信息", description = "獲取用戶的提現限額、手續費等信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登錄")
    })
    public ApiResponse<Map<String, Object>> getWithdrawInfo(
            @Parameter(description = "幣種", example = "USDT")
            @RequestParam(value = "currency", defaultValue = "USDT") String currency,
            
            @Parameter(description = "網絡", example = "TRC20")
            @RequestParam(value = "network", defaultValue = "TRC20") String network) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶查詢提現信息: userId={}, currency={}, network={}", userId, currency, network);
        
        return walletService.getWithdrawInfo(userId, currency, network);
    }

    /**
     * 驗證錢包地址
     */
    @PostMapping("/validate-address")
    @Operation(summary = "驗證錢包地址", description = "驗證外部錢包地址是否有效")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "驗證完成"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "地址格式錯誤")
    })
    public ApiResponse<Map<String, Object>> validateAddress(
            @Valid @RequestBody AddressValidationRequest request) {
        
        log.info("驗證錢包地址: address={}, network={}", request.getAddress(), request.getNetwork());
        
        return walletService.validateAddress(request.getAddress(), request.getNetwork());
    }

    // ==================== 管理端接口 ====================

    /**
     * 獲取所有提現申請（管理員）
     */
    @GetMapping("/admin/withdrawals")
    @SaCheckRole("ADMIN")
    @Operation(summary = "獲取所有提現申請", description = "管理員查看所有用戶的提現申請")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<Page<Map<String, Object>>> getAllWithdrawals(
            @Parameter(description = "頁碼", example = "1")
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            
            @Parameter(description = "頁大小", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            
            @Parameter(description = "用戶ID過濾")
            @RequestParam(value = "userId", required = false) Long userId,
            
            @Parameter(description = "狀態過濾")
            @RequestParam(value = "status", required = false) String status,
            
            @Parameter(description = "開始時間")
            @RequestParam(value = "startDate", required = false) String startDate,
            
            @Parameter(description = "結束時間")
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        log.info("管理員查詢所有提現申請: pageNum={}, pageSize={}, userId={}, status={}", 
                pageNum, pageSize, userId, status);
        
        return walletService.getAllWithdrawals(pageNum, pageSize, userId, status, startDate, endDate);
    }

    /**
     * 審核提現申請（管理員）
     */
    @PostMapping("/admin/withdrawals/{withdrawalId}/review")
    @SaCheckRole("ADMIN")
    @Operation(summary = "審核提現申請", description = "管理員審核用戶的提現申請")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "審核成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "審核參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "提現記錄不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<String> reviewWithdrawal(
            @Parameter(description = "提現記錄ID", required = true)
            @PathVariable("withdrawalId") @NotNull Long withdrawalId,
            @Valid @RequestBody WithdrawalReviewRequest request,
            HttpServletRequest httpRequest) {
        
        Long reviewerId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        
        log.info("管理員審核提現申請: reviewerId={}, withdrawalId={}, approved={}", 
                reviewerId, withdrawalId, request.getApproved());
        
        return walletService.reviewWithdrawal(withdrawalId, reviewerId, request.getApproved(), 
                request.getComment(), clientIp);
    }

    /**
     * 手動處理提現（管理員）
     */
    @PostMapping("/admin/withdrawals/{withdrawalId}/process")
    @SaCheckRole("ADMIN")
    @Operation(summary = "處理提現", description = "管理員手動處理已審核通過的提現申請")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "處理成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "提現狀態錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "提現記錄不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<String> processWithdrawal(
            @Parameter(description = "提現記錄ID", required = true)
            @PathVariable("withdrawalId") @NotNull Long withdrawalId,
            @RequestBody(required = false) Map<String, String> transactionInfo,
            HttpServletRequest httpRequest) {
        
        Long adminId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        
        log.info("管理員處理提現: adminId={}, withdrawalId={}", adminId, withdrawalId);
        
        return walletService.processWithdrawal(withdrawalId, adminId, transactionInfo, clientIp);
    }

    /**
     * 獲取錢包統計數據（管理員）
     */
    @GetMapping("/admin/statistics")
    @SaCheckRole("ADMIN")
    @Operation(summary = "獲取錢包統計", description = "管理員查看錢包相關統計數據")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<Map<String, Object>> getWalletStatistics(
            @Parameter(description = "統計週期", example = "30d")
            @RequestParam(value = "period", defaultValue = "30d") String period) {
        
        log.info("管理員查詢錢包統計: period={}", period);
        return walletService.getWalletStatistics(period);
    }

    // DTO classes
    public static class WithdrawRequest {
        @NotNull(message = "提現金額不能為空")
        @DecimalMin(value = "1.0", message = "最小提現金額為1 USDT")
        private BigDecimal amount;

        @NotBlank(message = "提現地址不能為空")
        private String toAddress;

        @NotBlank(message = "網絡類型不能為空")
        private String network;

        private String authCode; // 雙因子認證碼

        // getters and setters
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getToAddress() { return toAddress; }
        public void setToAddress(String toAddress) { this.toAddress = toAddress; }
        public String getNetwork() { return network; }
        public void setNetwork(String network) { this.network = network; }
        public String getAuthCode() { return authCode; }
        public void setAuthCode(String authCode) { this.authCode = authCode; }
    }

    public static class AddressValidationRequest {
        @NotBlank(message = "錢包地址不能為空")
        private String address;

        @NotBlank(message = "網絡類型不能為空")
        private String network;

        // getters and setters
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getNetwork() { return network; }
        public void setNetwork(String network) { this.network = network; }
    }

    public static class WithdrawalReviewRequest {
        @NotNull(message = "審核結果不能為空")
        private Boolean approved;

        private String comment;

        // getters and setters
        public Boolean getApproved() { return approved; }
        public void setApproved(Boolean approved) { this.approved = approved; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}