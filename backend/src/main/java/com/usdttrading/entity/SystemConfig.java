package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * 系统配置实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_config")
public class SystemConfig extends BaseEntity {

    /**
     * 配置键
     */
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 配置分类
     */
    private String category;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 是否为公开配置(前端可见)
     */
    private Boolean isPublic;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 检查是否为公开配置
     */
    public boolean isPublic() {
        return Boolean.TRUE.equals(isPublic);
    }

    /**
     * 检查是否启用
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * 获取字符串值
     */
    public String getStringValue() {
        return configValue;
    }

    /**
     * 获取数字值
     */
    public Double getNumberValue() {
        try {
            return configValue != null ? Double.parseDouble(configValue) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取布尔值
     */
    public Boolean getBooleanValue() {
        if (configValue == null) {
            return null;
        }
        return "true".equalsIgnoreCase(configValue) || "1".equals(configValue);
    }

    /**
     * 检查是否为字符串类型
     */
    public boolean isStringType() {
        return "string".equals(dataType);
    }

    /**
     * 检查是否为数字类型
     */
    public boolean isNumberType() {
        return "number".equals(dataType);
    }

    /**
     * 检查是否为布尔类型
     */
    public boolean isBooleanType() {
        return "boolean".equals(dataType);
    }

    /**
     * 检查是否为JSON类型
     */
    public boolean isJsonType() {
        return "json".equals(dataType);
    }
}