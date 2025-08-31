package com.usdttrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.usdttrading.entity.Admin;
import com.usdttrading.repository.AdminMapper;
import com.usdttrading.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 管理員服務實現類
 * 
 * @author Master Agent
 * @version 1.0.0
 * @since 2025-08-30
 */
@Slf4j
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    @Resource
    private AdminMapper adminMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Admin login(String username, String password) {
        try {
            // 根據用戶名查詢管理員
            Admin admin = getByUsername(username);
            if (admin == null) {
                log.warn("管理員登入失敗: 用戶名不存在 - {}", username);
                return null;
            }

            // 檢查帳戶狀態
            if (!admin.isActive()) {
                log.warn("管理員登入失敗: 帳戶未激活 - {}", username);
                return null;
            }

            // 檢查帳戶是否被鎖定
            if (admin.isAccountLocked()) {
                log.warn("管理員登入失敗: 帳戶被鎖定 - {}", username);
                return null;
            }

            // 驗證密碼
            if (!passwordEncoder.matches(password, admin.getPassword())) {
                log.warn("管理員登入失敗: 密碼錯誤 - {}", username);
                // 增加失敗次數
                incrementLoginAttempts(admin.getId());
                return null;
            }

            // 登入成功，重置失敗次數
            resetLoginAttempts(admin.getId());

            // 更新最後登入信息
            admin.setLastLoginAt(LocalDateTime.now());
            updateById(admin);

            log.info("管理員登入成功: {} (ID: {})", username, admin.getId());
            return admin;

        } catch (Exception e) {
            log.error("管理員登入異常: username={}, error={}", username, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Admin getByUsername(String username) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, username);
        return getOne(wrapper);
    }

    @Override
    @Transactional
    public void incrementLoginAttempts(Long adminId) {
        try {
            Admin admin = getById(adminId);
            if (admin != null) {
                int attempts = admin.getLoginAttempts() == null ? 0 : admin.getLoginAttempts();
                admin.setLoginAttempts(attempts + 1);
                
                // 如果失敗次數達到5次，鎖定帳戶30分鐘
                if (admin.getLoginAttempts() >= 5) {
                    lockAccount(adminId, 30);
                }
                
                updateById(admin);
                log.info("管理員登入失敗次數增加: adminId={}, attempts={}", adminId, admin.getLoginAttempts());
            }
        } catch (Exception e) {
            log.error("增加登入失敗次數異常: adminId={}", adminId, e);
        }
    }

    @Override
    @Transactional
    public void resetLoginAttempts(Long adminId) {
        try {
            Admin admin = getById(adminId);
            if (admin != null) {
                admin.setLoginAttempts(0);
                admin.setLockedUntil(null);
                updateById(admin);
                log.info("管理員登入失敗次數重置: adminId={}", adminId);
            }
        } catch (Exception e) {
            log.error("重置登入失敗次數異常: adminId={}", adminId, e);
        }
    }

    @Override
    @Transactional
    public void lockAccount(Long adminId, int lockMinutes) {
        try {
            Admin admin = getById(adminId);
            if (admin != null) {
                admin.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
                updateById(admin);
                log.warn("管理員帳戶已鎖定: adminId={}, lockMinutes={}", adminId, lockMinutes);
            }
        } catch (Exception e) {
            log.error("鎖定帳戶異常: adminId={}", adminId, e);
        }
    }

    @Override
    @Transactional
    public boolean changePassword(Long adminId, String currentPassword, String newPassword) {
        try {
            Admin admin = getById(adminId);
            if (admin == null) {
                return false;
            }

            // 驗證當前密碼
            if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
                log.warn("修改密碼失敗: 當前密碼錯誤 - adminId={}", adminId);
                return false;
            }

            // 加密新密碼並更新
            admin.setPassword(passwordEncoder.encode(newPassword));
            updateById(admin);
            
            log.info("管理員密碼修改成功: adminId={}", adminId);
            return true;

        } catch (Exception e) {
            log.error("修改密碼異常: adminId={}", adminId, e);
            return false;
        }
    }

    @Override
    public void recordLoginLog(Long adminId, String clientIp) {
        try {
            Admin admin = getById(adminId);
            if (admin != null) {
                admin.setLastLoginAt(LocalDateTime.now());
                admin.setLastLoginIp(clientIp);
                updateById(admin);
            }
            
            // 這裡可以記錄到專門的登入日誌表
            log.info("管理員登入日誌: adminId={}, clientIp={}", adminId, clientIp);
        } catch (Exception e) {
            log.error("記錄登入日誌異常: adminId={}", adminId, e);
        }
    }

    @Override
    public void recordLogoutLog(Long adminId) {
        try {
            // 這裡可以記錄到專門的登出日誌表
            log.info("管理員登出日誌: adminId={}", adminId);
        } catch (Exception e) {
            log.error("記錄登出日誌異常: adminId={}", adminId, e);
        }
    }

    @Override
    public void recordActionLog(Long adminId, String action, String description) {
        try {
            // 這裡可以記錄到專門的操作日誌表
            log.info("管理員操作日誌: adminId={}, action={}, description={}", adminId, action, description);
        } catch (Exception e) {
            log.error("記錄操作日誌異常: adminId={}", adminId, e);
        }
    }
}