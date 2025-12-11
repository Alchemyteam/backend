-- ============================================
-- 修复 Flyway V10 迁移失败（BC SSO 相关）
-- 请在 MySQL 数据库中执行此脚本
-- ============================================

USE ecoschema;

-- 步骤 1: 查看当前的迁移历史
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- 步骤 2: 删除失败的 V10 记录（如果存在）
DELETE FROM flyway_schema_history 
WHERE version = '10' AND success = 0;

-- 步骤 3: 检查 users 表是否已有 bc_user_id 字段
DESCRIBE users;

-- 步骤 4: 如果 bc_user_id 字段不存在，执行以下操作
-- 注意：如果执行报错说字段已存在，可以忽略

-- 4.1 检查字段是否存在，如果不存在则添加
SET @column_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'ecoschema' 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'bc_user_id'
);

-- 4.2 如果字段不存在，添加字段
SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE users ADD COLUMN bc_user_id VARCHAR(255) NULL',
    'SELECT "Column bc_user_id already exists" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4.3 添加索引（如果不存在）
SET @index_exists = (
    SELECT COUNT(*) 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = 'ecoschema' 
    AND TABLE_NAME = 'users' 
    AND INDEX_NAME = 'idx_users_bc_user_id'
);

SET @sql2 = IF(
    @index_exists = 0,
    'CREATE INDEX idx_users_bc_user_id ON users(bc_user_id)',
    'SELECT "Index idx_users_bc_user_id already exists" AS message'
);
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 步骤 5: 验证修复结果
SELECT * FROM flyway_schema_history WHERE version = '10';
DESCRIBE users;

-- 注意：不要手动插入 V10 记录到 flyway_schema_history
-- 因为迁移脚本已被删除，Flyway 不会再尝试执行它
-- 如果字段已添加成功，应用应该可以正常启动


