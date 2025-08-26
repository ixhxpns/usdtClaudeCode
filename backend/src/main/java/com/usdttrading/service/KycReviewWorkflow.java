package com.usdttrading.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.entity.*;
import com.usdttrading.enums.*;
import com.usdttrading.exception.BusinessException;
import com.usdttrading.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * KYC審核工作流服務
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycReviewWorkflow {

    private final UserKycMapper userKycMapper;
    private final KycWorkflowStepMapper kycWorkflowStepMapper;
    private final KycRiskAssessmentMapper kycRiskAssessmentMapper;
    private final KycReviewMapper kycReviewMapper;
    private final KycDocumentMapper kycDocumentMapper;
    private final NotificationService notificationService;

    @Value("${app.kyc.auto-approval-threshold:30}")
    private BigDecimal autoApprovalThreshold;

    @Value("${app.kyc.auto-rejection-threshold:70}")
    private BigDecimal autoRejectionThreshold;

    @Value("${app.kyc.enable-auto-review:true}")
    private boolean enableAutoReview;

    /**
     * 啟動KYC審核工作流
     *
     * @param kycId KYC ID
     * @return ApiResponse
     */
    @Transactional
    public ApiResponse<Void> startReviewWorkflow(Long kycId) {
        try {
            UserKyc kyc = userKycMapper.selectById(kycId);
            if (kyc == null) {
                return ApiResponse.error("KYC記錄不存在");
            }

            if (!KycStatus.PENDING.equals(kyc.getStatus())) {
                return ApiResponse.error("KYC狀態不正確，無法啟動審核流程");
            }

            // 創建工作流步驟
            createWorkflowSteps(kycId);

            // 更新KYC狀態為審核中
            kyc.setStatus(KycStatus.UNDER_REVIEW);
            kyc.setReviewStartedAt(LocalDateTime.now());
            userKycMapper.updateById(kyc);

            // 開始第一步：自動預審
            startAutomaticPreReview(kycId);

            log.info("KYC審核工作流啟動成功: kycId={}", kycId);

            return ApiResponse.success("審核工作流啟動成功", null);

        } catch (Exception e) {
            log.error("啟動KYC審核工作流失敗: kycId={}, error={}", kycId, e.getMessage(), e);
            return ApiResponse.error("啟動審核工作流失敗: " + e.getMessage());
        }
    }

    /**
     * 創建工作流步驟
     *
     * @param kycId KYC ID
     */
    private void createWorkflowSteps(Long kycId) {
        // Step 1: 自動預審
        createWorkflowStep(kycId, 1, "自動預審", "PENDING");

        // Step 2: 初級審核
        createWorkflowStep(kycId, 2, "初級審核", "PENDING");

        // Step 3: 高級審核 (高風險用戶或初級審核未通過)
        createWorkflowStep(kycId, 3, "高級審核", "PENDING");

        // Step 4: 風控終審 (特殊情況)
        createWorkflowStep(kycId, 4, "風控終審", "PENDING");
    }

    /**
     * 創建單個工作流步驟
     */
    private void createWorkflowStep(Long kycId, Integer stepNumber, String stepName, String status) {
        KycWorkflowStep step = new KycWorkflowStep();
        step.setKycId(kycId);
        step.setStepNumber(stepNumber);
        step.setStepName(stepName);
        step.setStatus(status);
        step.setRequiresManualIntervention(stepNumber > 1);
        kycWorkflowStepMapper.insert(step);
    }

    /**
     * 開始自動預審
     *
     * @param kycId KYC ID
     */
    private void startAutomaticPreReview(Long kycId) {
        try {
            // 獲取第一個步驟
            KycWorkflowStep step = getWorkflowStep(kycId, 1);
            if (step == null) return;

            // 更新步驟狀態為進行中
            updateStepStatus(step, "IN_PROGRESS", null, LocalDateTime.now());

            // 執行風險評估
            KycRiskAssessment riskAssessment = performRiskAssessment(kycId);

            // 根據風險評估結果決定下一步
            if (riskAssessment != null) {
                KycReviewResult result = determineAutoReviewResult(riskAssessment);
                
                switch (result) {
                    case AUTO_APPROVED:
                        completeAutoApproval(kycId, step, riskAssessment);
                        break;
                    case AUTO_REJECTED:
                        completeAutoRejection(kycId, step, riskAssessment);
                        break;
                    default:
                        proceedToManualReview(kycId, step, riskAssessment);
                        break;
                }
            } else {
                // 風險評估失敗，轉入人工審核
                proceedToManualReview(kycId, step, null);
            }

        } catch (Exception e) {
            log.error("自動預審失敗: kycId={}, error={}", kycId, e.getMessage(), e);
            // 自動預審失敗，轉入人工審核
            KycWorkflowStep step = getWorkflowStep(kycId, 1);
            if (step != null) {
                proceedToManualReview(kycId, step, null);
            }
        }
    }

    /**
     * 執行風險評估
     *
     * @param kycId KYC ID
     * @return 風險評估結果
     */
    private KycRiskAssessment performRiskAssessment(Long kycId) {
        try {
            UserKyc kyc = userKycMapper.selectById(kycId);
            if (kyc == null) return null;

            KycRiskAssessment assessment = new KycRiskAssessment();
            assessment.setKycId(kycId);
            assessment.setUserId(kyc.getUserId());

            // 計算各項風險分數
            BigDecimal ageRiskScore = calculateAgeRiskScore(kyc);
            BigDecimal locationRiskScore = calculateLocationRiskScore(kyc);
            BigDecimal occupationRiskScore = calculateOccupationRiskScore(kyc);
            BigDecimal incomeRiskScore = calculateIncomeRiskScore(kyc);

            assessment.setAgeRiskScore(ageRiskScore);
            assessment.setLocationRiskScore(locationRiskScore);
            assessment.setOccupationRiskScore(occupationRiskScore);
            assessment.setIncomeRiskScore(incomeRiskScore);

            // 執行各項檢查
            assessment.setBlacklistCheck(performBlacklistCheck(kyc));
            assessment.setDuplicateCheck(performDuplicateCheck(kyc));
            assessment.setAmlCheck(performAmlCheck(kyc));
            assessment.setIdentityVerification(performIdentityVerification(kycId));

            // 計算綜合風險分數
            BigDecimal totalRiskScore = calculateTotalRiskScore(assessment);
            assessment.setRiskScore(totalRiskScore);

            // 確定風險等級
            Integer riskLevel = determineRiskLevel(totalRiskScore);
            assessment.setRiskLevel(riskLevel);

            // 設置評估建議
            assessment.setRecommendation(generateRecommendation(assessment));
            assessment.setRequiresManualReview(riskLevel >= 5);
            assessment.setAssessedAt(LocalDateTime.now());
            assessment.setAssessmentVersion("1.0");

            // 保存評估結果
            kycRiskAssessmentMapper.insert(assessment);

            // 更新KYC記錄中的風險信息
            kyc.setRiskScore(totalRiskScore);
            kyc.setRiskLevel(riskLevel);
            userKycMapper.updateById(kyc);

            log.info("風險評估完成: kycId={}, riskScore={}, riskLevel={}", kycId, totalRiskScore, riskLevel);

            return assessment;

        } catch (Exception e) {
            log.error("風險評估失敗: kycId={}, error={}", kycId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 計算年齡風險分數
     */
    private BigDecimal calculateAgeRiskScore(UserKyc kyc) {
        if (kyc.getBirthDate() == null) return BigDecimal.valueOf(20);

        int age = java.time.Period.between(kyc.getBirthDate(), java.time.LocalDate.now()).getYears();
        
        if (age < 18) return BigDecimal.valueOf(100); // 未成年人高風險
        if (age < 21) return BigDecimal.valueOf(30);  // 年輕用戶中等風險
        if (age > 80) return BigDecimal.valueOf(25);  // 高齡用戶中等風險
        
        return BigDecimal.valueOf(5); // 正常年齡低風險
    }

    /**
     * 計算地域風險分數
     */
    private BigDecimal calculateLocationRiskScore(UserKyc kyc) {
        String country = kyc.getCountry();
        if (country == null) return BigDecimal.valueOf(30);

        // 這裡應該根據實際的國家/地區風險評級來計算
        switch (country.toUpperCase()) {
            case "CN": case "US": case "JP": case "KR": case "SG":
                return BigDecimal.valueOf(5); // 低風險國家
            case "AF": case "SY": case "IQ": case "SO":
                return BigDecimal.valueOf(80); // 高風險國家
            default:
                return BigDecimal.valueOf(15); // 中等風險
        }
    }

    /**
     * 計算職業風險分數
     */
    private BigDecimal calculateOccupationRiskScore(UserKyc kyc) {
        String occupation = kyc.getOccupation();
        if (occupation == null || occupation.trim().isEmpty()) {
            return BigDecimal.valueOf(20);
        }

        occupation = occupation.toLowerCase();
        if (occupation.contains("政府") || occupation.contains("公務員")) {
            return BigDecimal.valueOf(3); // 政府工作低風險
        }
        if (occupation.contains("學生")) {
            return BigDecimal.valueOf(10); // 學生中低風險
        }
        if (occupation.contains("無業") || occupation.contains("自由職業")) {
            return BigDecimal.valueOf(25); // 無固定職業中高風險
        }
        
        return BigDecimal.valueOf(8); // 一般職業低風險
    }

    /**
     * 計算收入風險分數
     */
    private BigDecimal calculateIncomeRiskScore(UserKyc kyc) {
        String incomeRange = kyc.getIncomeRange();
        if (incomeRange == null || incomeRange.trim().isEmpty()) {
            return BigDecimal.valueOf(25);
        }

        // 根據收入範圍評估風險
        if (incomeRange.contains("10萬以下")) {
            return BigDecimal.valueOf(15);
        }
        if (incomeRange.contains("100萬以上")) {
            return BigDecimal.valueOf(30); // 高收入需要額外驗證
        }
        
        return BigDecimal.valueOf(5);
    }

    /**
     * 執行黑名單檢查
     */
    private Boolean performBlacklistCheck(UserKyc kyc) {
        // 這裡應該查詢實際的黑名單數據庫
        // 現在返回模擬結果
        return true; // 假設通過檢查
    }

    /**
     * 執行重複申請檢查
     */
    private Boolean performDuplicateCheck(UserKyc kyc) {
        try {
            // 檢查是否有相同身份證號的其他已通過KYC記錄
            QueryWrapper<UserKyc> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id_number", kyc.getIdNumber());
            queryWrapper.eq("status", KycStatus.APPROVED);
            queryWrapper.ne("id", kyc.getId());
            
            List<UserKyc> duplicates = userKycMapper.selectList(queryWrapper);
            return duplicates.isEmpty();
            
        } catch (Exception e) {
            log.error("重複申請檢查失敗: kycId={}", kyc.getId(), e);
            return false; // 檢查失敗時保守處理
        }
    }

    /**
     * 執行反洗錢檢查
     */
    private Boolean performAmlCheck(UserKyc kyc) {
        // 這裡應該集成實際的AML檢查服務
        // 現在返回模擬結果
        return true; // 假設通過檢查
    }

    /**
     * 執行身份驗證檢查
     */
    private Boolean performIdentityVerification(Long kycId) {
        try {
            // 檢查必要文檔是否已上傳
            List<KycDocument> documents = kycDocumentMapper.selectByKycId(kycId);
            
            boolean hasIdFront = documents.stream()
                .anyMatch(doc -> "id_front".equals(doc.getDocumentType()));
            boolean hasIdBack = documents.stream()
                .anyMatch(doc -> "id_back".equals(doc.getDocumentType()));
            boolean hasSelfie = documents.stream()
                .anyMatch(doc -> "selfie".equals(doc.getDocumentType()));
            
            return hasIdFront && hasIdBack && hasSelfie;
            
        } catch (Exception e) {
            log.error("身份驗證檢查失敗: kycId={}", kycId, e);
            return false;
        }
    }

    /**
     * 計算綜合風險分數
     */
    private BigDecimal calculateTotalRiskScore(KycRiskAssessment assessment) {
        BigDecimal totalScore = BigDecimal.ZERO;
        
        // 各項風險分數權重
        if (assessment.getAgeRiskScore() != null) {
            totalScore = totalScore.add(assessment.getAgeRiskScore().multiply(BigDecimal.valueOf(0.2)));
        }
        if (assessment.getLocationRiskScore() != null) {
            totalScore = totalScore.add(assessment.getLocationRiskScore().multiply(BigDecimal.valueOf(0.3)));
        }
        if (assessment.getOccupationRiskScore() != null) {
            totalScore = totalScore.add(assessment.getOccupationRiskScore().multiply(BigDecimal.valueOf(0.2)));
        }
        if (assessment.getIncomeRiskScore() != null) {
            totalScore = totalScore.add(assessment.getIncomeRiskScore().multiply(BigDecimal.valueOf(0.1)));
        }
        
        // 檢查項目的風險加分
        if (Boolean.FALSE.equals(assessment.getBlacklistCheck())) {
            totalScore = totalScore.add(BigDecimal.valueOf(50));
        }
        if (Boolean.FALSE.equals(assessment.getDuplicateCheck())) {
            totalScore = totalScore.add(BigDecimal.valueOf(40));
        }
        if (Boolean.FALSE.equals(assessment.getAmlCheck())) {
            totalScore = totalScore.add(BigDecimal.valueOf(60));
        }
        if (Boolean.FALSE.equals(assessment.getIdentityVerification())) {
            totalScore = totalScore.add(BigDecimal.valueOf(30));
        }
        
        return totalScore;
    }

    /**
     * 確定風險等級
     */
    private Integer determineRiskLevel(BigDecimal riskScore) {
        if (riskScore.compareTo(BigDecimal.valueOf(10)) <= 0) return 1;
        if (riskScore.compareTo(BigDecimal.valueOf(20)) <= 0) return 2;
        if (riskScore.compareTo(BigDecimal.valueOf(30)) <= 0) return 3;
        if (riskScore.compareTo(BigDecimal.valueOf(40)) <= 0) return 4;
        if (riskScore.compareTo(BigDecimal.valueOf(50)) <= 0) return 5;
        if (riskScore.compareTo(BigDecimal.valueOf(60)) <= 0) return 6;
        if (riskScore.compareTo(BigDecimal.valueOf(70)) <= 0) return 7;
        return 8;
    }

    /**
     * 生成評估建議
     */
    private String generateRecommendation(KycRiskAssessment assessment) {
        StringBuilder recommendation = new StringBuilder();
        
        if (assessment.getRiskLevel() <= 3) {
            recommendation.append("低風險用戶，建議自動通過。");
        } else if (assessment.getRiskLevel() <= 5) {
            recommendation.append("中等風險用戶，建議人工審核。");
        } else {
            recommendation.append("高風險用戶，建議詳細審核。");
        }
        
        if (Boolean.FALSE.equals(assessment.getBlacklistCheck())) {
            recommendation.append(" 注意：用戶在黑名單中。");
        }
        if (Boolean.FALSE.equals(assessment.getDuplicateCheck())) {
            recommendation.append(" 注意：存在重複申請。");
        }
        if (Boolean.FALSE.equals(assessment.getAmlCheck())) {
            recommendation.append(" 注意：AML檢查未通過。");
        }
        
        return recommendation.toString();
    }

    /**
     * 確定自動審核結果
     */
    private KycReviewResult determineAutoReviewResult(KycRiskAssessment assessment) {
        if (!enableAutoReview) {
            return KycReviewResult.PENDING_HIGHER_REVIEW;
        }
        
        BigDecimal riskScore = assessment.getRiskScore();
        
        if (riskScore.compareTo(autoApprovalThreshold) <= 0 && assessment.passesAutoCheck()) {
            return KycReviewResult.AUTO_APPROVED;
        }
        
        if (riskScore.compareTo(autoRejectionThreshold) >= 0 || !assessment.passesAutoCheck()) {
            return KycReviewResult.AUTO_REJECTED;
        }
        
        return KycReviewResult.PENDING_HIGHER_REVIEW;
    }

    /**
     * 完成自動通過
     */
    private void completeAutoApproval(Long kycId, KycWorkflowStep step, KycRiskAssessment assessment) {
        // 更新步驟狀態
        updateStepStatus(step, "COMPLETED", KycReviewResult.AUTO_APPROVED.getCode(), null);

        // 更新KYC狀態
        UserKyc kyc = userKycMapper.selectById(kycId);
        kyc.setStatus(KycStatus.APPROVED);
        kyc.setAutoApproved(true);
        kyc.setVerifiedAt(LocalDateTime.now());
        kyc.setExpiresAt(LocalDateTime.now().plusYears(1)); // 1年有效期
        kyc.setCurrentStep(kyc.getTotalSteps());
        userKycMapper.updateById(kyc);

        // 創建審核記錄
        createReviewRecord(kycId, null, KycReviewResult.AUTO_APPROVED, "系統自動審核通過");

        // 發送通知
        notificationService.sendKycApprovalNotification(kyc.getUserId(), kycId);

        log.info("KYC自動審核通過: kycId={}, riskScore={}", kycId, assessment.getRiskScore());
    }

    /**
     * 完成自動拒絕
     */
    private void completeAutoRejection(Long kycId, KycWorkflowStep step, KycRiskAssessment assessment) {
        // 更新步驟狀態
        updateStepStatus(step, "REJECTED", KycReviewResult.AUTO_REJECTED.getCode(), null);

        // 更新KYC狀態
        UserKyc kyc = userKycMapper.selectById(kycId);
        kyc.setStatus(KycStatus.REJECTED);
        kyc.setRejectionReason("系統自動審核未通過，風險分數過高");
        kyc.incrementRejectionCount();
        userKycMapper.updateById(kyc);

        // 創建審核記錄
        createReviewRecord(kycId, null, KycReviewResult.AUTO_REJECTED, 
                          "系統自動審核未通過，風險分數: " + assessment.getRiskScore());

        // 發送通知
        notificationService.sendKycRejectionNotification(kyc.getUserId(), kycId, kyc.getRejectionReason());

        log.info("KYC自動審核拒絕: kycId={}, riskScore={}", kycId, assessment.getRiskScore());
    }

    /**
     * 轉入人工審核
     */
    private void proceedToManualReview(Long kycId, KycWorkflowStep currentStep, KycRiskAssessment assessment) {
        // 完成當前步驟
        updateStepStatus(currentStep, "COMPLETED", "REQUIRES_MANUAL_REVIEW", null);

        // 開始下一步驟（初級審核）
        KycWorkflowStep nextStep = getWorkflowStep(kycId, 2);
        if (nextStep != null) {
            updateStepStatus(nextStep, "PENDING", null, LocalDateTime.now());
            
            // 更新KYC當前步驟
            UserKyc kyc = userKycMapper.selectById(kycId);
            kyc.setCurrentStep(2);
            userKycMapper.updateById(kyc);
        }

        // 發送人工審核通知
        notificationService.sendKycManualReviewNotification(kycId);

        log.info("KYC轉入人工審核: kycId={}, riskScore={}", 
                kycId, assessment != null ? assessment.getRiskScore() : "N/A");
    }

    /**
     * 獲取工作流步驟
     */
    private KycWorkflowStep getWorkflowStep(Long kycId, Integer stepNumber) {
        QueryWrapper<KycWorkflowStep> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("kyc_id", kycId);
        queryWrapper.eq("step_number", stepNumber);
        return kycWorkflowStepMapper.selectOne(queryWrapper);
    }

    /**
     * 更新步驟狀態
     */
    private void updateStepStatus(KycWorkflowStep step, String status, String result, LocalDateTime startTime) {
        step.setStatus(status);
        if (result != null) {
            step.setResult(result);
        }
        if (startTime != null) {
            step.setStartedAt(startTime);
        }
        if ("COMPLETED".equals(status) || "REJECTED".equals(status)) {
            step.setCompletedAt(LocalDateTime.now());
            step.calculateProcessingTime();
        }
        kycWorkflowStepMapper.updateById(step);
    }

    /**
     * 創建審核記錄
     */
    private void createReviewRecord(Long kycId, Long reviewerId, KycReviewResult result, String note) {
        KycReview review = new KycReview();
        review.setKycId(kycId);
        review.setReviewerId(reviewerId);
        review.setStatus(result.getCode());
        review.setReviewNote(note);
        review.setReviewedAt(LocalDateTime.now());
        kycReviewMapper.insert(review);
    }
}