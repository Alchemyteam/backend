-- ============================================
-- 手动修复 Flyway V10 - 最安全的方法
-- 请根据实际情况选择执行
-- ============================================

USE ecoschema;

-- ============================================
-- 方法 A: 如果 sales_data 表已经有 id 字段
-- ============================================
-- 只需执行以下两行：

-- 1. 删除失败的记录
DELETE FROM flyway_schema_history WHERE version = '10' AND success = 0;

-- 2. 标记为成功
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

-- ============================================
-- 方法 B: 如果 sales_data 表还没有 id 字段
-- ============================================
-- 执行以下步骤：

-- 1. 删除失败的记录
DELETE FROM flyway_schema_history WHERE version = '10' AND success = 0;

-- 2. 删除旧主键（如果 TXNo 是主键）
ALTER TABLE sales_data DROP PRIMARY KEY;

-- 3. 添加 id 字段
ALTER TABLE sales_data 
ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

-- 4. 标记为成功
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

