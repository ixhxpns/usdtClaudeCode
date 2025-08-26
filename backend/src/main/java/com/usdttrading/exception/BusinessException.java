package com.usdttrading.exception;

import lombok.Getter;

/**
 * 業務異常類
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final String message;

    public BusinessException(String message) {
        super(message);
        this.code = 40000;
        this.message = message;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /**
     * 常用業務異常構造方法
     */
    public static BusinessException of(String message) {
        return new BusinessException(message);
    }

    public static BusinessException of(Integer code, String message) {
        return new BusinessException(code, message);
    }

    /**
     * 預定義業務異常
     */
    public static class Auth {
        public static final BusinessException INVALID_CREDENTIALS = new BusinessException(40001, "用戶名或密碼錯誤");
        public static final BusinessException ACCOUNT_LOCKED = new BusinessException(40002, "帳號已被鎖定");
        public static final BusinessException ACCOUNT_DISABLED = new BusinessException(40003, "帳號已被禁用");
        public static final BusinessException EMAIL_NOT_VERIFIED = new BusinessException(40004, "郵箱尚未驗證");
        public static final BusinessException TOKEN_EXPIRED = new BusinessException(40101, "登錄已過期，請重新登錄");
        public static final BusinessException INVALID_TOKEN = new BusinessException(40102, "無效的登錄憑證");
        public static final BusinessException NOT_LOGGED_IN = new BusinessException(40101, "用戶未登錄");
        public static final BusinessException INSUFFICIENT_PERMISSION = new BusinessException(40301, "權限不足");
        public static final BusinessException ACCESS_DENIED = new BusinessException(40301, "訪問被拒絕");
    }

    public static class User {
        public static final BusinessException USER_NOT_FOUND = new BusinessException(40401, "用戶不存在");
        public static final BusinessException EMAIL_ALREADY_EXISTS = new BusinessException(40901, "郵箱已存在");
        public static final BusinessException PHONE_ALREADY_EXISTS = new BusinessException(40902, "手機號已存在");
        public static final BusinessException WEAK_PASSWORD = new BusinessException(40003, "密碼強度不足");
    }

    public static class Kyc {
        public static final BusinessException KYC_NOT_FOUND = new BusinessException(40401, "KYC記錄不存在");
        public static final BusinessException KYC_ALREADY_SUBMITTED = new BusinessException(40901, "KYC已提交，請勿重複提交");
        public static final BusinessException KYC_ALREADY_APPROVED = new BusinessException(40902, "KYC已通過審核");
        public static final BusinessException KYC_REQUIRED = new BusinessException(40003, "請先完成KYC驗證");
    }

    public static class Order {
        public static final BusinessException ORDER_NOT_FOUND = new BusinessException(40401, "訂單不存在");
        public static final BusinessException INVALID_ORDER_STATUS = new BusinessException(40003, "訂單狀態無效");
        public static final BusinessException ORDER_EXPIRED = new BusinessException(40008, "訂單已過期");
        public static final BusinessException INSUFFICIENT_BALANCE = new BusinessException(40009, "餘額不足");
    }

    public static class Withdrawal {
        public static final BusinessException WITHDRAWAL_NOT_FOUND = new BusinessException(40401, "提款記錄不存在");
        public static final BusinessException INVALID_WITHDRAWAL_STATUS = new BusinessException(40003, "提款狀態無效");
        public static final BusinessException WITHDRAWAL_LIMIT_EXCEEDED = new BusinessException(40010, "提款金額超出限制");
        public static final BusinessException DAILY_LIMIT_EXCEEDED = new BusinessException(40011, "超出每日提款限額");
    }

    public static class System {
        public static final BusinessException MAINTENANCE_MODE = new BusinessException(50003, "系統維護中，請稍後再試");
        public static final BusinessException RATE_LIMIT_EXCEEDED = new BusinessException(40029, "請求過於頻繁，請稍後再試");
        public static final BusinessException FILE_UPLOAD_FAILED = new BusinessException(50001, "文件上傳失敗");
    }
}