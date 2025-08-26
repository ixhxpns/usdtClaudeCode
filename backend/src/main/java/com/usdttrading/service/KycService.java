package com.usdttrading.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.entity.*;
import com.usdttrading.enums.KycDocumentType;
import com.usdttrading.enums.KycStatus;
import com.usdttrading.exception.BusinessException;
import com.usdttrading.repository.*;
import com.usdttrading.utils.DataEncryptionUtil;
import com.usdttrading.utils.FileProcessingUtil;
import com.usdttrading.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

/**
 * KYC服務類
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycService {

    private final UserKycMapper userKycMapper;
    private final KycDocumentMapper kycDocumentMapper;
    private final KycRiskAssessmentMapper kycRiskAssessmentMapper;
    private final KycReviewMapper kycReviewMapper;
    private final UserMapper userMapper;
    private final DataEncryptionUtil encryptionUtil;
    private final FileProcessingUtil fileProcessingUtil;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    @Value("${app.kyc.min-age:18}")
    private int minAge;

    @Value("${app.kyc.max-submissions:3}")
    private int maxSubmissions;

    @Value("${app.kyc.auto-approval-threshold:30}")
    private BigDecimal autoApprovalThreshold;

    @Value("${app.kyc.auto-rejection-threshold:70}")
    private BigDecimal autoRejectionThreshold;

    /**
     * 提交基本KYC信息
     *
     * @param userId 用戶ID
     * @param kycData KYC數據
     * @return ApiResponse<UserKyc>
     */
    @Transactional
    public ApiResponse<UserKyc> submitBasicKycInfo(Long userId, Map<String, Object> kycData) {
        try {
            // 檢查用戶是否存在
            User user = userMapper.selectById(userId);
            if (user == null) {
                return ApiResponse.error("用戶不存在");
            }

            // 檢查是否已有KYC記錄
            UserKyc existingKyc = getUserKycByUserId(userId);
            if (existingKyc != null && existingKyc.isApproved()) {
                return ApiResponse.error("您已通過KYC驗證，無需重複提交");
            }

            // 檢查提交次數限制
            if (existingKyc != null && existingKyc.exceedsMaxSubmissions(maxSubmissions)) {
                return ApiResponse.error("您已達到最大提交次數限制");
            }

            // 驗證基本信息
            String validationError = validateBasicKycInfo(kycData);
            if (validationError != null) {
                return ApiResponse.error(validationError);
            }

            UserKyc kyc;
            if (existingKyc != null) {
                // 更新現有記錄
                kyc = existingKyc;
                kyc.incrementSubmissionCount();
            } else {
                // 創建新記錄
                kyc = new UserKyc();
                kyc.setUserId(userId);
                kyc.setSubmissionCount(1);
                kyc.setRejectionCount(0);
            }

            // 填充基本信息（加密敏感字段）
            fillBasicKycInfo(kyc, kycData);

            // 設置狀態和時間戳
            kyc.setStatus(KycStatus.PENDING);
            kyc.setLastSubmittedAt(LocalDateTime.now());
            kyc.setCurrentStep(1);
            kyc.setTotalSteps(4);
            kyc.setRequiresSupplement(false);
            kyc.setIpAddress(RequestUtils.getClientIP());
            kyc.setUserAgent(RequestUtils.getUserAgent());

            // 保存或更新
            if (kyc.getId() == null) {
                userKycMapper.insert(kyc);
            } else {
                userKycMapper.updateById(kyc);
            }

            log.info("KYC基本信息提交成功: userId={}, kycId={}", userId, kyc.getId());

            // 發送通知
            notificationService.sendKycSubmissionNotification(userId, kyc.getId());

            return ApiResponse.success("KYC基本信息提交成功", kyc);

        } catch (Exception e) {
            log.error("提交KYC基本信息失敗: userId={}, error={}", userId, e.getMessage(), e);
            return ApiResponse.error("提交失敗: " + e.getMessage());
        }
    }

    /**
     * 上傳身份證件
     *
     * @param userId 用戶ID
     * @param frontImage 身份證正面
     * @param backImage 身份證反面
     * @param selfieImage 手持身份證自拍照
     * @return ApiResponse
     */
    @Transactional
    public ApiResponse<Void> uploadIdDocuments(Long userId, MultipartFile frontImage, 
                                              MultipartFile backImage, MultipartFile selfieImage) {
        try {
            UserKyc kyc = getUserKycByUserId(userId);
            if (kyc == null) {
                return ApiResponse.error("請先提交基本信息");
            }

            if (kyc.isApproved()) {
                return ApiResponse.error("您已通過KYC驗證，無需重新上傳");
            }

            // 上傳身份證正面
            ApiResponse<KycDocument> frontResult = fileStorageService.uploadKycDocument(
                frontImage, kyc.getId(), userId, KycDocumentType.ID_FRONT
            );
            if (!frontResult.isSuccess()) {
                return ApiResponse.error("身份證正面上傳失敗: " + frontResult.getMessage());
            }

            // 上傳身份證反面
            ApiResponse<KycDocument> backResult = fileStorageService.uploadKycDocument(
                backImage, kyc.getId(), userId, KycDocumentType.ID_BACK
            );
            if (!backResult.isSuccess()) {
                return ApiResponse.error("身份證反面上傳失敗: " + backResult.getMessage());
            }

            // 上傳自拍照
            ApiResponse<KycDocument> selfieResult = fileStorageService.uploadKycDocument(
                selfieImage, kyc.getId(), userId, KycDocumentType.SELFIE
            );
            if (!selfieResult.isSuccess()) {
                return ApiResponse.error("自拍照上傳失敗: " + selfieResult.getMessage());
            }

            // 更新KYC記錄
            kyc.setIdCardFront(frontResult.getData().getFilePath());
            kyc.setIdCardBack(backResult.getData().getFilePath());
            kyc.setSelfiePhoto(selfieResult.getData().getFilePath());
            kyc.setCurrentStep(2);
            userKycMapper.updateById(kyc);

            log.info("身份證件上傳成功: userId={}, kycId={}", userId, kyc.getId());

            // 異步執行OCR和風險評估
            performAsyncDocumentProcessing(kyc.getId());

            return ApiResponse.success("身份證件上傳成功", null);

        } catch (Exception e) {
            log.error("上傳身份證件失敗: userId={}, error={}", userId, e.getMessage(), e);
            return ApiResponse.error("上傳失敗: " + e.getMessage());
        }
    }

    /**
     * 上傳第二證件
     *
     * @param userId 用戶ID
     * @param docType 證件類型
     * @param docImage 證件圖片
     * @return ApiResponse
     */
    @Transactional
    public ApiResponse<Void> uploadSecondaryDocument(Long userId, String docType, MultipartFile docImage) {
        try {
            UserKyc kyc = getUserKycByUserId(userId);
            if (kyc == null) {
                return ApiResponse.error("請先提交基本信息");
            }

            KycDocumentType documentType = KycDocumentType.fromCode(docType);
            if (documentType == null) {
                return ApiResponse.error("不支持的證件類型");
            }

            // 上傳第二證件
            ApiResponse<KycDocument> result = fileStorageService.uploadKycDocument(
                docImage, kyc.getId(), userId, documentType
            );
            if (!result.isSuccess()) {
                return ApiResponse.error("第二證件上傳失敗: " + result.getMessage());
            }

            // 更新KYC記錄
            kyc.setSecondDocType(docType);
            kyc.setSecondDocUrl(result.getData().getFilePath());
            userKycMapper.updateById(kyc);

            log.info("第二證件上傳成功: userId={}, docType={}", userId, docType);

            return ApiResponse.success("第二證件上傳成功", null);

        } catch (Exception e) {
            log.error("上傳第二證件失敗: userId={}, docType={}, error={}", userId, docType, e.getMessage(), e);
            return ApiResponse.error("上傳失敗: " + e.getMessage());
        }
    }

    /**
     * 綁定銀行賬戶
     *
     * @param userId 用戶ID
     * @param bankInfo 銀行信息
     * @return ApiResponse
     */
    @Transactional
    public ApiResponse<Void> bindBankAccount(Long userId, Map<String, String> bankInfo) {
        try {
            UserKyc kyc = getUserKycByUserId(userId);
            if (kyc == null) {
                return ApiResponse.error("請先提交基本信息");
            }

            // 驗證銀行信息
            String validationError = validateBankInfo(bankInfo);
            if (validationError != null) {
                return ApiResponse.error(validationError);
            }

            // 加密存儲銀行信息
            kyc.setBankAccount(encryptionUtil.encrypt(bankInfo.get("bankAccount")));
            kyc.setBankName(bankInfo.get("bankName"));
            kyc.setBankBranch(bankInfo.get("bankBranch"));
            kyc.setAccountHolderName(bankInfo.get("accountHolderName"));
            kyc.setCurrentStep(3);

            userKycMapper.updateById(kyc);

            log.info("銀行賬戶綁定成功: userId={}, bankName={}", userId, bankInfo.get("bankName"));

            return ApiResponse.success("銀行賬戶綁定成功", null);

        } catch (Exception e) {
            log.error("綁定銀行賬戶失敗: userId={}, error={}", userId, e.getMessage(), e);
            return ApiResponse.error("綁定失敗: " + e.getMessage());
        }
    }

    /**
     * 獲取KYC狀態
     *
     * @param userId 用戶ID
     * @return ApiResponse<KycStatusInfo>
     */
    public ApiResponse<Map<String, Object>> getKycStatus(Long userId) {
        try {
            UserKyc kyc = getUserKycByUserId(userId);
            if (kyc == null) {
                return ApiResponse.error("未找到KYC記錄");
            }

            Map<String, Object> statusInfo = new HashMap<>();
            statusInfo.put("kycId", kyc.getId());
            statusInfo.put("status", kyc.getStatus());
            statusInfo.put("currentStep", kyc.getCurrentStep());
            statusInfo.put("totalSteps", kyc.getTotalSteps());
            statusInfo.put("progressPercentage", kyc.getProgressPercentage());
            statusInfo.put("kycLevel", kyc.getKycLevel());
            statusInfo.put("riskLevel", kyc.getRiskLevel());
            statusInfo.put("submissionCount", kyc.getSubmissionCount());
            statusInfo.put("requiresSupplement", kyc.requiresSupplementary());
            statusInfo.put("supplementRequirement", kyc.getSupplementRequirement());
            statusInfo.put("lastSubmittedAt", kyc.getLastSubmittedAt());
            statusInfo.put("verifiedAt", kyc.getVerifiedAt());
            statusInfo.put("expiresAt", kyc.getExpiresAt());

            // 脫敏顯示敏感信息
            if (kyc.getRealName() != null) {
                statusInfo.put("realName", encryptionUtil.maskName(encryptionUtil.decrypt(kyc.getRealName())));
            }
            if (kyc.getIdNumber() != null) {
                statusInfo.put("idNumber", encryptionUtil.maskIdNumber(encryptionUtil.decrypt(kyc.getIdNumber())));
            }

            return ApiResponse.success("獲取KYC狀態成功", statusInfo);

        } catch (Exception e) {
            log.error("獲取KYC狀態失敗: userId={}, error={}", userId, e.getMessage(), e);
            return ApiResponse.error("獲取狀態失敗: " + e.getMessage());
        }
    }

    /**
     * 重新提交KYC
     *
     * @param userId 用戶ID
     * @return ApiResponse
     */
    @Transactional
    public ApiResponse<Void> resubmitKyc(Long userId) {
        try {
            UserKyc kyc = getUserKycByUserId(userId);
            if (kyc == null) {
                return ApiResponse.error("未找到KYC記錄");
            }

            if (!kyc.requiresSupplementary()) {
                return ApiResponse.error("當前狀態不需要重新提交");
            }

            if (kyc.exceedsMaxSubmissions(maxSubmissions)) {
                return ApiResponse.error("已達到最大提交次數限制");
            }

            // 重置狀態
            kyc.setStatus(KycStatus.PENDING);
            kyc.setRequiresSupplement(false);
            kyc.setSupplementRequirement(null);
            kyc.setLastSubmittedAt(LocalDateTime.now());
            kyc.incrementSubmissionCount();

            userKycMapper.updateById(kyc);

            log.info("KYC重新提交成功: userId={}, kycId={}", userId, kyc.getId());

            // 發送通知
            notificationService.sendKycResubmissionNotification(userId, kyc.getId());

            return ApiResponse.success("KYC重新提交成功", null);

        } catch (Exception e) {
            log.error("重新提交KYC失敗: userId={}, error={}", userId, e.getMessage(), e);
            return ApiResponse.error("重新提交失敗: " + e.getMessage());
        }
    }

    /**
     * 獲取用戶KYC記錄
     *
     * @param userId 用戶ID
     * @return UserKyc
     */
    private UserKyc getUserKycByUserId(Long userId) {
        QueryWrapper<UserKyc> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("deleted", false);
        queryWrapper.orderByDesc("created_at");
        queryWrapper.last("LIMIT 1");
        return userKycMapper.selectOne(queryWrapper);
    }

    /**
     * 驗證基本KYC信息
     */
    private String validateBasicKycInfo(Map<String, Object> kycData) {
        // 必填字段檢查
        String[] requiredFields = {"realName", "idNumber", "nationality", "address", "country"};
        for (String field : requiredFields) {
            if (!kycData.containsKey(field) || kycData.get(field) == null || 
                kycData.get(field).toString().trim().isEmpty()) {
                return field + " 不能為空";
            }
        }

        // 身份證號碼格式驗證
        String idNumber = kycData.get("idNumber").toString();
        if (!fileProcessingUtil.validateIdNumber(idNumber)) {
            return "身份證號碼格式不正確";
        }

        // 年齡驗證
        String birthDate = fileProcessingUtil.extractBirthDateFromIdNumber(idNumber);
        if (birthDate != null) {
            Integer age = fileProcessingUtil.calculateAge(birthDate);
            if (age != null && age < minAge) {
                return "年齡不滿" + minAge + "歲，無法進行KYC驗證";
            }
        }

        return null;
    }

    /**
     * 填充基本KYC信息
     */
    private void fillBasicKycInfo(UserKyc kyc, Map<String, Object> kycData) {
        // 加密敏感信息
        kyc.setRealName(encryptionUtil.encrypt(kycData.get("realName").toString()));
        kyc.setIdNumber(encryptionUtil.encrypt(kycData.get("idNumber").toString()));
        kyc.setAddress(encryptionUtil.encrypt(kycData.get("address").toString()));
        
        if (kycData.containsKey("phoneNumber")) {
            kyc.setPhoneNumber(encryptionUtil.encrypt(kycData.get("phoneNumber").toString()));
        }

        // 非敏感信息直接存儲
        kyc.setEnglishName((String) kycData.get("englishName"));
        kyc.setNationality(kycData.get("nationality").toString());
        kyc.setCity((String) kycData.get("city"));
        kyc.setState((String) kycData.get("state"));
        kyc.setZipCode((String) kycData.get("zipCode"));
        kyc.setCountry(kycData.get("country").toString());
        kyc.setOccupation((String) kycData.get("occupation"));
        kyc.setEmployerName((String) kycData.get("employerName"));
        kyc.setIncomeRange((String) kycData.get("incomeRange"));
        kyc.setIncomeSource((String) kycData.get("incomeSource"));
        kyc.setFundSource((String) kycData.get("fundSource"));
        kyc.setEmail((String) kycData.get("email"));

        // 從身份證號碼提取信息
        String idNumber = kycData.get("idNumber").toString();
        String birthDate = fileProcessingUtil.extractBirthDateFromIdNumber(idNumber);
        if (birthDate != null) {
            kyc.setBirthDate(LocalDate.parse(birthDate));
        }

        String gender = fileProcessingUtil.extractGenderFromIdNumber(idNumber);
        if (gender != null) {
            kyc.setGender(Enum.valueOf(com.usdttrading.enums.Gender.class, gender));
        }
    }

    /**
     * 驗證銀行信息
     */
    private String validateBankInfo(Map<String, String> bankInfo) {
        String[] requiredFields = {"bankAccount", "bankName", "accountHolderName"};
        for (String field : requiredFields) {
            if (!bankInfo.containsKey(field) || bankInfo.get(field) == null || 
                bankInfo.get(field).trim().isEmpty()) {
                return field + " 不能為空";
            }
        }

        String bankAccount = bankInfo.get("bankAccount");
        if (bankAccount.length() < 10 || bankAccount.length() > 25) {
            return "銀行賬號長度不正確";
        }

        if (!bankAccount.matches("\\d+")) {
            return "銀行賬號只能包含數字";
        }

        return null;
    }

    /**
     * 異步處理文檔（OCR、風險評估等）
     */
    private void performAsyncDocumentProcessing(Long kycId) {
        // 這裡應該使用異步任務處理
        // 可以使用@Async註解或消息隊列
        log.info("開始異步處理KYC文檔: kycId={}", kycId);
        
        // 在實際實現中，這些處理應該在後台異步進行
        // 這裡只是示例代碼
    }
}