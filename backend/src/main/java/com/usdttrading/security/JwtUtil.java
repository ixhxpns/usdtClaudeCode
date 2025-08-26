package com.usdttrading.security;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 结合Sa-Token使用，提供额外的JWT功能
 * 
 * @author ArchitectAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${sa-token.jwt-secret-key:UsdtTradingPlatformSecretKey2025}")
    private String secretKey;

    @Value("${sa-token.timeout:2592000}")
    private long expiration;

    @Value("${jwt.access-token-expiration:3600}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800}")
    private long refreshTokenExpiration;

    @Value("${jwt.reset-token-expiration:1800}")
    private long resetTokenExpiration;

    /**
     * 获取密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT Token
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + expiration * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("iat", now.getTime() / 1000);
        claims.put("exp", expireTime.getTime() / 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expireTime)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析JWT Token
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token已过期: {}", e.getMessage());
            throw new RuntimeException("Token已过期");
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的JWT Token: {}", e.getMessage());
            throw new RuntimeException("不支持的Token");
        } catch (MalformedJwtException e) {
            log.warn("JWT Token格式错误: {}", e.getMessage());
            throw new RuntimeException("Token格式错误");
        } catch (SecurityException e) {
            log.warn("JWT Token签名无效: {}", e.getMessage());
            throw new RuntimeException("Token签名无效");
        } catch (IllegalArgumentException e) {
            log.warn("JWT Token参数为空: {}", e.getMessage());
            throw new RuntimeException("Token参数为空");
        }
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 从Token中获取角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return (String) claims.get("role");
    }

    /**
     * 检查Token是否即将过期（30分钟内）
     */
    public boolean isTokenNearExpiry(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();
            long timeToExpire = expiration.getTime() - now.getTime();
            return timeToExpire < 30 * 60 * 1000; // 30分钟
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 刷新Token
     */
    public String refreshToken(String oldToken) {
        try {
            Claims claims = parseToken(oldToken);
            Long userId = getUserIdFromToken(oldToken);
            String username = claims.getSubject();
            String role = (String) claims.get("role");
            
            return generateToken(userId, username, role);
        } catch (Exception e) {
            throw new RuntimeException("刷新Token失败", e);
        }
    }

    /**
     * 获取当前用户信息
     */
    public JSONObject getCurrentUserInfo() {
        try {
            // 获取当前登录用户ID
            long userId = StpUtil.getLoginIdAsLong();
            
            // 获取Sa-Token的Token信息
            SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
            
            JSONObject userInfo = new JSONObject();
            userInfo.set("userId", userId);
            userInfo.set("tokenName", tokenInfo.getTokenName());
            userInfo.set("tokenValue", tokenInfo.getTokenValue());
            userInfo.set("isLogin", StpUtil.isLogin());
            userInfo.set("loginDevice", StpUtil.getLoginDevice());
            userInfo.set("tokenTimeout", StpUtil.getTokenTimeout());
            
            return userInfo;
        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            return new JSONObject();
        }
    }

    /**
     * 生成API签名
     */
    public String generateApiSignature(Map<String, Object> params, String timestamp) {
        try {
            // 将参数按字典序排序
            StringBuilder sb = new StringBuilder();
            params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                    });
            
            // 添加时间戳
            sb.append("timestamp=").append(timestamp);
            
            // 使用HMAC-SHA256签名
            return Jwts.builder()
                    .setSubject(sb.toString())
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("生成API签名失败", e);
        }
    }

    /**
     * 验证API签名
     */
    public boolean verifyApiSignature(Map<String, Object> params, String timestamp, String signature) {
        try {
            String expectedSignature = generateApiSignature(params, timestamp);
            return signature.equals(expectedSignature);
        } catch (Exception e) {
            log.warn("验证API签名失败", e);
            return false;
        }
    }

    /**
     * 生成訪問令牌
     */
    public String generateAccessToken(Long userId, Long roleId) {
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + accessTokenExpiration * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("roleId", roleId);
        claims.put("tokenType", "access");
        claims.put("iat", now.getTime() / 1000);
        claims.put("exp", expireTime.getTime() / 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expireTime)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + refreshTokenExpiration * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tokenType", "refresh");
        claims.put("iat", now.getTime() / 1000);
        claims.put("exp", expireTime.getTime() / 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expireTime)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成密碼重設令牌
     */
    public String generatePasswordResetToken(Long userId) {
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + resetTokenExpiration * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tokenType", "reset");
        claims.put("iat", now.getTime() / 1000);
        claims.put("exp", expireTime.getTime() / 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expireTime)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 驗證令牌類型
     */
    public boolean validateTokenType(String token, String expectedType) {
        try {
            Claims claims = parseToken(token);
            String tokenType = (String) claims.get("tokenType");
            return expectedType.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 獲取訪問令牌過期時間
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration * 1000; // 返回毫秒
    }

    /**
     * 獲取刷新令牌過期時間
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration * 1000; // 返回毫秒
    }

    /**
     * 獲取重設令牌過期時間
     */
    public long getResetTokenExpiration() {
        return resetTokenExpiration * 1000; // 返回毫秒
    }

    /**
     * 檢查令牌是否在黑名單中
     * 這個方法需要與Redis配合使用
     */
    public boolean isTokenBlacklisted(String token) {
        // 這裡需要檢查Redis中的黑名單
        // 暫時返回false，在實際使用時會從Controller中檢查
        return false;
    }

    /**
     * 從令牌中獲取角色ID
     */
    public Long getRoleIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            Object roleId = claims.get("roleId");
            if (roleId instanceof Integer) {
                return ((Integer) roleId).longValue();
            }
            return (Long) roleId;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 獲取令牌剩餘有效時間（秒）
     */
    public long getTokenRemainingTime(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();
            long remaining = expiration.getTime() - now.getTime();
            return Math.max(0, remaining / 1000);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 檢查令牌是否為指定用戶的
     */
    public boolean isTokenForUser(String token, Long userId) {
        try {
            Long tokenUserId = getUserIdFromToken(token);
            return userId.equals(tokenUserId);
        } catch (Exception e) {
            return false;
        }
    }
}