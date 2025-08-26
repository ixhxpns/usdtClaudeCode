package com.usdttrading.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.usdttrading.enums.Gender;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 用户个人资料实体类
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_profiles")
public class UserProfile extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 名
     */
    private String firstName;

    /**
     * 姓
     */
    private String lastName;

    /**
     * 出生日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    /**
     * 性别
     */
    private Gender gender;

    /**
     * 国家
     */
    private String country;

    /**
     * 省/州
     */
    private String state;

    /**
     * 城市
     */
    private String city;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 邮政编码
     */
    private String postalCode;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 时区
     */
    private String timezone;

    /**
     * 语言偏好
     */
    private String language;

    /**
     * 获取完整姓名
     */
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null) {
            fullName.append(firstName);
        }
        if (lastName != null) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName);
        }
        return fullName.toString();
    }
}