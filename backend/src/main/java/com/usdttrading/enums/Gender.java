package com.usdttrading.enums;

/**
 * 性別枚舉
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
public enum Gender {
    MALE("male", "男性"),
    FEMALE("female", "女性"),
    OTHER("other", "其他");

    private final String code;
    private final String description;

    Gender(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static Gender fromCode(String code) {
        for (Gender gender : values()) {
            if (gender.code.equals(code)) {
                return gender;
            }
        }
        return null;
    }
}