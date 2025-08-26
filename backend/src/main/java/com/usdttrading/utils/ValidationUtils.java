package com.usdttrading.utils;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 驗證工具類
 * 提供各種輸入驗證功能，包括郵箱、密碼、電話等格式驗證
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Component
public class ValidationUtils {

    // 郵箱正則表達式
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // 密碼複雜度正則表達式
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    // 手機號正則表達式（支持多種格式）
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+?86)?1[3-9]\\d{9}$|^\\+?[1-9]\\d{1,14}$"
    );

    // 用戶名正則表達式
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{3,20}$"
    );

    // IPv4地址正則表達式
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    // 身份證號正則表達式（中國大陸）
    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
            "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$"
    );

    /**
     * 驗證郵箱格式
     */
    public boolean isValidEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * 驗證密碼強度
     * 要求：至少8位，包含大小寫字母、數字和特殊字符
     */
    public boolean isValidPassword(String password) {
        if (StrUtil.isBlank(password)) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * 檢查密碼是否包含用戶信息
     */
    public boolean passwordContainsUserInfo(String password, String email, String username) {
        if (StrUtil.isBlank(password)) {
            return false;
        }
        
        String lowerPassword = password.toLowerCase();
        
        // 檢查是否包含郵箱地址部分
        if (StrUtil.isNotBlank(email)) {
            String emailPrefix = email.toLowerCase().split("@")[0];
            if (lowerPassword.contains(emailPrefix)) {
                return true;
            }
        }
        
        // 檢查是否包含用戶名
        if (StrUtil.isNotBlank(username)) {
            if (lowerPassword.contains(username.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 計算密碼強度分數
     * 返回1-5的分數，5為最強
     */
    public int calculatePasswordStrength(String password) {
        if (StrUtil.isBlank(password)) {
            return 0;
        }

        int score = 0;

        // 長度檢查
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;

        // 包含小寫字母
        if (password.matches(".*[a-z].*")) score++;

        // 包含大寫字母
        if (password.matches(".*[A-Z].*")) score++;

        // 包含數字
        if (password.matches(".*\\d.*")) score++;

        // 包含特殊字符
        if (password.matches(".*[@$!%*?&].*")) score++;

        // 複雜性獎勵
        if (password.length() >= 16 && score >= 4) score++;

        return Math.min(score, 5);
    }

    /**
     * 驗證手機號格式
     */
    public boolean isValidPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * 驗證用戶名格式
     * 要求：3-20位字母、數字、下劃線
     */
    public boolean isValidUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    /**
     * 驗證IP地址格式
     */
    public boolean isValidIpAddress(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip.trim()).matches();
    }

    /**
     * 驗證身份證號格式（中國大陸）
     */
    public boolean isValidIdCard(String idCard) {
        if (StrUtil.isBlank(idCard)) {
            return false;
        }
        
        String cleanIdCard = idCard.trim().toUpperCase();
        if (!ID_CARD_PATTERN.matcher(cleanIdCard).matches()) {
            return false;
        }

        // 驗證校驗位
        return validateIdCardChecksum(cleanIdCard);
    }

    /**
     * 驗證身份證號校驗位
     */
    private boolean validateIdCardChecksum(String idCard) {
        if (idCard.length() != 18) {
            return false;
        }

        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        String[] checkCodes = {"1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"};

        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += Integer.parseInt(idCard.substring(i, i + 1)) * weights[i];
        }

        int remainder = sum % 11;
        String expectedCheckCode = checkCodes[remainder];
        String actualCheckCode = idCard.substring(17);

        return expectedCheckCode.equals(actualCheckCode);
    }

    /**
     * 驗證驗證碼格式
     * 6位數字驗證碼
     */
    public boolean isValidVerificationCode(String code) {
        if (StrUtil.isBlank(code)) {
            return false;
        }
        return code.trim().matches("^\\d{6}$");
    }

    /**
     * 驗證JWT Token格式
     */
    public boolean isValidJwtToken(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }
        
        // JWT格式：header.payload.signature
        String[] parts = token.split("\\.");
        return parts.length == 3 && 
               StrUtil.isNotBlank(parts[0]) && 
               StrUtil.isNotBlank(parts[1]) && 
               StrUtil.isNotBlank(parts[2]);
    }

    /**
     * 驗證URL格式
     */
    public boolean isValidUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return false;
        }
        
        try {
            new java.net.URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 清理輸入字符串，防止XSS攻擊
     */
    public String sanitizeInput(String input) {
        if (StrUtil.isBlank(input)) {
            return input;
        }
        
        return input.trim()
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    /**
     * 驗證文件擴展名
     */
    public boolean isValidFileExtension(String fileName, String... allowedExtensions) {
        if (StrUtil.isBlank(fileName) || allowedExtensions == null || allowedExtensions.length == 0) {
            return false;
        }
        
        String extension = getFileExtension(fileName);
        if (StrUtil.isBlank(extension)) {
            return false;
        }
        
        for (String allowed : allowedExtensions) {
            if (extension.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 獲取文件擴展名
     */
    private String getFileExtension(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "";
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        
        return fileName.substring(lastDotIndex + 1);
    }

    /**
     * 驗證金額格式
     * 支持最多8位小數
     */
    public boolean isValidAmount(String amount) {
        if (StrUtil.isBlank(amount)) {
            return false;
        }
        
        try {
            double value = Double.parseDouble(amount);
            if (value < 0) {
                return false;
            }
            
            // 檢查小數位數
            String[] parts = amount.split("\\.");
            if (parts.length > 1 && parts[1].length() > 8) {
                return false;
            }
            
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 驗證分頁參數
     */
    public boolean isValidPageParams(Integer page, Integer size) {
        return page != null && page >= 1 && 
               size != null && size >= 1 && size <= 100;
    }

    /**
     * 檢查字符串長度範圍
     */
    public boolean isValidLength(String str, int minLength, int maxLength) {
        if (str == null) {
            return minLength <= 0;
        }
        
        int length = str.trim().length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * 檢查是否包含SQL注入風險
     */
    public boolean containsSqlInjectionRisk(String input) {
        if (StrUtil.isBlank(input)) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        String[] sqlKeywords = {
            "select", "insert", "update", "delete", "drop", "create", "alter",
            "exec", "execute", "union", "script", "javascript", "vbscript",
            "onload", "onerror", "onclick"
        };
        
        for (String keyword : sqlKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
}