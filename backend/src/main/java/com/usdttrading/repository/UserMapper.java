package com.usdttrading.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usdttrading.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户数据访问接口
 * 
 * @author ArchitectAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据邮箱查找用户
     */
    @Select("SELECT u.*, r.name as role_name FROM users u LEFT JOIN roles r ON u.role_id = r.id WHERE u.email = #{email} AND u.deleted = 0")
    User findByEmail(@Param("email") String email);

    /**
     * 根据手机号查找用户
     */
    @Select("SELECT u.*, r.name as role_name FROM users u LEFT JOIN roles r ON u.role_id = r.id WHERE u.phone = #{phone} AND u.deleted = 0")
    User findByPhone(@Param("phone") String phone);

    /**
     * 根据邮箱或手机号查找用户
     */
    @Select("SELECT u.*, r.name as role_name FROM users u LEFT JOIN roles r ON u.role_id = r.id WHERE (u.email = #{account} OR u.phone = #{account}) AND u.deleted = 0")
    User findByEmailOrPhone(@Param("account") String account);

    /**
     * 更新登录信息
     */
    int updateLoginInfo(@Param("userId") Long userId, @Param("loginIp") String loginIp);

    /**
     * 重置登录尝试次数
     */
    int resetLoginAttempts(@Param("userId") Long userId);

    /**
     * 增加登录尝试次数
     */
    int incrementLoginAttempts(@Param("userId") Long userId);

    /**
     * 锁定账户
     */
    int lockAccount(@Param("userId") Long userId, @Param("lockMinutes") int lockMinutes);

    /**
     * 解锁账户
     */
    int unlockAccount(@Param("userId") Long userId);
}