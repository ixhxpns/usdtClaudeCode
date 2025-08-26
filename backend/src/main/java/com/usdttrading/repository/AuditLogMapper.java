package com.usdttrading.repository;

import com.usdttrading.entity.AuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日誌Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    /**
     * 分頁查詢用戶操作日誌
     */
    @Select("SELECT * FROM audit_logs WHERE user_id = #{userId} ORDER BY created_at DESC")
    Page<AuditLog> selectPageByUserId(Page<AuditLog> page, @Param("userId") Long userId);

    /**
     * 分頁查詢操作日誌
     */
    @Select("SELECT * FROM audit_logs WHERE action = #{action} ORDER BY created_at DESC")
    Page<AuditLog> selectPageByAction(Page<AuditLog> page, @Param("action") String action);

    /**
     * 查詢指定資源的操作記錄
     */
    @Select("SELECT * FROM audit_logs WHERE resource = #{resource} AND resource_id = #{resourceId} ORDER BY created_at DESC")
    List<AuditLog> selectByResource(@Param("resource") String resource, @Param("resourceId") String resourceId);

    /**
     * 查詢指定IP的操作記錄
     */
    @Select("SELECT * FROM audit_logs WHERE ip_address = #{ipAddress} ORDER BY created_at DESC LIMIT #{limit}")
    List<AuditLog> selectByIpAddress(@Param("ipAddress") String ipAddress, @Param("limit") Integer limit);

    /**
     * 查詢失敗的操作記錄
     */
    @Select("SELECT * FROM audit_logs WHERE result IN ('failure', 'error') AND created_at BETWEEN #{startTime} AND #{endTime} ORDER BY created_at DESC")
    List<AuditLog> selectFailedOperations(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 統計操作次數
     */
    @Select("SELECT COUNT(*) FROM audit_logs WHERE user_id = #{userId} AND action = #{action} AND created_at >= #{since}")
    Long countUserActions(@Param("userId") Long userId, @Param("action") String action, @Param("since") LocalDateTime since);

    /**
     * 查詢慢操作記錄
     */
    @Select("SELECT * FROM audit_logs WHERE execution_time > #{threshold} AND created_at BETWEEN #{startTime} AND #{endTime} ORDER BY execution_time DESC")
    List<AuditLog> selectSlowOperations(@Param("threshold") Integer threshold, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}