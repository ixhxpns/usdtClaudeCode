package com.usdttrading.security;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token权限认证接口实现
 * 
 * @author ArchitectAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Slf4j
@Component
public class StpInterfaceImpl implements StpInterface {

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> permissions = new ArrayList<>();
        
        try {
            // 这里应该从数据库中查询用户权限
            // 临时实现，根据用户ID返回不同权限
            long userId = Long.parseLong(loginId.toString());
            
            if (userId == 1) {
                // 管理员权限
                permissions.add("admin");
                permissions.add("user:read");
                permissions.add("user:write");
                permissions.add("order:read");
                permissions.add("order:write");
                permissions.add("wallet:read");
                permissions.add("wallet:write");
                permissions.add("withdrawal:review");
                permissions.add("system:config");
            } else {
                // 普通用户权限
                permissions.add("user");
                permissions.add("profile:read");
                permissions.add("profile:update");
                permissions.add("order:create");
                permissions.add("order:read");
                permissions.add("wallet:read");
            }
            
        } catch (Exception e) {
            log.error("获取用户权限失败, userId: {}", loginId, e);
        }
        
        log.debug("用户 {} 的权限列表: {}", loginId, permissions);
        return permissions;
    }

    /**
     * 返回一个账号所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>();
        
        try {
            // 这里应该从数据库中查询用户角色
            // 临时实现，根据用户ID返回不同角色
            long userId = Long.parseLong(loginId.toString());
            
            if (userId == 1) {
                roles.add("admin");
                roles.add("manager");
            } else {
                roles.add("user");
            }
            
        } catch (Exception e) {
            log.error("获取用户角色失败, userId: {}", loginId, e);
        }
        
        log.debug("用户 {} 的角色列表: {}", loginId, roles);
        return roles;
    }

    /**
     * 检查用户是否拥有指定权限
     */
    public boolean hasPermission(Object loginId, String permission) {
        if (loginId == null || StrUtil.isBlank(permission)) {
            return false;
        }
        
        List<String> permissions = getPermissionList(loginId, null);
        return permissions.contains(permission) || permissions.contains("*");
    }

    /**
     * 检查用户是否拥有指定角色
     */
    public boolean hasRole(Object loginId, String role) {
        if (loginId == null || StrUtil.isBlank(role)) {
            return false;
        }
        
        List<String> roles = getRoleList(loginId, null);
        return roles.contains(role) || roles.contains("admin");
    }
}