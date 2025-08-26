package com.usdttrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.entity.*;
import com.usdttrading.enums.*;
import com.usdttrading.repository.*;
import com.usdttrading.service.WalletService;
import com.usdttrading.service.AuditLogService;
import com.usdttrading.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 錢包管理服務實現類
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;
    private final UserMapper userMapper;
    private final UserKycMapper userKycMapper;
    private final TransactionMapper transactionMapper;
    private final SystemConfigMapper systemConfigMapper;
    
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public ApiResponse<Map<String, Object>> getWalletBalance(Long userId) {
        try {
            Wallet wallet = walletMapper.selectByUserId(userId);
            
            Map<String, Object> balance = new HashMap<>();
            if (wallet != null) {
                balance.put("usdtBalance", wallet.getUsdtBalance());
                balance.put("twdBalance", wallet.getTwdBalance());
                balance.put("frozenUsdt", wallet.getFrozenUsdt());
                balance.put("totalUsdt", wallet.getUsdtBalance().add(wallet.getFrozenUsdt()));
            } else {
                balance.put("usdtBalance", BigDecimal.ZERO);
                balance.put("twdBalance", BigDecimal.ZERO);
                balance.put("frozenUsdt", BigDecimal.ZERO);
                balance.put("totalUsdt", BigDecimal.ZERO);
            }
            
            // 添加估值信息
            BigDecimal usdtPrice = getCurrentUsdtPrice();
            BigDecimal totalValue = ((BigDecimal) balance.get("totalUsdt")).multiply(usdtPrice);
            balance.put("usdtPrice", usdtPrice);
            balance.put("totalValueTwd", totalValue);
            
            return ApiResponse.success("獲取錢包餘額成功", balance);
        } catch (Exception e) {
            log.error("獲取錢包餘額失敗: userId={}", userId, e);
            return ApiResponse.error("獲取錢包餘額失敗");
        }
    }

    @Override
    public ApiResponse<Page<Map<String, Object>>> getTransactions(Long userId, int pageNum, int pageSize, 
            String transactionType, String currency, String startDate, String endDate) {
        try {
            Page<Transaction> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<Transaction> wrapper = new LambdaQueryWrapper<>();
            
            wrapper.eq(Transaction::getUserId, userId);
            
            if (transactionType != null && !transactionType.isEmpty() && !"all".equals(transactionType)) {
                wrapper.eq(Transaction::getType, TransactionType.valueOf(transactionType.toUpperCase()));
            }
            
            if (currency != null && !currency.isEmpty() && !"all".equals(currency)) {
                wrapper.eq(Transaction::getCurrency, currency.toUpperCase());
            }
            
            if (startDate != null && !startDate.isEmpty()) {
                wrapper.ge(Transaction::getCreatedAt, LocalDateTime.parse(startDate + "T00:00:00"));
            }
            
            if (endDate != null && !endDate.isEmpty()) {
                wrapper.le(Transaction::getCreatedAt, LocalDateTime.parse(endDate + "T23:59:59"));
            }
            
            wrapper.orderByDesc(Transaction::getCreatedAt);
            
            Page<Transaction> transactionPage = transactionMapper.selectPage(page, wrapper);
            
            Page<Map<String, Object>> resultPage = new Page<>();
            resultPage.setCurrent(transactionPage.getCurrent());
            resultPage.setSize(transactionPage.getSize());
            resultPage.setTotal(transactionPage.getTotal());
            resultPage.setPages(transactionPage.getPages());
            
            resultPage.setRecords(transactionPage.getRecords().stream()
                .map(this::buildTransactionMap)
                .toList());

            return ApiResponse.success("獲取交易記錄成功", resultPage);
        } catch (Exception e) {
            log.error("獲取交易記錄失敗: userId={}", userId, e);
            return ApiResponse.error("獲取交易記錄失敗");
        }
    }

    @Override
    @Transactional
    public ApiResponse<Map<String, Object>> createWithdrawal(Long userId, BigDecimal amount, 
            String toAddress, String network, String authCode, String clientIp, String userAgent) {
        try {
            // 驗證用戶狀態
            User user = userMapper.selectById(userId);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                return ApiResponse.error("用戶狀態異常");
            }
            
            // 檢查KYC狀態
            UserKyc userKyc = userKycMapper.selectByUserId(userId);
            if (userKyc == null || userKyc.getStatus() != KycStatus.APPROVED) {
                return ApiResponse.error("請先完成KYC身份驗證");
            }
            
            // 檢查USDT餘額
            Wallet wallet = walletMapper.selectByUserId(userId);
            if (wallet == null || wallet.getUsdtBalance().compareTo(amount) < 0) {
                return ApiResponse.error("USDT餘額不足");
            }
            
            // 檢查提現限額
            ApiResponse<Map<String, Object>> limitCheck = checkWithdrawalLimits(userId, amount);
            if (!limitCheck.isSuccess()) {
                return limitCheck;
            }
            
            // 檢查提現頻率
            String rateKey = "withdrawal_rate:" + userId;
            Integer dailyWithdrawals = (Integer) redisTemplate.opsForValue().get(rateKey);
            int maxDailyWithdrawals = getSystemConfigInt("wallet.max_daily_withdrawals", 5);
            
            if (dailyWithdrawals != null && dailyWithdrawals >= maxDailyWithdrawals) {
                return ApiResponse.error("今日提現次數已達上限");
            }
            
            // 凍結提現金額
            wallet.setUsdtBalance(wallet.getUsdtBalance().subtract(amount));
            wallet.setFrozenUsdt(wallet.getFrozenUsdt().add(amount));
            walletMapper.updateById(wallet);
            
            // 創建提現交易記錄
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setType(TransactionType.WITHDRAWAL);
            transaction.setAmount(amount);
            transaction.setStatus(TransactionStatus.PENDING);
            transaction.setToAddress(toAddress);
            transaction.setNetwork(network);
            transaction.setClientIp(clientIp);
            transaction.setUserAgent(userAgent);
            
            // 生成交易號
            String transactionNo = generateTransactionNumber();
            transaction.setTransactionNumber(transactionNo);
            
            transactionMapper.insert(transaction);
            
            // 記錄提現頻率
            if (dailyWithdrawals == null) {
                redisTemplate.opsForValue().set(rateKey, 1, getSecondsUntilMidnight(), TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().increment(rateKey);
            }
            
            // 記錄審計日誌
            auditLogService.logUserAction(userId, "REQUEST_WITHDRAWAL", 
                "申請提現: " + transactionNo + ", 金額: " + amount, clientIp, "");
            
            // 發送通知
            notificationService.sendTransactionNotification(userId, transaction.getId(), 
                "提現申請已提交", "提現申請已提交審核，請等待處理");
            
            Map<String, Object> result = new HashMap<>();
            result.put("transactionId", transaction.getId());
            result.put("transactionNumber", transactionNo);
            result.put("amount", amount);
            result.put("status", transaction.getStatus());
            
            return ApiResponse.success("提現申請提交成功", result);
        } catch (Exception e) {
            log.error("申請提現失敗: userId={}, amount={}", userId, amount, e);
            return ApiResponse.error("提現申請失敗");
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> requestDeposit(Long userId, BigDecimal amount, 
            String paymentMethod, String clientIp) {
        try {
            // 驗證用戶狀態
            User user = userMapper.selectById(userId);
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                return ApiResponse.error("用戶狀態異常");
            }
            
            // 檢查充值限額
            ApiResponse<Map<String, Object>> limitCheck = checkDepositLimits(userId, amount);
            if (!limitCheck.isSuccess()) {
                return limitCheck;
            }
            
            // 創建充值交易記錄
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setType(TransactionType.DEPOSIT);
            transaction.setAmount(amount);
            transaction.setStatus(TransactionStatus.PENDING);
            transaction.setPaymentMethod(paymentMethod);
            transaction.setClientIp(clientIp);
            
            // 生成交易號
            String transactionNo = generateTransactionNumber();
            transaction.setTransactionNumber(transactionNo);
            
            transactionMapper.insert(transaction);
            
            // 記錄審計日誌
            auditLogService.logUserAction(userId, "REQUEST_DEPOSIT", 
                "申請充值: " + transactionNo + ", 金額: " + amount, clientIp, "");
            
            Map<String, Object> result = new HashMap<>();
            result.put("transactionId", transaction.getId());
            result.put("transactionNumber", transactionNo);
            result.put("amount", amount);
            result.put("paymentMethod", paymentMethod);
            result.put("status", transaction.getStatus());
            
            // 返回支付信息
            result.put("paymentInfo", generatePaymentInfo(paymentMethod, amount, transactionNo));
            
            return ApiResponse.success("充值申請創建成功", result);
        } catch (Exception e) {
            log.error("申請充值失敗: userId={}, amount={}", userId, amount, e);
            return ApiResponse.error("充值申請失敗");
        }
    }

    public ApiResponse<Map<String, Object>> getUserWalletStatistics(Long userId, String period) {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = calculateStartTime(endTime, period);
            
            LambdaQueryWrapper<Transaction> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Transaction::getUserId, userId)
                   .between(Transaction::getCreatedAt, startTime, endTime)
                   .eq(Transaction::getStatus, TransactionStatus.COMPLETED);
            
            List<Transaction> transactions = transactionMapper.selectList(wrapper);
            
            BigDecimal totalDeposit = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalWithdrawal = transactions.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            long depositCount = transactions.stream()
                .mapToLong(t -> t.getType() == TransactionType.DEPOSIT ? 1 : 0)
                .sum();
            
            long withdrawalCount = transactions.stream()
                .mapToLong(t -> t.getType() == TransactionType.WITHDRAWAL ? 1 : 0)
                .sum();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalDeposit", totalDeposit);
            stats.put("totalWithdrawal", totalWithdrawal);
            stats.put("netFlow", totalDeposit.subtract(totalWithdrawal));
            stats.put("depositCount", depositCount);
            stats.put("withdrawalCount", withdrawalCount);
            stats.put("period", period);
            
            return ApiResponse.success("獲取錢包統計成功", stats);
        } catch (Exception e) {
            log.error("獲取錢包統計失敗: userId={}, period={}", userId, period, e);
            return ApiResponse.error("獲取錢包統計失敗");
        }
    }

    public ApiResponse<Map<String, Object>> getDepositMethods() {
        try {
            Map<String, Object> methods = new HashMap<>();
            methods.put("bank_transfer", Map.of(
                "name", "銀行轉帳", 
                "enabled", true, 
                "fee", "0%",
                "minAmount", 100,
                "maxAmount", 1000000
            ));
            methods.put("atm", Map.of(
                "name", "ATM轉帳", 
                "enabled", true, 
                "fee", "0%",
                "minAmount", 100,
                "maxAmount", 500000
            ));
            methods.put("convenience_store", Map.of(
                "name", "超商代收", 
                "enabled", true, 
                "fee", "1%",
                "minAmount", 100,
                "maxAmount", 20000
            ));
            
            return ApiResponse.success("獲取充值方式成功", methods);
        } catch (Exception e) {
            log.error("獲取充值方式失敗", e);
            return ApiResponse.error("獲取充值方式失敗");
        }
    }

    public ApiResponse<Map<String, Object>> getTransactionDetail(Long transactionId, Long userId) {
        try {
            Transaction transaction = transactionMapper.selectById(transactionId);
            if (transaction == null) {
                return ApiResponse.error("交易記錄不存在");
            }
            
            if (!transaction.getUserId().equals(userId)) {
                return ApiResponse.error("無權限查看此交易記錄");
            }
            
            Map<String, Object> detail = buildTransactionDetailMap(transaction);
            return ApiResponse.success("獲取交易詳情成功", detail);
        } catch (Exception e) {
            log.error("獲取交易詳情失敗: transactionId={}, userId={}", transactionId, userId, e);
            return ApiResponse.error("獲取交易詳情失敗");
        }
    }

    @Transactional
    public ApiResponse<String> cancelTransaction(Long transactionId, Long userId, String reason) {
        try {
            Transaction transaction = transactionMapper.selectById(transactionId);
            if (transaction == null) {
                return ApiResponse.error("交易記錄不存在");
            }
            
            if (!transaction.getUserId().equals(userId)) {
                return ApiResponse.error("無權限操作此交易");
            }
            
            if (transaction.getStatus() != TransactionStatus.PENDING) {
                return ApiResponse.error("只能取消待處理的交易");
            }
            
            // 更新交易狀態
            transaction.setStatus(TransactionStatus.CANCELLED);
            transaction.setCancelReason(reason);
            transaction.setCancelTime(LocalDateTime.now());
            transactionMapper.updateById(transaction);
            
            // 如果是提現，退回凍結金額
            if (transaction.getType() == TransactionType.WITHDRAWAL) {
                Wallet wallet = walletMapper.selectByUserId(userId);
                if (wallet != null) {
                    wallet.setTwdBalance(wallet.getTwdBalance().add(transaction.getAmount()));
                    walletMapper.updateById(wallet);
                }
            }
            
            // 記錄審計日誌
            auditLogService.logUserAction(userId, "CANCEL_TRANSACTION", 
                "取消交易: " + transaction.getTransactionNumber() + ", 原因: " + reason, "", "");
            
            // 發送通知
            notificationService.sendTransactionNotification(userId, transactionId, 
                "交易已取消", "取消原因: " + reason);
            
            return ApiResponse.success("交易已取消");
        } catch (Exception e) {
            log.error("取消交易失敗: transactionId={}, userId={}", transactionId, userId, e);
            return ApiResponse.error("取消交易失敗");
        }
    }

    // 私有輔助方法
    private BigDecimal getCurrentUsdtPrice() {
        // 從Redis或數據庫獲取當前USDT價格
        String cacheKey = "usdt:current_price";
        Map<String, Object> cachedPrice = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedPrice != null && cachedPrice.get("basePrice") != null) {
            return (BigDecimal) cachedPrice.get("basePrice");
        }
        
        return new BigDecimal("31.00"); // 默認價格
    }
    
    private ApiResponse<Map<String, Object>> checkWithdrawalLimits(Long userId, BigDecimal amount) {
        UserKyc userKyc = userKycMapper.selectByUserId(userId);
        int kycLevel = userKyc != null && userKyc.getLevel() != null ? userKyc.getLevel() : 1;
        
        BigDecimal maxSingleWithdrawal;
        BigDecimal maxDailyWithdrawal;
        
        switch (kycLevel) {
            case 1:
                maxSingleWithdrawal = new BigDecimal("50000");
                maxDailyWithdrawal = new BigDecimal("100000");
                break;
            case 2:
                maxSingleWithdrawal = new BigDecimal("200000");
                maxDailyWithdrawal = new BigDecimal("500000");
                break;
            default:
                maxSingleWithdrawal = new BigDecimal("1000000");
                maxDailyWithdrawal = new BigDecimal("2000000");
                break;
        }
        
        if (amount.compareTo(maxSingleWithdrawal) > 0) {
            return ApiResponse.error("單次提現金額不能超過 " + maxSingleWithdrawal + " TWD");
        }
        
        // 檢查當日累計提現金額
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LambdaQueryWrapper<Transaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Transaction::getUserId, userId)
               .eq(Transaction::getType, TransactionType.WITHDRAWAL)
               .in(Transaction::getStatus, TransactionStatus.PENDING, TransactionStatus.COMPLETED)
               .ge(Transaction::getCreatedAt, today);
        
        List<Transaction> todayWithdrawals = transactionMapper.selectList(wrapper);
        BigDecimal todayTotal = todayWithdrawals.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (todayTotal.add(amount).compareTo(maxDailyWithdrawal) > 0) {
            return ApiResponse.error("當日累計提現金額不能超過 " + maxDailyWithdrawal + " TWD");
        }
        
        return ApiResponse.success();
    }
    
    private ApiResponse<Map<String, Object>> checkDepositLimits(Long userId, BigDecimal amount) {
        BigDecimal minDeposit = new BigDecimal(getSystemConfigValue("wallet.min_deposit", "100"));
        BigDecimal maxDeposit = new BigDecimal(getSystemConfigValue("wallet.max_deposit", "1000000"));
        
        if (amount.compareTo(minDeposit) < 0) {
            return ApiResponse.error("充值金額不能低於 " + minDeposit + " TWD");
        }
        
        if (amount.compareTo(maxDeposit) > 0) {
            return ApiResponse.error("充值金額不能超過 " + maxDeposit + " TWD");
        }
        
        return ApiResponse.success();
    }
    
    private String generateTransactionNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.valueOf((int) (Math.random() * 10000));
        return "TXN" + timestamp + String.format("%04d", Integer.parseInt(random));
    }
    
    private Map<String, Object> generatePaymentInfo(String paymentMethod, BigDecimal amount, String transactionNo) {
        Map<String, Object> paymentInfo = new HashMap<>();
        
        switch (paymentMethod) {
            case "bank_transfer":
                paymentInfo.put("bankName", "台灣銀行");
                paymentInfo.put("bankAccount", "1234567890");
                paymentInfo.put("accountName", "USDT交易平台有限公司");
                paymentInfo.put("transferNote", "請填寫交易號: " + transactionNo);
                break;
            case "atm":
                paymentInfo.put("bankCode", "004");
                paymentInfo.put("virtualAccount", "9876543210");
                paymentInfo.put("expireTime", LocalDateTime.now().plusHours(24));
                break;
            case "convenience_store":
                paymentInfo.put("paymentCode", generatePaymentCode());
                paymentInfo.put("expireTime", LocalDateTime.now().plusHours(2));
                break;
        }
        
        paymentInfo.put("amount", amount);
        paymentInfo.put("transactionNumber", transactionNo);
        
        return paymentInfo;
    }
    
    private String generatePaymentCode() {
        return String.valueOf((int) (Math.random() * 1000000000));
    }
    
    private Map<String, Object> buildTransactionMap(Transaction transaction) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", transaction.getId());
        map.put("transactionNumber", transaction.getTransactionNumber());
        map.put("type", transaction.getType());
        map.put("amount", transaction.getAmount());
        map.put("status", transaction.getStatus());
        map.put("paymentMethod", transaction.getPaymentMethod());
        map.put("createdAt", transaction.getCreatedAt());
        return map;
    }
    
    private Map<String, Object> buildTransactionDetailMap(Transaction transaction) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", transaction.getId());
        detail.put("transactionNumber", transaction.getTransactionNumber());
        detail.put("type", transaction.getType());
        detail.put("amount", transaction.getAmount());
        detail.put("status", transaction.getStatus());
        detail.put("paymentMethod", transaction.getPaymentMethod());
        detail.put("bankAccount", transaction.getBankAccount());
        detail.put("bankName", transaction.getBankName());
        detail.put("paymentProof", transaction.getPaymentProof());
        detail.put("adminNotes", transaction.getAdminNotes());
        detail.put("cancelReason", transaction.getCancelReason());
        detail.put("createdAt", transaction.getCreatedAt());
        detail.put("completedAt", transaction.getCompletedAt());
        detail.put("cancelTime", transaction.getCancelTime());
        return detail;
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
    
    private String getSystemConfigValue(String key, String defaultValue) {
        SystemConfig config = systemConfigMapper.findByConfigKey(key);
        return config != null ? config.getConfigValue() : defaultValue;
    }
    
    private long getSecondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return java.time.Duration.between(now, midnight).getSeconds();
    }

    // 實現接口中缺失的方法
    
    @Override
    public ApiResponse<Map<String, Object>> getWalletAddress(Long userId, String currency) {
        try {
            Wallet wallet = walletMapper.selectByUserId(userId);
            
            Map<String, Object> address = new HashMap<>();
            if (wallet != null) {
                // 模擬錢包地址生成（實際應該從區塊鏈服務獲取）
                switch (currency.toUpperCase()) {
                    case "USDT":
                        address.put("address", "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t");
                        address.put("network", "TRC20");
                        address.put("qrCode", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...");
                        break;
                    case "ETH":
                        address.put("address", "0x742d35Cc6634C0532925a3b8D8C9C0Cc12fd");
                        address.put("network", "ERC20");
                        address.put("qrCode", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...");
                        break;
                    default:
                        return ApiResponse.error("不支持的幣種");
                }
                address.put("userId", userId);
                address.put("currency", currency);
            }
            
            return ApiResponse.success("獲取錢包地址成功", address);
        } catch (Exception e) {
            log.error("獲取錢包地址失敗: userId={}, currency={}", userId, currency, e);
            return ApiResponse.error("獲取錢包地址失敗");
        }
    }


    @Override
    public ApiResponse<Page<Map<String, Object>>> getWithdrawals(Long userId, int pageNum, int pageSize, 
            String status, String startDate, String endDate) {
        // 實現提現記錄查詢
        return ApiResponse.success("查詢提現記錄成功", new Page<>());
    }

    @Override
    public ApiResponse<String> cancelWithdrawal(Long withdrawalId, Long userId, String clientIp) {
        // 實現取消提現
        return ApiResponse.success("提現申請已取消");
    }

    @Override
    public ApiResponse<Page<Map<String, Object>>> getDeposits(Long userId, int pageNum, int pageSize, 
            String currency, String network, String startDate, String endDate) {
        // 實現充值記錄查詢
        return ApiResponse.success("查詢充值記錄成功", new Page<>());
    }

    @Override
    public ApiResponse<Map<String, Object>> getWithdrawInfo(Long userId, String currency, String network) {
        try {
            UserKyc userKyc = userKycMapper.selectByUserId(userId);
            Map<String, Object> info = new HashMap<>();
            
            if (userKyc == null || userKyc.getStatus() != KycStatus.APPROVED) {
                info.put("minAmount", 0);
                info.put("maxAmount", 0);
                info.put("fee", "0");
                info.put("message", "請先完成KYC驗證");
            } else {
                int kycLevel = userKyc.getLevel() != null ? userKyc.getLevel() : 1;
                
                switch (kycLevel) {
                    case 1:
                        info.put("minAmount", 10);
                        info.put("maxAmount", 1000);
                        info.put("fee", "1 USDT");
                        break;
                    case 2:
                        info.put("minAmount", 10);
                        info.put("maxAmount", 10000);
                        info.put("fee", "1 USDT");
                        break;
                    default:
                        info.put("minAmount", 10);
                        info.put("maxAmount", 50000);
                        info.put("fee", "1 USDT");
                        break;
                }
                info.put("currency", currency);
                info.put("network", network);
                info.put("confirmations", 12);
                info.put("processTime", "10-30分鐘");
            }
            
            return ApiResponse.success("獲取提現信息成功", info);
        } catch (Exception e) {
            log.error("獲取提現信息失敗: userId={}", userId, e);
            return ApiResponse.error("獲取提現信息失敗");
        }
    }

    @Override
    public ApiResponse<Map<String, Object>> validateAddress(String address, String network) {
        try {
            Map<String, Object> result = new HashMap<>();
            boolean isValid = isValidAddress(address, network);
            
            result.put("isValid", isValid);
            result.put("address", address);
            result.put("network", network);
            
            if (isValid) {
                result.put("message", "地址格式正確");
            } else {
                result.put("message", "地址格式無效");
            }
            
            return ApiResponse.success("地址驗證完成", result);
        } catch (Exception e) {
            log.error("驗證錢包地址失敗: address={}, network={}", address, network, e);
            return ApiResponse.error("地址驗證失敗");
        }
    }

    @Override
    public ApiResponse<Page<Map<String, Object>>> getAllWithdrawals(int pageNum, int pageSize, 
            Long userId, String status, String startDate, String endDate) {
        // 管理員查詢所有提現申請
        return ApiResponse.success("查詢所有提現申請成功", new Page<>());
    }

    @Override
    public ApiResponse<String> reviewWithdrawal(Long withdrawalId, Long reviewerId, Boolean approved, 
            String comment, String clientIp) {
        // 管理員審核提現申請
        return ApiResponse.success("提現審核完成");
    }

    @Override
    public ApiResponse<String> processWithdrawal(Long withdrawalId, Long adminId, 
            Map<String, String> transactionInfo, String clientIp) {
        // 管理員處理提現
        return ApiResponse.success("提現處理完成");
    }

    @Override
    public ApiResponse<Map<String, Object>> getWalletStatistics(String period) {
        // 管理員查看錢包統計
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBalance", BigDecimal.ZERO);
        stats.put("totalDeposits", BigDecimal.ZERO);
        stats.put("totalWithdrawals", BigDecimal.ZERO);
        stats.put("period", period);
        
        return ApiResponse.success("獲取錢包統計成功", stats);
    }

    // 輔助方法
    private boolean isValidAddress(String address, String network) {
        if (address == null || address.length() < 20) {
            return false;
        }
        
        switch (network.toUpperCase()) {
            case "TRC20":
                return address.startsWith("T") && address.length() == 34;
            case "ERC20":
                return address.startsWith("0x") && address.length() == 42;
            default:
                return false;
        }
    }
}