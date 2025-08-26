package com.usdttrading.enums;

/**
 * 證件類型枚舉
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
public enum IdType {
    ID_CARD("id_card", "身份證"),
    PASSPORT("passport", "護照"),
    DRIVING_LICENSE("driving_license", "駕照");

    private final String code;
    private final String description;

    IdType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static IdType fromCode(String code) {
        for (IdType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}