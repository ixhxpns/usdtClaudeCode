package com.usdttrading.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.entity.KycRiskAssessment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * KYC風險評估數據訪問層
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Mapper
public interface KycRiskAssessmentMapper extends BaseMapper<KycRiskAssessment> {

    /**
     * 根據KYC ID查詢最新風險評估
     *
     * @param kycId KYC ID
     * @return 風險評估記錄
     */
    @Select("SELECT * FROM kyc_risk_assessments WHERE kyc_id = #{kycId} ORDER BY created_at DESC LIMIT 1")
    KycRiskAssessment selectLatestByKycId(@Param("kycId") Long kycId);

    /**
     * 根據用戶ID查詢最新風險評估
     *
     * @param userId 用戶ID
     * @return 風險評估記錄
     */
    @Select("SELECT * FROM kyc_risk_assessments WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT 1")
    KycRiskAssessment selectLatestByUserId(@Param("userId") Long userId);

    /**
     * 查詢高風險用戶
     *
     * @param riskLevel 風險等級閾值
     * @param startTime 開始時間
     * @param endTime 結束時間
     * @return 高風險用戶列表
     */
    @Select("SELECT * FROM kyc_risk_assessments WHERE risk_level >= #{riskLevel} " +
            "AND assessed_at BETWEEN #{startTime} AND #{endTime} ORDER BY risk_level DESC")
    List<KycRiskAssessment> selectHighRiskUsers(@Param("riskLevel") Integer riskLevel,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 統計風險等級分布
     *
     * @param startTime 開始時間
     * @param endTime 結束時間
     * @return 風險等級統計
     */
    List<Map<String, Object>> selectRiskLevelStatistics(@Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 查詢需要人工審核的評估
     *
     * @return 需要人工審核的評估列表
     */
    @Select("SELECT * FROM kyc_risk_assessments WHERE requires_manual_review = true ORDER BY created_at ASC")
    List<KycRiskAssessment> selectRequiresManualReview();

    /**
     * 分頁查詢風險評估
     *
     * @param page 分頁對象
     * @param userId 用戶ID（可選）
     * @param riskLevelMin 最小風險等級（可選）
     * @param riskLevelMax 最大風險等級（可選）
     * @return 分頁結果
     */
    Page<KycRiskAssessment> selectRiskAssessmentsPage(Page<KycRiskAssessment> page,
                                                     @Param("userId") Long userId,
                                                     @Param("riskLevelMin") Integer riskLevelMin,
                                                     @Param("riskLevelMax") Integer riskLevelMax);
}