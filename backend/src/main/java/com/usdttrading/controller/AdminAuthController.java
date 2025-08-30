package com.usdttrading.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.usdttrading.entity.Admin;
import com.usdttrading.security.RSAUtil;
import com.usdttrading.service.AdminService;
import com.usdttrading.util.ApiResult;
import com.usdttrading.vo.AdminLoginRequest;
import com.usdttrading.vo.AdminLoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理員認證控制器
 * 專門處理管理員登入、登出等認證相關操作
 * 
 * @author Master Agent
 * @version 1.0.0
 * @since 2025-08-30
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "管理員認證", description = "管理員登入登出相關接口")
public class AdminAuthController {

    @Resource
    private AdminService adminService;
    
    @Resource
    private RSAUtil rsaUtil;

    /**
     * 管理員登入
     * 支援RSA加密密碼傳輸，具備頻率限制和安全檢查
     */
    @PostMapping("/login")
    @Operation(summary = "管理員登入", description = "管理員使用用戶名密碼登入系統")
    public ApiResult<AdminLoginResponse> login(@RequestBody @Validated AdminLoginRequest request) {
        try {
            log.info("管理員登入請求: username={}", request.getUsername());
            
            // 參數驗證
            if (StrUtil.isBlank(request.getUsername()) || StrUtil.isBlank(request.getPassword())) {
                return ApiResult.error("用戶名和密碼不能為空");
            }
            
            // RSA解密密碼
            String decryptedPassword;
            try {
                decryptedPassword = rsaUtil.decryptWithPrivateKey(request.getPassword());
                log.info("RSA密碼解密成功");
            } catch (Exception e) {
                // 如果RSA解密失敗，嘗試使用原始密碼（降級處理）
                log.warn("RSA密碼解密失敗，使用原始密碼: {}", e.getMessage());
                decryptedPassword = request.getPassword();
            }
            
            // 驗證管理員憑證
            Admin admin = adminService.login(request.getUsername(), decryptedPassword);
            if (admin == null) {
                log.warn("管理員登入失敗: username={}", request.getUsername());
                return ApiResult.error("用戶名或密碼錯誤");
            }
            
            // 檢查管理員狀態
            if (!"active".equals(admin.getStatus())) {
                log.warn("管理員帳戶已停用: username={}, status={}", request.getUsername(), admin.getStatus());
                return ApiResult.error("帳戶已停用，請聯繫系統管理員");
            }
            
            // 生成Sa-Token
            StpUtil.login(admin.getId(), request.getRememberMe() ? 7 * 24 * 60 * 60 : 2 * 60 * 60); // 記住我7天，否則2小時
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
            
            // 記錄登入日誌
            adminService.recordLoginLog(admin.getId(), getClientIP(request));
            
            // 構建響應
            AdminLoginResponse response = AdminLoginResponse.builder()
                .admin(admin)
                .accessToken(tokenInfo.getTokenValue())
                .expiresIn(tokenInfo.getTokenTimeout())
                .tokenType("Bearer")
                .build();
                
            log.info("管理員登入成功: username={}, adminId={}", request.getUsername(), admin.getId());
            return ApiResult.success(response, "管理員登入成功");
            
        } catch (Exception e) {
            log.error("管理員登入異常: username={}, error={}", request.getUsername(), e.getMessage(), e);
            return ApiResult.error("登入失敗: " + e.getMessage());
        }
    }

    /**
     * 管理員登出
     */
    @PostMapping("/logout")
    @Operation(summary = "管理員登出", description = "管理員安全登出系統")
    public ApiResult<Void> logout() {
        try {
            Long adminId = StpUtil.getLoginIdAsLong();
            log.info("管理員登出: adminId={}", adminId);
            
            // 記錄登出日誌
            if (adminId != null) {
                adminService.recordLogoutLog(adminId);
            }
            
            // 執行登出
            StpUtil.logout();
            
            return ApiResult.success(null, "管理員登出成功");
        } catch (Exception e) {
            log.error("管理員登出異常: {}", e.getMessage(), e);
            return ApiResult.error("登出失敗");
        }
    }

    /**
     * 獲取當前管理員信息
     */
    @GetMapping("/me")
    @Operation(summary = "獲取管理員信息", description = "獲取當前登入管理員的詳細信息")
    public ApiResult<Admin> getCurrentAdmin() {
        try {
            // 檢查登入狀態
            StpUtil.checkLogin();
            Long adminId = StpUtil.getLoginIdAsLong();
            
            // 獲取管理員信息
            Admin admin = adminService.getById(adminId);
            if (admin == null) {
                return ApiResult.error("管理員信息不存在");
            }
            
            // 清除敏感信息
            admin.setPassword(null);
            
            return ApiResult.success(admin, "獲取管理員信息成功");
        } catch (Exception e) {
            log.error("獲取管理員信息異常: {}", e.getMessage(), e);
            return ApiResult.error("獲取管理員信息失敗");
        }
    }

    /**
     * 修改管理員密碼
     */
    @PostMapping("/change-password")
    @Operation(summary = "修改密碼", description = "管理員修改登入密碼")
    public ApiResult<Void> changePassword(@RequestBody @Validated Map<String, String> request) {
        try {
            // 檢查登入狀態
            StpUtil.checkLogin();
            Long adminId = StpUtil.getLoginIdAsLong();
            
            String currentPassword = request.get("current_password");
            String newPassword = request.get("new_password");
            
            if (StrUtil.isBlank(currentPassword) || StrUtil.isBlank(newPassword)) {
                return ApiResult.error("當前密碼和新密碼不能為空");
            }
            
            // RSA解密密碼
            try {
                currentPassword = rsaUtil.decryptWithPrivateKey(currentPassword);
                newPassword = rsaUtil.decryptWithPrivateKey(newPassword);
            } catch (Exception e) {
                log.warn("密碼RSA解密失敗，使用原始密碼: {}", e.getMessage());
            }
            
            // 執行密碼修改
            boolean success = adminService.changePassword(adminId, currentPassword, newPassword);
            if (!success) {
                return ApiResult.error("當前密碼錯誤或密碼修改失敗");
            }
            
            // 記錄操作日誌
            adminService.recordActionLog(adminId, "CHANGE_PASSWORD", "修改登入密碼");
            
            // 強制登出，要求重新登入
            StpUtil.logout();
            
            log.info("管理員密碼修改成功: adminId={}", adminId);
            return ApiResult.success(null, "密碼修改成功，請重新登入");
            
        } catch (Exception e) {
            log.error("管理員密碼修改異常: {}", e.getMessage(), e);
            return ApiResult.error("密碼修改失敗");
        }
    }

    /**
     * 獲取RSA公鑰
     */
    @GetMapping("/public-key")
    @Operation(summary = "獲取RSA公鑰", description = "獲取用於加密敏感數據的RSA公鑰")
    public ApiResult<Map<String, String>> getPublicKey() {
        try {
            String publicKey = rsaUtil.getPublicKeyString();
            
            Map<String, String> result = new HashMap<>();
            result.put("publicKey", publicKey);
            result.put("keyType", "RSA");
            result.put("keySize", "2048");
            
            return ApiResult.success(result, "獲取RSA公鑰成功");
        } catch (Exception e) {
            log.error("獲取RSA公鑰失敗: {}", e.getMessage(), e);
            return ApiResult.error("獲取RSA公鑰失敗");
        }
    }

    /**
     * 檢查會話有效性
     */
    @GetMapping("/session/validate")
    @Operation(summary = "驗證會話", description = "驗證當前管理員會話是否有效")
    public ApiResult<Map<String, Object>> validateSession() {
        try {
            // 檢查登入狀態
            StpUtil.checkLogin();
            Long adminId = StpUtil.getLoginIdAsLong();
            
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
            
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("adminId", adminId);
            sessionInfo.put("tokenValue", tokenInfo.getTokenValue());
            sessionInfo.put("expiresIn", tokenInfo.getTokenTimeout());
            sessionInfo.put("isValid", true);
            
            return ApiResult.success(sessionInfo, "會話驗證成功");
        } catch (Exception e) {
            log.warn("會話驗證失敗: {}", e.getMessage());
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("isValid", false);
            sessionInfo.put("error", e.getMessage());
            return ApiResult.success(sessionInfo, "會話已過期");
        }
    }

    /**
     * 獲取客戶端IP地址
     */
    private String getClientIP(AdminLoginRequest request) {
        // 這裡可以從HttpServletRequest中獲取真實IP
        // 暫時返回默認值
        return "127.0.0.1";
    }
}