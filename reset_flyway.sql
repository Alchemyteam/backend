-- ============================================
-- 重置 Flyway 迁移历史
-- 执行此脚本后，Flyway 会重新执行所有迁移
-- ============================================

-- 删除 Flyway 历史记录表
DROP TABLE IF EXISTS ecoschema.flyway_schema_history;

-- 注意：执行此脚本后，重启应用时 Flyway 会：
-- 1. 重新创建 flyway_schema_history 表
-- 2. 重新执行所有迁移文件（如果表已存在，会跳过）

