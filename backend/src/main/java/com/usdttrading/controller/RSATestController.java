package com.usdttrading.controller;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class RSATestController {
    
    @GetMapping("/rsa-key")
    public Map<String, Object> getRSAKey() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Master Agent临时RSA端点");
        
        Map<String, String> data = new HashMap<>();
        data.put("publicKey", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxmTySloYZJcTd0QqsIxyhbcgeliik+16oAW5SRV+WpZWE7SuXtPiynZXPnPcrqSJ3HKcKvdqop9+u6YKpUhFEIOoktqybUhsjWhwfOidSXeoOEkk9Y2MIQYb5ktZFQ25uYP5pOdq5itgJiDRktCkgPD/ujjkSMf+ktJxDLiSGBD3I8aYBULBp4LqWfoeLDw9yhynJJrlmic3ccCO6PFTrovCCMnmw0oAo/WtvO5z06g6S5XcCMj/Z3un2z4I/CJYK/hN7OrscfwYZ7e1f4+4LJhf0JHKCiiYH0sQBSoG9xoBf0qvixWxLmq6rcEZcig3eHYxO1yNhJR98tFYNtQckwIDAQAB");
        data.put("keyType", "RSA");
        data.put("keySize", "2048");
        data.put("server", "Master Agent Temp Fix");
        
        result.put("data", data);
        return result;
    }
}
