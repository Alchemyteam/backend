-- 使用你的数据库（请替换为你的数据库名）
-- USE your_database_name;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 如果需要插入测试数据，可以取消下面的注释
-- INSERT INTO users (id, email, password, name, created_at, updated_at) 
-- VALUES 
-- ('test-user-1', 'test1@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Test User 1', NOW(), NOW()),
-- ('test-user-2', 'test2@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Test User 2', NOW(), NOW());

