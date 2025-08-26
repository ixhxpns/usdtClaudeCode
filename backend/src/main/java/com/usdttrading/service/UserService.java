package com.usdttrading.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.usdttrading.entity.User;
import com.usdttrading.entity.UserProfile;
import com.usdttrading.entity.Role;
import com.usdttrading.enums.UserStatus;
import com.usdttrading.exception.BusinessException;
import com.usdttrading.repository.UserMapper;
import com.usdttrading.repository.UserProfileMapper;
import com.usdttrading.repository.RoleMapper;
import com.usdttrading.security.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用戶服務類
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    /**
     * 用戶註冊
     */
    @Transactional
    public User register(String email, String password, String phone) {
        // 檢查郵箱是否已存在
        if (existsByEmail(email)) {
            throw BusinessException.User.EMAIL_ALREADY_EXISTS;
        }

        // 檢查手機號是否已存在
        if (StrUtil.isNotBlank(phone) && existsByPhone(phone)) {
            throw BusinessException.User.PHONE_ALREADY_EXISTS;
        }

        // 驗證密碼強度
        if (!passwordEncoder.isValidPassword(password)) {
            throw BusinessException.User.WEAK_PASSWORD;
        }

        // 創建用戶
        User user = new User();
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(UserStatus.INACTIVE);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setGoogleAuthEnabled(false);
        user.setRoleId(3L); // 默認為普通用戶角色
        user.setLoginAttempts(0);

        // 密碼加密
        String salt = passwordEncoder.generateSalt();
        String encodedPassword = passwordEncoder.encode(password, salt);
        user.setSalt(salt);
        user.setPasswordHash(encodedPassword);

        userMapper.insert(user);

        // 創建用戶檔案
        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setTimezone("UTC");
        profile.setLanguage("en");
        userProfileMapper.insert(profile);

        // 記錄審計日誌
        auditLogService.logUserAction(user.getId(), "user_register", "users", user.getId().toString(), 
                "用戶註冊", null, user);

        log.info("用戶註冊成功: {}", email);
        return user;
    }

    /**
     * 用戶登錄
     */
    @Transactional
    public User login(String email, String password, String clientIp, String userAgent) {
        User user = getByEmail(email);
        if (user == null) {
            throw BusinessException.Auth.INVALID_CREDENTIALS;
        }

        // 檢查賬戶狀態
        if (user.isAccountLocked()) {
            throw BusinessException.Auth.ACCOUNT_LOCKED;
        }

        if (!user.isActive()) {
            throw BusinessException.Auth.ACCOUNT_DISABLED;
        }

        // 驗證密碼
        if (!passwordEncoder.matches(password, user.getSalt(), user.getPasswordHash())) {
            // 登錄失敗，增加嘗試次數
            incrementLoginAttempts(user.getId());
            throw BusinessException.Auth.INVALID_CREDENTIALS;
        }

        // 登錄成功，重置登錄嘗試次數
        resetLoginAttempts(user.getId());

        // 更新最後登錄信息
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        userMapper.updateById(user);

        // Sa-Token 登錄
        StpUtil.login(user.getId());

        // 記錄審計日誌
        auditLogService.logUserAction(user.getId(), "user_login", "users", user.getId().toString(),
                "用戶登錄成功", null, null);

        log.info("用戶登錄成功: {} from IP: {}", email, clientIp);
        return user;
    }

    /**
     * 用戶登出
     */
    public void logout() {
        Long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();

        // 記錄審計日誌
        auditLogService.logUserAction(userId, "user_logout", "users", userId.toString(),
                "用戶登出", null, null);

        log.info("用戶登出: {}", userId);
    }

    /**
     * 根據ID獲取用戶
     */
    public User getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.User.USER_NOT_FOUND;
        }
        return user;
    }

    /**
     * 根據郵箱獲取用戶
     */
    public User getByEmail(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 檢查郵箱是否存在
     */
    public boolean existsByEmail(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        return userMapper.selectCount(wrapper) > 0;
    }

    /**
     * 檢查手機號是否存在
     */
    public boolean existsByPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return false;
        }
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("phone", phone);
        return userMapper.selectCount(wrapper) > 0;
    }

    /**
     * 分頁查詢用戶
     */
    public Page<User> getUsers(int page, int size, String keyword, UserStatus status) {
        Page<User> pageObj = new Page<>(page, size);
        QueryWrapper<User> wrapper = new QueryWrapper<>();

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like("email", keyword).or().like("phone", keyword);
        }

        if (status != null) {
            wrapper.eq("status", status);
        }

        wrapper.orderByDesc("created_at");
        return userMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 更新用戶狀態
     */
    @Transactional
    public void updateUserStatus(Long userId, UserStatus status) {
        User user = getById(userId);
        UserStatus oldStatus = user.getStatus();
        
        user.setStatus(status);
        userMapper.updateById(user);

        // 記錄審計日誌
        auditLogService.logUserAction(StpUtil.getLoginIdAsLong(), "update_user_status", "users", userId.toString(),
                "更新用戶狀態", oldStatus, status);

        log.info("用戶狀態更新: {} {} -> {}", userId, oldStatus, status);
    }

    /**
     * 修改密碼
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);

        // 驗證舊密碼
        if (!passwordEncoder.matches(oldPassword, user.getSalt(), user.getPasswordHash())) {
            throw new BusinessException("舊密碼錯誤");
        }

        // 驗證新密碼強度
        if (!passwordEncoder.isValidPassword(newPassword)) {
            throw BusinessException.User.WEAK_PASSWORD;
        }

        // 更新密碼
        String newSalt = passwordEncoder.generateSalt();
        String newEncodedPassword = passwordEncoder.encode(newPassword, newSalt);
        
        user.setSalt(newSalt);
        user.setPasswordHash(newEncodedPassword);
        userMapper.updateById(user);

        // 記錄審計日誌
        auditLogService.logUserAction(userId, "change_password", "users", userId.toString(),
                "修改密碼", null, null);

        log.info("用戶修改密碼: {}", userId);
    }

    /**
     * 重置密碼
     */
    @Transactional
    public String resetPassword(Long userId) {
        User user = getById(userId);

        // 生成新密碼
        String newPassword = passwordEncoder.generateRandomPassword(12);
        String newSalt = passwordEncoder.generateSalt();
        String newEncodedPassword = passwordEncoder.encode(newPassword, newSalt);

        user.setSalt(newSalt);
        user.setPasswordHash(newEncodedPassword);
        userMapper.updateById(user);

        // 記錄審計日誌
        auditLogService.logUserAction(StpUtil.getLoginIdAsLong(), "reset_password", "users", userId.toString(),
                "重置用戶密碼", null, null);

        log.info("管理員重置用戶密碼: {}", userId);
        return newPassword;
    }

    /**
     * 郵箱驗證
     */
    @Transactional
    public void verifyEmail(Long userId) {
        User user = getById(userId);
        user.setEmailVerified(true);
        userMapper.updateById(user);

        // 記錄審計日誌
        auditLogService.logUserAction(userId, "verify_email", "users", userId.toString(),
                "郵箱驗證成功", null, null);

        log.info("郵箱驗證成功: {}", user.getEmail());
    }

    /**
     * 增加登錄嘗試次數
     */
    private void incrementLoginAttempts(Long userId) {
        User user = userMapper.selectById(userId);
        int attempts = user.getLoginAttempts() + 1;
        user.setLoginAttempts(attempts);

        // 如果嘗試次數達到上限，鎖定賬戶
        if (attempts >= 5) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        }

        userMapper.updateById(user);
        log.warn("用戶登錄失敗次數: {} 次數: {}", userId, attempts);
    }

    /**
     * 重置登錄嘗試次數
     */
    private void resetLoginAttempts(Long userId) {
        User user = userMapper.selectById(userId);
        user.setLoginAttempts(0);
        user.setLockedUntil(null);
        userMapper.updateById(user);
    }
}