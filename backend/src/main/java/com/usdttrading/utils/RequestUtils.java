package com.usdttrading.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

/**
 * 請求工具類
 * 提供HTTP請求相關的工具方法
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Service
public class RequestUtils {

    /**
     * 獲取客戶端真實IP地址（簡化版本，當沒有request時）
     */
    public static String getClientIP() {
        return "127.0.0.1"; // 默認值，實際項目中可以從RequestContextHolder獲取
    }

    /**
     * 獲取用戶代理信息（簡化版本，當沒有request時）
     */
    public static String getUserAgent() {
        return "unknown"; // 默認值
    }

    /**
     * 獲取客戶端真實IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                // 取第一個IP（可能有多個代理）
                return ip.split(",")[0].trim();
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return StrUtil.isNotBlank(remoteAddr) ? remoteAddr : "unknown";
    }

    /**
     * 檢查IP是否有效
     */
    private static boolean isValidIp(String ip) {
        return StrUtil.isNotBlank(ip) && 
               !"unknown".equalsIgnoreCase(ip) && 
               !"0:0:0:0:0:0:0:1".equals(ip);
    }

    /**
     * 獲取用戶代理信息
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String userAgent = request.getHeader("User-Agent");
        return StrUtil.isNotBlank(userAgent) ? userAgent : "unknown";
    }

    /**
     * 獲取請求來源
     */
    public static String getReferer(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        return request.getHeader("Referer");
    }

    /**
     * 檢查是否為AJAX請求
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestedWith);
    }

    /**
     * 獲取請求的完整URL
     */
    public static String getFullRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        
        StringBuffer requestURL = request.getRequestURL();
        String queryString = request.getQueryString();
        
        if (StrUtil.isNotBlank(queryString)) {
            requestURL.append("?").append(queryString);
        }
        
        return requestURL.toString();
    }

    /**
     * 獲取請求體大小
     */
    public static long getContentLength(HttpServletRequest request) {
        if (request == null) {
            return 0;
        }
        
        return request.getContentLengthLong();
    }

    /**
     * 獲取請求的Content-Type
     */
    public static String getContentType(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        
        String contentType = request.getContentType();
        return StrUtil.isNotBlank(contentType) ? contentType : "";
    }

    /**
     * 檢查是否為移動設備請求
     */
    public static boolean isMobileDevice(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        
        String userAgent = getUserAgent(request).toLowerCase();
        String[] mobileAgents = {
            "mobile", "android", "iphone", "ipad", "ipod", "blackberry",
            "windows phone", "opera mini", "iemobile", "wpdesktop"
        };
        
        for (String agent : mobileAgents) {
            if (userAgent.contains(agent)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 獲取瀏覽器信息
     */
    public static String getBrowserInfo(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String userAgent = getUserAgent(request).toLowerCase();
        
        if (userAgent.contains("chrome")) {
            return "Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("safari") && !userAgent.contains("chrome")) {
            return "Safari";
        } else if (userAgent.contains("edge")) {
            return "Edge";
        } else if (userAgent.contains("opera")) {
            return "Opera";
        } else if (userAgent.contains("msie") || userAgent.contains("trident")) {
            return "Internet Explorer";
        }
        
        return "Other";
    }

    /**
     * 獲取操作系統信息
     */
    public static String getOperatingSystem(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String userAgent = getUserAgent(request).toLowerCase();
        
        if (userAgent.contains("windows")) {
            return "Windows";
        } else if (userAgent.contains("mac")) {
            return "macOS";
        } else if (userAgent.contains("linux")) {
            return "Linux";
        } else if (userAgent.contains("android")) {
            return "Android";
        } else if (userAgent.contains("iphone") || userAgent.contains("ipad")) {
            return "iOS";
        }
        
        return "Other";
    }

    /**
     * 生成設備指紋
     * 基於IP、User-Agent等信息生成唯一設備標識
     */
    public static String generateDeviceFingerprint(String userAgent, String clientIp) {
        if (StrUtil.isBlank(userAgent) || StrUtil.isBlank(clientIp)) {
            return "unknown";
        }
        
        String fingerprint = clientIp + "|" + userAgent;
        return DigestUtil.md5Hex(fingerprint);
    }

    /**
     * 檢查是否為可疑請求
     */
    public static boolean isSuspiciousRequest(HttpServletRequest request) {
        if (request == null) {
            return true;
        }
        
        String userAgent = getUserAgent(request);
        String ip = getClientIp(request);
        
        // 檢查空或可疑的User-Agent
        if ("unknown".equals(userAgent) || userAgent.length() < 10) {
            return true;
        }
        
        // 檢查本地IP（開發環境除外）
        if (ip.startsWith("127.0.0.1") || ip.startsWith("0:0:0:0:0:0:0:1")) {
            // 在生產環境中可能需要標記為可疑
            return false;
        }
        
        // 檢查常見的惡意User-Agent
        String[] maliciousAgents = {
            "bot", "crawler", "spider", "scraper", "scanner"
        };
        
        String lowerUserAgent = userAgent.toLowerCase();
        for (String malicious : maliciousAgents) {
            if (lowerUserAgent.contains(malicious)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 獲取請求來源國家/地區
     * 注意：這需要GeoIP數據庫支持，這裡只是示例
     */
    public static String getCountryFromIp(String ip) {
        // 這裡可以集成GeoIP2或其他IP地理位置服務
        // 暫時返回未知
        return "unknown";
    }

    /**
     * 檢查是否為內網IP
     */
    public static boolean isInternalIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        
        // IPv4內網地址段
        return ip.startsWith("192.168.") || 
               ip.startsWith("10.") || 
               ip.startsWith("172.16.") || 
               ip.startsWith("172.17.") || 
               ip.startsWith("172.18.") || 
               ip.startsWith("172.19.") || 
               ip.startsWith("172.2") || 
               ip.startsWith("172.30.") || 
               ip.startsWith("172.31.") ||
               ip.equals("127.0.0.1") ||
               ip.equals("localhost");
    }

    /**
     * 獲取請求協議
     */
    public static String getRequestProtocol(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        // 檢查是否通過代理
        String proto = request.getHeader("X-Forwarded-Proto");
        if (StrUtil.isNotBlank(proto)) {
            return proto;
        }
        
        return request.isSecure() ? "https" : "http";
    }

    /**
     * 獲取請求的主機信息
     */
    public static String getRequestHost(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String host = request.getHeader("X-Forwarded-Host");
        if (StrUtil.isBlank(host)) {
            host = request.getHeader("Host");
        }
        
        return StrUtil.isNotBlank(host) ? host : "unknown";
    }

    /**
     * 檢查請求頻率是否過快
     * 基於簡單的時間窗口檢查
     */
    public static boolean isRequestTooFrequent(long lastRequestTime, long minIntervalMs) {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastRequestTime) < minIntervalMs;
    }
}