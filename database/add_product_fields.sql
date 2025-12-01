-- ============================================
-- 为 products 表添加新字段
-- 用于匹配前端 BuyerProduct 接口
-- ============================================

USE ecoschema;

-- 添加历史最低价字段
ALTER TABLE products 
ADD COLUMN IF NOT EXISTS historical_low_price DECIMAL(10,2) DEFAULT NULL COMMENT '历史最低价' AFTER updated_at;

-- 添加最后交易价格字段
ALTER TABLE products 
ADD COLUMN IF NOT EXISTS last_transaction_price DECIMAL(10,2) DEFAULT NULL COMMENT '最后交易价格' AFTER historical_low_price;

-- 更新现有产品的历史最低价（如果为空，设置为当前价格）
UPDATE products 
SET historical_low_price = price 
WHERE historical_low_price IS NULL AND price IS NOT NULL;

-- 验证字段是否添加成功
-- DESCRIBE products;

