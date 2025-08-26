package com.usdttrading.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.entity.UserKyc;
import com.usdttrading.service.FileStorageService;
import com.usdttrading.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * KYC用戶端控制器
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Slf4j
@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC用戶端", description = "KYC身份驗證相關接口")
@SaCheckLogin
public class KycController {

    private final KycService kycService;
    private final FileStorageService fileStorageService;

    /**
     * 提交基本KYC信息
     *
     * @param kycData KYC基本信息
     * @return ApiResponse
     */
    @PostMapping("/basic")
    @Operation(summary = "提交基本KYC信息", description = "提交用戶基本身份信息進行KYC驗證")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "提交成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "參數錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "系統錯誤")
    })
    public ApiResponse<UserKyc> submitBasicKycInfo(
            @Parameter(description = "KYC基本信息", required = true)
            @Valid @RequestBody Map<String, Object> kycData) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶提交基本KYC信息: userId={}", userId);
        
        return kycService.submitBasicKycInfo(userId, kycData);
    }

    /**
     * 上傳身份證件
     *
     * @param frontImage 身份證正面
     * @param backImage 身份證反面
     * @param selfieImage 手持身份證自拍照
     * @return ApiResponse
     */
    @PostMapping("/upload-id")
    @Operation(summary = "上傳身份證件", description = "上傳身份證正面、反面和手持身份證自拍照")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "上傳成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "文件格式錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "文件過大"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "上傳失敗")
    })
    public ApiResponse<Void> uploadIdDocuments(
            @Parameter(description = "身份證正面圖片", required = true)
            @RequestParam("frontImage") @NotNull MultipartFile frontImage,
            
            @Parameter(description = "身份證反面圖片", required = true)
            @RequestParam("backImage") @NotNull MultipartFile backImage,
            
            @Parameter(description = "手持身份證自拍照", required = true)
            @RequestParam("selfieImage") @NotNull MultipartFile selfieImage) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶上傳身份證件: userId={}", userId);
        
        return kycService.uploadIdDocuments(userId, frontImage, backImage, selfieImage);
    }

    /**
     * 上傳第二證件
     *
     * @param docType 證件類型
     * @param docImage 證件圖片
     * @return ApiResponse
     */
    @PostMapping("/upload-secondary")
    @Operation(summary = "上傳第二證件", description = "上傳護照、駕照等第二身份證明文件")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "上傳成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "證件類型不支持"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "上傳失敗")
    })
    public ApiResponse<Void> uploadSecondaryDocument(
            @Parameter(description = "證件類型", required = true, 
                      schema = @Schema(allowableValues = {"passport", "driver_license", "utility_bill", "bank_statement"}))
            @RequestParam("docType") @NotBlank String docType,
            
            @Parameter(description = "證件圖片", required = true)
            @RequestParam("docImage") @NotNull MultipartFile docImage) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶上傳第二證件: userId={}, docType={}", userId, docType);
        
        return kycService.uploadSecondaryDocument(userId, docType, docImage);
    }

    /**
     * 綁定銀行賬戶
     *
     * @param bankInfo 銀行賬戶信息
     * @return ApiResponse
     */
    @PostMapping("/bind-bank")
    @Operation(summary = "綁定銀行賬戶", description = "綁定用戶銀行賬戶信息")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "綁定成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "銀行信息格式錯誤"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "綁定失敗")
    })
    public ApiResponse<Void> bindBankAccount(
            @Parameter(description = "銀行賬戶信息", required = true)
            @Valid @RequestBody Map<String, String> bankInfo) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶綁定銀行賬戶: userId={}, bankName={}", userId, bankInfo.get("bankName"));
        
        return kycService.bindBankAccount(userId, bankInfo);
    }

    /**
     * 查詢KYC狀態
     *
     * @return ApiResponse<KycStatusInfo>
     */
    @GetMapping("/status")
    @Operation(summary = "查詢KYC狀態", description = "獲取當前用戶的KYC驗證狀態和進度")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "未找到KYC記錄"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "查詢失敗")
    })
    public ApiResponse<Map<String, Object>> getKycStatus() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶查詢KYC狀態: userId={}", userId);
        
        return kycService.getKycStatus(userId);
    }

    /**
     * 重新提交KYC
     *
     * @return ApiResponse
     */
    @PutMapping("/resubmit")
    @Operation(summary = "重新提交KYC", description = "在KYC被拒絕或需要補充材料時重新提交")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "重新提交成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "當前狀態不允許重新提交"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "已達到最大提交次數"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "重新提交失敗")
    })
    public ApiResponse<Void> resubmitKyc() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶重新提交KYC: userId={}", userId);
        
        return kycService.resubmitKyc(userId);
    }

    /**
     * 獲取文件訪問URL
     *
     * @param documentId 文檔ID
     * @return ApiResponse<String>
     */
    @GetMapping("/file/{documentId}")
    @Operation(summary = "獲取文件訪問URL", description = "獲取已上傳文件的安全訪問鏈接")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無訪問權限"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "文件不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "文件已過期")
    })
    public ApiResponse<String> getFileUrl(
            @Parameter(description = "文檔ID", required = true)
            @PathVariable("documentId") @NotNull Long documentId) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶獲取文件URL: userId={}, documentId={}", userId, documentId);
        
        return fileStorageService.getSignedFileUrl(documentId, userId);
    }

    /**
     * 刪除已上傳的文件
     *
     * @param documentId 文檔ID
     * @return ApiResponse
     */
    @DeleteMapping("/file/{documentId}")
    @Operation(summary = "刪除文件", description = "刪除已上傳的KYC文件")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "刪除成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無刪除權限"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "文件不存在")
    })
    public ApiResponse<Void> deleteFile(
            @Parameter(description = "文檔ID", required = true)
            @PathVariable("documentId") @NotNull Long documentId) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("用戶刪除文件: userId={}, documentId={}", userId, documentId);
        
        return fileStorageService.deleteDocument(documentId, userId);
    }

    /**
     * 獲取KYC等級說明
     *
     * @return ApiResponse<Map<String, Object>>
     */
    @GetMapping("/levels")
    @Operation(summary = "獲取KYC等級說明", description = "獲取不同KYC等級的說明和權限")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功")
    })
    public ApiResponse<Map<String, Object>> getKycLevels() {
        log.info("用戶查詢KYC等級說明");
        
        Map<String, Object> levels = Map.of(
            "level1", Map.of(
                "name", "基礎認證",
                "description", "身份基本信息驗證",
                "maxWithdrawDaily", 1000,
                "maxWithdrawMonthly", 10000,
                "requirements", new String[]{"身份證正反面", "手持身份證自拍"}
            ),
            "level2", Map.of(
                "name", "進階認證", 
                "description", "銀行賬戶和地址驗證",
                "maxWithdrawDaily", 10000,
                "maxWithdrawMonthly", 100000,
                "requirements", new String[]{"基礎認證", "銀行賬戶信息", "地址證明"}
            ),
            "level3", Map.of(
                "name", "專業認證",
                "description", "收入來源和資金來源驗證",
                "maxWithdrawDaily", 50000,
                "maxWithdrawMonthly", 500000,
                "requirements", new String[]{"進階認證", "收入證明", "資金來源說明"}
            )
        );
        
        return ApiResponse.success("獲取KYC等級說明成功", levels);
    }

    /**
     * 獲取支持的文檔類型
     *
     * @return ApiResponse<Map<String, Object>>
     */
    @GetMapping("/supported-documents")
    @Operation(summary = "獲取支持的文檔類型", description = "獲取KYC支持的文檔類型和要求")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功")
    })
    public ApiResponse<Map<String, Object>> getSupportedDocuments() {
        log.info("用戶查詢支持的文檔類型");
        
        Map<String, Object> documents = Map.of(
            "required", Map.of(
                "id_front", Map.of("name", "身份證正面", "formats", new String[]{"JPG", "PNG"}, "maxSize", "5MB"),
                "id_back", Map.of("name", "身份證反面", "formats", new String[]{"JPG", "PNG"}, "maxSize", "5MB"),
                "selfie", Map.of("name", "手持身份證自拍", "formats", new String[]{"JPG", "PNG"}, "maxSize", "5MB")
            ),
            "optional", Map.of(
                "passport", Map.of("name", "護照", "formats", new String[]{"JPG", "PNG", "PDF"}, "maxSize", "10MB"),
                "driver_license", Map.of("name", "駕駛執照", "formats", new String[]{"JPG", "PNG"}, "maxSize", "5MB"),
                "utility_bill", Map.of("name", "水電煤賬單", "formats", new String[]{"JPG", "PNG", "PDF"}, "maxSize", "5MB"),
                "bank_statement", Map.of("name", "銀行對帳單", "formats", new String[]{"JPG", "PNG", "PDF"}, "maxSize", "10MB")
            ),
            "tips", new String[]{
                "請確保圖片清晰，光線充足",
                "身份證件需要完整顯示，四個角不能缺失",
                "手持身份證自拍需要清楚顯示人臉和身份證信息",
                "文件大小不超過限制，格式必須正確"
            }
        );
        
        return ApiResponse.success("獲取支持文檔類型成功", documents);
    }
}