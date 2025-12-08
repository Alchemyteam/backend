-- ============================================
-- V11: Update cart_items table to use sales_data.id instead of products.id
-- ============================================

-- 1. 删除旧的外键约束（如果存在）
SET @fk_exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
                   WHERE TABLE_SCHEMA = 'ecoschema' 
                   AND TABLE_NAME = 'cart_items' 
                   AND CONSTRAINT_NAME = 'fk_cart_product'
                   AND CONSTRAINT_TYPE = 'FOREIGN KEY');

SET @sqlstmt := IF(@fk_exists > 0,
    'ALTER TABLE ecoschema.cart_items DROP FOREIGN KEY fk_cart_product',
    'SELECT "Foreign key fk_cart_product does not exist" AS message');

PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 删除旧的唯一约束（如果存在）
SET @uk_exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
                   WHERE TABLE_SCHEMA = 'ecoschema' 
                   AND TABLE_NAME = 'cart_items' 
                   AND CONSTRAINT_NAME = 'uk_user_product'
                   AND CONSTRAINT_TYPE = 'UNIQUE');

SET @sqlstmt2 := IF(@uk_exists > 0,
    'ALTER TABLE ecoschema.cart_items DROP INDEX uk_user_product',
    'SELECT "Unique key uk_user_product does not exist" AS message');

PREPARE stmt2 FROM @sqlstmt2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 3. 修改 product_id 列类型从 VARCHAR(255) 改为 BIGINT
-- 注意：如果表中有数据，需要先清空或转换数据
ALTER TABLE ecoschema.cart_items 
MODIFY COLUMN product_id BIGINT NOT NULL COMMENT 'Product ID (foreign key references sales_data.id)';

-- 4. 添加新的外键约束，指向 sales_data 表
ALTER TABLE ecoschema.cart_items 
ADD CONSTRAINT fk_cart_sales_data 
FOREIGN KEY (product_id) 
REFERENCES ecoschema.sales_data(id) 
ON DELETE CASCADE;

-- 5. 重新添加唯一约束
ALTER TABLE ecoschema.cart_items 
ADD UNIQUE KEY uk_user_product (user_id, product_id);

