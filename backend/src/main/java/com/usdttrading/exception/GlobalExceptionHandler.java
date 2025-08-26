package com.usdttrading.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.usdttrading.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 全局異常處理器
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 處理業務異常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("業務異常 - URL: {}, 錯誤: {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.ok(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * 處理未登錄異常
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        log.warn("未登錄訪問 - URL: {}, 錯誤: {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(40001, "請先登錄"));
    }

    /**
     * 處理無權限異常
     */
    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        log.warn("無權限訪問 - URL: {}, 所需權限: {}", request.getRequestURI(), e.getPermission());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(40003, "權限不足"));
    }

    /**
     * 處理無角色異常
     */
    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotRoleException(NotRoleException e, HttpServletRequest request) {
        log.warn("無角色權限訪問 - URL: {}, 所需角色: {}", request.getRequestURI(), e.getRole());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(40003, "角色權限不足"));
    }

    /**
     * 處理參數校驗異常 - @RequestBody
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("參數校驗失敗: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(40000, "參數校驗失敗", errors));
    }

    /**
     * 處理參數綁定異常 - @ModelAttribute
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBindException(BindException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("參數綁定失敗: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(40000, "參數綁定失敗", errors));
    }

    /**
     * 處理約束違反異常 - @RequestParam
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(ConstraintViolationException e) {
        Map<String, String> errors = new HashMap<>();
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        
        for (ConstraintViolation<?> violation : violations) {
            String path = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(path, message);
        }
        
        log.warn("約束違反: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(40000, "參數約束違反", errors));
    }

    /**
     * 處理非法參數異常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法參數 - URL: {}, 錯誤: {}", request.getRequestURI(), e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(40000, "參數錯誤: " + e.getMessage()));
    }

    /**
     * 處理空指針異常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指針異常 - URL: {}", request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(50000, "系統內部錯誤"));
    }

    /**
     * 處理運行時異常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("運行時異常 - URL: {}", request.getRequestURI(), e);
        
        // 如果是已知的業務異常，返回具體錯誤信息
        if (e.getMessage() != null && e.getMessage().contains("Token")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(40001, e.getMessage()));
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(50000, "系統運行異常"));
    }

    /**
     * 處理其他所有異常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        log.error("未處理異常 - URL: {}, 異常類型: {}", request.getRequestURI(), e.getClass().getSimpleName(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(50000, "系統異常，請聯繫管理員"));
    }
}