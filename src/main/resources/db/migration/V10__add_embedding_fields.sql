-- ============================================
-- V10: Add embedding_text and embedding_hash fields to sales_data table
-- ============================================

ALTER TABLE ecoschema.sales_data
    ADD COLUMN embedding_text TEXT NULL COMMENT 'Embedding text for AI search (concatenated product description)',
    ADD COLUMN embedding_hash VARCHAR(64) NULL COMMENT 'SHA256 hash of embedding_text';

-- Add index on embedding_hash for faster lookups
CREATE INDEX idx_embedding_hash ON ecoschema.sales_data(embedding_hash);

