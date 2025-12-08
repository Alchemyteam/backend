-- ============================================
-- V10: Add id field to sales_data table as primary key
-- ============================================

-- 把 id 自增主键加上，并把原本以 TXNo 为主键的约束去掉
ALTER TABLE ecoschema.sales_data
    ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (id);

-- 如果你希望 TXNo 仍然保持唯一，可以加上这一句（可选）：
-- ALTER TABLE ecoschema.sales_data
--     ADD UNIQUE KEY uk_sales_data_txno (TXNo);
