-- ============================================
-- V1: Create users table
-- ============================================

CREATE TABLE IF NOT EXISTS ecoschema.users (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT 'User ID (UUID)',
    email VARCHAR(255) NOT NULL UNIQUE COMMENT 'User email',
    password VARCHAR(255) NOT NULL COMMENT 'User password (hashed)',
    name VARCHAR(255) DEFAULT NULL COMMENT 'User name',
    created_at DATETIME NOT NULL COMMENT 'Creation timestamp',
    updated_at DATETIME DEFAULT NULL COMMENT 'Last update timestamp',
    
    INDEX idx_email (email) COMMENT 'Email index for login'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Users table';

