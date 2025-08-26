package com.usdttrading.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 区块链配置类
 * 
 * @author ArchitectAgent
 * @version 1.0.0
 * @since 2025-08-18
 */
@Configuration
@ConfigurationProperties(prefix = "blockchain.tron")
public class BlockchainConfig {

    /**
     * TRON主网节点URL
     */
    private String mainnetUrl = "https://api.trongrid.io";

    /**
     * TRON测试网节点URL
     */
    private String testnetUrl = "https://api.shasta.trongrid.io";

    /**
     * 是否使用测试网
     */
    private boolean useTestnet = true;

    /**
     * USDT合约地址 (TRC20)
     */
    private String usdtContractAddress = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";

    /**
     * 平台主钱包地址
     */
    private String platformWalletAddress;

    /**
     * 平台主钱包私钥
     */
    private String platformWalletPrivateKey;

    /**
     * Gas费用钱包地址
     */
    private String feeWalletAddress;

    /**
     * Gas费用钱包私钥
     */
    private String feeWalletPrivateKey;

    /**
     * 交易确认数量
     */
    private int confirmationCount = 19;

    /**
     * Gas价格 (sun)
     */
    private long gasPrice = 420000000L;

    /**
     * Gas限制
     */
    private long gasLimit = 1000000L;

    // Getters and Setters
    public String getMainnetUrl() {
        return mainnetUrl;
    }

    public void setMainnetUrl(String mainnetUrl) {
        this.mainnetUrl = mainnetUrl;
    }

    public String getTestnetUrl() {
        return testnetUrl;
    }

    public void setTestnetUrl(String testnetUrl) {
        this.testnetUrl = testnetUrl;
    }

    public boolean isUseTestnet() {
        return useTestnet;
    }

    public void setUseTestnet(boolean useTestnet) {
        this.useTestnet = useTestnet;
    }

    public String getUsdtContractAddress() {
        return usdtContractAddress;
    }

    public void setUsdtContractAddress(String usdtContractAddress) {
        this.usdtContractAddress = usdtContractAddress;
    }

    public String getPlatformWalletAddress() {
        return platformWalletAddress;
    }

    public void setPlatformWalletAddress(String platformWalletAddress) {
        this.platformWalletAddress = platformWalletAddress;
    }

    public String getPlatformWalletPrivateKey() {
        return platformWalletPrivateKey;
    }

    public void setPlatformWalletPrivateKey(String platformWalletPrivateKey) {
        this.platformWalletPrivateKey = platformWalletPrivateKey;
    }

    public String getFeeWalletAddress() {
        return feeWalletAddress;
    }

    public void setFeeWalletAddress(String feeWalletAddress) {
        this.feeWalletAddress = feeWalletAddress;
    }

    public String getFeeWalletPrivateKey() {
        return feeWalletPrivateKey;
    }

    public void setFeeWalletPrivateKey(String feeWalletPrivateKey) {
        this.feeWalletPrivateKey = feeWalletPrivateKey;
    }

    public int getConfirmationCount() {
        return confirmationCount;
    }

    public void setConfirmationCount(int confirmationCount) {
        this.confirmationCount = confirmationCount;
    }

    public long getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(long gasPrice) {
        this.gasPrice = gasPrice;
    }

    public long getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(long gasLimit) {
        this.gasLimit = gasLimit;
    }

    /**
     * 获取当前使用的节点URL
     */
    public String getNodeUrl() {
        return useTestnet ? testnetUrl : mainnetUrl;
    }
}