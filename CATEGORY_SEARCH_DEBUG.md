# 品类搜索问题排查指南

## 问题：搜索 "Site Safety Equipment" 返回空结果

### 已修复的问题

#### 1. **搜索优先级问题** ✅

**问题：**
- 之前的搜索优先级：`hasItemNameKeyword()` 在 `hasCategory()` 之前
- 导致 "Site Safety Equipment" 被识别为物料名称关键字，而不是品类

**修复：**
- 调整搜索优先级为：**品类 > 功能 > 品牌 > 物料名称关键字**
- 确保品类搜索优先执行

#### 2. **品类识别逻辑**

品类识别有两种方式：

**方式1：关键词映射**
```java
"site safety equipment" → "Site Safety Equipment"
"site safety" → "Site Safety Equipment"
"safety equipment" → "Site Safety Equipment"
```

**方式2：智能识别**
```java
如果查询格式像品类名称（首字母大写，多个单词）：
  "Site Safety Equipment" → 直接使用作为品类名称
```

### 当前工作流程

当用户输入 "Site Safety Equipment" 时：

1. **品类识别**
   - 关键词映射：`lowerQuery.contains("site safety equipment")` → `productHierarchy3 = "Site Safety Equipment"`
   - 或智能识别：查询格式匹配 → `productHierarchy3 = "Site Safety Equipment"`

2. **搜索执行**
   - 优先级检查：`hasCategory()` = true → 执行品类搜索
   - SQL 查询：`WHERE LOWER(\`Product Hierarchy 3\`) = LOWER('Site Safety Equipment')`

3. **返回结果**
   - 所有 `Product Hierarchy 3` = "Site Safety Equipment" 的记录

### 排查步骤

#### 1. 检查后端日志

查看控制台输出，确认：

```
[INFO] Parsing search query: Site Safety Equipment
[INFO] Detected Category: Site Safety Equipment (matched keyword: site safety equipment)
或
[INFO] Using query as category name: Site Safety Equipment
[INFO] Parsed criteria: MaterialSearchCriteria{productHierarchy3='Site Safety Equipment', ...}
[INFO] Searching by Product Hierarchy 3: Site Safety Equipment
[INFO] Found X results
```

**如果看到：**
```
[INFO] Searching by ItemName keyword: Site Safety Equipment
```
说明品类识别失败，被识别为物料名称关键字了。

#### 2. 检查数据库中的实际值

直接在数据库中查询，确认：

```sql
-- 查看 Product Hierarchy 3 的实际值
SELECT DISTINCT `Product Hierarchy 3` 
FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` IS NOT NULL
ORDER BY `Product Hierarchy 3`;

-- 检查是否有 "Site Safety Equipment" 的数据
SELECT COUNT(*) 
FROM ecoschema.sales_data 
WHERE LOWER(`Product Hierarchy 3`) = LOWER('Site Safety Equipment');

-- 查看实际的数据格式（可能有空格、大小写等差异）
SELECT DISTINCT `Product Hierarchy 3` 
FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` LIKE '%Safety%' OR `Product Hierarchy 3` LIKE '%Equipment%';
```

#### 3. 可能的问题

**问题 1: 数据库中的值格式不同**
- 可能是 "Site Safety Equipment "（末尾有空格）
- 可能是 "Site Safety  Equipment"（中间有多个空格）
- 可能是其他变体

**解决方案：**
```sql
-- 使用 TRIM 和 LOWER 进行匹配
SELECT * FROM ecoschema.sales_data 
WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER('Site Safety Equipment');
```

**问题 2: 品类识别失败**
- 关键词映射没有匹配到
- 智能识别也没有匹配到

**解决方案：**
- 查看日志，确认是否识别为品类
- 如果识别为物料名称关键字，检查关键词映射逻辑

**问题 3: SQL 查询问题**
- 列名可能有问题
- 大小写匹配可能有问题

**解决方案：**
- 检查 SQL 查询是否正确执行
- 确认 `LOWER()` 函数是否正常工作

### 测试建议

重启后端服务后，测试以下查询：

1. **完整品类名称**
   ```
   Site Safety Equipment
   ```

2. **小写形式**
   ```
   site safety equipment
   ```

3. **部分关键词**
   ```
   site safety
   safety equipment
   ```

### 如果仍然没有结果

1. **检查数据库中的实际值格式**
   ```sql
   SELECT 
     `Product Hierarchy 3`,
     LENGTH(`Product Hierarchy 3`) as length,
     HEX(`Product Hierarchy 3`) as hex
   FROM ecoschema.sales_data 
   WHERE `Product Hierarchy 3` LIKE '%Safety%'
   LIMIT 5;
   ```

2. **检查是否有隐藏字符**
   ```sql
   SELECT 
     `Product Hierarchy 3`,
     REPLACE(`Product Hierarchy 3`, ' ', '') as no_spaces
   FROM ecoschema.sales_data 
   WHERE `Product Hierarchy 3` LIKE '%Safety%'
   LIMIT 5;
   ```

3. **使用模糊匹配测试**
   ```sql
   SELECT * FROM ecoschema.sales_data 
   WHERE `Product Hierarchy 3` LIKE '%Site Safety Equipment%';
   ```

### 调试代码位置

- **品类识别**: `LLMSearchParser.applyRuleBasedParsing()` (第 139-175 行)
- **搜索执行**: `MaterialSearchService.searchMaterials()` (第 83-85 行)
- **数据库查询**: `SalesDataRepository.findByProductHierarchy3()` (第 108-117 行)

### 快速检查清单

- [ ] 后端服务已重启
- [ ] 查看日志确认查询被正确解析为品类搜索
- [ ] 确认数据库中有数据：`SELECT COUNT(*) FROM sales_data WHERE LOWER(\`Product Hierarchy 3\`) = LOWER('Site Safety Equipment');`
- [ ] 确认数据库中的值格式与查询匹配
- [ ] 直接在数据库中测试 SQL 查询

按照这个指南排查，应该能找到问题所在！

