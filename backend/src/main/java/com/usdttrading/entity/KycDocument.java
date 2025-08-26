package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * KYC文件管理實體類
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kyc_documents")
public class KycDocument extends BaseEntity {

    /**
     * KYC ID
     */
    @NotNull(message = "KYC ID不能為空")
    private Long kycId;

    /**
     * 用戶ID
     */
    @NotNull(message = "用戶ID不能為空")
    private Long userId;

    /**
     * 文件類型 (ID_FRONT, ID_BACK, SELFIE, PASSPORT, DRIVER_LICENSE, BANK_STATEMENT)
     */
    @NotBlank(message = "文件類型不能為空")
    private String documentType;

    /**
     * 原始文件名
     */
    @NotBlank(message = "原始文件名不能為空")
    private String originalFileName;

    /**
     * 存儲文件名 (UUID)
     */
    @NotBlank(message = "存儲文件名不能為空")
    private String storedFileName;

    /**
     * 文件存儲路徑
     */
    @NotBlank(message = "文件路徑不能為空")
    private String filePath;

    /**
     * 文件訪問URL (簽名URL)
     */
    private String fileUrl;

    /**
     * 文件MIME類型
     */
    private String mimeType;

    /**
     * 文件大小 (bytes)
     */
    private Long fileSize;

    /**
     * 文件MD5哈希值
     */
    private String fileMd5;

    /**
     * 是否已加密
     */
    private Boolean encrypted;

    /**
     * 加密算法
     */
    private String encryptionAlgorithm;

    /**
     * 加密密鑰ID
     */
    private String encryptionKeyId;

    /**
     * 文件狀態 (UPLOADED, PROCESSING, VERIFIED, REJECTED)
     */
    private String status;

    /**
     * OCR識別結果 (JSON格式)
     */
    private String ocrResult;

    /**
     * OCR識別準確度
     */
    private Double ocrAccuracy;

    /**
     * 人臉識別結果 (JSON格式)
     */
    private String faceRecognitionResult;

    /**
     * 人臉匹配分數
     */
    private Double faceMatchScore;

    /**
     * 文件驗證結果
     */
    private String verificationResult;

    /**
     * 文件質量評分 (1-10)
     */
    private Integer qualityScore;

    /**
     * 水印信息
     */
    private String watermarkInfo;

    /**
     * 病毒掃描結果
     */
    private Boolean virusScanPassed;

    /**
     * 病毒掃描報告
     */
    private String virusScanReport;

    /**
     * 文件上傳IP
     */
    private String uploadIp;

    /**
     * 文件上傳用戶代理
     */
    private String uploadUserAgent;

    /**
     * 文件到期時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    /**
     * 最後訪問時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastAccessedAt;

    /**
     * 訪問次數
     */
    private Integer accessCount;


    /**
     * 刪除時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deletedAt;

    /**
     * 檢查文件是否已過期
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * 檢查文件是否已驗證
     */
    public boolean isVerified() {
        return "VERIFIED".equals(status);
    }

    /**
     * 檢查文件是否被拒絕
     */
    public boolean isRejected() {
        return "REJECTED".equals(status);
    }

    /**
     * 檢查是否通過病毒掃描
     */
    public boolean passesVirusScan() {
        return Boolean.TRUE.equals(virusScanPassed);
    }

    /**
     * 增加訪問次數
     */
    public void incrementAccessCount() {
        if (accessCount == null) {
            accessCount = 1;
        } else {
            accessCount++;
        }
        lastAccessedAt = LocalDateTime.now();
    }
}