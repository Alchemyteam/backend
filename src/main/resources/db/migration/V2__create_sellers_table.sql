-- ============================================
-- V2: Create sellers table
-- ============================================

CREATE TABLE IF NOT EXISTS ecoschema.sellers (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT 'Seller ID (UUID)',
    name VARCHAR(255) NOT NULL COMMENT 'Seller name',
    verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Seller verification status',
    rating DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT 'Seller rating',
    created_at DATETIME NOT NULL COMMENT 'Creation timestamp',
    updated_at DATETIME DEFAULT NULL COMMENT 'Last update timestamp',
    
    INDEX idx_verified (verified) COMMENT 'Verified sellers index',
    INDEX idx_rating (rating) COMMENT 'Rating index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sellers table';

