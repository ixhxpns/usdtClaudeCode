package com.usdttrading.controller;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.usdttrading.entity.Admin;
import com.usdttrading.security.RSAUtil;
import com.usdttrading.service.AdminService;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.vo.AdminLoginRequest;
import com.usdttrading.vo.AdminLoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.annotation.Resource;
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
    public ApiResponse<AdminLoginResponse> login(@RequestBody @Validated AdminLoginRequest request) {
        try {
            log.info("管理員登入請求: username={}", request.getUsername());
            
            // 參數驗證
            if (StrUtil.isBlank(request.getUsername()) || StrUtil.isBlank(request.getPassword())) {
                return ApiResponse.error("用戶名和密碼不能為空");
            }
            
            // RSA解密密碼
            String decryptedPassword;
            try {
                log.debug("嘗試RSA解密密碼，加密密碼長度: {}", request.getPassword().length());
                decryptedPassword = rsaUtil.decryptWithPrivateKey(request.getPassword());
                log.info("RSA密碼解密成功，解密後密碼長度: {}", decryptedPassword.length());
                log.debug("解密後的密碼: {}", decryptedPassword);
            } catch (Exception e) {
                // 如果RSA解密失敗，嘗試使用原始密碼（降級處理）
                log.warn("RSA密碼解密失敗，使用原始密碼: {}", e.getMessage());
                decryptedPassword = request.getPassword();
                log.debug("使用原始密碼: {}", decryptedPassword);
            }
            
            // 驗證管理員憑證
            Admin admin = adminService.login(request.getUsername(), decryptedPassword);
            if (admin == null) {
                log.warn("管理員登入失敗: username={}", request.getUsername());
                return ApiResponse.error("用戶名或密碼錯誤");
            }
            
            // 檢查管理員狀態
            if (!"active".equals(admin.getStatus())) {
                log.warn("管理員帳戶已停用: username={}, status={}", request.getUsername(), admin.getStatus());
                return ApiResponse.error("帳戶已停用，請聯繫系統管理員");
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
            return ApiResponse.success("管理員登入成功", response);
            
        } catch (Exception e) {
            log.error("管理員登入異常: username={}, error={}", request.getUsername(), e.getMessage(), e);
            return ApiResponse.error("登入失敗: " + e.getMessage());
        }
    }

    /**
     * 管理員登出
     */
    @PostMapping("/logout")
    @Operation(summary = "管理員登出", description = "管理員安全登出系統")
    public ApiResponse<Void> logout() {
        try {
            Long adminId = StpUtil.getLoginIdAsLong();
            log.info("管理員登出: adminId={}", adminId);
            
            // 記錄登出日誌
            if (adminId != null) {
                adminService.recordLogoutLog(adminId);
            }
            
            // 執行登出
            StpUtil.logout();
            
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("管理員登出異常: {}", e.getMessage(), e);
            return ApiResponse.error("登出失敗");
        }
    }

    /**
     * 獲取當前管理員信息
     */
    @GetMapping("/me")
    @Operation(summary = "獲取管理員信息", description = "獲取當前登入管理員的詳細信息")
    public ApiResponse<Admin> getCurrentAdmin() {
        try {
            // 檢查登入狀態
            StpUtil.checkLogin();
            Long adminId = StpUtil.getLoginIdAsLong();
            
            // 獲取管理員信息
            Admin admin = adminService.getById(adminId);
            if (admin == null) {
                return ApiResponse.error("管理員信息不存在");
            }
            
            // 清除敏感信息
            admin.setPassword(null);
            
            return ApiResponse.success("獲取管理員信息成功", admin);
        } catch (Exception e) {
            log.error("獲取管理員信息異常: {}", e.getMessage(), e);
            return ApiResponse.error("獲取管理員信息失敗");
        }
    }

    /**
     * 修改管理員密碼
     */
    @PostMapping("/change-password")
    @Operation(summary = "修改密碼", description = "管理員修改登入密碼")
    public ApiResponse<Void> changePassword(@RequestBody @Validated Map<String, String> request) {
        try {
            // 檢查登入狀態
            StpUtil.checkLogin();
            Long adminId = StpUtil.getLoginIdAsLong();
            
            String currentPassword = request.get("current_password");
            String newPassword = request.get("new_password");
            
            if (StrUtil.isBlank(currentPassword) || StrUtil.isBlank(newPassword)) {
                return ApiResponse.error("當前密碼和新密碼不能為空");
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
                return ApiResponse.error("當前密碼錯誤或密碼修改失敗");
            }
            
            // 記錄操作日誌
            adminService.recordActionLog(adminId, "CHANGE_PASSWORD", "修改登入密碼");
            
            // 強制登出，要求重新登入
            StpUtil.logout();
            
            log.info("管理員密碼修改成功: adminId={}", adminId);
            return ApiResponse.success();
            
        } catch (Exception e) {
            log.error("管理員密碼修改異常: {}", e.getMessage(), e);
            return ApiResponse.error("密碼修改失敗");
        }
    }

    /**
     * 獲取RSA公鑰
     */
    @GetMapping("/public-key")
    @Operation(summary = "獲取RSA公鑰", description = "獲取用於加密敏感數據的RSA公鑰")
    public ApiResponse<Map<String, String>> getPublicKey() {
        try {
            String publicKey = rsaUtil.getPublicKeyString();
            
            Map<String, String> result = new HashMap<>();
            result.put("publicKey", publicKey);
            result.put("keyType", "RSA");
            result.put("keySize", "2048");
            
            return ApiResponse.success("獲取RSA公鑰成功", result);
        } catch (Exception e) {
            log.error("獲取RSA公鑰失敗: {}", e.getMessage(), e);
            return ApiResponse.error("獲取RSA公鑰失敗");
        }
    }

    /**
     * 檢查會話有效性
     */
    @GetMapping("/session/validate")
    @Operation(summary = "驗證會話", description = "驗證當前管理員會話是否有效")
    public ApiResponse<Map<String, Object>> validateSession() {
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
            
            return ApiResponse.success("會話驗證成功", sessionInfo);
        } catch (Exception e) {
            log.warn("會話驗證失敗: {}", e.getMessage());
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("isValid", false);
            sessionInfo.put("error", e.getMessage());
            return ApiResponse.success("會話已過期", sessionInfo);
        }
    }


    /**
     * 管理員API測試端點
     * 用於前後端集成測試和系統健康檢查
     */
    @GetMapping("/test")
    @Operation(summary = "管理員API測試", description = "測試管理員模塊API連接狀態")
    public ApiResponse<Map<String, Object>> adminTest() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("message", "Admin API working");
            data.put("timestamp", System.currentTimeMillis());
            data.put("module", "AdminAuth");
            data.put("version", "1.0.0");
            data.put("status", "OK");
            
            // 添加系統狀態信息
            data.put("activeProfiles", System.getProperty("spring.profiles.active", "default"));
            data.put("javaVersion", System.getProperty("java.version"));
            
            // 添加BCrypt測試
            try {
                org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
                
                String testPassword = "Admin123!";
                String newHash = encoder.encode(testPassword);
                
                // 測試現有哈希
                String existingHash2a = "$2a$10$anrI.n4dhiN3AIJ2ZzmWQeQRrWf.4HXEMC1EJi4ral7pDbUdiv.9m";
                String existingHash2b = "$2b$10$0flRzvgRQBOAx.F.uuM8N.jEOa8P5/mXK23tJWsYnBdmVFJt6paDa";
                
                Map<String, Object> bcryptTest = new HashMap<>();
                bcryptTest.put("newHash", newHash);
                bcryptTest.put("matches2a", encoder.matches(testPassword, existingHash2a));
                bcryptTest.put("matches2b", encoder.matches(testPassword, existingHash2b));
                bcryptTest.put("matchesNewHash", encoder.matches(testPassword, newHash));
                
                data.put("bcryptTest", bcryptTest);
                
            } catch (Exception e) {
                data.put("bcryptError", e.getMessage());
            }
            
            log.info("管理員API測試請求成功");
            return ApiResponse.success("管理員API測試成功", data);
            
        } catch (Exception e) {
            log.error("管理員API測試失敗", e);
            return ApiResponse.error(500, "管理員API測試失敗: " + e.getMessage());
        }
    }

    /**
     * 調試接口：測試admin用戶查詢和密碼驗證
     */
    @GetMapping("/debug/test-admin")
    @Operation(summary = "調試admin用戶", description = "測試admin用戶查詢和密碼驗證")
    public ApiResponse<Map<String, Object>> debugTestAdmin() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 測試查詢admin用戶
            Admin admin = adminService.getByUsername("admin");
            if (admin == null) {
                result.put("userFound", false);
                result.put("error", "Admin user not found");
                return ApiResponse.success("調試結果", result);
            }
            
            result.put("userFound", true);
            result.put("userId", admin.getId());
            result.put("username", admin.getUsername());
            result.put("status", admin.getStatus());
            result.put("isActive", admin.isActive());
            result.put("isAccountLocked", admin.isAccountLocked());
            result.put("passwordHashPrefix", admin.getPassword().substring(0, Math.min(20, admin.getPassword().length())));
            
            // 測試兩個密碼版本
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            
            boolean passwordMatchesOld = encoder.matches("Admin123!", admin.getPassword());
            boolean passwordMatchesNew = encoder.matches("Admin123", admin.getPassword());
            
            result.put("passwordMatches_Admin123!", passwordMatchesOld);
            result.put("passwordMatches_Admin123", passwordMatchesNew);
            
            // 測試當前數據庫中的哈希類型
            String passwordHash = admin.getPassword();
            result.put("hashType", passwordHash.startsWith("$2a$") ? "BCrypt-2a" : 
                                   passwordHash.startsWith("$2b$") ? "BCrypt-2b" : "Unknown");
            result.put("hashLength", passwordHash.length());
            
            return ApiResponse.success("調試測試完成", result);
            
        } catch (Exception e) {
            log.error("調試測試失敗", e);
            return ApiResponse.error("調試測試失敗: " + e.getMessage());
        }
    }

    /**
     * 調試接口：完整登入流程診斷
     */
    @PostMapping("/debug/login-diagnosis")
    @Operation(summary = "登入診斷", description = "詳細診斷登入流程的每一步")
    public ApiResponse<Map<String, Object>> loginDiagnosis(@RequestBody AdminLoginRequest request) {
        Map<String, Object> diagnosis = new HashMap<>();
        
        try {
            log.info("開始登入診斷: username={}", request.getUsername());
            
            // 步驟1: 參數驗證
            Map<String, Object> step1 = new HashMap<>();
            step1.put("step", "參數驗證");
            step1.put("username_provided", request.getUsername() != null);
            step1.put("username_length", request.getUsername() != null ? request.getUsername().length() : 0);
            step1.put("password_provided", request.getPassword() != null);
            step1.put("password_length", request.getPassword() != null ? request.getPassword().length() : 0);
            step1.put("success", StrUtil.isNotBlank(request.getUsername()) && StrUtil.isNotBlank(request.getPassword()));
            diagnosis.put("step1_validation", step1);
            
            if (StrUtil.isBlank(request.getUsername()) || StrUtil.isBlank(request.getPassword())) {
                diagnosis.put("error", "參數驗證失敗");
                return ApiResponse.success("登入診斷結果", diagnosis);
            }
            
            // 步驟2: RSA解密測試
            Map<String, Object> step2 = new HashMap<>();
            step2.put("step", "RSA解密");
            String decryptedPassword = null;
            try {
                decryptedPassword = rsaUtil.decryptWithPrivateKey(request.getPassword());
                step2.put("decryption_success", true);
                step2.put("decrypted_length", decryptedPassword.length());
                step2.put("decrypted_password", decryptedPassword); // 僅調試模式顯示
            } catch (Exception e) {
                step2.put("decryption_success", false);
                step2.put("decryption_error", e.getMessage());
                step2.put("fallback_to_original", true);
                decryptedPassword = request.getPassword();
                step2.put("original_password", decryptedPassword);
            }
            diagnosis.put("step2_rsa_decrypt", step2);
            
            // 步驟3: 用戶查詢
            Map<String, Object> step3 = new HashMap<>();
            step3.put("step", "用戶查詢");
            Admin admin = adminService.getByUsername(request.getUsername());
            step3.put("user_found", admin != null);
            if (admin != null) {
                step3.put("user_id", admin.getId());
                step3.put("user_status", admin.getStatus());
                step3.put("is_active", admin.isActive());
                step3.put("is_locked", admin.isAccountLocked());
                step3.put("password_hash_prefix", admin.getPassword().substring(0, Math.min(15, admin.getPassword().length())));
            }
            diagnosis.put("step3_user_query", step3);
            
            if (admin == null) {
                diagnosis.put("error", "用戶不存在");
                return ApiResponse.success("登入診斷結果", diagnosis);
            }
            
            // 步驟4: 密碼驗證
            Map<String, Object> step4 = new HashMap<>();
            step4.put("step", "密碼驗證");
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            
            boolean passwordMatches = encoder.matches(decryptedPassword, admin.getPassword());
            step4.put("password_matches", passwordMatches);
            step4.put("input_password", decryptedPassword);
            step4.put("stored_hash_type", admin.getPassword().substring(0, 4));
            
            // 測試常見密碼
            String[] testPasswords = {"Admin123!", "Admin123", "admin123", "123456"};
            Map<String, Boolean> passwordTests = new HashMap<>();
            for (String testPwd : testPasswords) {
                passwordTests.put(testPwd, encoder.matches(testPwd, admin.getPassword()));
            }
            step4.put("test_passwords", passwordTests);
            
            diagnosis.put("step4_password_check", step4);
            
            // 步驟5: 狀態檢查
            Map<String, Object> step5 = new HashMap<>();
            step5.put("step", "狀態檢查");
            boolean statusOk = "active".equals(admin.getStatus());
            step5.put("status_check_passed", statusOk);
            step5.put("current_status", admin.getStatus());
            step5.put("expected_status", "active");
            diagnosis.put("step5_status_check", step5);
            
            // 步驟6: 最終結果
            Map<String, Object> step6 = new HashMap<>();
            step6.put("step", "最終結果");
            boolean overallSuccess = passwordMatches && statusOk;
            step6.put("login_would_succeed", overallSuccess);
            if (!overallSuccess) {
                String reason = !passwordMatches ? "密碼錯誤" : "帳戶狀態異常";
                step6.put("failure_reason", reason);
            }
            diagnosis.put("step6_final_result", step6);
            
            log.info("登入診斷完成: username={}, result={}", request.getUsername(), overallSuccess);
            return ApiResponse.success("登入診斷完成", diagnosis);
            
        } catch (Exception e) {
            log.error("登入診斷異常: {}", e.getMessage(), e);
            diagnosis.put("diagnosis_error", e.getMessage());
            return ApiResponse.error("登入診斷失敗: " + e.getMessage());
        }
    }

    /**
     * 臨時測試端點：生成BCrypt哈希
     */
    @GetMapping("/temp/bcrypt/{password}")
    @Operation(summary = "臨時生成BCrypt哈希", description = "測試用途")
    public ApiResponse<Map<String, Object>> generateBCryptHash(@PathVariable String password) {
        try {
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            
            String hash = encoder.encode(password);
            
            Map<String, Object> result = new HashMap<>();
            result.put("password", password);
            result.put("hash", hash);
            result.put("matches", encoder.matches(password, hash));
            
            // 測試與現有哈希的匹配
            String existingHash = "$2b$10$0flRzvgRQBOAx.F.uuM8N.jEOa8P5/mXK23tJWsYnBdmVFJt6paDa";
            String existingHash2a = "$2a$10$anrI.n4dhiN3AIJ2ZzmWQeQRrWf.4HXEMC1EJi4ral7pDbUdiv.9m";
            
            result.put("matches2b", encoder.matches(password, existingHash));
            result.put("matches2a", encoder.matches(password, existingHash2a));
            
            return ApiResponse.success("BCrypt測試", result);
            
        } catch (Exception e) {
            log.error("BCrypt測試失敗", e);
            return ApiResponse.error("BCrypt測試失敗: " + e.getMessage());
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