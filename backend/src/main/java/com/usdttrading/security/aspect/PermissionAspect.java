package com.usdttrading.security.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.usdttrading.entity.User;
import com.usdttrading.exception.BusinessException;
import com.usdttrading.security.annotation.RequiresPermission;
import com.usdttrading.security.annotation.RequiresRole;
import com.usdttrading.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 權限檢查切面
 * 處理 @RequiresRole 和 @RequiresPermission 註解
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final UserService userService;

    /**
     * 角色檢查切面
     */
    @Around("@annotation(com.usdttrading.security.annotation.RequiresRole) || " +
            "@within(com.usdttrading.security.annotation.RequiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        // 檢查是否已登入
        if (!StpUtil.isLogin()) {
            throw BusinessException.Auth.NOT_LOGGED_IN;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 優先檢查方法級註解，然後檢查類級註解
        RequiresRole requiresRole = method.getAnnotation(RequiresRole.class);
        if (requiresRole == null) {
            requiresRole = method.getDeclaringClass().getAnnotation(RequiresRole.class);
        }
        
        if (requiresRole != null) {
            String[] requiredRoles = requiresRole.value();
            if (requiredRoles.length > 0) {
                boolean hasPermission = checkUserRoles(requiredRoles, requiresRole.logic());
                
                if (!hasPermission) {
                    log.warn("用戶 {} 缺少角色權限: {}, 需要: {}", 
                            StpUtil.getLoginIdAsLong(), getCurrentUserRoles(), Arrays.toString(requiredRoles));
                    throw new BusinessException(requiresRole.message());
                }
            }
        }
        
        return joinPoint.proceed();
    }

    /**
     * 權限檢查切面
     */
    @Around("@annotation(com.usdttrading.security.annotation.RequiresPermission) || " +
            "@within(com.usdttrading.security.annotation.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 檢查是否已登入
        if (!StpUtil.isLogin()) {
            throw BusinessException.Auth.NOT_LOGGED_IN;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 優先檢查方法級註解，然後檢查類級註解
        RequiresPermission requiresPermission = method.getAnnotation(RequiresPermission.class);
        if (requiresPermission == null) {
            requiresPermission = method.getDeclaringClass().getAnnotation(RequiresPermission.class);
        }
        
        if (requiresPermission != null) {
            String[] requiredPermissions = requiresPermission.value();
            if (requiredPermissions.length > 0) {
                boolean hasPermission = checkUserPermissions(requiredPermissions, requiresPermission.logic());
                
                if (!hasPermission) {
                    log.warn("用戶 {} 缺少權限: 需要 {}", 
                            StpUtil.getLoginIdAsLong(), Arrays.toString(requiredPermissions));
                    throw new BusinessException(requiresPermission.message());
                }
            }
        }
        
        return joinPoint.proceed();
    }

    /**
     * 檢查用戶角色
     */
    private boolean checkUserRoles(String[] requiredRoles, RequiresRole.Logic logic) {
        try {
            List<String> userRoles = getCurrentUserRoles();
            
            if (logic == RequiresRole.Logic.AND) {
                // AND邏輯：需要擁有所有指定角色
                return Arrays.stream(requiredRoles)
                        .allMatch(userRoles::contains);
            } else {
                // OR邏輯：需要擁有其中一個角色
                return Arrays.stream(requiredRoles)
                        .anyMatch(userRoles::contains);
            }
        } catch (Exception e) {
            log.error("檢查用戶角色時發生錯誤", e);
            return false;
        }
    }

    /**
     * 檢查用戶權限
     */
    private boolean checkUserPermissions(String[] requiredPermissions, RequiresPermission.Logic logic) {
        try {
            List<String> userPermissions = getCurrentUserPermissions();
            
            if (logic == RequiresPermission.Logic.AND) {
                // AND邏輯：需要擁有所有指定權限
                return Arrays.stream(requiredPermissions)
                        .allMatch(userPermissions::contains);
            } else {
                // OR邏輯：需要擁有其中一個權限
                return Arrays.stream(requiredPermissions)
                        .anyMatch(userPermissions::contains);
            }
        } catch (Exception e) {
            log.error("檢查用戶權限時發生錯誤", e);
            return false;
        }
    }

    /**
     * 獲取當前用戶角色列表
     */
    private List<String> getCurrentUserRoles() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        
        // 根據roleId獲取角色名稱
        return getRoleNamesByRoleId(user.getRoleId());
    }

    /**
     * 獲取當前用戶權限列表
     */
    private List<String> getCurrentUserPermissions() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        
        // 根據roleId獲取權限列表
        return getPermissionsByRoleId(user.getRoleId());
    }

    /**
     * 根據角色ID獲取角色名稱
     */
    private List<String> getRoleNamesByRoleId(Long roleId) {
        switch (roleId.intValue()) {
            case 1:
                return Arrays.asList("SUPER_ADMIN", "ADMIN", "USER");
            case 2:
                return Arrays.asList("ADMIN", "USER");
            case 3:
            default:
                return Arrays.asList("USER");
        }
    }

    /**
     * 根據角色ID獲取權限列表
     */
    private List<String> getPermissionsByRoleId(Long roleId) {
        switch (roleId.intValue()) {
            case 1: // SUPER_ADMIN
                return Arrays.asList(
                    // 系統管理
                    "system:config", "system:backup", "system:monitor",
                    // 用戶管理
                    "user:create", "user:read", "user:update", "user:delete", "user:manage",
                    // 訂單管理
                    "order:create", "order:read", "order:update", "order:delete", "order:manage",
                    // 錢包管理
                    "wallet:create", "wallet:read", "wallet:update", "wallet:delete", "wallet:manage",
                    // KYC管理
                    "kyc:review", "kyc:approve", "kyc:reject", "kyc:manage",
                    // 提款管理
                    "withdrawal:review", "withdrawal:approve", "withdrawal:reject", "withdrawal:manage",
                    // 公告管理
                    "announcement:create", "announcement:read", "announcement:update", "announcement:delete",
                    // 審計日誌
                    "audit:read", "audit:export"
                );
            case 2: // ADMIN
                return Arrays.asList(
                    // 用戶管理（受限）
                    "user:read", "user:update", "user:manage",
                    // 訂單管理
                    "order:read", "order:update", "order:manage",
                    // 錢包管理（受限）
                    "wallet:read", "wallet:manage",
                    // KYC管理
                    "kyc:review", "kyc:approve", "kyc:reject",
                    // 提款管理
                    "withdrawal:review", "withdrawal:approve", "withdrawal:reject",
                    // 公告管理（受限）
                    "announcement:create", "announcement:read", "announcement:update",
                    // 審計日誌（受限）
                    "audit:read"
                );
            case 3: // USER
            default:
                return Arrays.asList(
                    // 基本用戶權限
                    "profile:read", "profile:update",
                    "order:create", "order:read",
                    "wallet:read", "wallet:deposit", "wallet:withdraw",
                    "kyc:submit", "kyc:read",
                    "notification:read"
                );
        }
    }

    /**
     * 檢查是否為超級管理員
     */
    public boolean isSuperAdmin() {
        if (!StpUtil.isLogin()) {
            return false;
        }
        
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            User user = userService.getById(userId);
            return user.getRoleId() == 1L;
        } catch (Exception e) {
            log.error("檢查超級管理員權限時發生錯誤", e);
            return false;
        }
    }

    /**
     * 檢查是否為管理員（包括超級管理員）
     */
    public boolean isAdmin() {
        if (!StpUtil.isLogin()) {
            return false;
        }
        
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            User user = userService.getById(userId);
            return user.getRoleId() == 1L || user.getRoleId() == 2L;
        } catch (Exception e) {
            log.error("檢查管理員權限時發生錯誤", e);
            return false;
        }
    }

    /**
     * 檢查當前用戶是否擁有指定角色
     */
    public boolean hasRole(String roleName) {
        if (!StpUtil.isLogin()) {
            return false;
        }
        
        try {
            List<String> userRoles = getCurrentUserRoles();
            return userRoles.contains(roleName);
        } catch (Exception e) {
            log.error("檢查用戶角色時發生錯誤", e);
            return false;
        }
    }

    /**
     * 檢查當前用戶是否擁有指定權限
     */
    public boolean hasPermission(String permission) {
        if (!StpUtil.isLogin()) {
            return false;
        }
        
        try {
            List<String> userPermissions = getCurrentUserPermissions();
            return userPermissions.contains(permission);
        } catch (Exception e) {
            log.error("檢查用戶權限時發生錯誤", e);
            return false;
        }
    }
}