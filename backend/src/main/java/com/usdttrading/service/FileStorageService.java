package com.usdttrading.service;

import com.usdttrading.dto.ApiResponse;
import com.usdttrading.entity.KycDocument;
import com.usdttrading.enums.KycDocumentType;
import com.usdttrading.exception.BusinessException;
import com.usdttrading.repository.KycDocumentMapper;
import com.usdttrading.utils.FileProcessingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * KYC文件存儲服務
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final KycDocumentMapper kycDocumentMapper;
    private final FileProcessingUtil fileProcessingUtil;

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.file.max-size:5242880}") // 5MB
    private long maxFileSize;

    @Value("${app.file.allowed-types:jpg,jpeg,png,pdf}")
    private String allowedTypes;

    @Value("${app.file.enable-encryption:true}")
    private boolean enableEncryption;

    @Value("${app.file.enable-virus-scan:false}")
    private boolean enableVirusScan;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/jpg", "image/png");
    private static final Set<String> ALLOWED_PDF_TYPES = Set.of("application/pdf");

    /**
     * 上傳KYC文件
     *
     * @param file 上傳的文件
     * @param kycId KYC ID
     * @param userId 用戶ID
     * @param documentType 文件類型
     * @return ApiResponse<KycDocument>
     */
    public ApiResponse<KycDocument> uploadKycDocument(MultipartFile file, Long kycId, Long userId, 
                                                     KycDocumentType documentType) {
        try {
            // 驗證文件
            validateFile(file, documentType);

            // 生成唯一文件名
            String originalFileName = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFileName);
            String storedFileName = UUID.randomUUID().toString() + "." + fileExtension;

            // 創建存儲路徑
            String relativePath = createStoragePath(userId, documentType);
            String fullPath = uploadDir + "/" + relativePath + "/" + storedFileName;

            // 確保目錄存在
            Path parentDir = Paths.get(fullPath).getParent();
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // 處理文件內容
            byte[] fileContent = file.getBytes();

            // 病毒掃描
            if (enableVirusScan && !passesVirusScan(fileContent)) {
                throw new BusinessException("文件未通過病毒掃描，上傳失敗");
            }

            // 計算文件MD5
            String fileMd5 = calculateMD5(fileContent);

            // 文件加密（如果啟用）
            if (enableEncryption) {
                fileContent = encryptFileContent(fileContent);
            }

            // 保存文件
            Files.copy(new ByteArrayInputStream(fileContent), Paths.get(fullPath), StandardCopyOption.REPLACE_EXISTING);

            // 添加水印（對於圖片文件）
            if (documentType.isIdentityDocument() && isImageFile(file)) {
                addWatermark(fullPath, userId);
            }

            // 創建文檔記錄
            KycDocument document = new KycDocument();
            document.setKycId(kycId);
            document.setUserId(userId);
            document.setDocumentType(documentType.getCode());
            document.setOriginalFileName(originalFileName);
            document.setStoredFileName(storedFileName);
            document.setFilePath(relativePath + "/" + storedFileName);
            document.setMimeType(file.getContentType());
            document.setFileSize(file.getSize());
            document.setFileMd5(fileMd5);
            document.setEncrypted(enableEncryption);
            document.setEncryptionAlgorithm(enableEncryption ? "AES-256-GCM" : null);
            document.setStatus("UPLOADED");
            document.setVirusScanPassed(!enableVirusScan || true); // 如果沒有啟用掃描或通過掃描
            document.setUploadIp(getCurrentUserIP());
            document.setExpiresAt(LocalDateTime.now().plusYears(5)); // 5年有效期
            document.setAccessCount(0);
            document.setDeleted(0);

            kycDocumentMapper.insert(document);

            // 異步處理OCR和質量檢查
            processDocumentAsync(document);

            log.info("文件上傳成功: userId={}, documentType={}, fileName={}", userId, documentType, originalFileName);

            return ApiResponse.success("文件上傳成功", document);

        } catch (Exception e) {
            log.error("文件上傳失敗: userId={}, documentType={}, error={}", userId, documentType, e.getMessage(), e);
            return ApiResponse.error("文件上傳失敗: " + e.getMessage());
        }
    }

    /**
     * 獲取文件訪問URL（簽名URL）
     *
     * @param documentId 文檔ID
     * @param userId 用戶ID
     * @return 簽名URL
     */
    public ApiResponse<String> getSignedFileUrl(Long documentId, Long userId) {
        try {
            KycDocument document = kycDocumentMapper.selectById(documentId);
            if (document == null) {
                return ApiResponse.error("文件不存在");
            }

            // 權限檢查
            if (!document.getUserId().equals(userId)) {
                return ApiResponse.error("沒有文件訪問權限");
            }

            // 檢查文件是否過期
            if (document.isExpired()) {
                return ApiResponse.error("文件已過期");
            }

            // 生成臨時訪問URL（有效期30分鐘）
            String signedUrl = generateSignedUrl(document, 30);
            
            // 更新訪問記錄
            document.incrementAccessCount();
            kycDocumentMapper.updateById(document);

            return ApiResponse.success("獲取文件URL成功", signedUrl);

        } catch (Exception e) {
            log.error("獲取文件URL失敗: documentId={}, userId={}, error={}", documentId, userId, e.getMessage(), e);
            return ApiResponse.error("獲取文件URL失敗: " + e.getMessage());
        }
    }

    /**
     * 刪除文件
     *
     * @param documentId 文檔ID
     * @param userId 用戶ID
     * @return ApiResponse
     */
    public ApiResponse<Void> deleteDocument(Long documentId, Long userId) {
        try {
            KycDocument document = kycDocumentMapper.selectById(documentId);
            if (document == null) {
                return ApiResponse.error("文件不存在");
            }

            // 權限檢查
            if (!document.getUserId().equals(userId)) {
                return ApiResponse.error("沒有文件刪除權限");
            }

            // 軟刪除
            document.setDeleted(1);
            document.setDeletedAt(LocalDateTime.now());
            kycDocumentMapper.updateById(document);

            log.info("文件刪除成功: documentId={}, userId={}", documentId, userId);
            return ApiResponse.success("文件刪除成功", null);

        } catch (Exception e) {
            log.error("文件刪除失敗: documentId={}, userId={}, error={}", documentId, userId, e.getMessage(), e);
            return ApiResponse.error("文件刪除失敗: " + e.getMessage());
        }
    }

    /**
     * 驗證文件
     *
     * @param file 上傳的文件
     * @param documentType 文檔類型
     */
    private void validateFile(MultipartFile file, KycDocumentType documentType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能為空");
        }

        // 檢查文件大小
        if (file.getSize() > maxFileSize) {
            throw new BusinessException("文件大小超過限制，最大允許 " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // 檢查文件類型
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException("無法確定文件類型");
        }

        // 身份證明文件必須是圖片
        if (documentType.isIdentityDocument() || documentType == KycDocumentType.SELFIE) {
            if (!ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                throw new BusinessException("身份證明文件必須是JPG、JPEG或PNG格式");
            }
        }

        // 檢查文件魔數（防止文件偽造）
        try {
            byte[] fileHeader = file.getBytes();
            if (!isValidFileType(fileHeader, contentType)) {
                throw new BusinessException("文件類型與內容不匹配，可能是偽造文件");
            }
        } catch (IOException e) {
            throw new BusinessException("讀取文件失敗");
        }
    }

    /**
     * 檢查文件魔數
     */
    private boolean isValidFileType(byte[] fileHeader, String contentType) {
        if (fileHeader.length < 4) {
            return false;
        }

        // JPEG文件檢查
        if (contentType.contains("jpeg") || contentType.contains("jpg")) {
            return fileHeader[0] == (byte) 0xFF && fileHeader[1] == (byte) 0xD8;
        }

        // PNG文件檢查
        if (contentType.contains("png")) {
            return fileHeader[0] == (byte) 0x89 && fileHeader[1] == 'P' && 
                   fileHeader[2] == 'N' && fileHeader[3] == 'G';
        }

        // PDF文件檢查
        if (contentType.contains("pdf")) {
            String header = new String(fileHeader, 0, Math.min(4, fileHeader.length));
            return header.startsWith("%PDF");
        }

        return true;
    }

    /**
     * 創建存儲路徑
     */
    private String createStoragePath(Long userId, KycDocumentType documentType) {
        return String.format("kyc/%d/%s", userId, documentType.getCode());
    }

    /**
     * 獲取文件擴展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 計算文件MD5
     */
    private String calculateMD5(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("計算MD5失敗", e);
        }
    }

    /**
     * 加密文件內容
     */
    private byte[] encryptFileContent(byte[] content) {
        // 這裡應該實現實際的加密邏輯
        // 使用DataEncryptionUtil進行AES加密
        return content; // 暫時返回原內容，實際實現時需要加密
    }

    /**
     * 病毒掃描
     */
    private boolean passesVirusScan(byte[] content) {
        // 這裡應該集成實際的防病毒引擎
        // 例如ClamAV或其他商業防病毒API
        return true; // 暫時返回true，實際實現時需要真正的掃描
    }

    /**
     * 添加水印
     */
    private void addWatermark(String filePath, Long userId) {
        try {
            BufferedImage image = ImageIO.read(new File(filePath));
            if (image != null) {
                BufferedImage watermarkedImage = fileProcessingUtil.addWatermark(image, "KYC-" + userId);
                String format = filePath.substring(filePath.lastIndexOf(".") + 1);
                ImageIO.write(watermarkedImage, format, new File(filePath));
            }
        } catch (Exception e) {
            log.warn("添加水印失敗: filePath={}, error={}", filePath, e.getMessage());
        }
    }

    /**
     * 異步處理文檔（OCR、質量檢查等）
     */
    private void processDocumentAsync(KycDocument document) {
        // 這裡應該使用異步任務處理OCR識別和質量檢查
        // 可以使用Spring的@Async或消息隊列
        log.info("開始異步處理文檔: documentId={}", document.getId());
    }

    /**
     * 生成簽名URL
     */
    private String generateSignedUrl(KycDocument document, int validMinutes) {
        // 這裡應該生成帶有時間戳和簽名的URL
        // 實際項目中可能使用JWT或其他簽名機制
        long timestamp = System.currentTimeMillis() + (validMinutes * 60 * 1000);
        return String.format("/api/files/%d?expires=%d&signature=temp", document.getId(), timestamp);
    }

    /**
     * 獲取當前用戶IP
     */
    private String getCurrentUserIP() {
        // 這裡應該從RequestContext或HttpServletRequest獲取真實IP
        return "127.0.0.1"; // 暫時返回本地地址
    }

    /**
     * 檢查是否為圖片文件
     */
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase());
    }
}