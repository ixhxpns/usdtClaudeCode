package com.usdttrading.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.usdttrading.entity.Admin;

/**
 * 管理員服務接口
 * 
 * @author Master Agent
 * @version 1.0.0
 * @since 2025-08-30
 */
public interface AdminService extends IService<Admin> {

    /**
     * 管理員登入驗證
     * @param username 用戶名
     * @param password 密碼
     * @return 管理員信息，登入失敗返回null
     */
    Admin login(String username, String password);

    /**
     * 記錄登入日誌
     * @param adminId 管理員ID
     * @param clientIp 客戶端IP
     */
    void recordLoginLog(Long adminId, String clientIp);

    /**
     * 記錄登出日誌
     * @param adminId 管理員ID
     */
    void recordLogoutLog(Long adminId);

    /**
     * 修改密碼
     * @param adminId 管理員ID
     * @param currentPassword 當前密碼
     * @param newPassword 新密碼
     * @return 是否成功
     */
    boolean changePassword(Long adminId, String currentPassword, String newPassword);

    /**
     * 記錄操作日誌
     * @param adminId 管理員ID
     * @param action 操作動作
     * @param description 操作描述
     */
    void recordActionLog(Long adminId, String action, String description);

    /**
     * 根據用戶名查詢管理員
     * @param username 用戶名
     * @return 管理員信息
     */
    Admin getByUsername(String username);

    /**
     * 增加登入失敗次數
     * @param adminId 管理員ID
     */
    void incrementLoginAttempts(Long adminId);

    /**
     * 重置登入失敗次數
     * @param adminId 管理員ID
     */
    void resetLoginAttempts(Long adminId);

    /**
     * 鎖定帳戶
     * @param adminId 管理員ID
     * @param lockMinutes 鎖定分鐘數
     */
    void lockAccount(Long adminId, int lockMinutes);
}