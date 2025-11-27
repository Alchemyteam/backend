-- 创建数据库
CREATE DATABASE IF NOT EXISTS ecosystemdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE ecosystemdb;

-- 注意：表结构会由 JPA 自动创建（ddl-auto: update）
-- 如果需要手动创建，可以使用以下 SQL：

-- CREATE TABLE IF NOT EXISTS users (
--     id VARCHAR(255) PRIMARY KEY,
--     email VARCHAR(255) NOT NULL UNIQUE,
--     password VARCHAR(255) NOT NULL,
--     name VARCHAR(255),
--     created_at DATETIME NOT NULL,
--     updated_at DATETIME,
--     INDEX idx_email (email)
-- );

