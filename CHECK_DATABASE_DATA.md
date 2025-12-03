# 检查数据库中的数据

## 方法 1: 使用 MySQL 命令行

### 连接到数据库
```bash
mysql -u root -pallinton -D ecoschema
```

### 执行查询

#### 1. 检查精确匹配（大小写不敏感，去除空格）
```sql
SELECT COUNT(*) as count
FROM ecoschema.sales_data 
WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER('Site Safety Equipment');
```

#### 2. 检查模糊匹配（包含 "Site Safety Equipment"）
```sql
SELECT COUNT(*) as count
FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` LIKE '%Site Safety Equipment%';
```

#### 3. 查看实际的数据格式和示例
```sql
SELECT 
  `Product Hierarchy 3`,
  LENGTH(`Product Hierarchy 3`) as length,
  TRIM(`Product Hierarchy 3`) as trimmed_value,
  `ItemCode`,
  `ItemName`
FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` LIKE '%Safety%' 
   OR `Product Hierarchy 3` LIKE '%Equipment%'
LIMIT 20;
```

#### 4. 查看所有不同的 Product Hierarchy 3 值
```sql
SELECT DISTINCT 
  `Product Hierarchy 3`,
  COUNT(*) as record_count
FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` IS NOT NULL
GROUP BY `Product Hierarchy 3`
ORDER BY record_count DESC;
```

#### 5. 检查是否有空格或格式问题
```sql
SELECT 
  `Product Hierarchy 3`,
  LENGTH(`Product Hierarchy 3`) as original_length,
  LENGTH(TRIM(`Product Hierarchy 3`)) as trimmed_length,
  CASE 
    WHEN LENGTH(`Product Hierarchy 3`) != LENGTH(TRIM(`Product Hierarchy 3`)) 
    THEN 'Has leading/trailing spaces'
    ELSE 'No leading/trailing spaces'
  END as space_status
FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` LIKE '%Safety%' OR `Product Hierarchy 3` LIKE '%Equipment%'
LIMIT 10;
```

#### 6. 检查十六进制编码（查看是否有隐藏字符）
```sql
SELECT 
  `Product Hierarchy 3`,
  HEX(`Product Hierarchy 3`) as hex_value
FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` LIKE '%Safety%' OR `Product Hierarchy 3` LIKE '%Equipment%'
LIMIT 5;
```

#### 7. 对比 Maintenance Chemicals 和 Site Safety Equipment
```sql
-- 检查 Maintenance Chemicals
SELECT 
  'Maintenance Chemicals' as category,
  COUNT(*) as count
FROM ecoschema.sales_data 
WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER('Maintenance Chemicals')

UNION ALL

-- 检查 Site Safety Equipment
SELECT 
  'Site Safety Equipment' as category,
  COUNT(*) as count
FROM ecoschema.sales_data 
WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER('Site Safety Equipment');
```

#### 8. 检查 Function 字段中是否有相关数据
```sql
SELECT 
  `Function`,
  COUNT(*) as count
FROM ecoschema.sales_data 
WHERE LOWER(TRIM(`Function`)) = LOWER('Site Safety Equipment')
   OR LOWER(TRIM(`Function`)) = LOWER('Maintenance Chemicals')
GROUP BY `Function`;
```

---

## 方法 2: 使用数据库管理工具

### MySQL Workbench / DBeaver / Navicat 等

1. **连接到数据库**
   - Host: `localhost`
   - Port: `3306`
   - Database: `ecoschema`
   - Username: `root`
   - Password: `allinton`

2. **执行上述 SQL 查询**

---

## 方法 3: 使用后端日志

### 查看后端日志输出

当搜索 "Site Safety Equipment" 时，查看后端控制台日志：

```
[INFO] Parsing search query: Site Safety Equipment
[INFO] Detected Category: Site Safety Equipment (matched keyword: site safety equipment)
或
[INFO] Using query as category name: Site Safety Equipment
[INFO] Parsed criteria: MaterialSearchCriteria{productHierarchy3='Site Safety Equipment', ...}
[INFO] Searching by Product Hierarchy 3: Site Safety Equipment
[INFO] Found X results by Product Hierarchy 3
```

如果看到 `Found 0 results`，说明数据库中没有匹配的数据。

---

## 方法 4: 创建一个测试脚本

### 在项目中创建测试文件

创建文件：`database/test_query.sql`

```sql
-- 测试查询：检查 Site Safety Equipment 数据

-- 1. 精确匹配
SELECT 
  'Exact Match (with TRIM)' as test_type,
  COUNT(*) as count
FROM ecoschema.sales_data 
WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER('Site Safety Equipment');

-- 2. 模糊匹配
SELECT 
  'Fuzzy Match (LIKE)' as test_type,
  COUNT(*) as count
FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` LIKE '%Site Safety Equipment%';

-- 3. 查看实际值
SELECT 
  'Actual Values' as test_type,
  `Product Hierarchy 3`,
  `ItemCode`,
  `ItemName`
FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` LIKE '%Safety%' OR `Product Hierarchy 3` LIKE '%Equipment%'
LIMIT 10;

-- 4. 对比 Maintenance Chemicals
SELECT 
  'Maintenance Chemicals' as category,
  COUNT(*) as count
FROM ecoschema.sales_data 
WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER('Maintenance Chemicals');
```

然后执行：
```bash
mysql -u root -pallinton -D ecoschema < database/test_query.sql
```

---

## 快速检查命令（一行）

### Windows CMD
```cmd
mysql -u root -pallinton -D ecoschema -e "SELECT COUNT(*) FROM sales_data WHERE LOWER(TRIM(\`Product Hierarchy 3\`)) = LOWER('Site Safety Equipment');"
```

### PowerShell
```powershell
mysql -u root -pallinton -D ecoschema -e "SELECT COUNT(*) FROM sales_data WHERE LOWER(TRIM(\`Product Hierarchy 3\`)) = LOWER('Site Safety Equipment');"
```

---

## 推荐的检查步骤

### 步骤 1: 快速检查数量
```sql
SELECT COUNT(*) 
FROM ecoschema.sales_data 
WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER('Site Safety Equipment');
```

**如果返回 0**：说明数据库中没有这个值

### 步骤 2: 查看所有相关的值
```sql
SELECT DISTINCT `Product Hierarchy 3`
FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` LIKE '%Safety%' 
   OR `Product Hierarchy 3` LIKE '%Equipment%'
   OR `Product Hierarchy 3` LIKE '%Site%';
```

**查看实际的值格式**，可能不是 "Site Safety Equipment"

### 步骤 3: 查看示例数据
```sql
SELECT 
  `Product Hierarchy 3`,
  `ItemCode`,
  `ItemName`,
  `Function`
FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` LIKE '%Safety%' 
   OR `Product Hierarchy 3` LIKE '%Equipment%'
LIMIT 10;
```

**查看实际的数据**，确认格式是否正确

### 步骤 4: 对比 Maintenance Chemicals
```sql
-- 查看 Maintenance Chemicals 的数据
SELECT 
  `Product Hierarchy 3`,
  `Function`,
  COUNT(*) as count
FROM ecoschema.sales_data 
WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER('Maintenance Chemicals')
   OR LOWER(TRIM(`Function`)) = LOWER('Maintenance Chemicals')
GROUP BY `Product Hierarchy 3`, `Function`;
```

**了解为什么 Maintenance Chemicals 能找到数据**

---

## 常见问题排查

### 问题 1: 返回 0 条记录

**可能原因：**
- 数据库中确实没有这个值
- 值格式不同（如 "Site Safety  Equipment" 中间有多个空格）
- 值在其他字段中（如 `Function` 字段）

**解决方案：**
```sql
-- 检查模糊匹配
SELECT * FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` LIKE '%Safety%Equipment%';

-- 检查 Function 字段
SELECT * FROM ecoschema.sales_data 
WHERE `Function` LIKE '%Safety%Equipment%';
```

### 问题 2: 值格式不同

**检查实际格式：**
```sql
SELECT 
  `Product Hierarchy 3`,
  LENGTH(`Product Hierarchy 3`) as length,
  REPLACE(`Product Hierarchy 3`, ' ', '') as no_spaces
FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` LIKE '%Safety%'
LIMIT 5;
```

### 问题 3: 数据在其他字段

**检查 Function 字段：**
```sql
SELECT 
  `Function`,
  COUNT(*) as count
FROM ecoschema.sales_data 
WHERE `Function` LIKE '%Safety%Equipment%'
GROUP BY `Function`;
```

---

## 执行结果示例

### 如果有数据
```
+------+
| count|
+------+
|   15 |
+------+
```

### 如果没有数据
```
+------+
| count|
+------+
|    0 |
+------+
```

### 查看实际值
```
+--------------------------+----------+----------------------------+
| Product Hierarchy 3      | ItemCode | ItemName                   |
+--------------------------+----------+----------------------------+
| Site Safety Equipment    | TI00040  | LEAKAGE CURRENT CLAMP...  |
| Site Safety Equipment   | TI00041  | SAFETY HELMET              |
+--------------------------+----------+----------------------------+
```

---

## 下一步

根据查询结果：

1. **如果有数据**：检查后端日志，确认搜索逻辑是否正确执行
2. **如果没有数据**：检查实际的值格式，可能需要调整搜索逻辑
3. **如果数据在其他字段**：可能需要同时搜索多个字段

