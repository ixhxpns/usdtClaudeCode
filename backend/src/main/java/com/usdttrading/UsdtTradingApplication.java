package com.usdttrading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication(scanBasePackages = {"com.usdttrading"})
@EntityScan(basePackages = {"com.usdttrading.entity"})
@MapperScan(basePackages = {"com.usdttrading.repository"})
public class UsdtTradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsdtTradingApplication.class, args);
    }
    
    // 緊急API端點 - 用於驗證路由功能
    @RestController
    @RequestMapping("/api")
    public static class EmergencyController {
        
        @GetMapping("/emergency/ping")
        public ResponseEntity<?> emergencyPing() {
            return ResponseEntity.ok()
                .body("{\"status\":\"ok\",\"message\":\"Emergency API working\",\"timestamp\":\"" + 
                      System.currentTimeMillis() + "\"}");
        }
        
        @GetMapping("/emergency/health")
        public ResponseEntity<?> emergencyHealth() {
            return ResponseEntity.ok()
                .body("{\"status\":\"UP\",\"service\":\"emergency\"}");
        }
    }
}