package com.usdttrading.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.service.KycManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * KYC管理端控制器
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC管理端", description = "KYC審核管理相關接口")
@SaCheckLogin
@SaCheckRole("ADMIN")
public class KycManagementController {

    private final KycManagementService kycManagementService;

    /**
     * 分頁查詢KYC申請列表
     *
     * @param pageNum 頁碼
     * @param pageSize 頁大小  
     * @param status 狀態過濾
     * @param riskLevel 風險等級過濾
     * @return 分頁結果
     */
    @GetMapping("/applications")
    @Operation(summary = "查詢KYC申請列表", description = "分頁查詢待審核的KYC申請")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<Page<Map<String, Object>>> getKycApplications(
            @Parameter(description = "頁碼", example = "1")
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            
            @Parameter(description = "頁大小", example = "20")
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            
            @Parameter(description = "狀態過濾")
            @RequestParam(value = "status", required = false) String status,
            
            @Parameter(description = "風險等級過濾")
            @RequestParam(value = "riskLevel", required = false) Integer riskLevel) {
        
        log.info("管理員查詢KYC申請列表: pageNum={}, pageSize={}, status={}, riskLevel={}", 
                pageNum, pageSize, status, riskLevel);
        
        return kycManagementService.getKycApplications(pageNum, pageSize, status, riskLevel);
    }

    /**
     * 獲取KYC申請詳細信息
     *
     * @param kycId KYC ID
     * @return KYC詳細信息
     */
    @GetMapping("/{kycId}")
    @Operation(summary = "獲取KYC詳情", description = "查看特定KYC申請的詳細信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "KYC記錄不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<Map<String, Object>> getKycDetail(
            @Parameter(description = "KYC ID", required = true)
            @PathVariable("kycId") @NotNull Long kycId) {
        
        log.info("管理員查詢KYC詳情: kycId={}", kycId);
        
        return kycManagementService.getKycDetail(kycId);
    }

    /**
     * 審核KYC申請
     *
     * @param kycId KYC ID
     * @param reviewData 審核數據
     * @return ApiResponse
     */
    @PostMapping("/{kycId}/review")
    @Operation(summary = "審核KYC申請", description = "對KYC申請進行通過、拒絕或要求補充材料的操作")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "審核成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "審核參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "KYC記錄不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<Object> reviewKycApplication(
            @Parameter(description = "KYC ID", required = true)
            @PathVariable("kycId") @NotNull Long kycId,
            
            @Parameter(description = "審核數據", required = true)
            @Valid @RequestBody Map<String, Object> reviewData) {
        
        Long reviewerId = StpUtil.getLoginIdAsLong();
        String result = (String) reviewData.get("result");
        String comment = (String) reviewData.get("comment");
        String supplementRequirement = (String) reviewData.get("supplementRequirement");
        
        log.info("管理員審核KYC: kycId={}, reviewerId={}, result={}", kycId, reviewerId, result);
        
        ApiResponse<Void> response = kycManagementService.reviewKycApplication(kycId, reviewerId, result, comment, supplementRequirement);
        return (ApiResponse<Object>) (Object) response;
    }

    /**
     * 批量審核KYC申請
     *
     * @param batchReviewData 批量審核數據
     * @return ApiResponse
     */
    @PostMapping("/batch-review")
    @Operation(summary = "批量審核KYC", description = "批量處理多個KYC申請")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "批量審核完成"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<Map<String, Object>> batchReviewKyc(
            @Parameter(description = "批量審核數據", required = true)
            @Valid @RequestBody Map<String, Object> batchReviewData) {
        
        Long reviewerId = StpUtil.getLoginIdAsLong();
        
        @SuppressWarnings("unchecked")
        List<Long> kycIds = (List<Long>) batchReviewData.get("kycIds");
        String result = (String) batchReviewData.get("result");
        String comment = (String) batchReviewData.get("comment");
        
        if (kycIds == null || kycIds.isEmpty()) {
            return ApiResponse.error("KYC ID列表不能為空");
        }
        
        log.info("管理員批量審核KYC: reviewerId={}, count={}, result={}", reviewerId, kycIds.size(), result);
        
        return kycManagementService.batchReviewKyc(kycIds, reviewerId, result, comment);
    }

    /**
     * 獲取審核統計數據
     *
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return 統計數據
     */
    @GetMapping("/statistics")
    @Operation(summary = "獲取審核統計", description = "獲取KYC審核的統計數據")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "日期參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "權限不足")
    })
    public ApiResponse<Map<String, Object>> getReviewStatistics(
            @Parameter(description = "開始日期", example = "2025-01-01", required = true)
            @RequestParam("startDate") @NotBlank String startDate,
            
            @Parameter(description = "結束日期", example = "2025-01-31", required = true)
            @RequestParam("endDate") @NotBlank String endDate) {
        
        log.info("管理員查詢審核統計: startDate={}, endDate={}", startDate, endDate);
        
        return kycManagementService.getReviewStatistics(startDate, endDate);
    }

    /**
     * 獲取待辦任務數量
     *
     * @return 待辦數量
     */
    @GetMapping("/pending-count")
    @Operation(summary = "獲取待辦數量", description = "獲取當前待審核的KYC申請數量")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功")
    })
    public ApiResponse<Map<String, Object>> getPendingCount() {
        log.info("管理員查詢待辦KYC數量");
        
        // 這裡可以添加具體的待辦統計邏輯
        Map<String, Object> result = Map.of(
            "pendingReview", 0,
            "underReview", 0,
            "requiresSupplement", 0,
            "highRiskPending", 0
        );
        
        return ApiResponse.success("獲取待辦數量成功", result);
    }

    /**
     * 獲取審核員工作負載統計
     *
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return 工作負載統計
     */
    @GetMapping("/reviewer-workload")
    @Operation(summary = "審核員工作負載", description = "獲取審核員的工作負載統計")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "日期參數錯誤")
    })
    @SaCheckRole("SUPER_ADMIN") // 需要更高權限
    public ApiResponse<Map<String, Object>> getReviewerWorkload(
            @Parameter(description = "開始日期", example = "2025-01-01", required = true)
            @RequestParam("startDate") @NotBlank String startDate,
            
            @Parameter(description = "結束日期", example = "2025-01-31", required = true)
            @RequestParam("endDate") @NotBlank String endDate) {
        
        log.info("查詢審核員工作負載: startDate={}, endDate={}", startDate, endDate);
        
        // 這裡可以添加具體的工作負載統計邏輯
        Map<String, Object> result = Map.of(
            "reviewerStats", List.of(),
            "avgProcessingTime", "0分鐘",
            "totalProcessed", 0
        );
        
        return ApiResponse.success("獲取工作負載統計成功", result);
    }

    /**
     * 導出KYC審核報告
     *
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @param format 導出格式
     * @return ApiResponse
     */
    @GetMapping("/export-report")
    @Operation(summary = "導出審核報告", description = "導出指定時間範圍的KYC審核報告")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "導出成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤")
    })
    @SaCheckRole("SUPER_ADMIN") // 需要更高權限
    public ApiResponse<Object> exportReport(
            @Parameter(description = "開始日期", example = "2025-01-01", required = true)
            @RequestParam("startDate") @NotBlank String startDate,
            
            @Parameter(description = "結束日期", example = "2025-01-31", required = true)
            @RequestParam("endDate") @NotBlank String endDate,
            
            @Parameter(description = "導出格式", example = "excel")
            @RequestParam(value = "format", defaultValue = "excel") String format) {
        
        log.info("導出KYC審核報告: startDate={}, endDate={}, format={}", startDate, endDate, format);
        
        // 這裡應該實現實際的報告導出邏輯
        return ApiResponse.success("報告導出功能開發中", null);
    }

    /**
     * 獲取KYC配置信息
     *
     * @return 配置信息
     */
    @GetMapping("/config")
    @Operation(summary = "獲取KYC配置", description = "獲取KYC審核的配置參數")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功")
    })
    public ApiResponse<Map<String, Object>> getKycConfig() {
        log.info("管理員查詢KYC配置");
        
        Map<String, Object> config = Map.of(
            "autoApprovalThreshold", 30,
            "autoRejectionThreshold", 70,
            "maxSubmissions", 3,
            "minAge", 18,
            "enableAutoReview", true,
            "reviewTimeoutHours", 24,
            "supportedDocumentTypes", List.of("id_front", "id_back", "selfie", "passport", "driver_license"),
            "riskLevels", Map.of(
                "1", "極低風險",
                "2", "低風險", 
                "3", "中低風險",
                "4", "中等風險",
                "5", "中高風險",
                "6", "高風險",
                "7", "極高風險",
                "8", "關鍵風險"
            )
        );
        
        return ApiResponse.success("獲取KYC配置成功", config);
    }
}