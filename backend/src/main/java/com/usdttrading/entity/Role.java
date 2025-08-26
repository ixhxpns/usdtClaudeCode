package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 角色实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("roles")
public class Role extends BaseEntity {

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    private String name;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 权限列表(JSON格式)
     */
    private String permissions;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 权限列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<String> permissionList;

    /**
     * 检查角色是否激活
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }
}