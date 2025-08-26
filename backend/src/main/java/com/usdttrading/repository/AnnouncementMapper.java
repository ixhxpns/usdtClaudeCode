package com.usdttrading.repository;

import com.usdttrading.entity.Announcement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系統公告Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {

    /**
     * 查詢有效的公告
     */
    @Select("SELECT * FROM announcements WHERE is_active = 1 AND publish_at <= NOW() AND (expire_at IS NULL OR expire_at > NOW()) ORDER BY priority DESC, publish_at DESC")
    List<Announcement> selectActiveAnnouncements();

    /**
     * 查詢彈窗公告
     */
    @Select("SELECT * FROM announcements WHERE is_active = 1 AND is_popup = 1 AND publish_at <= NOW() AND (expire_at IS NULL OR expire_at > NOW()) ORDER BY priority DESC, publish_at DESC")
    List<Announcement> selectPopupAnnouncements();

    /**
     * 分頁查詢指定目標受眾的公告
     */
    @Select("SELECT * FROM announcements WHERE is_active = 1 AND (target_audience = #{audience} OR target_audience = 'all') AND publish_at <= NOW() AND (expire_at IS NULL OR expire_at > NOW()) ORDER BY priority DESC, publish_at DESC")
    Page<Announcement> selectPageByAudience(Page<Announcement> page, @Param("audience") String audience);

    /**
     * 查詢即將過期的公告
     */
    @Select("SELECT * FROM announcements WHERE is_active = 1 AND expire_at IS NOT NULL AND expire_at BETWEEN NOW() AND #{beforeTime}")
    List<Announcement> selectExpiringAnnouncements(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 根據類型查詢公告
     */
    @Select("SELECT * FROM announcements WHERE is_active = 1 AND type = #{type} ORDER BY priority DESC, publish_at DESC")
    List<Announcement> selectByType(@Param("type") String type);
}