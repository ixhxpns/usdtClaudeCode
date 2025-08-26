package com.usdttrading.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.usdttrading.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色Mapper接口
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据角色名称查找角色
     */
    @Select("SELECT * FROM roles WHERE name = #{name} AND deleted = 0")
    Role findByName(@Param("name") String name);

    /**
     * 获取所有激活的角色
     */
    @Select("SELECT * FROM roles WHERE is_active = 1 AND deleted = 0 ORDER BY created_at ASC")
    List<Role> findActiveRoles();

    /**
     * 根据权限查找角色
     */
    List<Role> findByPermission(@Param("permission") String permission);

    /**
     * 获取用户的角色
     */
    @Select("SELECT r.* FROM roles r INNER JOIN users u ON r.id = u.role_id WHERE u.id = #{userId} AND r.deleted = 0")
    Role findByUserId(@Param("userId") Long userId);
}