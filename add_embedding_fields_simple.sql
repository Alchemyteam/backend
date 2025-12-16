-- ============================================
-- 简单版本：直接添加字段（如果字段已存在会报错，可以忽略）
-- ============================================

USE ecoschema;

-- 添加 embedding_text 字段
ALTER TABLE sales_data
    ADD COLUMN embedding_text TEXT NULL COMMENT 'Embedding text for AI search (concatenated product description)';

-- 添加 embedding_hash 字段
ALTER TABLE sales_data
    ADD COLUMN embedding_hash VARCHAR(64) NULL COMMENT 'SHA256 hash of embedding_text';

-- 添加索引
CREATE INDEX idx_embedding_hash ON sales_data(embedding_hash);

SELECT 'Fields added successfully!' AS result;

