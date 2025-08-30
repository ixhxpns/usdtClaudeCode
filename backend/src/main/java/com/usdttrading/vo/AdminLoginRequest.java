package com.usdttrading.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理員登入請求VO
 * 
 * @author Master Agent
 * @version 1.0.0
 * @since 2025-08-30
 */
@Data
@Schema(description = "管理員登入請求")
public class AdminLoginRequest {

    @NotBlank(message = "用戶名不能為空")
    @Size(min = 3, max = 50, message = "用戶名長度必須在3-50字符之間")
    @Schema(description = "管理員用戶名", example = "admin")
    private String username;

    @NotBlank(message = "密碼不能為空")
    @Schema(description = "登入密碼（RSA加密）", example = "encrypted_password")
    private String password;

    @JsonProperty("mfa_code")
    @Schema(description = "雙因子認證碼", example = "123456")
    private String mfaCode;

    @JsonProperty("remember_me")
    @Schema(description = "是否記住登入狀態", example = "false")
    private Boolean rememberMe = false;

    @Schema(description = "客戶端IP地址", hidden = true)
    private String clientIp;

    @Schema(description = "用戶代理", hidden = true)
    private String userAgent;
}