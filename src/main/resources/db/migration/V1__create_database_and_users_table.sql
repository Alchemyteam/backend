-- ============================================
-- V1: Create users table
-- ============================================
-- Note: Database should be created manually or via connection URL
-- Flyway will connect to the database specified in application.yml

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT 'User ID (UUID)',
    email VARCHAR(255) NOT NULL UNIQUE COMMENT 'Email address',
    password VARCHAR(255) NOT NULL COMMENT 'Encrypted password',
    name VARCHAR(255) DEFAULT NULL COMMENT 'User name',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    
    INDEX idx_email (email) COMMENT 'Email index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Users table';

