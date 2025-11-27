-- ============================================
-- Ecosystem Backend 数据库初始化脚本
-- 数据库名: eco_db
-- ============================================

-- 使用数据库
USE eco_db;

-- ============================================
-- 1. 创建用户表 (users)
-- ============================================
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT '用户ID (UUID)',
    email VARCHAR(255) NOT NULL UNIQUE COMMENT '邮箱地址',
    password VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    name VARCHAR(255) DEFAULT NULL COMMENT '用户姓名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_email (email) COMMENT '邮箱索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 插入测试数据（可选）
-- ============================================
-- 注意：密码是 'password123' 的 BCrypt 加密结果
-- 如果需要测试，可以取消下面的注释

-- INSERT INTO users (id, email, password, name, created_at, updated_at) 
-- VALUES 
-- (
--     UUID(),
--     'admin@example.com',
--     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
--     '管理员',
--     NOW(),
--     NOW()
-- ),
-- (
--     UUID(),
--     'test@example.com',
--     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
--     '测试用户',
--     NOW(),
--     NOW()
-- );

-- ============================================
-- 3. 验证表结构
-- ============================================
-- SELECT * FROM users;
-- DESCRIBE users;

