package com.usdttrading.controller;

import com.usdttrading.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 測試控制器 - 用於前後端集成測試
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "http://localhost:80", "http://localhost:443"})
public class TestController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Backend is running");
        data.put("timestamp", System.currentTimeMillis());
        data.put("status", "OK");
        
        return ApiResponse.success("測試連接成功", data);
    }

    @PostMapping("/echo")
    public ApiResponse<Map<String, Object>> echo(@RequestBody Map<String, Object> request) {
        Map<String, Object> data = new HashMap<>();
        data.put("received", request);
        data.put("echo_timestamp", System.currentTimeMillis());
        
        return ApiResponse.success("回聲測試成功", data);
    }

    @GetMapping("/auth-test")
    public ApiResponse<Map<String, Object>> authTest(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        Map<String, Object> data = new HashMap<>();
        data.put("has_auth_header", authorization != null);
        data.put("auth_value", authorization);
        data.put("timestamp", System.currentTimeMillis());
        
        return ApiResponse.success("認證測試完成", data);
    }

    @PostMapping("/simulate-error")
    public ApiResponse<Object> simulateError(@RequestParam(defaultValue = "500") int errorCode) {
        switch (errorCode) {
            case 400:
                return ApiResponse.error(400, "模擬400錯誤 - 請求參數錯誤");
            case 401:
                return ApiResponse.error(401, "模擬401錯誤 - 未授權訪問");
            case 403:
                return ApiResponse.error(403, "模擬403錯誤 - 權限不足");
            case 404:
                return ApiResponse.error(404, "模擬404錯誤 - 資源不存在");
            case 500:
            default:
                return ApiResponse.error(500, "模擬500錯誤 - 服務器內部錯誤");
        }
    }

    @GetMapping("/cors-test")
    public ApiResponse<Map<String, Object>> corsTest(
            @RequestHeader(value = "Origin", required = false) String origin) {
        
        Map<String, Object> data = new HashMap<>();
        data.put("origin", origin);
        data.put("cors_enabled", true);
        data.put("timestamp", System.currentTimeMillis());
        
        return ApiResponse.success("CORS測試完成", data);
    }
}