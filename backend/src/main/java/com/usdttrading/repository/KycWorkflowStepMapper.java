package com.usdttrading.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.entity.KycWorkflowStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * KYC工作流步驟數據訪問層
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Mapper
public interface KycWorkflowStepMapper extends BaseMapper<KycWorkflowStep> {

    /**
     * 根據KYC ID查詢工作流步驟
     *
     * @param kycId KYC ID
     * @return 工作流步驟列表
     */
    @Select("SELECT * FROM kyc_workflow_steps WHERE kyc_id = #{kycId} ORDER BY step_number ASC")
    List<KycWorkflowStep> selectByKycId(@Param("kycId") Long kycId);

    /**
     * 根據KYC ID和步驟編號查詢步驟
     *
     * @param kycId KYC ID
     * @param stepNumber 步驟編號
     * @return 工作流步驟
     */
    @Select("SELECT * FROM kyc_workflow_steps WHERE kyc_id = #{kycId} AND step_number = #{stepNumber}")
    KycWorkflowStep selectByKycIdAndStepNumber(@Param("kycId") Long kycId, @Param("stepNumber") Integer stepNumber);

    /**
     * 查詢當前正在進行的步驟
     *
     * @param kycId KYC ID
     * @return 當前步驟
     */
    @Select("SELECT * FROM kyc_workflow_steps WHERE kyc_id = #{kycId} AND status = 'IN_PROGRESS' ORDER BY step_number ASC LIMIT 1")
    KycWorkflowStep selectCurrentStep(@Param("kycId") Long kycId);

    /**
     * 查詢待處理的步驟
     *
     * @param assignedReviewerId 審核員ID（可選）
     * @return 待處理步驟列表
     */
    @Select("SELECT * FROM kyc_workflow_steps WHERE status = 'PENDING' AND requires_manual_intervention = true " +
            "#{assignedReviewerId != null ? 'AND assigned_reviewer_id = #{assignedReviewerId}' : ''} " +
            "ORDER BY created_at ASC")
    List<KycWorkflowStep> selectPendingSteps(@Param("assignedReviewerId") Long assignedReviewerId);

    /**
     * 查詢超時的步驟
     *
     * @param timeoutTime 超時時間
     * @return 超時步驟列表
     */
    @Select("SELECT * FROM kyc_workflow_steps WHERE status = 'IN_PROGRESS' AND started_at < #{timeoutTime}")
    List<KycWorkflowStep> selectTimeoutSteps(@Param("timeoutTime") LocalDateTime timeoutTime);

    /**
     * 統計審核員工作量
     *
     * @param reviewerId 審核員ID
     * @param startTime 開始時間
     * @param endTime 結束時間
     * @return 工作量統計
     */
    @Select("SELECT COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed, " +
            "AVG(processing_time_minutes) as avg_processing_time " +
            "FROM kyc_workflow_steps " +
            "WHERE actual_reviewer_id = #{reviewerId} " +
            "AND completed_at BETWEEN #{startTime} AND #{endTime}")
    Map<String, Object> selectReviewerWorkloadStats(@Param("reviewerId") Long reviewerId,
                                                   @Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 分頁查詢工作流步驟
     *
     * @param page 分頁對象
     * @param kycId KYC ID（可選）
     * @param status 狀態（可選）
     * @param stepNumber 步驟編號（可選）
     * @return 分頁結果
     */
    Page<KycWorkflowStep> selectWorkflowStepsPage(Page<KycWorkflowStep> page,
                                                 @Param("kycId") Long kycId,
                                                 @Param("status") String status,
                                                 @Param("stepNumber") Integer stepNumber);
}