package com.usdttrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.controller.PriceController.PriceAlertRequest;
import com.usdttrading.controller.PriceController.PriceStrategyRequest;
import com.usdttrading.dto.ApiResponse;
import com.usdttrading.entity.PriceHistory;
import com.usdttrading.entity.SystemConfig;
import com.usdttrading.repository.PriceHistoryMapper;
import com.usdttrading.repository.SystemConfigMapper;
import com.usdttrading.service.PriceService;
import com.usdttrading.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceServiceImpl implements PriceService {

    private final PriceHistoryMapper priceHistoryMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final AuditLogService auditLogService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public ApiResponse<Map<String, Object>> getCurrentPrice() {
        try {
            String cacheKey = "usdt:current_price";
            Map<String, Object> cachedPrice = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedPrice != null) {
                return ApiResponse.success("獲取價格成功", cachedPrice);
            }

            LambdaQueryWrapper<PriceHistory> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(PriceHistory::getCreatedAt).last("LIMIT 1");
            PriceHistory latestPrice = priceHistoryMapper.selectOne(wrapper);

            Map<String, Object> priceData = buildPriceData(latestPrice);
            redisTemplate.opsForValue().set(cacheKey, priceData, 30, TimeUnit.SECONDS);

            return ApiResponse.success("獲取價格成功", priceData);
        } catch (Exception e) {
            log.error("獲取當前價格失敗", e);
            return ApiResponse.error("獲取價格失敗");
        }
    }

    @Override
    public ApiResponse<Map<String, Object>> getRealtimePriceDetail() {
        try {
            ApiResponse<Map<String, Object>> priceResponse = getCurrentPrice();
            if (!priceResponse.isSuccess()) {
                return priceResponse;
            }

            Map<String, Object> priceData = priceResponse.getData();
            
            // 添加更多實時信息
            priceData.put("volume24h", calculateVolume24h());
            priceData.put("change24h", calculatePriceChange24h());
            priceData.put("high24h", getPrice24hHigh());
            priceData.put("low24h", getPrice24hLow());

            return ApiResponse.success("獲取實時價格詳情成功", priceData);
        } catch (Exception e) {
            log.error("獲取實時價格詳情失敗", e);
            return ApiResponse.error("獲取實時價格詳情失敗");
        }
    }

    @Override
    public ApiResponse<List<Map<String, Object>>> getPriceHistory(String period, String interval) {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = calculateStartTime(endTime, period);
            
            LambdaQueryWrapper<PriceHistory> wrapper = new LambdaQueryWrapper<>();
            wrapper.between(PriceHistory::getCreatedAt, startTime, endTime)
                   .orderByAsc(PriceHistory::getCreatedAt);

            List<PriceHistory> historyList = priceHistoryMapper.selectList(wrapper);
            List<Map<String, Object>> result = historyList.stream()
                .map(this::buildHistoryDataPoint)
                .toList();

            return ApiResponse.success("獲取價格歷史成功", result);
        } catch (Exception e) {
            log.error("獲取價格歷史失敗: period={}, interval={}", period, interval, e);
            return ApiResponse.error("獲取價格歷史失敗");
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> updatePrice(BigDecimal buyPrice, BigDecimal sellPrice, String reason) {
        try {
            // 驗證價格合理性 - 賣出價應該高於買入價（平台賺取差價）
            if (sellPrice.compareTo(buyPrice) <= 0) {
                return ApiResponse.error("賣出價格必須高於買入價格");
            }

            BigDecimal basePrice = buyPrice.add(sellPrice).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            
            // 保存價格歷史
            PriceHistory priceHistory = new PriceHistory();
            priceHistory.setPrice(basePrice);
            priceHistory.setBuyPrice(buyPrice);
            priceHistory.setSellPrice(sellPrice);
            priceHistory.setSource("MANUAL");
            priceHistory.setReason(reason);
            priceHistoryMapper.insert(priceHistory);

            // 清除緩存
            redisTemplate.delete("usdt:current_price");

            // 記錄操作日誌
            auditLogService.logAdminAction(null, "UPDATE_PRICE", 
                "手動更新價格: 買入=" + buyPrice + ", 賣出=" + sellPrice, null, null);

            return ApiResponse.success("價格更新成功");
        } catch (Exception e) {
            log.error("手動更新價格失敗", e);
            return ApiResponse.error("價格更新失敗");
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> updatePriceStrategy(PriceStrategyRequest request) {
        try {
            // 更新系統配置
            updateOrCreateConfig("price.auto_update", request.getAutoUpdate().toString());
            updateOrCreateConfig("price.spread_percent", request.getSpreadPercent().toString());
            
            if (request.getExternalApiUrl() != null) {
                updateOrCreateConfig("price.external_api_url", request.getExternalApiUrl());
            }
            if (request.getUpdateIntervalMinutes() != null) {
                updateOrCreateConfig("price.update_interval_minutes", request.getUpdateIntervalMinutes().toString());
            }
            if (request.getMaxChangePercent() != null) {
                updateOrCreateConfig("price.max_change_percent", request.getMaxChangePercent().toString());
            }

            return ApiResponse.success("價格策略更新成功");
        } catch (Exception e) {
            log.error("更新價格策略失敗", e);
            return ApiResponse.error("價格策略更新失敗");
        }
    }

    @Override
    public ApiResponse<Map<String, Object>> getPriceConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("autoUpdate", getConfigValue("price.auto_update", "true"));
            config.put("spreadPercent", getConfigValue("price.spread_percent", "0.02"));
            config.put("externalApiUrl", getConfigValue("price.external_api_url", ""));
            config.put("updateIntervalMinutes", getConfigValue("price.update_interval_minutes", "5"));
            config.put("maxChangePercent", getConfigValue("price.max_change_percent", "5"));

            return ApiResponse.success("獲取價格配置成功", config);
        } catch (Exception e) {
            log.error("獲取價格配置失敗", e);
            return ApiResponse.error("獲取價格配置失敗");
        }
    }

    @Override
    public ApiResponse<Page<Map<String, Object>>> getPriceUpdateLogs(int pageNum, int pageSize, 
            String startDate, String endDate) {
        try {
            Page<PriceHistory> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<PriceHistory> wrapper = new LambdaQueryWrapper<>();
            
            if (startDate != null && !startDate.isEmpty()) {
                wrapper.ge(PriceHistory::getCreatedAt, LocalDateTime.parse(startDate + "T00:00:00"));
            }
            if (endDate != null && !endDate.isEmpty()) {
                wrapper.le(PriceHistory::getCreatedAt, LocalDateTime.parse(endDate + "T23:59:59"));
            }
            
            wrapper.orderByDesc(PriceHistory::getCreatedAt);
            
            Page<PriceHistory> historyPage = priceHistoryMapper.selectPage(page, wrapper);
            
            Page<Map<String, Object>> resultPage = new Page<>();
            resultPage.setCurrent(historyPage.getCurrent());
            resultPage.setSize(historyPage.getSize());
            resultPage.setTotal(historyPage.getTotal());
            resultPage.setPages(historyPage.getPages());
            
            resultPage.setRecords(historyPage.getRecords().stream()
                .map(this::buildLogEntry)
                .toList());

            return ApiResponse.success("獲取價格更新日誌成功", resultPage);
        } catch (Exception e) {
            log.error("獲取價格更新日誌失敗", e);
            return ApiResponse.error("獲取價格更新日誌失敗");
        }
    }

    @Override
    public ApiResponse<Map<String, Object>> refreshExternalPrice() {
        try {
            // 模擬從外部API獲取價格
            BigDecimal externalPrice = fetchExternalPrice();
            
            if (externalPrice != null) {
                BigDecimal spreadPercent = new BigDecimal(getConfigValue("price.spread_percent", "0.02"));
                BigDecimal spread = externalPrice.multiply(spreadPercent);
                
                BigDecimal buyPrice = externalPrice.subtract(spread).setScale(2, RoundingMode.HALF_UP);
                BigDecimal sellPrice = externalPrice.add(spread).setScale(2, RoundingMode.HALF_UP);
                
                // 保存價格
                PriceHistory priceHistory = new PriceHistory();
                priceHistory.setPrice(externalPrice);
                priceHistory.setBuyPrice(buyPrice);
                priceHistory.setSellPrice(sellPrice);
                priceHistory.setSource("EXTERNAL_API");
                priceHistoryMapper.insert(priceHistory);
                
                // 清除緩存
                redisTemplate.delete("usdt:current_price");
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("price", externalPrice);
                result.put("buyPrice", buyPrice);
                result.put("sellPrice", sellPrice);
                
                return ApiResponse.success("外部價格刷新成功", result);
            } else {
                return ApiResponse.error("無法獲取外部價格數據");
            }
        } catch (Exception e) {
            log.error("刷新外部價格失敗", e);
            return ApiResponse.error("刷新外部價格失敗");
        }
    }

    @Override
    public ApiResponse<String> setPriceAlerts(PriceAlertRequest request) {
        try {
            updateOrCreateConfig("price.alert_enabled", request.getEnabled().toString());
            updateOrCreateConfig("price.alert_threshold", request.getThreshold().toString());
            
            if (request.getNotificationEmail() != null) {
                updateOrCreateConfig("price.alert_email", request.getNotificationEmail());
            }
            if (request.getSmsAlert() != null) {
                updateOrCreateConfig("price.alert_sms", request.getSmsAlert().toString());
            }
            if (request.getCheckIntervalMinutes() != null) {
                updateOrCreateConfig("price.alert_interval", request.getCheckIntervalMinutes().toString());
            }

            return ApiResponse.success("價格預警設置成功");
        } catch (Exception e) {
            log.error("設置價格預警失敗", e);
            return ApiResponse.error("設置價格預警失敗");
        }
    }

    @Override
    public ApiResponse<Map<String, Object>> getPriceStatistics(String period) {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = calculateStartTime(endTime, period);
            
            LambdaQueryWrapper<PriceHistory> wrapper = new LambdaQueryWrapper<>();
            wrapper.between(PriceHistory::getCreatedAt, startTime, endTime);
            
            List<PriceHistory> historyList = priceHistoryMapper.selectList(wrapper);
            
            if (historyList.isEmpty()) {
                Map<String, Object> emptyStats = new HashMap<>();
                emptyStats.put("change", "0.00%");
                emptyStats.put("high", BigDecimal.ZERO);
                emptyStats.put("low", BigDecimal.ZERO);
                emptyStats.put("volume", BigDecimal.ZERO);
                return ApiResponse.success("獲取價格統計成功", emptyStats);
            }
            
            BigDecimal firstPrice = historyList.get(0).getPrice();
            BigDecimal lastPrice = historyList.get(historyList.size() - 1).getPrice();
            BigDecimal change = lastPrice.subtract(firstPrice).divide(firstPrice, 4, RoundingMode.HALF_UP);
            
            BigDecimal high = historyList.stream().map(PriceHistory::getPrice).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal low = historyList.stream().map(PriceHistory::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("change", change.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%");
            stats.put("high", high);
            stats.put("low", low);
            stats.put("volume", calculateVolume24h());
            stats.put("period", period);
            
            return ApiResponse.success("獲取價格統計成功", stats);
        } catch (Exception e) {
            log.error("獲取價格統計失敗: period={}", period, e);
            return ApiResponse.error("獲取價格統計失敗");
        }
    }

    // 私有輔助方法
    private Map<String, Object> buildPriceData(PriceHistory latestPrice) {
        Map<String, Object> priceData = new HashMap<>();
        
        if (latestPrice != null) {
            BigDecimal spreadPercent = new BigDecimal(getConfigValue("price.spread_percent", "0.02"));
            BigDecimal basePrice = latestPrice.getPrice();
            BigDecimal spread = basePrice.multiply(spreadPercent);
            
            priceData.put("buyPrice", basePrice.subtract(spread).setScale(2, RoundingMode.HALF_UP));
            priceData.put("sellPrice", basePrice.add(spread).setScale(2, RoundingMode.HALF_UP));
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
        
        return priceData;
    }
    
    private BigDecimal calculateVolume24h() {
        // 模擬計算24小時成交量
        return new BigDecimal("1250000.00");
    }
    
    private BigDecimal calculatePriceChange24h() {
        // 模擬計算24小時漲跌幅
        return new BigDecimal("1.25");
    }
    
    private BigDecimal getPrice24hHigh() {
        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        LambdaQueryWrapper<PriceHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(PriceHistory::getCreatedAt, startTime);
        
        List<PriceHistory> historyList = priceHistoryMapper.selectList(wrapper);
        return historyList.stream()
            .map(PriceHistory::getPrice)
            .max(BigDecimal::compareTo)
            .orElse(new BigDecimal("31.50"));
    }
    
    private BigDecimal getPrice24hLow() {
        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        LambdaQueryWrapper<PriceHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(PriceHistory::getCreatedAt, startTime);
        
        List<PriceHistory> historyList = priceHistoryMapper.selectList(wrapper);
        return historyList.stream()
            .map(PriceHistory::getPrice)
            .min(BigDecimal::compareTo)
            .orElse(new BigDecimal("30.50"));
    }
    
    private LocalDateTime calculateStartTime(LocalDateTime endTime, String period) {
        return switch (period.toLowerCase()) {
            case "1h" -> endTime.minusHours(1);
            case "4h" -> endTime.minusHours(4);
            case "12h" -> endTime.minusHours(12);
            case "24h", "1d" -> endTime.minusDays(1);
            case "7d" -> endTime.minusDays(7);
            case "30d" -> endTime.minusDays(30);
            default -> endTime.minusHours(24);
        };
    }
    
    private Map<String, Object> buildHistoryDataPoint(PriceHistory history) {
        Map<String, Object> dataPoint = new HashMap<>();
        dataPoint.put("time", history.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        dataPoint.put("price", history.getPrice());
        dataPoint.put("buyPrice", history.getBuyPrice());
        dataPoint.put("sellPrice", history.getSellPrice());
        return dataPoint;
    }
    
    private Map<String, Object> buildLogEntry(PriceHistory history) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("id", history.getId());
        logEntry.put("price", history.getPrice());
        logEntry.put("buyPrice", history.getBuyPrice());
        logEntry.put("sellPrice", history.getSellPrice());
        logEntry.put("source", history.getSource());
        logEntry.put("reason", history.getReason());
        logEntry.put("createdAt", history.getCreatedAt());
        return logEntry;
    }
    
    private BigDecimal fetchExternalPrice() {
        // 模擬從外部API獲取價格
        // 實際實現應該調用真實的API
        Random random = new Random();
        double basePrice = 31.0;
        double variation = (random.nextDouble() - 0.5) * 0.1; // ±5%變化
        return new BigDecimal(basePrice + variation).setScale(2, RoundingMode.HALF_UP);
    }
    
    private void updateOrCreateConfig(String key, String value) {
        SystemConfig config = systemConfigMapper.selectByKey(key);
        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription("自動生成的配置項");
            systemConfigMapper.insert(config);
        } else {
            config.setConfigValue(value);
            systemConfigMapper.updateById(config);
        }
    }
    
    private String getConfigValue(String key, String defaultValue) {
        SystemConfig config = systemConfigMapper.selectByKey(key);
        return config != null ? config.getConfigValue() : defaultValue;
    }
}