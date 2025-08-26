package com.usdttrading.repository;

import com.usdttrading.entity.UserSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用戶會話Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface UserSessionMapper extends BaseMapper<UserSession> {

    /**
     * 根據會話ID查詢會話
     */
    @Select("SELECT * FROM user_sessions WHERE session_id = #{sessionId} AND is_active = 1")
    UserSession selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 根據用戶ID查詢活躍會話
     */
    @Select("SELECT * FROM user_sessions WHERE user_id = #{userId} AND is_active = 1 AND expires_at > NOW() ORDER BY last_activity DESC")
    List<UserSession> selectActiveSessionsByUserId(@Param("userId") Long userId);

    /**
     * 根據Token查詢會話
     */
    @Select("SELECT * FROM user_sessions WHERE token = #{token} AND is_active = 1 AND expires_at > NOW()")
    UserSession selectByToken(@Param("token") String token);

    /**
     * 更新會話活動時間
     */
    @Update("UPDATE user_sessions SET last_activity = #{lastActivity} WHERE session_id = #{sessionId}")
    int updateLastActivity(@Param("sessionId") String sessionId, @Param("lastActivity") LocalDateTime lastActivity);

    /**
     * 禁用用戶所有會話
     */
    @Update("UPDATE user_sessions SET is_active = 0 WHERE user_id = #{userId}")
    int disableAllUserSessions(@Param("userId") Long userId);

    /**
     * 禁用指定會話
     */
    @Update("UPDATE user_sessions SET is_active = 0 WHERE session_id = #{sessionId}")
    int disableSession(@Param("sessionId") String sessionId);

    /**
     * 清理過期會話
     */
    @Update("UPDATE user_sessions SET is_active = 0 WHERE expires_at <= NOW() AND is_active = 1")
    int cleanupExpiredSessions();

    /**
     * 查詢過期會話
     */
    @Select("SELECT * FROM user_sessions WHERE expires_at <= NOW() AND is_active = 1")
    List<UserSession> selectExpiredSessions();

    /**
     * 查詢指定設備類型的會話
     */
    @Select("SELECT * FROM user_sessions WHERE user_id = #{userId} AND device_type = #{deviceType} AND is_active = 1")
    List<UserSession> selectByUserIdAndDeviceType(@Param("userId") Long userId, @Param("deviceType") String deviceType);
}