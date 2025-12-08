-- ============================================
-- 修复 Flyway V10 迁移失败的 SQL 脚本
-- 请在 MySQL 数据库中执行此脚本
-- ============================================

USE ecoschema;

-- 1. 检查并删除失败的 V10 迁移记录
DELETE FROM flyway_schema_history 
WHERE version = '10' AND success = 0;

-- 2. 检查 sales_data 表是否已有 id 字段
-- 如果执行下面的 DESCRIBE 命令后看到 id 字段，说明已经存在，可以跳过步骤 3-4

-- 3. 如果 id 字段不存在，执行以下操作：
-- 3.1 删除旧主键（如果 TXNo 是主键）
ALTER TABLE sales_data DROP PRIMARY KEY;

-- 3.2 添加 id 字段作为主键
ALTER TABLE sales_data 
ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- 4. 手动标记 V10 迁移为成功（如果步骤 3 已执行）
-- 注意：installed_rank 应该是当前最大 rank + 1
INSERT INTO flyway_schema_history 
(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT 
    COALESCE(MAX(installed_rank), 0) + 1,
    '10',
    'add id to sales data',
    'SQL',
    'V10__add_id_to_sales_data.sql',
    NULL,
    USER(),
    NOW(),
    0,
    1
FROM flyway_schema_history;

-- 5. 验证修复结果
SELECT * FROM flyway_schema_history WHERE version = '10';
DESCRIBE sales_data;

