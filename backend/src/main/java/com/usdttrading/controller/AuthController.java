package com.usdttrading.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.entity.User;
import com.usdttrading.service.UserService;
import com.usdttrading.service.EmailService;
import com.usdttrading.service.AuditLogService;
import com.usdttrading.security.JwtUtil;
import com.usdttrading.security.RSAUtil;
import com.usdttrading.utils.ValidationUtils;
import com.usdttrading.utils.RequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 認證控制器
 * 提供用戶註冊、登錄、登出、密碼管理等API端點
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "認證管理", description = "用戶認證相關API")
public class AuthController {

    private final UserService userService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final JwtUtil jwtUtil;
    private final RSAUtil rsaUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ValidationUtils validationUtils;

    /**
     * 發送註冊前驗證碼
     */
    @PostMapping("/send-email-verification")
    @Operation(summary = "發送註冊前驗證碼", description = "為註冊流程發送郵箱驗證碼")
    public ApiResponse<Map<String, Object>> sendEmailVerification(
            @Valid @RequestBody SendVerificationRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = RequestUtils.getClientIp(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);

        // 檢查發送頻率限制
        String rateLimitKey = "send_verification_limit:" + clientIp;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(rateLimitKey);
        if (attempts != null && attempts >= 5) {
            return ApiResponse.error("發送過於頻繁，請10分鐘後重試");
        }

        try {
            // 驗證郵箱格式
            if (!validationUtils.isValidEmail(request.getEmail())) {
                return ApiResponse.error("郵箱格式無效");
            }
            
            // 檢查郵箱是否已註冊
            if (userService.existsByEmail(request.getEmail())) {
                return ApiResponse.error("該郵箱已被註冊");
            }

            // 生成驗證碼
            String verificationCode = emailService.generateVerificationCode();
            String verifyKey = "pre_register_verify:" + request.getEmail();
            
            // 存儲驗證碼，有效期10分鐘
            redisTemplate.opsForValue().set(verifyKey, verificationCode, 10, TimeUnit.MINUTES);

            // 發送驗證郵件
            emailService.sendPreRegistrationVerificationEmail(request.getEmail(), verificationCode);

            // 記錄發送頻率限制
            redisTemplate.opsForValue().set(rateLimitKey, (attempts == null ? 0 : attempts) + 1, 10, TimeUnit.MINUTES);

            // 記錄安全事件
            auditLogService.logSecurityEvent(null, "PRE_REGISTER_VERIFICATION_SENT", 
                    "發送註冊前驗證碼: " + request.getEmail(), clientIp, userAgent, true, null);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "驗證碼已發送，請檢查郵箱");
            result.put("email", request.getEmail());
            result.put("expiryTime", 10 * 60); // 10分鐘，單位秒

            return ApiResponse.success(result);

        } catch (Exception e) {
            // 記錄失敗嘗試
            redisTemplate.opsForValue().set(rateLimitKey, (attempts == null ? 0 : attempts) + 1, 10, TimeUnit.MINUTES);
            
            auditLogService.logSecurityEvent(null, "PRE_REGISTER_VERIFICATION_FAILED", 
                    "發送註冊前驗證碼失敗: " + e.getMessage(), clientIp, userAgent, false, e.getMessage());
            
            log.error("發送註冊前驗證碼失敗: {}", e.getMessage());
            return ApiResponse.error("發送失敗，請稍後重試");
        }
    }

    /**
     * 用戶註冊
     */
    @PostMapping("/register")
    @Operation(summary = "用戶註冊", description = "註冊新用戶，需要提供預驗證的驗證碼")
    public ApiResponse<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = RequestUtils.getClientIp(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);

        // 檢查註冊頻率限制
        String rateLimitKey = "register_limit:" + clientIp;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(rateLimitKey);
        if (attempts != null && attempts >= 3) {
            return ApiResponse.error("註冊過於頻繁，請5分鐘後重試");
        }

        // 驗證輸入
        if (!validationUtils.isValidEmail(request.getEmail())) {
            return ApiResponse.error("郵箱格式無效");
        }

        if (!validationUtils.isValidPassword(request.getPassword())) {
            return ApiResponse.error("密碼必須包含8位以上，且包含大小寫字母、數字和特殊字符");
        }

        // 驗證用戶名（如果提供）
        if (StrUtil.isNotBlank(request.getUsername()) && !validationUtils.isValidUsername(request.getUsername())) {
            return ApiResponse.error("用戶名格式無效，請使用3-20位字母、數字或下劃線");
        }

        try {
            // 驗證預註冊驗證碼
            String verifyKey = "pre_register_verify:" + request.getEmail();
            String storedCode = (String) redisTemplate.opsForValue().get(verifyKey);
            
            if (storedCode == null) {
                return ApiResponse.error("驗證碼已過期，請重新獲取");
            }
            
            if (!storedCode.equals(request.getVerificationCode())) {
                return ApiResponse.error("驗證碼錯誤");
            }

            // 註冊用戶（包含用戶名）
            User user = userService.register(request.getUsername(), request.getEmail(), request.getPassword(), request.getPhone());

            // 刪除預註冊驗證碼
            redisTemplate.delete(verifyKey);

            // 直接激活用戶並設置郵箱已驗證（因為已經預驗證）
            userService.activateUser(user.getId());

            // 生成JWT Token
            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRoleId());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());

            // 存儲刷新令牌
            String refreshKey = "refresh_token:" + user.getId();
            redisTemplate.opsForValue().set(refreshKey, refreshToken, 7, TimeUnit.DAYS);

            // Sa-Token 登錄
            StpUtil.login(user.getId());

            // 記錄註冊頻率限制
            redisTemplate.opsForValue().set(rateLimitKey, (attempts == null ? 0 : attempts) + 1, 5, TimeUnit.MINUTES);

            // 記錄安全事件
            auditLogService.logSecurityEvent(user.getId(), "USER_REGISTER", 
                    "用戶註冊", clientIp, userAgent, true, null);

            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            result.put("expiresIn", jwtUtil.getAccessTokenExpiration() / 1000);
            result.put("user", user);
            result.put("message", "註冊成功");

            return ApiResponse.success(result);

        } catch (Exception e) {
            // 記錄失敗嘗試
            redisTemplate.opsForValue().set(rateLimitKey, (attempts == null ? 0 : attempts) + 1, 5, TimeUnit.MINUTES);
            
            auditLogService.logSecurityEvent(null, "USER_REGISTER_FAILED", 
                    "註冊失敗: " + e.getMessage(), clientIp, userAgent, false, e.getMessage());
            
            log.error("用戶註冊失敗: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 用戶登錄
     */
    @PostMapping("/login")
    @Operation(summary = "用戶登錄", description = "用戶郵箱密碼登錄")
    public ApiResponse<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = RequestUtils.getClientIp(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);

        // 檢查登錄頻率限制
        String rateLimitKey = "login_limit:" + clientIp;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(rateLimitKey);
        if (attempts != null && attempts >= 5) {
            return ApiResponse.error("登錄嘗試過於頻繁，請15分鐘後重試");
        }

        try {
            // 用戶登錄（支持RSA解密）
            User user;
            try {
                // 嘗試RSA解密登錄
                user = userService.loginWithRSADecryption(request.getEmail(), request.getPassword(), clientIp, userAgent);
                log.debug("RSA解密登錄成功：{}", request.getEmail());
            } catch (Exception rsaException) {
                // RSA解密失敗，嘗試普通登錄（兼容性）
                log.debug("RSA解密失敗，嘗試普通登錄：{}", request.getEmail());
                user = userService.login(request.getEmail(), request.getPassword(), clientIp, userAgent);
            }

            // 生成JWT Token
            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRoleId());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());

            // 存儲刷新令牌
            String refreshKey = "refresh_token:" + user.getId();
            redisTemplate.opsForValue().set(refreshKey, refreshToken, 7, TimeUnit.DAYS);

            // 記錄設備信息
            String deviceKey = "user_device:" + user.getId() + ":" + RequestUtils.generateDeviceFingerprint(userAgent, clientIp);
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("ip", clientIp);
            deviceInfo.put("userAgent", userAgent);
            deviceInfo.put("loginTime", System.currentTimeMillis());
            redisTemplate.opsForValue().set(deviceKey, deviceInfo, 30, TimeUnit.DAYS);

            // 重置登錄失敗計數
            redisTemplate.delete(rateLimitKey);

            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            result.put("user", user);
            result.put("expiresIn", jwtUtil.getAccessTokenExpiration() / 1000);

            return ApiResponse.success(result);

        } catch (Exception e) {
            // 記錄失敗嘗試
            redisTemplate.opsForValue().set(rateLimitKey, (attempts == null ? 0 : attempts) + 1, 15, TimeUnit.MINUTES);
            
            auditLogService.logSecurityEvent(null, "USER_LOGIN_FAILED", 
                    "登錄失敗: " + e.getMessage(), clientIp, userAgent, false, e.getMessage());
            
            log.error("用戶登錄失敗: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 用戶登出
     */
    @PostMapping("/logout")
    @SaCheckLogin
    @Operation(summary = "用戶登出", description = "登出當前用戶")
    public ApiResponse<String> logout(HttpServletRequest httpRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);

        try {
            // 獲取當前Token
            String token = StpUtil.getTokenValue();
            
            // 將Token加入黑名單
            if (StrUtil.isNotBlank(token)) {
                String blacklistKey = "jwt_blacklist:" + token;
                redisTemplate.opsForValue().set(blacklistKey, "logout", 
                        jwtUtil.getAccessTokenExpiration(), TimeUnit.MILLISECONDS);
            }

            // 刪除刷新令牌
            String refreshKey = "refresh_token:" + userId;
            redisTemplate.delete(refreshKey);

            // Sa-Token登出
            userService.logout();

            // 記錄安全事件
            auditLogService.logSecurityEvent(userId, "USER_LOGOUT", 
                    "用戶登出", clientIp, userAgent, true, null);

            return ApiResponse.success("登出成功");

        } catch (Exception e) {
            log.error("用戶登出失敗: {}", e.getMessage());
            return ApiResponse.error("登出失敗");
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用刷新令牌獲取新的訪問令牌")
    public ApiResponse<Map<String, Object>> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            // 驗證刷新令牌
            if (!jwtUtil.validateToken(request.getRefreshToken())) {
                return ApiResponse.error("刷新令牌無效");
            }

            Long userId = jwtUtil.getUserIdFromToken(request.getRefreshToken());
            
            // 檢查Redis中的刷新令牌
            String refreshKey = "refresh_token:" + userId;
            String storedToken = (String) redisTemplate.opsForValue().get(refreshKey);
            
            if (!request.getRefreshToken().equals(storedToken)) {
                return ApiResponse.error("刷新令牌不匹配");
            }

            // 獲取用戶信息
            User user = userService.getById(userId);
            
            // 生成新的訪問令牌
            String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getRoleId());

            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", newAccessToken);
            result.put("expiresIn", jwtUtil.getAccessTokenExpiration() / 1000);

            return ApiResponse.success(result);

        } catch (Exception e) {
            log.error("刷新Token失敗: {}", e.getMessage());
            return ApiResponse.error("刷新Token失敗");
        }
    }

    /**
     * 郵箱驗證
     */
    @PostMapping("/verify-email")
    @Operation(summary = "郵箱驗證", description = "驗證用戶郵箱")
    public ApiResponse<String> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            String verifyKey = "email_verify:" + request.getUserId();
            String storedCode = (String) redisTemplate.opsForValue().get(verifyKey);

            if (storedCode == null) {
                return ApiResponse.error("驗證碼已過期");
            }

            if (!storedCode.equals(request.getCode())) {
                return ApiResponse.error("驗證碼錯誤");
            }

            // 驗證郵箱
            userService.verifyEmail(request.getUserId());
            
            // 刪除驗證碼
            redisTemplate.delete(verifyKey);

            return ApiResponse.success("郵箱驗證成功");

        } catch (Exception e) {
            log.error("郵箱驗證失敗: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 重發驗證郵件
     */
    @PostMapping("/resend-verification")
    @Operation(summary = "重發驗證郵件", description = "重新發送郵箱驗證碼")
    public ApiResponse<String> resendVerification(@RequestParam Long userId) {
        try {
            User user = userService.getById(userId);
            
            if (user.isEmailVerified()) {
                return ApiResponse.error("郵箱已驗證");
            }

            // 檢查重發頻率
            String resendKey = "resend_limit:" + userId;
            Integer attempts = (Integer) redisTemplate.opsForValue().get(resendKey);
            if (attempts != null && attempts >= 3) {
                return ApiResponse.error("重發次數過多，請1小時後重試");
            }

            // 生成新驗證碼
            String verificationCode = emailService.generateVerificationCode();
            String verifyKey = "email_verify:" + userId;
            redisTemplate.opsForValue().set(verifyKey, verificationCode, 5, TimeUnit.MINUTES);

            // 發送驗證郵件
            emailService.sendVerificationEmail(user.getEmail(), userId, verificationCode);

            // 記錄重發次數
            redisTemplate.opsForValue().set(resendKey, (attempts == null ? 0 : attempts) + 1, 1, TimeUnit.HOURS);

            return ApiResponse.success("驗證郵件已重新發送");

        } catch (Exception e) {
            log.error("重發驗證郵件失敗: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 忘記密碼
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "忘記密碼", description = "發送密碼重設郵件")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                            HttpServletRequest httpRequest) {
        String clientIp = RequestUtils.getClientIp(httpRequest);
        
        try {
            User user = userService.getByEmail(request.getEmail());
            if (user == null) {
                // 為了安全，不透露用戶是否存在
                return ApiResponse.success("如果郵箱存在，重設鏈接已發送");
            }

            // 檢查重設頻率
            String resetKey = "reset_limit:" + user.getId();
            Integer attempts = (Integer) redisTemplate.opsForValue().get(resetKey);
            if (attempts != null && attempts >= 3) {
                return ApiResponse.error("重設次數過多，請1小時後重試");
            }

            // 生成重設令牌
            String resetToken = jwtUtil.generatePasswordResetToken(user.getId());
            String tokenKey = "reset_token:" + user.getId();
            redisTemplate.opsForValue().set(tokenKey, resetToken, 30, TimeUnit.MINUTES);

            // 發送重設郵件
            emailService.sendPasswordResetEmail(user.getEmail(), user.getId(), resetToken);

            // 記錄重設次數
            redisTemplate.opsForValue().set(resetKey, (attempts == null ? 0 : attempts) + 1, 1, TimeUnit.HOURS);

            // 記錄安全事件
            auditLogService.logSecurityEvent(user.getId(), "PASSWORD_RESET_REQUEST", 
                    "密碼重設請求", clientIp, "", true, null);

            return ApiResponse.success("如果郵箱存在，重設鏈接已發送");

        } catch (Exception e) {
            log.error("忘記密碼處理失敗: {}", e.getMessage());
            return ApiResponse.error("處理失敗，請稍後重試");
        }
    }

    /**
     * 重設密碼
     */
    @PostMapping("/reset-password")
    @Operation(summary = "重設密碼", description = "使用重設令牌重設密碼")
    public ApiResponse<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                           HttpServletRequest httpRequest) {
        String clientIp = RequestUtils.getClientIp(httpRequest);
        
        try {
            // 驗證重設令牌
            if (!jwtUtil.validateToken(request.getToken())) {
                return ApiResponse.error("重設令牌無效或已過期");
            }

            Long userId = jwtUtil.getUserIdFromToken(request.getToken());
            
            // 檢查Redis中的令牌
            String tokenKey = "reset_token:" + userId;
            String storedToken = (String) redisTemplate.opsForValue().get(tokenKey);
            
            if (!request.getToken().equals(storedToken)) {
                return ApiResponse.error("重設令牌不匹配");
            }

            // 驗證新密碼
            if (!validationUtils.isValidPassword(request.getNewPassword())) {
                return ApiResponse.error("密碼必須包含8位以上，且包含大小寫字母、數字和特殊字符");
            }

            // 重設密碼
            userService.changePassword(userId, "", request.getNewPassword());

            // 刪除重設令牌
            redisTemplate.delete(tokenKey);
            
            // 刪除所有該用戶的會話
            StpUtil.kickout(userId);

            // 記錄安全事件
            auditLogService.logSecurityEvent(userId, "PASSWORD_RESET_SUCCESS", 
                    "密碼重設成功", clientIp, "", true, null);

            return ApiResponse.success("密碼重設成功，請重新登錄");

        } catch (Exception e) {
            log.error("重設密碼失敗: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 修改密碼
     */
    @PostMapping("/change-password")
    @SaCheckLogin
    @Operation(summary = "修改密碼", description = "修改當前用戶密碼")
    public ApiResponse<String> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                            HttpServletRequest httpRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        String clientIp = RequestUtils.getClientIp(httpRequest);
        
        try {
            // 驗證新密碼
            if (!validationUtils.isValidPassword(request.getNewPassword())) {
                return ApiResponse.error("密碼必須包含8位以上，且包含大小寫字母、數字和特殊字符");
            }

            // 修改密碼
            userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());

            // 記錄安全事件
            auditLogService.logSecurityEvent(userId, "PASSWORD_CHANGE", 
                    "用戶修改密碼", clientIp, "", true, null);

            return ApiResponse.success("密碼修改成功");

        } catch (Exception e) {
            // 記錄失敗嘗試
            auditLogService.logSecurityEvent(userId, "PASSWORD_CHANGE_FAILED", 
                    "密碼修改失敗: " + e.getMessage(), clientIp, "", false, e.getMessage());
            
            log.error("修改密碼失敗: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 檢查用戶名可用性
     */
    @GetMapping("/check-username")
    @Operation(summary = "檢查用戶名可用性", description = "檢查用戶名是否可用於註冊")
    public ApiResponse<Map<String, Object>> checkUsername(
            @Parameter(description = "用戶名", required = true) @RequestParam String username,
            HttpServletRequest httpRequest) {
        
        String clientIp = RequestUtils.getClientIp(httpRequest);
        
        // 檢查頻率限制
        String rateLimitKey = "check_username_limit:" + clientIp;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(rateLimitKey);
        if (attempts != null && attempts >= 10) {
            return ApiResponse.error("檢查過於頻繁，請1分鐘後重試");
        }
        
        try {
            // 驗證用戶名格式
            if (!validationUtils.isValidUsername(username)) {
                return ApiResponse.error("用戶名格式無效，請使用3-20位字母、數字或下劃線");
            }
            
            // 檢查用戶名可用性
            boolean isAvailable = !userService.existsByUsername(username);
            
            // 記錄頻率限制
            redisTemplate.opsForValue().set(rateLimitKey, (attempts == null ? 0 : attempts) + 1, 1, TimeUnit.MINUTES);
            
            Map<String, Object> result = new HashMap<>();
            result.put("available", isAvailable);
            result.put("message", isAvailable ? "用戶名可用" : "用戶名已被使用");
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("檢查用戶名可用性失敗: {}", e.getMessage());
            return ApiResponse.error("檢查失敗，請稍後重試");
        }
    }
    
    /**
     * 檢查郵箱可用性
     */
    @GetMapping("/check-email")
    @Operation(summary = "檢查郵箱可用性", description = "檢查郵箱是否可用於註冊")
    public ApiResponse<Map<String, Object>> checkEmail(
            @Parameter(description = "郵箱地址", required = true) @RequestParam String email,
            HttpServletRequest httpRequest) {
        
        String clientIp = RequestUtils.getClientIp(httpRequest);
        
        // 檢查頻率限制
        String rateLimitKey = "check_email_limit:" + clientIp;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(rateLimitKey);
        if (attempts != null && attempts >= 10) {
            return ApiResponse.error("檢查過於頻繁，請1分鐘後重試");
        }
        
        try {
            // 驗證郵箱格式
            if (!validationUtils.isValidEmail(email)) {
                return ApiResponse.error("郵箱格式無效");
            }
            
            // 檢查郵箱可用性
            boolean isAvailable = !userService.existsByEmail(email);
            
            // 記錄頻率限制
            redisTemplate.opsForValue().set(rateLimitKey, (attempts == null ? 0 : attempts) + 1, 1, TimeUnit.MINUTES);
            
            Map<String, Object> result = new HashMap<>();
            result.put("available", isAvailable);
            result.put("message", isAvailable ? "郵箱可用" : "郵箱已被使用");
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("檢查郵箱可用性失敗: {}", e.getMessage());
            return ApiResponse.error("檢查失敗，請稍後重試");
        }
    }

    /**
     * 獲取RSA公钥
     */
    @GetMapping("/public-key")
    @Operation(summary = "獲取RSA公钥", description = "獲取用於前端加密的RSA公钥，支持多種格式")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "獲取成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "獲取失敗")
    })
    public ApiResponse<Map<String, Object>> getPublicKey(
            @Parameter(description = "公钥格式: base64(原始), pem(標準PEM), both(兩種格式)")
            @RequestParam(defaultValue = "both") String format) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 獲取公钥詳細資訊
            Map<String, Object> keyDetails = rsaUtil.getPublicKeyDetails();
            result.putAll(keyDetails);
            
            // 根據要求的格式返回公钥
            switch (format.toLowerCase()) {
                case "base64":
                    result.put("publicKey", rsaUtil.getPublicKeyString());
                    result.put("format", "Base64 (X.509 DER)");
                    break;
                case "pem":
                    result.put("publicKey", rsaUtil.getPublicKeyPEMString());
                    result.put("format", "PEM (X.509)");
                    break;
                case "both":
                default:
                    result.put("publicKey", rsaUtil.getPublicKeyString());
                    result.put("publicKeyPEM", rsaUtil.getPublicKeyPEMString());
                    result.put("format", "Both (Base64 and PEM)");
                    break;
            }
            
            // 添加使用指南
            result.put("usage", createUsageGuide());
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("獲取RSA公钥失敗: {}", e.getMessage());
            return ApiResponse.error("獲取公钥失敗，請聯繫系統管理員");
        }
    }
    
    /**
     * 創建使用指南
     */
    private Map<String, Object> createUsageGuide() {
        Map<String, Object> usage = new HashMap<>();
        
        Map<String, String> libraries = new HashMap<>();
        libraries.put("JSEncrypt", "PEM格式 - 使用publicKeyPEM字段");
        libraries.put("node-rsa", "PEM格式 - 使用publicKeyPEM字段");
        libraries.put("crypto-js", "Base64或PEM格式 - 使用publicKey或publicKeyPEM字段");
        libraries.put("Web Crypto API", "Base64轉為ArrayBuffer - 使用publicKey字段");
        
        usage.put("recommendedLibraries", libraries);
        usage.put("algorithm", "RSA/ECB/PKCS1Padding");
        usage.put("maxEncryptSize", "245 bytes (對於2048位RSA钥" + ")");
        
        return usage;
    }

    /**
     * 獲取當前用戶信息
     */
    @GetMapping("/me")
    @SaCheckLogin
    @Operation(summary = "獲取當前用戶", description = "獲取當前登錄用戶信息")
    public ApiResponse<User> getCurrentUser() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            User user = userService.getById(userId);
            return ApiResponse.success(user);
        } catch (Exception e) {
            log.error("獲取用戶信息失敗: {}", e.getMessage());
            return ApiResponse.error("獲取用戶信息失敗");
        }
    }

    // DTO classes
    public static class RegisterRequest {
        private String username;

        @NotBlank(message = "郵箱不能為空")
        @Email(message = "郵箱格式無效")
        private String email;

        @NotBlank(message = "密碼不能為空")
        @Size(min = 8, message = "密碼長度至少8位")
        private String password;

        private String phone;

        @NotBlank(message = "驗證碼不能為空")
        private String verificationCode;

        // getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getVerificationCode() { return verificationCode; }
        public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
    }

    public static class LoginRequest {
        @NotBlank(message = "郵箱不能為空")
        @Email(message = "郵箱格式無效")
        private String email;

        @NotBlank(message = "密碼不能為空")
        private String password;

        // getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RefreshTokenRequest {
        @NotBlank(message = "刷新令牌不能為空")
        private String refreshToken;

        // getters and setters
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class VerifyEmailRequest {
        private Long userId;
        @NotBlank(message = "驗證碼不能為空")
        private String code;

        // getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class ForgotPasswordRequest {
        @NotBlank(message = "郵箱不能為空")
        @Email(message = "郵箱格式無效")
        private String email;

        // getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResetPasswordRequest {
        @NotBlank(message = "重設令牌不能為空")
        private String token;

        @NotBlank(message = "新密碼不能為空")
        @Size(min = 8, message = "密碼長度至少8位")
        private String newPassword;

        // getters and setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class ChangePasswordRequest {
        @NotBlank(message = "舊密碼不能為空")
        private String oldPassword;

        @NotBlank(message = "新密碼不能為空")
        @Size(min = 8, message = "密碼長度至少8位")
        private String newPassword;

        // getters and setters
        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class SendVerificationRequest {
        @NotBlank(message = "郵箱不能為空")
        @Email(message = "郵箱格式無效")
        private String email;

        // getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}