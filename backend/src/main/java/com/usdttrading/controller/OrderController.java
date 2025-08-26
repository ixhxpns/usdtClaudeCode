package com.usdttrading.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.enums.OrderStatus;
import com.usdttrading.service.OrderService;
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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 訂單管理控制器
 * 提供訂單查詢和管理相關API端點
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-20
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "訂單管理", description = "訂單查詢和管理相關API")
@SaCheckLogin
public class OrderController {

    private final OrderService orderService;

    /**
     * 獲取用戶訂單列表
     */
    @GetMapping("/my")
    @Operation(summary = "獲取我的訂單", description = "分頁查詢當前用戶的所有訂單")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登錄")
    })
    public ApiResponse<Page<Map<String, Object>>> getMyOrders(
            @Parameter(description = "頁碼", example = "1")
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            
            @Parameter(description = "頁大小", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            
            @Parameter(description = "訂單類型", example = "BUY")
            @RequestParam(value = "orderType", required = false) String orderType,
            
            @Parameter(description = "訂單狀態")
            @RequestParam(value = "status", required = false) String status,
            
            @Parameter(description = "開始時間", example = "2025-01-01")
            @RequestParam(value = "startDate", required = false) String startDate,
            
            @Parameter(description = "結束時間", example = "2025-01-31")
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶查詢個人訂單: userId={}, pageNum={}, pageSize={}, orderType={}, status={}", 
                userId, pageNum, pageSize, orderType, status);
        
        return orderService.getUserOrders(userId, pageNum, pageSize, orderType, status, startDate, endDate);
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
        
        return orderService.getOrderDetail(orderId, userId);
    }

    /**
     * 獲取訂單交易記錄
     */
    @GetMapping("/{orderId}/transactions")
    @Operation(summary = "獲取訂單交易記錄", description = "查看訂單相關的所有交易記錄")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無權限查看此訂單")
    })
    public ApiResponse<List<Map<String, Object>>> getOrderTransactions(
            @Parameter(description = "訂單ID", required = true)
            @PathVariable("orderId") @NotNull Long orderId) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶查詢訂單交易記錄: userId={}, orderId={}", userId, orderId);
        
        return orderService.getOrderTransactions(orderId, userId);
    }

    /**
     * 重新提交支付憑證
     */
    @PostMapping("/{orderId}/resubmit-payment")
    @Operation(summary = "重新提交支付憑證", description = "當支付憑證審核失敗時重新提交")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "提交成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "訂單狀態不允許重新提交"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無權限操作此訂單")
    })
    public ApiResponse<String> resubmitPaymentProof(
            @Parameter(description = "訂單ID", required = true)
            @PathVariable("orderId") @NotNull Long orderId,
            @Valid @RequestBody PaymentProofRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        
        log.info("用戶重新提交支付憑證: userId={}, orderId={}", userId, orderId);
        
        return orderService.resubmitPaymentProof(orderId, userId, request.getPaymentProof(), clientIp);
    }

    /**
     * 申請訂單仲裁
     */
    @PostMapping("/{orderId}/dispute")
    @Operation(summary = "申請訂單仲裁", description = "當訂單出現爭議時申請人工仲裁")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "申請成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "訂單狀態不允許申請仲裁"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無權限操作此訂單")
    })
    public ApiResponse<String> createDispute(
            @Parameter(description = "訂單ID", required = true)
            @PathVariable("orderId") @NotNull Long orderId,
            @Valid @RequestBody DisputeRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        
        log.info("用戶申請訂單仲裁: userId={}, orderId={}, reason={}", 
                userId, orderId, request.getReason());
        
        return orderService.createDispute(orderId, userId, request.getReason(), 
                request.getDescription(), request.getEvidence(), clientIp);
    }

    // ==================== 管理端接口 ====================

    /**
     * 獲取所有訂單列表（管理員）
     */
    @GetMapping("/admin/all")
    @SaCheckRole("ADMIN")
    @Operation(summary = "獲取所有訂單", description = "管理員查看所有訂單")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<Page<Map<String, Object>>> getAllOrders(
            @Parameter(description = "頁碼", example = "1")
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            
            @Parameter(description = "頁大小", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            
            @Parameter(description = "用戶ID過濾")
            @RequestParam(value = "userId", required = false) Long userId,
            
            @Parameter(description = "訂單類型過濾")
            @RequestParam(value = "orderType", required = false) String orderType,
            
            @Parameter(description = "狀態過濾")
            @RequestParam(value = "status", required = false) String status,
            
            @Parameter(description = "開始時間")
            @RequestParam(value = "startDate", required = false) String startDate,
            
            @Parameter(description = "結束時間")
            @RequestParam(value = "endDate", required = false) String endDate) {
        
        log.info("管理員查詢所有訂單: pageNum={}, pageSize={}, userId={}, orderType={}, status={}", 
                pageNum, pageSize, userId, orderType, status);
        
        return orderService.getAllOrders(pageNum, pageSize, userId, orderType, status, startDate, endDate);
    }

    /**
     * 審核支付憑證（管理員）
     */
    @PostMapping("/admin/{orderId}/review-payment")
    @SaCheckRole("ADMIN")
    @Operation(summary = "審核支付憑證", description = "管理員審核用戶提交的支付憑證")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "審核成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "審核參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<String> reviewPayment(
            @Parameter(description = "訂單ID", required = true)
            @PathVariable("orderId") @NotNull Long orderId,
            @Valid @RequestBody PaymentReviewRequest request,
            HttpServletRequest httpRequest) {
        
        Long reviewerId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        
        log.info("管理員審核支付憑證: reviewerId={}, orderId={}, approved={}", 
                reviewerId, orderId, request.getApproved());
        
        return orderService.reviewPayment(orderId, reviewerId, request.getApproved(), 
                request.getComment(), clientIp);
    }

    /**
     * 手動完成訂單（管理員）
     */
    @PostMapping("/admin/{orderId}/complete")
    @SaCheckRole("ADMIN")
    @Operation(summary = "手動完成訂單", description = "管理員手動將訂單標記為完成")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "操作成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "訂單狀態不允許完成"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<String> manualCompleteOrder(
            @Parameter(description = "訂單ID", required = true)
            @PathVariable("orderId") @NotNull Long orderId,
            @RequestBody(required = false) Map<String, String> reason,
            HttpServletRequest httpRequest) {
        
        Long adminId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        String completeReason = reason != null ? reason.get("reason") : "管理員手動完成";
        
        log.info("管理員手動完成訂單: adminId={}, orderId={}, reason={}", 
                adminId, orderId, completeReason);
        
        return orderService.manualCompleteOrder(orderId, adminId, completeReason, clientIp);
    }

    /**
     * 處理訂單爭議（管理員）
     */
    @PostMapping("/admin/{orderId}/resolve-dispute")
    @SaCheckRole("ADMIN")
    @Operation(summary = "處理訂單爭議", description = "管理員處理訂單仲裁申請")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "處理成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "處理參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單或爭議不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<String> resolveDispute(
            @Parameter(description = "訂單ID", required = true)
            @PathVariable("orderId") @NotNull Long orderId,
            @Valid @RequestBody DisputeResolutionRequest request,
            HttpServletRequest httpRequest) {
        
        Long adminId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        
        log.info("管理員處理訂單爭議: adminId={}, orderId={}, resolution={}", 
                adminId, orderId, request.getResolution());
        
        return orderService.resolveDispute(orderId, adminId, request.getResolution(), 
                request.getComment(), request.getCompensation(), clientIp);
    }

    /**
     * 獲取訂單統計數據（管理員）
     */
    @GetMapping("/admin/statistics")
    @SaCheckRole("ADMIN")
    @Operation(summary = "獲取訂單統計", description = "管理員查看訂單統計數據")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<Map<String, Object>> getOrderStatistics(
            @Parameter(description = "統計週期", example = "30d")
            @RequestParam(value = "period", defaultValue = "30d") String period,
            
            @Parameter(description = "統計類型")
            @RequestParam(value = "type", defaultValue = "overview") String type) {
        
        log.info("管理員查詢訂單統計: period={}, type={}", period, type);
        return orderService.getOrderStatistics(period, type);
    }

    // DTO classes
    public static class PaymentProofRequest {
        @NotNull(message = "支付憑證不能為空")
        private Map<String, String> paymentProof;

        // getters and setters
        public Map<String, String> getPaymentProof() { return paymentProof; }
        public void setPaymentProof(Map<String, String> paymentProof) { this.paymentProof = paymentProof; }
    }

    public static class DisputeRequest {
        @NotBlank(message = "爭議原因不能為空")
        private String reason;

        @NotBlank(message = "詳細描述不能為空")
        private String description;

        private List<String> evidence;

        // getters and setters
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getEvidence() { return evidence; }
        public void setEvidence(List<String> evidence) { this.evidence = evidence; }
    }

    public static class PaymentReviewRequest {
        @NotNull(message = "審核結果不能為空")
        private Boolean approved;

        private String comment;

        // getters and setters
        public Boolean getApproved() { return approved; }
        public void setApproved(Boolean approved) { this.approved = approved; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public static class DisputeResolutionRequest {
        @NotBlank(message = "處理決定不能為空")
        private String resolution;

        @NotBlank(message = "處理說明不能為空")
        private String comment;

        private Map<String, Object> compensation;

        // getters and setters
        public String getResolution() { return resolution; }
        public void setResolution(String resolution) { this.resolution = resolution; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public Map<String, Object> getCompensation() { return compensation; }
        public void setCompensation(Map<String, Object> compensation) { this.compensation = compensation; }
    }
}