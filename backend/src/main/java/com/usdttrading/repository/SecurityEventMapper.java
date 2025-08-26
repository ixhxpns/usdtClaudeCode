package com.usdttrading.repository;

import com.usdttrading.entity.SecurityEvent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 安全事件Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface SecurityEventMapper extends BaseMapper<SecurityEvent> {

    /**
     * 分頁查詢用戶安全事件
     */
    @Select("SELECT * FROM security_events WHERE user_id = #{userId} ORDER BY created_at DESC")
    Page<SecurityEvent> selectPageByUserId(Page<SecurityEvent> page, @Param("userId") Long userId);

    /**
     * 查詢未處理的高危事件
     */
    @Select("SELECT * FROM security_events WHERE is_resolved = 0 AND severity IN ('high', 'critical') ORDER BY created_at DESC")
    List<SecurityEvent> selectUnresolvedHighRiskEvents();

    /**
     * 根據事件類型查詢
     */
    @Select("SELECT * FROM security_events WHERE event_type = #{eventType} AND created_at BETWEEN #{startTime} AND #{endTime} ORDER BY created_at DESC")
    List<SecurityEvent> selectByEventType(@Param("eventType") String eventType, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查詢指定IP的安全事件
     */
    @Select("SELECT * FROM security_events WHERE ip_address = #{ipAddress} ORDER BY created_at DESC LIMIT #{limit}")
    List<SecurityEvent> selectByIpAddress(@Param("ipAddress") String ipAddress, @Param("limit") Integer limit);

    /**
     * 統計用戶特定事件次數
     */
    @Select("SELECT COUNT(*) FROM security_events WHERE user_id = #{userId} AND event_type = #{eventType} AND created_at >= #{since}")
    Long countUserEventsSince(@Param("userId") Long userId, @Param("eventType") String eventType, @Param("since") LocalDateTime since);

    /**
     * 標記事件為已處理
     */
    @Update("UPDATE security_events SET is_resolved = 1, resolved_by = #{resolvedBy}, resolved_at = NOW() WHERE id = #{id}")
    int markAsResolved(@Param("id") Long id, @Param("resolvedBy") Long resolvedBy);

    /**
     * 查詢需要關注的事件
     */
    @Select("SELECT * FROM security_events WHERE is_resolved = 0 AND severity = #{severity} ORDER BY created_at ASC")
    List<SecurityEvent> selectUnresolvedBySeverity(@Param("severity") String severity);

    /**
     * 查詢最近的登錄失敗事件
     */
    @Select("SELECT * FROM security_events WHERE user_id = #{userId} AND event_type = 'login_failed' AND created_at >= #{since} ORDER BY created_at DESC")
    List<SecurityEvent> selectRecentLoginFailures(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}