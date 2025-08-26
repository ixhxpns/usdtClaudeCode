package com.usdttrading.repository;

import com.usdttrading.entity.KycReview;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * KYC審核記錄Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface KycReviewMapper extends BaseMapper<KycReview> {

    /**
     * 根據KYC申請ID查詢審核記錄
     */
    @Select("SELECT * FROM kyc_reviews WHERE kyc_id = #{kycId} ORDER BY created_at DESC")
    List<KycReview> selectByKycId(@Param("kycId") Long kycId);

    /**
     * 根據審核人ID查詢審核記錄
     */
    @Select("SELECT * FROM kyc_reviews WHERE reviewer_id = #{reviewerId} ORDER BY created_at DESC")
    List<KycReview> selectByReviewerId(@Param("reviewerId") Long reviewerId);

    /**
     * 查詢待審核的記錄
     */
    @Select("SELECT * FROM kyc_reviews WHERE status = 'pending' ORDER BY created_at ASC")
    List<KycReview> selectPendingReviews();
}