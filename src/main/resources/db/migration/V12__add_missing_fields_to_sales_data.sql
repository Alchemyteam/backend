-- ============================================
-- V12: Add missing fields (TXP2, Bundled, Origin) to sales_data table
-- ============================================

-- 注意：如果字段已存在，这些语句会报错，但可以安全忽略

-- 添加 TXP2 字段
ALTER TABLE ecoschema.sales_data 
ADD COLUMN `TXP2` VARCHAR(255) DEFAULT NULL COMMENT 'Transaction price 2' AFTER `TXP1`;

-- 添加 Bundled 字段
ALTER TABLE ecoschema.sales_data 
ADD COLUMN `Bundled` VARCHAR(255) DEFAULT NULL COMMENT 'Bundled' AFTER `UOM`;

-- 添加 Origin 字段
ALTER TABLE ecoschema.sales_data 
ADD COLUMN `Origin` VARCHAR(255) DEFAULT NULL COMMENT 'Origin' AFTER `Bundled`;

