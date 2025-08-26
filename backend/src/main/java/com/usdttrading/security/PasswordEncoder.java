package com.usdttrading.security;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.stereotype.Component;

/**
 * 密码编码器
 * 
 * @author ArchitectAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Component
public class PasswordEncoder {

    private static final int SALT_LENGTH = 16;

    /**
     * 生成随机盐值
     */
    public String generateSalt() {
        return RandomUtil.randomString(SALT_LENGTH);
    }

    /**
     * 加密密码
     */
    public String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /**
     * 使用指定盐值加密密码
     */
    public String encode(String rawPassword, String salt) {
        return BCrypt.hashpw(rawPassword + salt, BCrypt.gensalt());
    }

    /**
     * 验证密码
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }

    /**
     * 使用指定盐值验证密码
     */
    public boolean matches(String rawPassword, String salt, String encodedPassword) {
        return BCrypt.checkpw(rawPassword + salt, encodedPassword);
    }

    /**
     * 检查密码强度
     * @param password 原始密码
     * @return 强度等级：1-弱，2-中等，3-强，4-非常强
     */
    public int getPasswordStrength(String password) {
        if (password == null || password.length() == 0) {
            return 0;
        }

        int score = 0;
        
        // 长度检查
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        
        // 字符类型检查
        if (password.matches(".*[a-z].*")) score++; // 小写字母
        if (password.matches(".*[A-Z].*")) score++; // 大写字母
        if (password.matches(".*[0-9].*")) score++; // 数字
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) score++; // 特殊字符
        
        // 复杂度检查
        if (password.matches(".*[a-zA-Z].*") && password.matches(".*[0-9].*")) score++;
        if (!password.matches(".*(.)\\1{2,}.*")) score++; // 没有连续3个相同字符
        
        return Math.min(score, 4);
    }

    /**
     * 验证密码格式
     */
    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            return false;
        }
        
        // 必须包含字母和数字
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        
        return hasLetter && hasDigit;
    }

    /**
     * 生成随机密码
     */
    public String generateRandomPassword(int length) {
        if (length < 8) {
            length = 8;
        }
        
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String digits = "0123456789";
        String special = "!@#$%^&*";
        
        StringBuilder password = new StringBuilder();
        
        // 确保至少包含一个小写字母、大写字母、数字和特殊字符
        password.append(RandomUtil.randomChar(lowercase));
        password.append(RandomUtil.randomChar(uppercase));
        password.append(RandomUtil.randomChar(digits));
        password.append(RandomUtil.randomChar(special));
        
        // 填充剩余长度
        String allChars = lowercase + uppercase + digits + special;
        for (int i = 4; i < length; i++) {
            password.append(RandomUtil.randomChar(allChars));
        }
        
        // 打乱顺序
        return RandomUtil.randomString(password.toString(), length);
    }
}