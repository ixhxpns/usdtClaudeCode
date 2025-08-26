package com.usdttrading.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usdttrading.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户个人资料Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    /**
     * 根据用户ID查找个人资料
     */
    @Select("SELECT * FROM user_profiles WHERE user_id = #{userId} AND deleted = 0")
    UserProfile findByUserId(@Param("userId") Long userId);

    /**
     * 根据国家查找用户档案
     */
    @Select("SELECT * FROM user_profiles WHERE country = #{country} AND deleted = 0")
    List<UserProfile> findByCountry(@Param("country") String country);

    /**
     * 搜索用户档案
     */
    List<UserProfile> searchProfiles(@Param("keyword") String keyword);
}