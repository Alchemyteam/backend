# AI 搜索问题排查指南

## 问题：搜索 "Safety Shoes" 返回空结果

### 可能的原因

1. **数据库中确实没有相关记录**
   - 数据库中的物料名称可能不包含 "Safety Shoes"
   - 可能使用的是其他名称，如 "Safety Shoe"（单数）或 "安全鞋"

2. **搜索逻辑问题**
   - 关键词提取失败
   - 意图识别错误
   - 数据库查询条件不匹配

### 排查步骤

#### 1. 检查后端日志

查看后端控制台日志，确认：

```
[INFO] Processing chat message: Safety Shoes
[INFO] Detected intent: SEARCH_PRODUCTS
[INFO] Parsed search criteria: MaterialSearchCriteria{itemNameKeyword='Safety Shoes', ...}
[INFO] Searching by ItemName keyword: Safety Shoes
[INFO] Full keyword search found X results
```

如果看到 `Found 0 results`，说明数据库查询没有找到匹配的记录。

#### 2. 检查数据库中的数据

直接在数据库中查询：

```sql
-- 检查是否有包含 "Safety" 的记录
SELECT `ItemName`, `ItemCode`, `Product Hierarchy 3` 
FROM ecoschema.sales_data 
WHERE LOWER(`ItemName`) LIKE '%safety%' 
LIMIT 10;

-- 检查是否有包含 "Shoe" 的记录
SELECT `ItemName`, `ItemCode`, `Product Hierarchy 3` 
FROM ecoschema.sales_data 
WHERE LOWER(`ItemName`) LIKE '%shoe%' 
LIMIT 10;

-- 检查是否有包含 "Safety" 和 "Shoe" 的记录
SELECT `ItemName`, `ItemCode`, `Product Hierarchy 3` 
FROM ecoschema.sales_data 
WHERE LOWER(`ItemName`) LIKE '%safety%' 
  AND LOWER(`ItemName`) LIKE '%shoe%' 
LIMIT 10;
```

#### 3. 测试不同的搜索方式

尝试以下查询，看看哪个能返回结果：

```
Safety
Shoe
Shoes
安全鞋
安全
```

#### 4. 检查意图识别

如果查询没有被识别为 `SEARCH_PRODUCTS`，尝试添加明确的搜索词：

```
查找 Safety Shoes
搜索 Safety Shoes
show Safety Shoes
find Safety Shoes
```

### 已实现的优化

我已经优化了搜索逻辑，现在支持：

1. **关键词拆分搜索**
   - "Safety Shoes" 会同时搜索 "Safety" 和 "Shoes"
   - 返回包含任一关键词的结果

2. **更好的意图识别**
   - 添加了 "safety", "shoe", "shoes" 到关键词列表
   - 短查询（如 "Safety Shoes"）会被自动识别为搜索意图

3. **改进的查询逻辑**
   - 支持空格分隔的关键词
   - 使用 `LOWER()` 函数进行大小写不敏感搜索

### 测试建议

重启后端服务后，测试以下查询：

1. **基础测试**
   ```
   Safety
   Shoe
   Shoes
   ```

2. **组合测试**
   ```
   Safety Shoes
   Safety Shoe
   ```

3. **中文测试**
   ```
   安全鞋
   安全
   ```

4. **带搜索动词**
   ```
   查找 Safety Shoes
   搜索 Safety Shoes
   ```

### 如果仍然没有结果

1. **确认数据库中有数据**
   ```sql
   SELECT COUNT(*) FROM ecoschema.sales_data WHERE `ItemName` IS NOT NULL;
   ```

2. **检查数据格式**
   ```sql
   SELECT DISTINCT `ItemName` 
   FROM ecoschema.sales_data 
   WHERE `ItemName` LIKE '%安全%' OR `ItemName` LIKE '%safety%' 
   LIMIT 20;
   ```

3. **查看完整日志**
   - 启用 DEBUG 日志级别
   - 检查每个步骤的输出

### 调试代码位置

- **意图识别**: `ChatService.analyzeIntent()`
- **关键词提取**: `LLMSearchParser.applyRuleBasedParsing()`
- **搜索执行**: `MaterialSearchService.searchMaterials()`
- **数据库查询**: `SalesDataRepository.searchByItemNameKeyword()`

### 常见问题

**Q: 为什么搜索 "Safety Shoes" 找不到，但搜索 "Safety" 能找到？**

A: 可能是因为数据库中的记录只包含 "Safety" 而不包含 "Shoes"。现在的优化版本会拆分关键词，同时搜索 "Safety" 和 "Shoes"，应该能找到相关结果。

**Q: 如何知道搜索是否执行了？**

A: 查看后端日志，应该能看到：
- `Processing chat message: Safety Shoes`
- `Detected intent: SEARCH_PRODUCTS`
- `Parsed search criteria: ...`
- `Searching by ItemName keyword: Safety Shoes`
- `Found X results`

**Q: 数据库中有数据，但搜索不到？**

A: 可能的原因：
1. 数据中的名称与搜索关键词不完全匹配
2. 数据中有特殊字符或空格
3. 数据库编码问题

建议：直接查询数据库，看看实际的数据格式。

