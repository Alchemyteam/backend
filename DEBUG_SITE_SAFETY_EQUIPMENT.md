# 调试 "Site Safety Equipment" 搜索问题

## 问题描述

数据库中有 `Product Hierarchy 3 = "Site Safety Equipment"` 的数据，但后端搜索返回空结果。

## 调试步骤

### 步骤 1: 查看后端日志

重启后端服务后，搜索 "Site Safety Equipment"，查看控制台日志输出。

#### 应该看到的日志（按顺序）：

```
[INFO] Processing chat message: Site Safety Equipment
[INFO] Detected intent: SEARCH_PRODUCTS
[INFO] Parsing search query: Site Safety Equipment
[INFO] Detected Category: 'Site Safety Equipment' (matched keyword: 'site safety equipment' from query: 'Site Safety Equipment')
或
[INFO] Using query as category name: 'Site Safety Equipment'
[INFO] Parsed criteria: MaterialSearchCriteria{productHierarchy3='Site Safety Equipment', ...}
[INFO] Searching by Product Hierarchy 3: 'Site Safety Equipment'
[INFO] Search criteria details: hasPriceRange=false, hasDateRange=false
[INFO] Found X results by Product Hierarchy 3 before filtering
[INFO] After filtering: X results (filtered out: 0)
[INFO] Total results found: X
[INFO] Found X sales data records
```

### 步骤 2: 检查关键日志点

#### 2.1 检查品类识别
```
[INFO] Detected Category: 'Site Safety Equipment' ...
或
[INFO] Using query as category name: 'Site Safety Equipment'
```

**如果没有看到这些日志**：
- 品类识别失败
- 可能被识别为物料名称关键字或其他类型

**如果看到**：
- 品类识别成功，继续检查下一步

#### 2.2 检查数据库查询结果
```
[INFO] Found X results by Product Hierarchy 3 before filtering
```

**如果 X = 0**：
- 数据库查询没有找到数据
- 可能的原因：
  - SQL 查询条件不匹配
  - 数据库中的值格式不同
  - 数据在其他字段中

**如果 X > 0**：
- 数据库查询找到了数据
- 继续检查过滤逻辑

#### 2.3 检查过滤结果
```
[INFO] After filtering: X results (filtered out: Y)
```

**如果 filtered out > 0**：
- 有数据被过滤掉了
- 检查是否有价格或日期过滤条件

**如果看到警告**：
```
[WARN] All results were filtered out! Criteria: hasPriceRange=..., hasDateRange=...
```
- 所有结果都被过滤掉了
- 检查价格和日期过滤条件

### 步骤 3: 可能的问题和解决方案

#### 问题 1: 品类识别失败

**症状**：
- 日志中没有 "Detected Category" 或 "Using query as category name"
- 可能看到 "Searching by ItemName keyword"

**原因**：
- 关键词映射没有匹配到
- 智能识别也没有匹配到

**解决方案**：
1. 检查查询是否完全匹配 "Site Safety Equipment"
2. 尝试小写输入：`site safety equipment`
3. 检查日志中的实际查询文本

#### 问题 2: 数据库查询返回 0 条记录

**症状**：
```
[INFO] Found 0 results by Product Hierarchy 3 before filtering
```

**原因**：
- SQL 查询条件不匹配
- 数据库中的值格式不同（空格、大小写等）

**解决方案**：
1. 直接在数据库中执行查询：
   ```sql
   SELECT COUNT(*) 
   FROM ecoschema.sales_data 
   WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER(TRIM('Site Safety Equipment'));
   ```

2. 检查实际的值格式：
   ```sql
   SELECT DISTINCT `Product Hierarchy 3`
   FROM ecoschema.sales_data 
   WHERE `Product Hierarchy 3` LIKE '%Safety%Equipment%';
   ```

3. 如果值格式不同，调整 SQL 查询或数据

#### 问题 3: 数据被过滤掉

**症状**：
```
[INFO] Found 10 results by Product Hierarchy 3 before filtering
[INFO] After filtering: 0 results (filtered out: 10)
[WARN] All results were filtered out!
```

**原因**：
- 有价格或日期过滤条件
- 所有数据都不满足过滤条件

**解决方案**：
1. 检查日志中的过滤条件：
   ```
   hasPriceRange=true, minPrice=..., maxPrice=...
   hasDateRange=true, startDate=..., endDate=...
   ```

2. 如果不需要过滤，检查为什么会有这些条件

3. 临时禁用过滤（仅用于调试）：
   ```java
   // 在 MaterialSearchService.searchMaterials() 中
   // 临时注释掉过滤逻辑
   // if (!results.isEmpty()) {
   //     results = applyFilters(results, criteria);
   // }
   ```

#### 问题 4: 数据在其他字段中

**症状**：
```
[INFO] Found 0 results by Product Hierarchy 3 before filtering
[INFO] No results found by Product Hierarchy 3, trying Function field
[INFO] Found X results by Function
```

**原因**：
- 数据在 `Function` 字段中，而不是 `Product Hierarchy 3` 字段中

**解决方案**：
1. 检查数据库中的实际字段：
   ```sql
   SELECT 
     `Product Hierarchy 3`,
     `Function`
   FROM ecoschema.sales_data 
   WHERE `Product Hierarchy 3` LIKE '%Safety%' 
      OR `Function` LIKE '%Safety%'
   LIMIT 10;
   ```

2. 如果数据在 `Function` 字段中，需要调整数据或搜索逻辑

### 步骤 4: 直接测试 SQL 查询

在数据库中直接执行后端使用的 SQL 查询：

```sql
SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, 
       `Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, 
       `UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` 
FROM ecoschema.sales_data 
WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER(TRIM('Site Safety Equipment'))
ORDER BY STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC 
LIMIT 100;
```

**如果这个查询返回数据**：
- SQL 查询是正确的
- 问题可能在 Java 代码中（参数传递、结果映射等）

**如果这个查询返回 0 条记录**：
- SQL 查询条件不匹配
- 检查数据库中的实际值格式

### 步骤 5: 检查 Java 代码

#### 5.1 检查参数传递

在 `MaterialSearchService.searchMaterials()` 中添加日志：

```java
log.info("Searching with productHierarchy3: '{}'", criteria.getProductHierarchy3());
log.info("Parameter type: {}, length: {}", 
    criteria.getProductHierarchy3().getClass().getName(),
    criteria.getProductHierarchy3().length());
```

#### 5.2 检查结果映射

在 `findByProductHierarchy3()` 返回后添加日志：

```java
if (!results.isEmpty()) {
    log.info("First result: Product Hierarchy 3 = '{}'", 
        results.get(0).getProductHierarchy3());
}
```

### 步骤 6: 对比 Maintenance Chemicals

既然 "Maintenance Chemicals" 可以返回结果，对比两者的差异：

#### 6.1 查看 Maintenance Chemicals 的日志
```
[INFO] Searching by Product Hierarchy 3: 'Maintenance Chemicals'
[INFO] Found X results by Product Hierarchy 3 before filtering
```

#### 6.2 对比数据库中的值
```sql
-- Maintenance Chemicals
SELECT DISTINCT `Product Hierarchy 3`
FROM ecoschema.sales_data 
WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER('Maintenance Chemicals');

-- Site Safety Equipment
SELECT DISTINCT `Product Hierarchy 3`
FROM ecoschema.sales_data 
WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER('Site Safety Equipment');
```

#### 6.3 检查是否有差异
- 值格式是否相同？
- 是否有特殊字符？
- 字段类型是否相同？

## 快速检查清单

- [ ] 查看后端日志，确认品类识别是否成功
- [ ] 查看后端日志，确认数据库查询返回的记录数
- [ ] 查看后端日志，确认是否有数据被过滤掉
- [ ] 在数据库中直接执行 SQL 查询，确认是否能找到数据
- [ ] 检查数据库中的实际值格式
- [ ] 对比 Maintenance Chemicals 和 Site Safety Equipment 的差异

## 常见问题总结

| 问题 | 症状 | 解决方案 |
|------|------|----------|
| 品类识别失败 | 没有 "Detected Category" 日志 | 检查关键词映射和智能识别逻辑 |
| SQL 查询无结果 | "Found 0 results" | 检查数据库中的值格式，使用 TRIM() 处理空格 |
| 数据被过滤 | "filtered out: X" | 检查价格和日期过滤条件 |
| 数据在其他字段 | "Found X results by Function" | 检查数据实际存储的字段 |

## 需要的信息

请提供以下信息以便进一步调试：

1. **后端日志输出**（完整的搜索过程日志）
2. **数据库查询结果**（直接执行 SQL 查询的结果）
3. **数据库中的实际值格式**（SELECT DISTINCT 的结果）

有了这些信息，我可以更准确地定位问题！

