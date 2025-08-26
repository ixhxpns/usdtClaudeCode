package com.usdttrading.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usdttrading.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统配置Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 根据配置键查找配置
     */
    @Select("SELECT * FROM system_config WHERE config_key = #{configKey} AND deleted = 0")
    SystemConfig findByConfigKey(@Param("configKey") String configKey);

    /**
     * 根据配置键查找配置 (与findByConfigKey相同功能，为了兼容性)
     */
    @Select("SELECT * FROM system_config WHERE config_key = #{configKey} AND deleted = 0")
    SystemConfig selectByKey(@Param("configKey") String configKey);

    /**
     * 获取公开配置
     */
    @Select("SELECT * FROM system_config WHERE is_public = 1 AND is_active = 1 AND deleted = 0")
    List<SystemConfig> findPublicConfigs();

    /**
     * 根据分类获取配置
     */
    @Select("SELECT * FROM system_config WHERE category = #{category} AND is_active = 1 AND deleted = 0")
    List<SystemConfig> findByCategory(@Param("category") String category);

    /**
     * 获取所有激活的配置
     */
    @Select("SELECT * FROM system_config WHERE is_active = 1 AND deleted = 0 ORDER BY category, config_key")
    List<SystemConfig> findActiveConfigs();
}