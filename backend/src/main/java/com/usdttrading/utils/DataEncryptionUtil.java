package com.usdttrading.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 數據加密工具類
 * 支援AES-256-GCM加密，用於敏感數據的加密存儲
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Slf4j
@Component
public class DataEncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int KEY_LENGTH = 256;

    @Value("${app.encryption.master-key:}")
    private String masterKeyBase64;

    @Value("${app.encryption.enabled:true}")
    private boolean encryptionEnabled;

    private SecretKey masterKey;

    /**
     * 初始化主密鑰
     */
    private synchronized void initializeMasterKey() {
        if (masterKey != null) {
            return;
        }

        if (masterKeyBase64 != null && !masterKeyBase64.isEmpty()) {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
                masterKey = new SecretKeySpec(keyBytes, ALGORITHM);
                log.info("主密鑰已從配置載入");
            } catch (Exception e) {
                log.error("載入主密鑰失敗，將生成新密鑰", e);
                masterKey = generateSecretKey();
            }
        } else {
            masterKey = generateSecretKey();
            log.warn("未配置主密鑰，已生成臨時密鑰。生產環境請配置固定密鑰！");
        }
    }

    /**
     * 生成AES-256密鑰
     *
     * @return SecretKey
     */
    public static SecretKey generateSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_LENGTH);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("生成AES密鑰失敗", e);
        }
    }

    /**
     * 加密字符串
     *
     * @param plaintext 明文
     * @return 加密後的Base64字符串
     */
    public String encrypt(String plaintext) {
        if (!encryptionEnabled) {
            log.debug("加密已禁用，返回原文");
            return plaintext;
        }

        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            if (masterKey == null) {
                initializeMasterKey();
            }

            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            return Base64.getEncoder().encodeToString(encrypt(plaintextBytes, masterKey));
        } catch (Exception e) {
            log.error("字符串加密失敗: plaintext={}", plaintext, e);
            throw new RuntimeException("加密失敗", e);
        }
    }

    /**
     * 解密字符串
     *
     * @param encryptedBase64 加密的Base64字符串
     * @return 解密後的明文
     */
    public String decrypt(String encryptedBase64) {
        if (!encryptionEnabled) {
            log.debug("加密已禁用，返回原文");
            return encryptedBase64;
        }

        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return encryptedBase64;
        }

        try {
            if (masterKey == null) {
                initializeMasterKey();
            }

            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);
            byte[] decryptedBytes = decrypt(encryptedBytes, masterKey);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("字符串解密失敗: encryptedBase64={}", encryptedBase64, e);
            throw new RuntimeException("解密失敗", e);
        }
    }

    /**
     * 加密字節數組
     *
     * @param plaintext 明文字節數組
     * @param key 加密密鑰
     * @return 加密後的字節數組（包含IV）
     */
    public byte[] encrypt(byte[] plaintext, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            
            // 生成隨機IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            
            byte[] encryptedText = cipher.doFinal(plaintext);
            
            // 將IV和加密數據合併
            byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedText.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedText, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedText.length);
            
            return encryptedWithIv;
        } catch (Exception e) {
            log.error("字節數組加密失敗", e);
            throw new RuntimeException("加密失敗", e);
        }
    }

    /**
     * 解密字節數組
     *
     * @param encryptedWithIv 加密的字節數組（包含IV）
     * @param key 解密密鑰
     * @return 解密後的字節數組
     */
    public byte[] decrypt(byte[] encryptedWithIv, SecretKey key) {
        try {
            // 提取IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, GCM_IV_LENGTH);
            
            // 提取加密數據
            byte[] encryptedText = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, GCM_IV_LENGTH, encryptedText, 0, encryptedText.length);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            
            return cipher.doFinal(encryptedText);
        } catch (Exception e) {
            log.error("字節數組解密失敗", e);
            throw new RuntimeException("解密失敗", e);
        }
    }

    /**
     * 對敏感字段進行脫敏處理
     *
     * @param sensitiveData 敏感數據
     * @param visibleLength 可見字符數
     * @return 脫敏後的數據
     */
    public String maskSensitiveData(String sensitiveData, int visibleLength) {
        if (sensitiveData == null || sensitiveData.length() <= visibleLength) {
            return sensitiveData;
        }

        StringBuilder masked = new StringBuilder();
        
        // 顯示前面幾位
        if (visibleLength > 0) {
            masked.append(sensitiveData.substring(0, Math.min(visibleLength, sensitiveData.length())));
        }
        
        // 中間用星號替代
        int maskLength = sensitiveData.length() - visibleLength;
        if (maskLength > 0) {
            masked.append("*".repeat(Math.min(maskLength, 6))); // 最多顯示6個星號
        }
        
        return masked.toString();
    }

    /**
     * 身份證號碼脫敏
     *
     * @param idNumber 身份證號碼
     * @return 脫敏後的身份證號碼
     */
    public String maskIdNumber(String idNumber) {
        if (idNumber == null || idNumber.length() < 6) {
            return idNumber;
        }
        
        return idNumber.substring(0, 3) + "***" + idNumber.substring(idNumber.length() - 3);
    }

    /**
     * 銀行賬號脫敏
     *
     * @param bankAccount 銀行賬號
     * @return 脫敏後的銀行賬號
     */
    public String maskBankAccount(String bankAccount) {
        if (bankAccount == null || bankAccount.length() < 8) {
            return bankAccount;
        }
        
        return bankAccount.substring(0, 4) + "****" + bankAccount.substring(bankAccount.length() - 4);
    }

    /**
     * 手機號碼脫敏
     *
     * @param phoneNumber 手機號碼
     * @return 脫敏後的手機號碼
     */
    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return phoneNumber;
        }
        
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    /**
     * 電子郵箱脫敏
     *
     * @param email 電子郵箱
     * @return 脫敏後的電子郵箱
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        if (parts[0].length() <= 3) {
            return email;
        }
        
        return parts[0].substring(0, 2) + "***@" + parts[1];
    }

    /**
     * 姓名脫敏
     *
     * @param name 姓名
     * @return 脫敏後的姓名
     */
    public String maskName(String name) {
        if (name == null || name.length() <= 1) {
            return name;
        }
        
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }

    /**
     * 地址脫敏
     *
     * @param address 地址
     * @return 脫敏後的地址
     */
    public String maskAddress(String address) {
        if (address == null || address.length() <= 10) {
            return address;
        }
        
        return address.substring(0, 6) + "***" + address.substring(address.length() - 3);
    }

    /**
     * 生成密鑰的Base64表示（用於配置）
     *
     * @return 密鑰的Base64字符串
     */
    public static String generateKeyBase64() {
        SecretKey key = generateSecretKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * 驗證加密是否正常工作
     *
     * @return true 如果加密解密正常
     */
    public boolean testEncryption() {
        try {
            String testData = "test-encryption-data-" + System.currentTimeMillis();
            String encrypted = encrypt(testData);
            String decrypted = decrypt(encrypted);
            boolean success = testData.equals(decrypted);
            
            if (success) {
                log.info("加密測試通過");
            } else {
                log.error("加密測試失敗：原文={}, 解密結果={}", testData, decrypted);
            }
            
            return success;
        } catch (Exception e) {
            log.error("加密測試異常", e);
            return false;
        }
    }
}