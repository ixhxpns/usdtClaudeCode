package com.usdttrading.security;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * RSA加密工具类
 * 
 * @author ArchitectAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Component
public class RSAUtil {

    @Value("${business.security.rsa.public-key:}")
    private String publicKeyStr;

    @Value("${business.security.rsa.private-key:}")
    private String privateKeyStr;

    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    /**
     * 生成RSA密钥对
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
            generator.initialize(KEY_SIZE);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("生成RSA密钥对失败", e);
        }
    }

    /**
     * 获取公钥
     */
    public PublicKey getPublicKey() {
        try {
            if (publicKeyStr == null || publicKeyStr.isEmpty()) {
                throw new IllegalStateException("RSA公钥未配置");
            }
            byte[] keyBytes = Base64.decode(publicKeyStr);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance(ALGORITHM);
            return factory.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("获取RSA公钥失败", e);
        }
    }

    /**
     * 获取私钥
     */
    public PrivateKey getPrivateKey() {
        try {
            if (privateKeyStr == null || privateKeyStr.isEmpty()) {
                throw new IllegalStateException("RSA私钥未配置");
            }
            byte[] keyBytes = Base64.decode(privateKeyStr);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance(ALGORITHM);
            return factory.generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("获取RSA私钥失败", e);
        }
    }

    /**
     * 公钥加密
     */
    public String encryptWithPublicKey(String data) {
        try {
            RSA rsa = new RSA(null, getPublicKey());
            return rsa.encryptBase64(data, KeyType.PublicKey);
        } catch (Exception e) {
            throw new RuntimeException("RSA公钥加密失败", e);
        }
    }

    /**
     * 私钥解密
     */
    public String decryptWithPrivateKey(String encryptedData) {
        try {
            RSA rsa = new RSA(getPrivateKey(), null);
            return rsa.decryptStr(encryptedData, KeyType.PrivateKey);
        } catch (Exception e) {
            throw new RuntimeException("RSA私钥解密失败", e);
        }
    }

    /**
     * 私钥加密
     */
    public String encryptWithPrivateKey(String data) {
        try {
            RSA rsa = new RSA(getPrivateKey(), null);
            return rsa.encryptBase64(data, KeyType.PrivateKey);
        } catch (Exception e) {
            throw new RuntimeException("RSA私钥加密失败", e);
        }
    }

    /**
     * 公钥解密
     */
    public String decryptWithPublicKey(String encryptedData) {
        try {
            RSA rsa = new RSA(null, getPublicKey());
            return rsa.decryptStr(encryptedData, KeyType.PublicKey);
        } catch (Exception e) {
            throw new RuntimeException("RSA公钥解密失败", e);
        }
    }

    /**
     * 获取公钥字符串(Base64编码)
     */
    public String getPublicKeyString() {
        if (publicKeyStr == null || publicKeyStr.isEmpty()) {
            throw new IllegalStateException("RSA公钥未配置");
        }
        return publicKeyStr;
    }

    /**
     * 密钥对转换为Base64字符串
     */
    public static String[] keyPairToString(KeyPair keyPair) {
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        
        String publicKeyStr = Base64.encode(publicKey.getEncoded());
        String privateKeyStr = Base64.encode(privateKey.getEncoded());
        
        return new String[]{publicKeyStr, privateKeyStr};
    }
}