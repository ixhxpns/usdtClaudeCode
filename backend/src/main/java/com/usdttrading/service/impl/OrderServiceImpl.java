package com.usdttrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.entity.*;
import com.usdttrading.enums.*;
import com.usdttrading.repository.*;
import com.usdttrading.service.OrderService;
import com.usdttrading.service.AuditLogService;
import com.usdttrading.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 訂單管理服務實現類
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final WalletMapper walletMapper;
    private final UserKycMapper userKycMapper;
    private final SystemConfigMapper systemConfigMapper;
    
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Override
    public ApiResponse<Page<Map<String, Object>>> getAllOrders(int pageNum, int pageSize, 
            Long userId, String orderType, String status, String startDate, String endDate) {
        try {
            Page<Order> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            
            // 訂單類型篩選
            if (orderType != null && !orderType.isEmpty() && !"all".equals(orderType)) {
                wrapper.eq(Order::getOrderType, OrderType.valueOf(orderType.toUpperCase()));
            }
            
            // 狀態篩選
            if (status != null && !status.isEmpty() && !"all".equals(status)) {
                wrapper.eq(Order::getOrderStatus, OrderStatus.valueOf(status.toUpperCase()));
            }
            
                    // 用戶ID篩選
            if (userId != null) {
                wrapper.eq(Order::getUserId, userId);
            }
            
            // 日期範圍篩選
            if (startDate != null && !startDate.isEmpty()) {
                wrapper.ge(Order::getCreatedAt, LocalDateTime.parse(startDate + "T00:00:00"));
            }
            
            if (endDate != null && !endDate.isEmpty()) {
                wrapper.le(Order::getCreatedAt, LocalDateTime.parse(endDate + "T23:59:59"));
            }
            
            wrapper.orderByDesc(Order::getCreatedAt);
            
            Page<Order> orderPage = orderMapper.selectPage(page, wrapper);
            
            Page<Map<String, Object>> resultPage = new Page<>();
            resultPage.setCurrent(orderPage.getCurrent());
            resultPage.setSize(orderPage.getSize());
            resultPage.setTotal(orderPage.getTotal());
            resultPage.setPages(orderPage.getPages());
            
            resultPage.setRecords(orderPage.getRecords().stream()
                .map(this::buildOrderListItem)
                .toList());

            return ApiResponse.success("獲取訂單列表成功", resultPage);
        } catch (Exception e) {
            log.error("獲取訂單列表失敗", e);
            return ApiResponse.error("獲取訂單列表失敗");
        }
    }

    @Override
    public ApiResponse<Map<String, Object>> getOrderDetail(Long orderId, Long userId) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return ApiResponse.error("訂單不存在");
            }
            
            Map<String, Object> result = buildOrderDetailMap(order);
            return ApiResponse.success("獲取訂單詳情成功", result);
        } catch (Exception e) {
            log.error("獲取訂單詳情失敗: orderId={}", orderId, e);
            return ApiResponse.error("獲取訂單詳情失敗");
        }
    }

    @Transactional
    public ApiResponse<String> approveOrder(Long orderId, Long adminId, String notes) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return ApiResponse.error("訂單不存在");
            }
            
            if (order.getOrderStatus() != OrderStatus.PROCESSING) {
                return ApiResponse.error("只能審核處理中的訂單");
            }
            
            // 更新訂單狀態
            order.setOrderStatus(OrderStatus.COMPLETED);
            order.setCompletedAt(LocalDateTime.now());
            order.setAdminNotes(notes);
            orderMapper.updateById(order);
            
            // 處理買入訂單 - 增加用戶USDT餘額
            if (order.getOrderType() == OrderType.BUY) {
                Wallet wallet = walletMapper.selectByUserId(order.getUserId());
                if (wallet == null) {
                    wallet = new Wallet();
                    wallet.setUserId(order.getUserId());
                    wallet.setUsdtBalance(order.getUsdtAmount());
                    wallet.setFrozenUsdt(BigDecimal.ZERO);
                    wallet.setTwdBalance(BigDecimal.ZERO);
                    walletMapper.insert(wallet);
                } else {
                    wallet.setUsdtBalance(wallet.getUsdtBalance().add(order.getUsdtAmount()));
                    walletMapper.updateById(wallet);
                }
            } else {
                // 處理賣出訂單 - 將凍結的USDT移除，增加TWD餘額
                Wallet wallet = walletMapper.selectByUserId(order.getUserId());
                if (wallet != null) {
                    wallet.setFrozenUsdt(wallet.getFrozenUsdt().subtract(order.getUsdtAmount()));
                    wallet.setTwdBalance(wallet.getTwdBalance().add(order.getAmount()));
                    walletMapper.updateById(wallet);
                }
            }
            
            // 記錄審計日誌
            auditLogService.logAdminAction(adminId, "APPROVE_ORDER", 
                "審核通過訂單: " + order.getOrderNumber(), null, null);
            
            // 發送用戶通知
            notificationService.sendOrderNotification(order.getUserId(), orderId, 
                "訂單已完成", "您的訂單已審核通過並完成處理");
            
            return ApiResponse.success("訂單審核通過");
        } catch (Exception e) {
            log.error("訂單審核失敗: orderId={}, adminId={}", orderId, adminId, e);
            return ApiResponse.error("訂單審核失敗");
        }
    }

    @Transactional
    public ApiResponse<String> rejectOrder(Long orderId, Long adminId, String reason) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return ApiResponse.error("訂單不存在");
            }
            
            if (order.getOrderStatus() != OrderStatus.PROCESSING) {
                return ApiResponse.error("只能拒絕處理中的訂單");
            }
            
            // 更新訂單狀態
            order.setOrderStatus(OrderStatus.REJECTED);
            order.setCancelReason(reason);
            order.setCancelTime(LocalDateTime.now());
            orderMapper.updateById(order);
            
            // 如果是賣出訂單，解凍用戶的USDT
            if (order.getOrderType() == OrderType.SELL) {
                Wallet wallet = walletMapper.selectByUserId(order.getUserId());
                if (wallet != null) {
                    wallet.setUsdtBalance(wallet.getUsdtBalance().add(order.getUsdtAmount()));
                    wallet.setFrozenUsdt(wallet.getFrozenUsdt().subtract(order.getUsdtAmount()));
                    walletMapper.updateById(wallet);
                }
            }
            
            // 記錄審計日誌
            auditLogService.logAdminAction(adminId, "REJECT_ORDER", 
                "拒絕訂單: " + order.getOrderNumber() + ", 原因: " + reason, null, null);
            
            // 發送用戶通知
            notificationService.sendOrderNotification(order.getUserId(), orderId, 
                "訂單已拒絕", "拒絕原因: " + reason);
            
            return ApiResponse.success("訂單已拒絕");
        } catch (Exception e) {
            log.error("訂單拒絕失敗: orderId={}, adminId={}", orderId, adminId, e);
            return ApiResponse.error("訂單拒絕失敗");
        }
    }


    public ApiResponse<Map<String, Object>> getOrderAnalytics(String period) {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = calculateStartTime(endTime, period);
            
            // 獲取訂單數據
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.between(Order::getCreatedAt, startTime, endTime);
            List<Order> orders = orderMapper.selectList(wrapper);
            
            // 按日期分組統計
            Map<String, Long> dailyOrders = new HashMap<>();
            Map<String, BigDecimal> dailyVolume = new HashMap<>();
            
            orders.forEach(order -> {
                String date = order.getCreatedAt().toLocalDate().toString();
                dailyOrders.merge(date, 1L, Long::sum);
                if (order.getOrderStatus() == OrderStatus.COMPLETED) {
                    dailyVolume.merge(date, order.getAmount(), BigDecimal::add);
                }
            });
            
            // 按訂單類型統計
            long buyOrders = orders.stream().mapToLong(o -> 
                o.getOrderType() == OrderType.BUY ? 1 : 0).sum();
            long sellOrders = orders.stream().mapToLong(o -> 
                o.getOrderType() == OrderType.SELL ? 1 : 0).sum();
            
            Map<String, Object> analytics = new HashMap<>();
            analytics.put("dailyOrders", dailyOrders);
            analytics.put("dailyVolume", dailyVolume);
            analytics.put("buyOrders", buyOrders);
            analytics.put("sellOrders", sellOrders);
            analytics.put("period", period);
            
            return ApiResponse.success("獲取訂單分析成功", analytics);
        } catch (Exception e) {
            log.error("獲取訂單分析失敗: period={}", period, e);
            return ApiResponse.error("獲取訂單分析失敗");
        }
    }

    public ApiResponse<Page<Map<String, Object>>> getOrderDisputes(int pageNum, int pageSize, String status) {
        try {
            Page<Order> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            
            // 查詢有爭議的訂單（有客服備注的訂單）
            wrapper.isNotNull(Order::getCustomerServiceNotes);
            
            if (status != null && !status.isEmpty() && !"all".equals(status)) {
                wrapper.eq(Order::getOrderStatus, OrderStatus.valueOf(status.toUpperCase()));
            }
            
            wrapper.orderByDesc(Order::getUpdatedAt);
            
            Page<Order> orderPage = orderMapper.selectPage(page, wrapper);
            
            Page<Map<String, Object>> resultPage = new Page<>();
            resultPage.setCurrent(orderPage.getCurrent());
            resultPage.setSize(orderPage.getSize());
            resultPage.setTotal(orderPage.getTotal());
            resultPage.setPages(orderPage.getPages());
            
            resultPage.setRecords(orderPage.getRecords().stream()
                .map(this::buildDisputeOrderMap)
                .toList());

            return ApiResponse.success("獲取爭議訂單列表成功", resultPage);
        } catch (Exception e) {
            log.error("獲取爭議訂單列表失敗", e);
            return ApiResponse.error("獲取爭議訂單列表失敗");
        }
    }

    @Transactional
    public ApiResponse<String> handleOrderDispute(Long orderId, Long adminId, String action, String notes) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return ApiResponse.error("訂單不存在");
            }
            
            // 更新客服備注
            order.setCustomerServiceNotes(notes);
            order.setUpdatedAt(LocalDateTime.now());
            
            // 根據action執行相應操作
            switch (action.toLowerCase()) {
                case "approve":
                    return approveOrder(orderId, adminId, notes);
                case "reject":
                    return rejectOrder(orderId, adminId, notes);
                case "note":
                    orderMapper.updateById(order);
                    break;
                default:
                    return ApiResponse.error("無效的處理動作");
            }
            
            // 記錄審計日誌
            auditLogService.logAdminAction(adminId, "HANDLE_DISPUTE", 
                "處理爭議訂單: " + order.getOrderNumber() + ", 動作: " + action, null, null);
            
            return ApiResponse.success("爭議處理成功");
        } catch (Exception e) {
            log.error("處理訂單爭議失敗: orderId={}, adminId={}", orderId, adminId, e);
            return ApiResponse.error("處理爭議失敗");
        }
    }

    // 私有輔助方法
    private Map<String, Object> buildOrderListItem(Order order) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", order.getId());
        item.put("orderNumber", order.getOrderNumber());
        item.put("orderType", order.getOrderType());
        item.put("amount", order.getAmount());
        item.put("usdtAmount", order.getUsdtAmount());
        item.put("price", order.getPrice());
        item.put("status", order.getOrderStatus());
        item.put("createdAt", order.getCreatedAt());
        
        // 獲取用戶信息
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            item.put("userPhone", user.getPhone());
            item.put("userEmail", user.getEmail());
        }
        
        return item;
    }
    
    private Map<String, Object> buildOrderDetailMap(Order order) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", order.getId());
        detail.put("orderNumber", order.getOrderNumber());
        detail.put("orderType", order.getOrderType());
        detail.put("amount", order.getAmount());
        detail.put("usdtAmount", order.getUsdtAmount());
        detail.put("price", order.getPrice());
        detail.put("status", order.getOrderStatus());
        detail.put("paymentMethod", order.getPaymentMethod());
        detail.put("receivingAccount", order.getReceivingAccount());
        detail.put("receivingBank", order.getReceivingBank());
        detail.put("paymentProof", order.getPaymentProof());
        detail.put("adminNotes", order.getAdminNotes());
        detail.put("customerServiceNotes", order.getCustomerServiceNotes());
        detail.put("cancelReason", order.getCancelReason());
        detail.put("createdAt", order.getCreatedAt());
        detail.put("paymentConfirmTime", order.getPaymentConfirmTime());
        detail.put("completedAt", order.getCompletedAt());
        detail.put("cancelTime", order.getCancelTime());
        detail.put("clientIp", order.getClientIp());
        detail.put("userAgent", order.getUserAgent());
        
        // 獲取用戶詳細信息
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("phone", user.getPhone());
            userInfo.put("email", user.getEmail());
            userInfo.put("status", user.getStatus());
            userInfo.put("createdAt", user.getCreatedAt());
            detail.put("userInfo", userInfo);
            
            // 獲取KYC信息
            UserKyc kyc = userKycMapper.selectByUserId(user.getId());
            if (kyc != null) {
                Map<String, Object> kycInfo = new HashMap<>();
                kycInfo.put("status", kyc.getStatus());
                kycInfo.put("level", kyc.getLevel());
                kycInfo.put("realName", kyc.getRealName());
                detail.put("kycInfo", kycInfo);
            }
        }
        
        return detail;
    }
    
    private Map<String, Object> buildDisputeOrderMap(Order order) {
        Map<String, Object> dispute = new HashMap<>();
        dispute.put("id", order.getId());
        dispute.put("orderNumber", order.getOrderNumber());
        dispute.put("orderType", order.getOrderType());
        dispute.put("amount", order.getAmount());
        dispute.put("status", order.getOrderStatus());
        dispute.put("customerServiceNotes", order.getCustomerServiceNotes());
        dispute.put("createdAt", order.getCreatedAt());
        dispute.put("updatedAt", order.getUpdatedAt());
        
        // 獲取用戶信息
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            dispute.put("userPhone", user.getPhone());
        }
        
        return dispute;
    }
    
    private LocalDateTime calculateStartTime(LocalDateTime endTime, String period) {
        return switch (period.toLowerCase()) {
            case "today", "1d" -> endTime.toLocalDate().atStartOfDay();
            case "week", "7d" -> endTime.minusDays(7);
            case "month", "30d" -> endTime.minusDays(30);
            case "quarter", "90d" -> endTime.minusDays(90);
            case "year", "365d" -> endTime.minusDays(365);
            default -> endTime.minusDays(30);
        };
    }

    // 需要添加的接口方法
    @Override
    public ApiResponse<Page<Map<String, Object>>> getUserOrders(Long userId, int pageNum, int pageSize, 
            String orderType, String status, String startDate, String endDate) {
        try {
            Page<Order> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            
            // 用戶ID篩選
            wrapper.eq(Order::getUserId, userId);
            
            // 訂單類型篩選
            if (orderType != null && !orderType.isEmpty() && !"all".equals(orderType)) {
                wrapper.eq(Order::getOrderType, OrderType.valueOf(orderType.toUpperCase()));
            }
            
            // 狀態篩選
            if (status != null && !status.isEmpty() && !"all".equals(status)) {
                wrapper.eq(Order::getOrderStatus, OrderStatus.valueOf(status.toUpperCase()));
            }
            
            // 日期範圍篩選
            if (startDate != null && !startDate.isEmpty()) {
                wrapper.ge(Order::getCreatedAt, LocalDateTime.parse(startDate + "T00:00:00"));
            }
            
            if (endDate != null && !endDate.isEmpty()) {
                wrapper.le(Order::getCreatedAt, LocalDateTime.parse(endDate + "T23:59:59"));
            }
            
            wrapper.orderByDesc(Order::getCreatedAt);
            
            Page<Order> orderPage = orderMapper.selectPage(page, wrapper);
            
            Page<Map<String, Object>> resultPage = new Page<>();
            resultPage.setCurrent(orderPage.getCurrent());
            resultPage.setSize(orderPage.getSize());
            resultPage.setTotal(orderPage.getTotal());
            resultPage.setPages(orderPage.getPages());
            
            resultPage.setRecords(orderPage.getRecords().stream()
                .map(this::buildOrderListItem)
                .toList());

            return ApiResponse.success("獲取用戶訂單列表成功", resultPage);
        } catch (Exception e) {
            log.error("獲取用戶訂單列表失敗", e);
            return ApiResponse.error("獲取用戶訂單列表失敗");
        }
    }

    @Override
    public ApiResponse<List<Map<String, Object>>> getOrderTransactions(Long orderId, Long userId) {
        // 這裡可以從訂單交易記錄表獲取數據，現在返回空列表
        return ApiResponse.success("獲取訂單交易記錄成功", List.of());
    }

    @Override
    public ApiResponse<String> resubmitPaymentProof(Long orderId, Long userId, 
            Map<String, String> paymentProof, String clientIp) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null || !order.getUserId().equals(userId)) {
                return ApiResponse.error("訂單不存在");
            }
            
            // 更新支付憑證
            order.setPaymentProof(paymentProof.toString());
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            
            return ApiResponse.success("重新提交支付憑證成功");
        } catch (Exception e) {
            log.error("重新提交支付憑證失敗", e);
            return ApiResponse.error("重新提交支付憑證失敗");
        }
    }

    @Override
    public ApiResponse<String> createDispute(Long orderId, Long userId, String reason, 
            String description, List<String> evidence, String clientIp) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null || !order.getUserId().equals(userId)) {
                return ApiResponse.error("訂單不存在");
            }
            
            // 創建爭議記錄
            order.setCustomerServiceNotes(reason + ": " + description);
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            
            return ApiResponse.success("申請訂單仲裁成功");
        } catch (Exception e) {
            log.error("申請訂單仲裁失敗", e);
            return ApiResponse.error("申請訂單仲裁失敗");
        }
    }

    @Override
    public ApiResponse<String> reviewPayment(Long orderId, Long reviewerId, Boolean approved, 
            String comment, String clientIp) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return ApiResponse.error("訂單不存在");
            }
            
            if (approved) {
                return approveOrder(orderId, reviewerId, comment);
            } else {
                return rejectOrder(orderId, reviewerId, comment);
            }
        } catch (Exception e) {
            log.error("審核支付憑證失敗", e);
            return ApiResponse.error("審核支付憑證失敗");
        }
    }

    @Override
    public ApiResponse<String> manualCompleteOrder(Long orderId, Long adminId, String reason, String clientIp) {
        return approveOrder(orderId, adminId, reason);
    }

    @Override
    public ApiResponse<String> resolveDispute(Long orderId, Long adminId, String resolution, 
            String comment, Map<String, Object> compensation, String clientIp) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return ApiResponse.error("訂單不存在");
            }
            
            // 更新爭議解決方案
            order.setCustomerServiceNotes(resolution + ": " + comment);
            order.setAdminNotes(comment);
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            
            // 記錄審計日誌
            auditLogService.logAdminAction(adminId, "RESOLVE_DISPUTE", 
                "解決爭議訂單: " + order.getOrderNumber(), null, null);
            
            return ApiResponse.success("處理訂單爭議成功");
        } catch (Exception e) {
            log.error("處理訂單爭議失敗", e);
            return ApiResponse.error("處理訂單爭議失敗");
        }
    }

    @Override
    public ApiResponse<Map<String, Object>> getOrderStatistics(String period, String type) {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = calculateStartTime(endTime, period);
            
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.between(Order::getCreatedAt, startTime, endTime);
            
            if (type != null && !type.isEmpty() && !"all".equals(type)) {
                wrapper.eq(Order::getOrderType, OrderType.valueOf(type.toUpperCase()));
            }
            
            List<Order> orders = orderMapper.selectList(wrapper);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalOrders", orders.size());
            
            long completedOrders = orders.stream()
                .mapToLong(o -> o.getOrderStatus() == OrderStatus.COMPLETED ? 1 : 0)
                .sum();
            
            BigDecimal totalAmount = orders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.COMPLETED)
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            stats.put("completedOrders", completedOrders);
            stats.put("totalAmount", totalAmount);
            stats.put("period", period);
            stats.put("type", type);
            
            return ApiResponse.success("獲取訂單統計成功", stats);
        } catch (Exception e) {
            log.error("獲取訂單統計失敗: period={}, type={}", period, type, e);
            return ApiResponse.error("獲取訂單統計失敗");
        }
    }
}