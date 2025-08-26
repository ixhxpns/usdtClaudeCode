package com.usdttrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.entity.*;
import com.usdttrading.enums.*;
import com.usdttrading.repository.*;
import com.usdttrading.service.TradingService;
import com.usdttrading.service.AuditLogService;
import com.usdttrading.service.NotificationService;
import com.usdttrading.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 交易服務實現類
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingServiceImpl implements TradingService {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final UserKycMapper userKycMapper;
    private final WalletMapper walletMapper;
    private final PriceHistoryMapper priceHistoryMapper;
    private final SystemConfigMapper systemConfigMapper;
    
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final ValidationUtils validationUtils;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public ApiResponse<Map<String, Object>> getCurrentPrice() {
        try {
            // 從Redis緩存獲取當前價格
            String priceKey = "usdt:current_price";
            Map<String, Object> cachedPrice = (Map<String, Object>) redisTemplate.opsForValue().get(priceKey);
            
            if (cachedPrice != null) {
                return ApiResponse.success("獲取價格成功", cachedPrice);
            }
            
            // 從數據庫獲取最新價格
            LambdaQueryWrapper<PriceHistory> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(PriceHistory::getCreatedAt).last("LIMIT 1");
            PriceHistory latestPrice = priceHistoryMapper.selectOne(wrapper);
            
            Map<String, Object> priceData = new HashMap<>();
            if (latestPrice != null) {
                // 計算買賣價差
                BigDecimal spreadPercent = getSystemConfigDecimal("price.spread_percent", new BigDecimal("0.02"));
                BigDecimal basePrice = latestPrice.getPrice();
                BigDecimal spread = basePrice.multiply(spreadPercent);
                
                priceData.put("buyPrice", basePrice.add(spread).setScale(2, RoundingMode.HALF_UP));
                priceData.put("sellPrice", basePrice.subtract(spread).setScale(2, RoundingMode.HALF_UP));
                priceData.put("basePrice", basePrice);
                priceData.put("spreadPercent", spreadPercent.multiply(new BigDecimal("100")));
                priceData.put("lastUpdate", latestPrice.getCreatedAt());
            } else {
                // 默認價格
                priceData.put("buyPrice", new BigDecimal("31.50"));
                priceData.put("sellPrice", new BigDecimal("30.50"));
                priceData.put("basePrice", new BigDecimal("31.00"));
                priceData.put("spreadPercent", new BigDecimal("1.61"));
                priceData.put("lastUpdate", LocalDateTime.now());
            }
            
            // 緩存價格數據 30秒
            redisTemplate.opsForValue().set(priceKey, priceData, 30, TimeUnit.SECONDS);
            
            return ApiResponse.success("獲取價格成功", priceData);
            
        } catch (Exception e) {
            log.error("獲取當前價格失敗", e);
            return ApiResponse.error("獲取價格失敗");
        }
    }

    @Override
    @Transactional
    public ApiResponse<Map<String, Object>> createBuyOrder(Long userId, BigDecimal amount, 
            String paymentMethod, String clientIp, String userAgent) {
        try {
            // 驗證用戶狀態
            User user = userMapper.selectById(userId);
            if (user == null) {
                return ApiResponse.error("用戶不存在");
            }
            
            if (user.getStatus() != UserStatus.ACTIVE) {
                return ApiResponse.error("用戶狀態異常，無法交易");
            }
            
            // 檢查KYC狀態
            UserKyc userKyc = userKycMapper.selectByUserId(userId);
            if (userKyc == null || userKyc.getStatus() != KycStatus.APPROVED) {
                return ApiResponse.error("請先完成KYC身份驗證");
            }
            
            // 驗證交易限額
            ApiResponse<Map<String, Object>> limitCheck = checkTradingLimits(userId, amount, OrderType.BUY);
            if (!limitCheck.isSuccess()) {
                return limitCheck;
            }
            
            // 獲取當前價格
            ApiResponse<Map<String, Object>> priceResponse = getCurrentPrice();
            if (!priceResponse.isSuccess()) {
                return ApiResponse.error("獲取價格失敗");
            }
            
            Map<String, Object> priceData = priceResponse.getData();
            BigDecimal buyPrice = (BigDecimal) priceData.get("buyPrice");
            BigDecimal usdtAmount = amount.divide(buyPrice, 6, RoundingMode.HALF_DOWN);
            
            // 檢查交易頻率限制
            String rateKey = "trade_rate:" + userId;
            Integer todayTrades = (Integer) redisTemplate.opsForValue().get(rateKey);
            int maxDailyTrades = getSystemConfigInt("trade.max_daily_trades", 20);
            
            if (todayTrades != null && todayTrades >= maxDailyTrades) {
                return ApiResponse.error("今日交易次數已達上限");
            }
            
            // 創建訂單
            Order order = new Order();
            order.setUserId(userId);
            order.setOrderType(OrderType.BUY);
            order.setAmount(amount);
            order.setUsdtAmount(usdtAmount);
            order.setPrice(buyPrice);
            order.setPaymentMethod(paymentMethod);
            order.setOrderStatus(OrderStatus.PENDING);
            order.setClientIp(clientIp);
            order.setUserAgent(userAgent);
            
            // 生成訂單號
            String orderNo = generateOrderNumber();
            order.setOrderNumber(orderNo);
            
            orderMapper.insert(order);
            
            // 記錄交易頻率
            if (todayTrades == null) {
                redisTemplate.opsForValue().set(rateKey, 1, getSecondsUntilMidnight(), TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().increment(rateKey);
            }
            
            // 記錄審計日誌
            auditLogService.logUserAction(userId, "CREATE_BUY_ORDER", 
                "創建買入訂單: " + orderNo + ", 金額: " + amount, clientIp, userAgent);
            
            // 發送通知
            notificationService.sendOrderNotification(userId, order.getId(), 
                "買入訂單已創建", "訂單號: " + orderNo);
            
            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.getId());
            result.put("orderNumber", orderNo);
            result.put("amount", amount);
            result.put("usdtAmount", usdtAmount);
            result.put("price", buyPrice);
            result.put("paymentMethod", paymentMethod);
            result.put("status", order.getStatus());
            
            return ApiResponse.success("訂單創建成功", result);
            
        } catch (Exception e) {
            log.error("創建買入訂單失敗: userId={}, amount={}", userId, amount, e);
            return ApiResponse.error("創建訂單失敗");
        }
    }

    @Override
    @Transactional
    public ApiResponse<Map<String, Object>> createSellOrder(Long userId, BigDecimal usdtAmount, 
            String receivingAccount, String receivingBank, String clientIp, String userAgent) {
        try {
            // 驗證用戶狀態
            User user = userMapper.selectById(userId);
            if (user == null) {
                return ApiResponse.error("用戶不存在");
            }
            
            if (user.getStatus() != UserStatus.ACTIVE) {
                return ApiResponse.error("用戶狀態異常，無法交易");
            }
            
            // 檢查KYC狀態
            UserKyc userKyc = userKycMapper.selectByUserId(userId);
            if (userKyc == null || userKyc.getStatus() != KycStatus.APPROVED) {
                return ApiResponse.error("請先完成KYC身份驗證");
            }
            
            // 檢查USDT餘額
            Wallet wallet = walletMapper.selectByUserId(userId);
            if (wallet == null || wallet.getUsdtBalance().compareTo(usdtAmount) < 0) {
                return ApiResponse.error("USDT餘額不足");
            }
            
            // 獲取當前價格
            ApiResponse<Map<String, Object>> priceResponse = getCurrentPrice();
            if (!priceResponse.isSuccess()) {
                return ApiResponse.error("獲取價格失敗");
            }
            
            Map<String, Object> priceData = priceResponse.getData();
            BigDecimal sellPrice = (BigDecimal) priceData.get("sellPrice");
            BigDecimal twdAmount = usdtAmount.multiply(sellPrice).setScale(0, RoundingMode.HALF_DOWN);
            
            // 驗證交易限額
            ApiResponse<Map<String, Object>> limitCheck = checkTradingLimits(userId, twdAmount, OrderType.SELL);
            if (!limitCheck.isSuccess()) {
                return limitCheck;
            }
            
            // 凍結USDT餘額
            wallet.setUsdtBalance(wallet.getUsdtBalance().subtract(usdtAmount));
            wallet.setFrozenUsdt(wallet.getFrozenUsdt().add(usdtAmount));
            walletMapper.updateById(wallet);
            
            // 創建訂單
            Order order = new Order();
            order.setUserId(userId);
            order.setOrderType(OrderType.SELL);
            order.setAmount(twdAmount);
            order.setUsdtAmount(usdtAmount);
            order.setPrice(sellPrice);
            order.setReceivingAccount(receivingAccount);
            order.setReceivingBank(receivingBank);
            order.setOrderStatus(OrderStatus.PENDING);
            order.setClientIp(clientIp);
            order.setUserAgent(userAgent);
            
            // 生成訂單號
            String orderNo = generateOrderNumber();
            order.setOrderNumber(orderNo);
            
            orderMapper.insert(order);
            
            // 記錄審計日誌
            auditLogService.logUserAction(userId, "CREATE_SELL_ORDER", 
                "創建賣出訂單: " + orderNo + ", USDT數量: " + usdtAmount, clientIp, userAgent);
            
            // 發送通知
            notificationService.sendOrderNotification(userId, order.getId(), 
                "賣出訂單已創建", "訂單號: " + orderNo);
            
            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.getId());
            result.put("orderNumber", orderNo);
            result.put("amount", twdAmount);
            result.put("usdtAmount", usdtAmount);
            result.put("price", sellPrice);
            result.put("receivingAccount", receivingAccount);
            result.put("receivingBank", receivingBank);
            result.put("status", order.getStatus());
            
            return ApiResponse.success("訂單創建成功", result);
            
        } catch (Exception e) {
            log.error("創建賣出訂單失敗: userId={}, usdtAmount={}", userId, usdtAmount, e);
            return ApiResponse.error("創建訂單失敗");
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> confirmPayment(Long orderId, Long userId, 
            Map<String, String> paymentProof, String clientIp) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return ApiResponse.error("訂單不存在");
            }
            
            if (!order.getUserId().equals(userId)) {
                return ApiResponse.error("無權限操作此訂單");
            }
            
            if (order.getOrderStatus() != OrderStatus.PENDING) {
                return ApiResponse.error("訂單狀態不允許確認支付");
            }
            
            if (order.getOrderType() != OrderType.BUY) {
                return ApiResponse.error("只有買入訂單可以確認支付");
            }
            
            // 更新訂單狀態
            order.setOrderStatus(OrderStatus.PROCESSING);
            order.setPaymentProof(paymentProof != null ? paymentProof.toString() : null);
            order.setPaymentConfirmTime(LocalDateTime.now());
            orderMapper.updateById(order);
            
            // 記錄審計日誌
            auditLogService.logUserAction(userId, "CONFIRM_PAYMENT", 
                "確認訂單支付: " + order.getOrderNumber(), clientIp, "");
            
            // 發送通知
            notificationService.sendOrderNotification(userId, orderId, 
                "支付確認成功", "訂單已進入處理狀態，請等待審核");
            
            return ApiResponse.success("支付確認成功");
            
        } catch (Exception e) {
            log.error("確認支付失敗: orderId={}, userId={}", orderId, userId, e);
            return ApiResponse.error("確認支付失敗");
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> cancelOrder(Long orderId, Long userId, String reason, String clientIp) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return ApiResponse.error("訂單不存在");
            }
            
            if (!order.getUserId().equals(userId)) {
                return ApiResponse.error("無權限操作此訂單");
            }
            
            if (!canCancelOrder(order.getOrderStatus())) {
                return ApiResponse.error("當前狀態不允許取消訂單");
            }
            
            // 更新訂單狀態
            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setCancelReason(reason);
            order.setCancelTime(LocalDateTime.now());
            orderMapper.updateById(order);
            
            // 如果是賣出訂單，解凍USDT
            if (order.getOrderType() == OrderType.SELL) {
                Wallet wallet = walletMapper.selectByUserId(userId);
                if (wallet != null) {
                    wallet.setUsdtBalance(wallet.getUsdtBalance().add(order.getUsdtAmount()));
                    wallet.setFrozenUsdt(wallet.getFrozenUsdt().subtract(order.getUsdtAmount()));
                    walletMapper.updateById(wallet);
                }
            }
            
            // 記錄審計日誌
            auditLogService.logUserAction(userId, "CANCEL_ORDER", 
                "取消訂單: " + order.getOrderNumber() + ", 原因: " + reason, clientIp, "");
            
            // 發送通知
            notificationService.sendOrderNotification(userId, orderId, 
                "訂單已取消", "取消原因: " + reason);
            
            return ApiResponse.success("訂單已取消");
            
        } catch (Exception e) {
            log.error("取消訂單失敗: orderId={}, userId={}", orderId, userId, e);
            return ApiResponse.error("取消訂單失敗");
        }
    }

    @Override
    public ApiResponse<Map<String, Object>> getOrderDetail(Long orderId, Long userId) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return ApiResponse.error("訂單不存在");
            }
            
            if (!order.getUserId().equals(userId)) {
                return ApiResponse.error("無權限查看此訂單");
            }
            
            Map<String, Object> result = buildOrderDetailMap(order);
            return ApiResponse.success("獲取訂單詳情成功", result);
            
        } catch (Exception e) {
            log.error("獲取訂單詳情失敗: orderId={}, userId={}", orderId, userId, e);
            return ApiResponse.error("獲取訂單詳情失敗");
        }
    }

    @Override
    public ApiResponse<Page<Map<String, Object>>> getUserOrders(Long userId, int pageNum, int pageSize, 
            String orderType, String status, String startDate, String endDate) {
        try {
            Page<Order> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            
            wrapper.eq(Order::getUserId, userId);
            
            if (orderType != null && !orderType.isEmpty()) {
                wrapper.eq(Order::getOrderType, OrderType.valueOf(orderType.toUpperCase()));
            }
            
            if (status != null && !status.isEmpty()) {
                wrapper.eq(Order::getStatus, OrderStatus.valueOf(status.toUpperCase()));
            }
            
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
                .map(this::buildOrderSummaryMap)
                .toList());
            
            return ApiResponse.success("獲取訂單列表成功", resultPage);
            
        } catch (Exception e) {
            log.error("獲取用戶訂單失敗: userId={}", userId, e);
            return ApiResponse.error("獲取訂單列表失敗");
        }
    }

    @Override
    public ApiResponse<Map<String, Object>> getTradingStatistics(Long userId, String period) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 這裡可以根據period參數查詢不同時間範圍的統計數據
            // 暫時返回模擬數據
            stats.put("totalTrades", 0);
            stats.put("totalVolume", BigDecimal.ZERO);
            stats.put("avgOrderSize", BigDecimal.ZERO);
            stats.put("successRate", BigDecimal.ZERO);
            
            return ApiResponse.success("獲取交易統計成功", stats);
            
        } catch (Exception e) {
            log.error("獲取交易統計失敗: userId={}, period={}", userId, period, e);
            return ApiResponse.error("獲取交易統計失敗");
        }
    }

    @Override
    public ApiResponse<Map<String, Object>> getPaymentMethods() {
        try {
            Map<String, Object> methods = new HashMap<>();
            methods.put("bank_transfer", Map.of("name", "銀行轉帳", "enabled", true, "fee", "0%"));
            methods.put("convenience_store", Map.of("name", "超商付款", "enabled", true, "fee", "1%"));
            methods.put("online_payment", Map.of("name", "線上支付", "enabled", true, "fee", "2%"));
            
            return ApiResponse.success("獲取支付方式成功", methods);
            
        } catch (Exception e) {
            log.error("獲取支付方式失敗", e);
            return ApiResponse.error("獲取支付方式失敗");
        }
    }

    @Override
    public ApiResponse<Map<String, Object>> getTradingLimits(Long userId) {
        try {
            UserKyc userKyc = userKycMapper.selectByUserId(userId);
            Map<String, Object> limits = new HashMap<>();
            
            if (userKyc == null || userKyc.getStatus() != KycStatus.APPROVED) {
                limits.put("minBuy", 0);
                limits.put("maxBuy", 0);
                limits.put("minSell", 0);
                limits.put("maxSell", 0);
                limits.put("dailyRemaining", 0);
                limits.put("message", "請先完成KYC驗證");
            } else {
                // 根據KYC等級設置限額
                int kycLevel = userKyc.getLevel() != null ? userKyc.getLevel() : 1;
                
                switch (kycLevel) {
                    case 1:
                        limits.put("minBuy", 10);
                        limits.put("maxBuy", 10000);
                        limits.put("minSell", 1);
                        limits.put("maxSell", 100);
                        limits.put("dailyRemaining", 10000);
                        break;
                    case 2:
                        limits.put("minBuy", 10);
                        limits.put("maxBuy", 100000);
                        limits.put("minSell", 1);
                        limits.put("maxSell", 1000);
                        limits.put("dailyRemaining", 100000);
                        break;
                    default:
                        limits.put("minBuy", 10);
                        limits.put("maxBuy", 500000);
                        limits.put("minSell", 1);
                        limits.put("maxSell", 5000);
                        limits.put("dailyRemaining", 500000);
                        break;
                }
            }
            
            return ApiResponse.success("獲取交易限額成功", limits);
            
        } catch (Exception e) {
            log.error("獲取交易限額失敗: userId={}", userId, e);
            return ApiResponse.error("獲取交易限額失敗");
        }
    }

    // 私有輔助方法
    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.valueOf((int) (Math.random() * 10000));
        return "ORD" + timestamp + String.format("%04d", Integer.parseInt(random));
    }

    private ApiResponse<Map<String, Object>> checkTradingLimits(Long userId, BigDecimal amount, OrderType orderType) {
        ApiResponse<Map<String, Object>> limitsResponse = getTradingLimits(userId);
        if (!limitsResponse.isSuccess()) {
            return limitsResponse;
        }
        
        Map<String, Object> limits = limitsResponse.getData();
        
        if (orderType == OrderType.BUY) {
            BigDecimal minBuy = new BigDecimal(limits.get("minBuy").toString());
            BigDecimal maxBuy = new BigDecimal(limits.get("maxBuy").toString());
            
            if (amount.compareTo(minBuy) < 0) {
                return ApiResponse.error("買入金額不能低於 " + minBuy + " TWD");
            }
            
            if (amount.compareTo(maxBuy) > 0) {
                return ApiResponse.error("買入金額不能超過 " + maxBuy + " TWD");
            }
        } else {
            BigDecimal minSell = new BigDecimal(limits.get("minSell").toString());
            BigDecimal maxSell = new BigDecimal(limits.get("maxSell").toString());
            
            if (amount.compareTo(minSell) < 0) {
                return ApiResponse.error("賣出數量不能低於 " + minSell + " USDT");
            }
            
            if (amount.compareTo(maxSell) > 0) {
                return ApiResponse.error("賣出數量不能超過 " + maxSell + " USDT");
            }
        }
        
        return ApiResponse.success();
    }

    private boolean canCancelOrder(OrderStatus status) {
        return status == OrderStatus.PENDING || status == OrderStatus.PROCESSING;
    }

    private Map<String, Object> buildOrderDetailMap(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("orderNumber", order.getOrderNumber());
        map.put("orderType", order.getOrderType());
        map.put("amount", order.getAmount());
        map.put("usdtAmount", order.getUsdtAmount());
        map.put("price", order.getPrice());
        map.put("status", order.getStatus());
        map.put("paymentMethod", order.getPaymentMethod());
        map.put("receivingAccount", order.getReceivingAccount());
        map.put("receivingBank", order.getReceivingBank());
        map.put("createdAt", order.getCreatedAt());
        map.put("paymentConfirmTime", order.getPaymentConfirmTime());
        map.put("completedAt", order.getCompletedAt());
        map.put("cancelReason", order.getCancelReason());
        return map;
    }

    private Map<String, Object> buildOrderSummaryMap(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("orderNumber", order.getOrderNumber());
        map.put("orderType", order.getOrderType());
        map.put("amount", order.getAmount());
        map.put("usdtAmount", order.getUsdtAmount());
        map.put("price", order.getPrice());
        map.put("status", order.getStatus());
        map.put("createdAt", order.getCreatedAt());
        return map;
    }

    private BigDecimal getSystemConfigDecimal(String key, BigDecimal defaultValue) {
        SystemConfig config = systemConfigMapper.findByConfigKey(key);
        if (config != null) {
            try {
                return new BigDecimal(config.getConfigValue());
            } catch (NumberFormatException e) {
                log.warn("系統配置數值格式錯誤: key={}, value={}", key, config.getConfigValue());
            }
        }
        return defaultValue;
    }

    private int getSystemConfigInt(String key, int defaultValue) {
        SystemConfig config = systemConfigMapper.findByConfigKey(key);
        if (config != null) {
            try {
                return Integer.parseInt(config.getConfigValue());
            } catch (NumberFormatException e) {
                log.warn("系統配置數值格式錯誤: key={}, value={}", key, config.getConfigValue());
            }
        }
        return defaultValue;
    }

    private long getSecondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return java.time.Duration.between(now, midnight).getSeconds();
    }
}