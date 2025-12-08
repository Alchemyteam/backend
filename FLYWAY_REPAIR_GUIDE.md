# Flyway 修复指南

## 问题描述

如果遇到以下错误：
```
Detected failed migration to version 10 (add product id to sales data).
Please remove any half-completed changes then run repair to fix the schema history.
```

这表示 Flyway 检测到 V10 迁移失败，需要修复。

## 解决方案

### 方法 1：使用 Flyway Repair 命令（推荐）

1. **停止应用**（如果正在运行）

2. **运行 Flyway Repair 命令**：
```bash
# Windows (使用 Maven)
mvn flyway:repair

# 或者直接使用 Flyway CLI（如果已安装）
flyway repair -url=jdbc:mysql://localhost:3306/ecoschema -user=root -password=your_password
```

3. **重新启动应用**

### 方法 2：手动修复数据库

如果方法 1 不起作用，可以手动修复：

1. **连接到数据库**：
```sql
USE ecoschema;
```

2. **检查 flyway_schema_history 表**：
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

3. **删除失败的 V10 记录**（如果存在）：
```sql
DELETE FROM flyway_schema_history WHERE version = '10' AND success = 0;
```

4. **检查 sales_data 表是否已有 id 字段**：
```sql
DESCRIBE sales_data;
```

5. **如果 id 字段不存在，手动执行 V10 迁移**：
```sql
-- 删除旧主键（如果存在）
ALTER TABLE sales_data DROP PRIMARY KEY;

-- 添加 id 字段
ALTER TABLE sales_data 
ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
```

6. **手动标记 V10 为成功**（如果已执行）：
```sql
INSERT INTO flyway_schema_history 
(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES 
(10, '10', 'add id to sales data', 'SQL', 'V10__add_id_to_sales_data.sql', NULL, 'root', NOW(), 0, 1);
```

7. **重新启动应用**

### 方法 3：重置 Flyway（谨慎使用）

⚠️ **警告**：这会删除所有迁移历史记录，只适用于开发环境！

1. **删除 flyway_schema_history 表**：
```sql
DROP TABLE IF EXISTS flyway_schema_history;
```

2. **重新启动应用**（Flyway 会重新创建历史表并执行所有迁移）

## 验证修复

修复后，检查：

1. **查看迁移历史**：
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

2. **确认 sales_data 表结构**：
```sql
DESCRIBE sales_data;
-- 应该看到 id 字段作为主键
```

3. **确认 cart_items 表结构**：
```sql
DESCRIBE cart_items;
-- product_id 应该是 BIGINT 类型
```

## 常见问题

### Q: 执行 V11 时提示外键约束错误
A: 确保 V10 已成功执行，sales_data 表已有 id 字段。

### Q: 修改 product_id 类型时提示数据不兼容
A: 如果 cart_items 表中有数据，需要先清空或转换：
```sql
-- 清空购物车数据（谨慎操作！）
TRUNCATE TABLE cart_items;

-- 或者删除所有购物车项
DELETE FROM cart_items;
```

### Q: 外键约束已存在
A: 删除旧的外键约束：
```sql
ALTER TABLE cart_items DROP FOREIGN KEY fk_cart_product;
```

