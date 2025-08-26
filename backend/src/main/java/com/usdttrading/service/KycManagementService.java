package com.usdttrading.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.entity.*;
import com.usdttrading.enums.KycReviewResult;
import com.usdttrading.enums.KycStatus;
import com.usdttrading.exception.BusinessException;
import com.usdttrading.repository.*;
import com.usdttrading.utils.DataEncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KYC管理服務
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycManagementService {

    private final UserKycMapper userKycMapper;
    private final KycReviewMapper kycReviewMapper;
    private final KycWorkflowStepMapper kycWorkflowStepMapper;
    private final KycRiskAssessmentMapper kycRiskAssessmentMapper;
    private final KycDocumentMapper kycDocumentMapper;
    private final UserMapper userMapper;
    private final DataEncryptionUtil encryptionUtil;
    private final NotificationService notificationService;

    /**
     * 分頁查詢KYC申請列表
     *
     * @param pageNum 頁碼
     * @param pageSize 頁大小
     * @param status 狀態過濾
     * @param riskLevel 風險等級過濾
     * @return 分頁結果
     */
    public ApiResponse<Page<Map<String, Object>>> getKycApplications(int pageNum, int pageSize, 
                                                                    String status, Integer riskLevel) {
        try {
            Page<UserKyc> page = new Page<>(pageNum, pageSize);
            QueryWrapper<UserKyc> queryWrapper = new QueryWrapper<>();
            
            if (status != null && !status.isEmpty()) {
                queryWrapper.eq("status", status);
            }
            if (riskLevel != null) {
                queryWrapper.eq("risk_level", riskLevel);
            }
            
            queryWrapper.orderByDesc("created_at");
            Page<UserKyc> kycPage = userKycMapper.selectPage(page, queryWrapper);

            // 構建返回數據
            Page<Map<String, Object>> resultPage = new Page<>();
            resultPage.setCurrent(kycPage.getCurrent());
            resultPage.setSize(kycPage.getSize());
            resultPage.setTotal(kycPage.getTotal());
            resultPage.setPages(kycPage.getPages());

            List<Map<String, Object>> records = kycPage.getRecords().stream().map(kyc -> {
                Map<String, Object> record = new HashMap<>();
                record.put("kycId", kyc.getId());
                record.put("userId", kyc.getUserId());
                record.put("status", kyc.getStatus());
                record.put("kycLevel", kyc.getKycLevel());
                record.put("riskLevel", kyc.getRiskLevel());
                record.put("riskScore", kyc.getRiskScore());
                record.put("currentStep", kyc.getCurrentStep());
                record.put("totalSteps", kyc.getTotalSteps());
                record.put("submissionCount", kyc.getSubmissionCount());
                record.put("lastSubmittedAt", kyc.getLastSubmittedAt());
                record.put("createdAt", kyc.getCreatedAt());
                
                // 脫敏顯示敏感信息
                if (kyc.getRealName() != null) {
                    record.put("realName", encryptionUtil.maskName(encryptionUtil.decrypt(kyc.getRealName())));
                }
                if (kyc.getIdNumber() != null) {
                    record.put("idNumber", encryptionUtil.maskIdNumber(encryptionUtil.decrypt(kyc.getIdNumber())));
                }
                
                // 獲取用戶基本信息
                User user = userMapper.selectById(kyc.getUserId());
                if (user != null) {
                    record.put("username", user.getUsername());
                    record.put("email", user.getEmail());
                }
                
                return record;
            }).toList();
            
            resultPage.setRecords(records);
            
            return ApiResponse.success("查詢成功", resultPage);

        } catch (Exception e) {
            log.error("查詢KYC申請列表失敗: pageNum={}, pageSize={}, error={}", pageNum, pageSize, e.getMessage(), e);
            return ApiResponse.error("查詢失敗: " + e.getMessage());
        }
    }

    /**
     * 獲取KYC詳細信息
     *
     * @param kycId KYC ID
     * @return KYC詳細信息
     */
    public ApiResponse<Map<String, Object>> getKycDetail(Long kycId) {
        try {
            UserKyc kyc = userKycMapper.selectById(kycId);
            if (kyc == null) {
                return ApiResponse.error("KYC記錄不存在");
            }

            Map<String, Object> detail = new HashMap<>();
            
            // KYC基本信息
            detail.put("kycId", kyc.getId());
            detail.put("userId", kyc.getUserId());
            detail.put("status", kyc.getStatus());
            detail.put("kycLevel", kyc.getKycLevel());
            detail.put("riskLevel", kyc.getRiskLevel());
            detail.put("riskScore", kyc.getRiskScore());
            detail.put("currentStep", kyc.getCurrentStep());
            detail.put("totalSteps", kyc.getTotalSteps());
            detail.put("submissionCount", kyc.getSubmissionCount());
            detail.put("rejectionCount", kyc.getRejectionCount());
            detail.put("lastSubmittedAt", kyc.getLastSubmittedAt());
            detail.put("verifiedAt", kyc.getVerifiedAt());
            detail.put("expiresAt", kyc.getExpiresAt());
            detail.put("rejectionReason", kyc.getRejectionReason());
            detail.put("requiresSupplement", kyc.getRequiresSupplement());
            detail.put("supplementRequirement", kyc.getSupplementRequirement());

            // 解密並脫敏顯示敏感信息
            if (kyc.getRealName() != null) {
                detail.put("realName", encryptionUtil.decrypt(kyc.getRealName()));
            }
            if (kyc.getIdNumber() != null) {
                detail.put("idNumber", encryptionUtil.decrypt(kyc.getIdNumber()));
            }
            if (kyc.getAddress() != null) {
                detail.put("address", encryptionUtil.decrypt(kyc.getAddress()));
            }
            if (kyc.getPhoneNumber() != null) {
                detail.put("phoneNumber", encryptionUtil.decrypt(kyc.getPhoneNumber()));
            }
            if (kyc.getBankAccount() != null) {
                detail.put("bankAccount", encryptionUtil.decrypt(kyc.getBankAccount()));
            }

            // 非敏感信息
            detail.put("englishName", kyc.getEnglishName());
            detail.put("gender", kyc.getGender());
            detail.put("birthDate", kyc.getBirthDate());
            detail.put("nationality", kyc.getNationality());
            detail.put("city", kyc.getCity());
            detail.put("state", kyc.getState());
            detail.put("zipCode", kyc.getZipCode());
            detail.put("country", kyc.getCountry());
            detail.put("occupation", kyc.getOccupation());
            detail.put("employerName", kyc.getEmployerName());
            detail.put("incomeRange", kyc.getIncomeRange());
            detail.put("incomeSource", kyc.getIncomeSource());
            detail.put("fundSource", kyc.getFundSource());
            detail.put("email", kyc.getEmail());
            detail.put("bankName", kyc.getBankName());
            detail.put("bankBranch", kyc.getBankBranch());
            detail.put("accountHolderName", kyc.getAccountHolderName());

            // 用戶信息
            User user = userMapper.selectById(kyc.getUserId());
            if (user != null) {
                detail.put("username", user.getUsername());
                detail.put("userEmail", user.getEmail());
                detail.put("userCreatedAt", user.getCreatedAt());
            }

            // 工作流步驟
            List<KycWorkflowStep> steps = kycWorkflowStepMapper.selectByKycId(kycId);
            detail.put("workflowSteps", steps);

            // 風險評估
            KycRiskAssessment riskAssessment = kycRiskAssessmentMapper.selectLatestByKycId(kycId);
            detail.put("riskAssessment", riskAssessment);

            // 審核記錄
            QueryWrapper<KycReview> reviewQuery = new QueryWrapper<>();
            reviewQuery.eq("kyc_id", kycId);
            reviewQuery.orderByDesc("created_at");
            List<KycReview> reviews = kycReviewMapper.selectList(reviewQuery);
            detail.put("reviewHistory", reviews);

            // 文檔列表
            List<KycDocument> documents = kycDocumentMapper.selectByKycId(kycId);
            detail.put("documents", documents);

            return ApiResponse.success("獲取KYC詳情成功", detail);

        } catch (Exception e) {
            log.error("獲取KYC詳情失敗: kycId={}, error={}", kycId, e.getMessage(), e);
            return ApiResponse.error("獲取詳情失敗: " + e.getMessage());
        }
    }

    /**
     * 審核KYC申請
     *
     * @param kycId KYC ID
     * @param reviewerId 審核員ID
     * @param result 審核結果
     * @param comment 審核意見
     * @param supplementRequirement 補充要求
     * @return ApiResponse
     */
    @Transactional
    public ApiResponse<Void> reviewKycApplication(Long kycId, Long reviewerId, String result, 
                                                 String comment, String supplementRequirement) {
        try {
            UserKyc kyc = userKycMapper.selectById(kycId);
            if (kyc == null) {
                return ApiResponse.error("KYC記錄不存在");
            }

            if (!KycStatus.UNDER_REVIEW.equals(kyc.getStatus()) && !KycStatus.PENDING.equals(kyc.getStatus())) {
                return ApiResponse.error("KYC狀態不正確，無法進行審核");
            }

            KycReviewResult reviewResult = KycReviewResult.fromCode(result);
            if (reviewResult == null) {
                return ApiResponse.error("無效的審核結果");
            }

            // 創建審核記錄
            KycReview review = new KycReview();
            review.setKycId(kycId);
            review.setReviewerId(reviewerId);
            review.setStatus(result);
            review.setReviewNote(comment);
            review.setReviewedAt(LocalDateTime.now());
            kycReviewMapper.insert(review);

            // 更新KYC狀態
            switch (reviewResult) {
                case APPROVED:
                    approveKyc(kyc, reviewerId);
                    break;
                case REJECTED:
                    rejectKyc(kyc, reviewerId, comment);
                    break;
                case REQUIRES_SUPPLEMENT:
                    requireSupplement(kyc, reviewerId, supplementRequirement);
                    break;
                default:
                    return ApiResponse.error("不支持的審核結果");
            }

            log.info("KYC審核完成: kycId={}, reviewerId={}, result={}", kycId, reviewerId, result);

            return ApiResponse.success("審核完成", null);

        } catch (Exception e) {
            log.error("審核KYC失敗: kycId={}, reviewerId={}, result={}, error={}", 
                     kycId, reviewerId, result, e.getMessage(), e);
            return ApiResponse.error("審核失敗: " + e.getMessage());
        }
    }

    /**
     * 通過KYC
     */
    private void approveKyc(UserKyc kyc, Long reviewerId) {
        kyc.setStatus(KycStatus.APPROVED);
        kyc.setLastReviewerId(reviewerId);
        kyc.setVerifiedAt(LocalDateTime.now());
        kyc.setExpiresAt(LocalDateTime.now().plusYears(1)); // 1年有效期
        kyc.setCurrentStep(kyc.getTotalSteps());
        kyc.setRequiresSupplement(false);
        kyc.setSupplementRequirement(null);
        userKycMapper.updateById(kyc);

        // 發送通知
        notificationService.sendKycApprovalNotification(kyc.getUserId(), kyc.getId());
    }

    /**
     * 拒絕KYC
     */
    private void rejectKyc(UserKyc kyc, Long reviewerId, String reason) {
        kyc.setStatus(KycStatus.REJECTED);
        kyc.setLastReviewerId(reviewerId);
        kyc.setRejectionReason(reason);
        kyc.incrementRejectionCount();
        kyc.setRequiresSupplement(false);
        kyc.setSupplementRequirement(null);
        userKycMapper.updateById(kyc);

        // 發送通知
        notificationService.sendKycRejectionNotification(kyc.getUserId(), kyc.getId(), reason);
    }

    /**
     * 要求補充材料
     */
    private void requireSupplement(UserKyc kyc, Long reviewerId, String requirement) {
        kyc.setStatus(KycStatus.REQUIRES_RESUBMIT);
        kyc.setLastReviewerId(reviewerId);
        kyc.setRequiresSupplement(true);
        kyc.setSupplementRequirement(requirement);
        userKycMapper.updateById(kyc);

        // 發送通知
        notificationService.sendKycSupplementRequiredNotification(kyc.getUserId(), kyc.getId(), requirement);
    }

    /**
     * 批量審核KYC申請
     *
     * @param kycIds KYC ID列表
     * @param reviewerId 審核員ID
     * @param result 審核結果
     * @param comment 審核意見
     * @return ApiResponse
     */
    @Transactional
    public ApiResponse<Map<String, Object>> batchReviewKyc(List<Long> kycIds, Long reviewerId, 
                                                          String result, String comment) {
        try {
            int successCount = 0;
            int failCount = 0;
            
            for (Long kycId : kycIds) {
                try {
                    ApiResponse<Void> response = reviewKycApplication(kycId, reviewerId, result, comment, null);
                    if (response.isSuccess()) {
                        successCount++;
                    } else {
                        failCount++;
                        log.warn("批量審核失敗: kycId={}, error={}", kycId, response.getMessage());
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("批量審核異常: kycId={}, error={}", kycId, e.getMessage(), e);
                }
            }

            Map<String, Object> result_map = new HashMap<>();
            result_map.put("total", kycIds.size());
            result_map.put("successCount", successCount);
            result_map.put("failCount", failCount);

            log.info("批量審核完成: reviewerId={}, total={}, success={}, fail={}", 
                    reviewerId, kycIds.size(), successCount, failCount);

            return ApiResponse.success("批量審核完成", result_map);

        } catch (Exception e) {
            log.error("批量審核失敗: reviewerId={}, error={}", reviewerId, e.getMessage(), e);
            return ApiResponse.error("批量審核失敗: " + e.getMessage());
        }
    }

    /**
     * 獲取審核統計
     *
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @return 統計數據
     */
    public ApiResponse<Map<String, Object>> getReviewStatistics(String startDate, String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate + " 00:00:00", 
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime end = LocalDateTime.parse(endDate + " 23:59:59", 
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            Map<String, Object> statistics = new HashMap<>();

            // KYC狀態統計
            QueryWrapper<UserKyc> queryWrapper = new QueryWrapper<>();
            queryWrapper.between("created_at", start, end);
            
            long totalApplications = userKycMapper.selectCount(queryWrapper);
            
            queryWrapper.clear();
            queryWrapper.between("created_at", start, end);
            queryWrapper.eq("status", KycStatus.APPROVED);
            long approvedCount = userKycMapper.selectCount(queryWrapper);
            
            queryWrapper.clear();
            queryWrapper.between("created_at", start, end);
            queryWrapper.eq("status", KycStatus.REJECTED);
            long rejectedCount = userKycMapper.selectCount(queryWrapper);
            
            queryWrapper.clear();
            queryWrapper.between("created_at", start, end);
            queryWrapper.eq("status", KycStatus.UNDER_REVIEW);
            long underReviewCount = userKycMapper.selectCount(queryWrapper);
            
            queryWrapper.clear();
            queryWrapper.between("created_at", start, end);
            queryWrapper.eq("status", KycStatus.PENDING);
            long pendingCount = userKycMapper.selectCount(queryWrapper);

            statistics.put("totalApplications", totalApplications);
            statistics.put("approvedCount", approvedCount);
            statistics.put("rejectedCount", rejectedCount);
            statistics.put("underReviewCount", underReviewCount);
            statistics.put("pendingCount", pendingCount);
            statistics.put("approvalRate", totalApplications > 0 ? 
                String.format("%.2f%%", (double) approvedCount / totalApplications * 100) : "0%");

            // 風險等級統計
            Map<Integer, Long> riskLevelStats = new HashMap<>();
            for (int level = 1; level <= 8; level++) {
                queryWrapper.clear();
                queryWrapper.between("created_at", start, end);
                queryWrapper.eq("risk_level", level);
                long count = userKycMapper.selectCount(queryWrapper);
                riskLevelStats.put(level, count);
            }
            statistics.put("riskLevelStats", riskLevelStats);

            return ApiResponse.success("獲取統計數據成功", statistics);

        } catch (Exception e) {
            log.error("獲取審核統計失敗: startDate={}, endDate={}, error={}", 
                     startDate, endDate, e.getMessage(), e);
            return ApiResponse.error("獲取統計失敗: " + e.getMessage());
        }
    }
}