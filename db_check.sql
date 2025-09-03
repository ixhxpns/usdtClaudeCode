-- 檢查數據庫結構
SHOW DATABASES;

-- 檢查usdt_trading_platform數據庫的表
USE usdt_trading_platform;
SHOW TABLES;

-- 檢查是否存在usdttrading數據庫
CREATE DATABASE IF NOT EXISTS usdttrading;

-- 檢查usdttrading數據庫的表
USE usdttrading;
SHOW TABLES;

-- 檢查admins表是否存在
SHOW TABLES LIKE 'admins';

-- 如果admins表存在，檢查其結構
DESC admins;