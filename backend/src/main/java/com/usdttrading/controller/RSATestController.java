package com.usdttrading.controller;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController  
public class RSATestController {
    
    @GetMapping("/api/test/rsa-key")
    public Map<String, Object> getRSAKey() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Master Agent RSA端点");
        
        Map<String, String> data = new HashMap<>();
        data.put("publicKey", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2/qUH3yNrObL6sk74LN1RnKWR+JOP7ofruaY/m8J6HSU8ym9wVp01kCX0Vyy/XxgOeXJS99RUCaH6U8r0FCMxnhB+h2fr6ZO7pcn5u4/oNmY3o0zUcD3/QSWF1DEzX2w5TQgE/LjLnILBEJb7zwgPjG6tAjLoWhDSWlotY0RfYw3XmHZxH+dQi8Np9eXgOLak43JZ4ZMMmEmCl7V8uIbItEhWTD/hEGLyh2Skws2uTtX4YvTbQk8CPgZA0628+veYuiUlvHJOmYLfECH5jpuDLzwaiSX68xF2QLx6ZkpSzV31UTE+cOZjJWJeHYTw3iLTX2XkQUaQxwZv5p+lLGBCwIDAQAB");
        data.put("keyType", "RSA");
        data.put("keySize", "2048");
        
        result.put("data", data);
        return result;
    }
    
    @GetMapping("/api/admin/auth/public-key")
    public Map<String, Object> getAdminPublicKey() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "获取RSA公钥成功");
        
        Map<String, String> data = new HashMap<>();
        data.put("publicKey", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2/qUH3yNrObL6sk74LN1RnKWR+JOP7ofruaY/m8J6HSU8ym9wVp01kCX0Vyy/XxgOeXJS99RUCaH6U8r0FCMxnhB+h2fr6ZO7pcn5u4/oNmY3o0zUcD3/QSWF1DEzX2w5TQgE/LjLnILBEJb7zwgPjG6tAjLoWhDSWlotY0RfYw3XmHZxH+dQi8Np9eXgOLak43JZ4ZMMmEmCl7V8uIbItEhWTD/hEGLyh2Skws2uTtX4YvTbQk8CPgZA0628+veYuiUlvHJOmYLfECH5jpuDLzwaiSX68xF2QLx6ZkpSzV31UTE+cOZjJWJeHYTw3iLTX2XkQUaQxwZv5p+lLGBCwIDAQAB");
        data.put("keyType", "RSA");
        data.put("keySize", "2048");
        
        result.put("data", data);
        return result;
    }
}
