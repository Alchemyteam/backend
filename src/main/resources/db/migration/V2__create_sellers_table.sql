-- ============================================
-- V2: Create sellers table
-- ============================================

CREATE TABLE IF NOT EXISTS sellers (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT 'Seller ID (UUID)',
    name VARCHAR(255) NOT NULL COMMENT 'Seller name',
    verified TINYINT(1) DEFAULT 0 COMMENT 'Verification status: 1-verified, 0-not verified',
    rating DECIMAL(3,2) DEFAULT 0.00 COMMENT 'Seller rating (0-5)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    
    INDEX idx_verified (verified) COMMENT 'Verification status index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sellers table';

