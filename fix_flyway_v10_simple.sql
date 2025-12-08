-- ============================================
-- 简单修复 Flyway V10 迁移失败的 SQL 脚本
-- 请逐行执行，如果某行报错，检查原因后继续
-- ============================================

USE ecoschema;

-- 步骤 1: 查看当前的迁移历史
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- 步骤 2: 删除失败的 V10 记录（如果存在）
DELETE FROM flyway_schema_history WHERE version = '10' AND success = 0;

-- 步骤 3: 检查 sales_data 表结构
DESCRIBE sales_data;

-- 步骤 4: 如果 id 字段不存在，执行以下操作
-- 注意：如果表已经有主键，先删除旧主键
-- 如果执行报错说主键不存在，可以忽略

-- 4.1 尝试删除旧主键（如果存在）
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS 
     WHERE TABLE_SCHEMA = 'ecoschema' 
     AND TABLE_NAME = 'sales_data' 
     AND CONSTRAINT_TYPE = 'PRIMARY KEY') > 0,
    'ALTER TABLE sales_data DROP PRIMARY KEY',
    'SELECT "No primary key to drop" AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4.2 添加 id 字段（如果不存在）
SET @sql2 = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = 'ecoschema' 
     AND TABLE_NAME = 'sales_data' 
     AND COLUMN_NAME = 'id') = 0,
    'ALTER TABLE sales_data ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST',
    'SELECT "Column id already exists" AS message'
);
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- 步骤 5: 手动标记 V10 迁移为成功
-- 获取当前最大的 installed_rank
SET @max_rank = (SELECT COALESCE(MAX(installed_rank), 0) FROM flyway_schema_history);

-- 插入成功记录
INSERT INTO flyway_schema_history 
(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES 
(@max_rank + 1, '10', 'add id to sales data', 'SQL', 'V10__add_id_to_sales_data.sql', NULL, USER(), NOW(), 0, 1);

-- 步骤 6: 验证结果
SELECT * FROM flyway_schema_history WHERE version = '10';
DESCRIBE sales_data;

