package com.usdttrading.repository;

import com.usdttrading.entity.Notification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 通知Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 分頁查詢用戶通知
     */
    @Select("SELECT * FROM notifications WHERE user_id = #{userId} ORDER BY created_at DESC")
    Page<Notification> selectPageByUserId(Page<Notification> page, @Param("userId") Long userId);

    /**
     * 查詢用戶未讀通知數量
     */
    @Select("SELECT COUNT(*) FROM notifications WHERE user_id = #{userId} AND status IN ('pending', 'sent', 'delivered')")
    Long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 查詢待發送的通知
     */
    @Select("SELECT * FROM notifications WHERE status = 'pending' AND send_at <= NOW() ORDER BY priority DESC, send_at ASC")
    List<Notification> selectPendingNotifications();

    /**
     * 查詢需要重試的通知
     */
    @Select("SELECT * FROM notifications WHERE status = 'failed' AND retry_count < 3 ORDER BY created_at ASC")
    List<Notification> selectRetryNotifications();

    /**
     * 標記通知為已讀
     */
    @Update("UPDATE notifications SET status = 'read', read_at = NOW() WHERE id = #{id} AND user_id = #{userId}")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 批量標記通知為已讀
     */
    @Update("UPDATE notifications SET status = 'read', read_at = NOW() WHERE user_id = #{userId} AND status != 'read'")
    int markAllAsRead(@Param("userId") Long userId);

    /**
     * 查詢系統通知
     */
    @Select("SELECT * FROM notifications WHERE user_id IS NULL ORDER BY created_at DESC")
    List<Notification> selectSystemNotifications();
}