-- ============================================
-- 添加 embedding_text 和 embedding_hash 字段到 sales_data 表
-- ============================================

USE ecoschema;

-- 检查字段是否已存在，如果不存在则添加
SET @dbname = DATABASE();
SET @tablename = "sales_data";
SET @columnname = "embedding_text";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column embedding_text already exists.' AS result;",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " TEXT NULL COMMENT 'Embedding text for AI search (concatenated product description)';")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = "embedding_hash";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 'Column embedding_hash already exists.' AS result;",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(64) NULL COMMENT 'SHA256 hash of embedding_text';")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_embedding_hash ON ecoschema.sales_data(embedding_hash);

SELECT 'Fields added successfully!' AS result;

