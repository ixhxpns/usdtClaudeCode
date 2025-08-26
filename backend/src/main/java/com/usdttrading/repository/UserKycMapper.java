package com.usdttrading.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usdttrading.entity.UserKyc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * KYC验证Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface UserKycMapper extends BaseMapper<UserKyc> {

    /**
     * 根据用户ID查找KYC信息
     */
    @Select("SELECT * FROM user_kyc WHERE user_id = #{userId} AND deleted = 0")
    UserKyc findByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID选择KYC信息
     */
    @Select("SELECT * FROM user_kyc WHERE user_id = #{userId} AND deleted = 0")
    UserKyc selectByUserId(@Param("userId") Long userId);

    /**
     * 根据状态查找KYC记录
     */
    @Select("SELECT * FROM user_kyc WHERE status = #{status} AND deleted = 0 ORDER BY created_at ASC")
    List<UserKyc> findByStatus(@Param("status") String status);

    /**
     * 获取待审核的KYC记录
     */
    @Select("SELECT * FROM user_kyc WHERE status IN ('pending', 'processing') AND deleted = 0 ORDER BY created_at ASC")
    List<UserKyc> findPendingKyc();

    /**
     * 根据证件号码查找KYC记录（用于重复检查）
     */
    @Select("SELECT * FROM user_kyc WHERE id_number = #{idNumber} AND deleted = 0")
    List<UserKyc> findByIdNumber(@Param("idNumber") String idNumber);

    /**
     * 获取即将过期的KYC记录
     */
    List<UserKyc> findExpiringKyc(@Param("days") int days);
}