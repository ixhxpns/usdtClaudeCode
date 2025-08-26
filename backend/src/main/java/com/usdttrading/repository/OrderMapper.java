package com.usdttrading.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 根据订单号查找订单
     */
    @Select("SELECT * FROM orders WHERE order_no = #{orderNo} AND deleted = 0")
    Order findByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据用户ID查找订单
     */
    IPage<Order> findByUserId(Page<Order> page, @Param("userId") Long userId);

    /**
     * 根据状态查找订单
     */
    @Select("SELECT * FROM orders WHERE status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<Order> findByStatus(@Param("status") String status);

    /**
     * 根据类型和状态查找订单
     */
    List<Order> findByTypeAndStatus(@Param("type") String type, @Param("status") String status);

    /**
     * 获取用户的订单统计
     */
    OrderStatistics getUserOrderStatistics(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 获取过期订单
     */
    @Select("SELECT * FROM orders WHERE status = 'pending' AND payment_deadline < NOW() AND deleted = 0")
    List<Order> findExpiredOrders();

    /**
     * 获取指定时间范围内的订单
     */
    List<Order> findOrdersByDateRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计用户今日交易金额
     */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE user_id = #{userId} AND DATE(created_at) = CURDATE() AND status = 'completed' AND deleted = 0")
    BigDecimal getTodayTradingVolume(@Param("userId") Long userId);

    /**
     * 订单统计信息内部类
     */
    class OrderStatistics {
        private Long totalOrders;
        private Long completedOrders;
        private Long cancelledOrders;
        private BigDecimal totalAmount;
        private BigDecimal completedAmount;

        // Getters and setters
        public Long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
        public Long getCompletedOrders() { return completedOrders; }
        public void setCompletedOrders(Long completedOrders) { this.completedOrders = completedOrders; }
        public Long getCancelledOrders() { return cancelledOrders; }
        public void setCancelledOrders(Long cancelledOrders) { this.cancelledOrders = cancelledOrders; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public BigDecimal getCompletedAmount() { return completedAmount; }
        public void setCompletedAmount(BigDecimal completedAmount) { this.completedAmount = completedAmount; }
    }
}