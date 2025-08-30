package com.usdttrading.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.usdttrading.entity.Admin;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理員登入響應VO
 * 
 * @author Master Agent
 * @version 1.0.0
 * @since 2025-08-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理員登入響應")
public class AdminLoginResponse {

    @Schema(description = "管理員信息")
    private Admin admin;

    @JsonProperty("access_token")
    @Schema(description = "訪問令牌", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @JsonProperty("token_type")
    @Schema(description = "令牌類型", example = "Bearer")
    private String tokenType = "Bearer";

    @JsonProperty("expires_in")
    @Schema(description = "令牌過期時間（秒）", example = "7200")
    private Long expiresIn;

    @JsonProperty("refresh_token")
    @Schema(description = "刷新令牌", example = "refresh_token_value")
    private String refreshToken;

    @Schema(description = "管理員權限列表")
    private String[] permissions;

    @Schema(description = "管理員角色")
    private String role;

    @JsonProperty("login_time")
    @Schema(description = "登入時間戳", example = "1640995200")
    private Long loginTime = System.currentTimeMillis() / 1000;
}