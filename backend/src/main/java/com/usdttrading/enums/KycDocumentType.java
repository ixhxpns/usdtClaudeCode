package com.usdttrading.enums;

/**
 * KYC文件類型枚舉
 * 
 * @author BackendAgent
 * @version 1.0.0
 * @since 2025-08-19
 */
public enum KycDocumentType {
    ID_FRONT("id_front", "身份證正面"),
    ID_BACK("id_back", "身份證反面"),
    SELFIE("selfie", "手持身份證自拍照"),
    PASSPORT("passport", "護照"),
    DRIVER_LICENSE("driver_license", "駕駛執照"),
    BANK_STATEMENT("bank_statement", "銀行對帳單"),
    UTILITY_BILL("utility_bill", "水電煤賬單"),
    PROOF_OF_ADDRESS("proof_of_address", "地址證明"),
    INCOME_CERTIFICATE("income_certificate", "收入證明"),
    OTHER("other", "其他");

    private final String code;
    private final String description;

    KycDocumentType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static KycDocumentType fromCode(String code) {
        for (KycDocumentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 檢查是否為必需文件
     */
    public boolean isRequired() {
        return this == ID_FRONT || this == ID_BACK || this == SELFIE;
    }

    /**
     * 檢查是否為身份證明文件
     */
    public boolean isIdentityDocument() {
        return this == ID_FRONT || this == ID_BACK || this == PASSPORT || this == DRIVER_LICENSE;
    }

    /**
     * 檢查是否為地址證明文件
     */
    public boolean isAddressProof() {
        return this == UTILITY_BILL || this == BANK_STATEMENT || this == PROOF_OF_ADDRESS;
    }
}