package com.usdttrading.security.annotation;

import java.lang.annotation.*;

/**
 * 權限驗證註解
 * 用於方法級別的權限控制
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    
    /**
     * 需要的權限標識
     */
    String[] value() default {};
    
    /**
     * 邏輯關係 - AND表示需要所有權限，OR表示需要其中一個權限
     */
    Logic logic() default Logic.OR;
    
    /**
     * 權限檢查失敗時的錯誤消息
     */
    String message() default "權限不足，需要相應權限";
    
    /**
     * 邏輯關係枚舉
     */
    enum Logic {
        AND, OR
    }
}