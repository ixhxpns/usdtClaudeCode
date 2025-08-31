package com.usdttrading.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usdttrading.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员Mapper
 * 
 * @author Master Agent
 * @version 1.0.0
 * @since 2025-08-30
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
    
}