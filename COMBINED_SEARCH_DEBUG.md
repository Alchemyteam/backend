# 组合搜索问题排查指南

## 问题：查询 "AIR LIQUIDE + Cutting Tool + unit cost 0-100" 返回空结果

### 已实现的修复

我已经修复了以下问题：

1. ✅ **品牌识别优化**
   - 添加了 "AIR LIQUIDE" 到已知品牌列表
   - 支持识别多词大写品牌名称（如 "AIR LIQUIDE"）
   - 改进了品牌代码提取逻辑

2. ✅ **品类/功能识别**
   - 添加了 "Cutting Tool" 到品类关键词映射
   - 添加了 "Cutting Tool" 到功能关键词映射

3. ✅ **价格区间解析**
   - 支持 "unit cost 0-100" 格式
   - 优先在价格关键词后查找数字区间
   - 支持多种分隔符（-、到、~、至、to）

4. ✅ **价格字段支持**
   - 组合搜索同时支持 `Unit Cost` 和 `TXP1` 字段
   - 价格过滤优先使用 `Unit Cost`，如果没有则使用 `TXP1`

5. ✅ **品牌匹配优化**
   - 使用 `LOWER()` 函数进行大小写不敏感匹配
   - 支持空格分隔的品牌名称

### 查询解析流程

当用户输入 "AIR LIQUIDE + Cutting Tool + unit cost 0-100" 时：

1. **品牌识别**
   - 检测到 "AIR LIQUIDE" → `brandCode = "AIR LIQUIDE"`

2. **品类/功能识别**
   - 检测到 "Cutting Tool" → `productHierarchy3 = "Cutting Tool"` 或 `function = "Cutting Tool"`

3. **价格区间识别**
   - 检测到 "unit cost 0-100" → `minPrice = 0`, `maxPrice = 100`

4. **组合搜索**
   - 识别为组合搜索 → `searchType = COMBINED`
   - 执行组合查询

### 数据库查询

执行的 SQL 查询类似：

```sql
SELECT * FROM ecoschema.sales_data
WHERE LOWER(`Brand Code`) = LOWER('AIR LIQUIDE')
  AND (`Product Hierarchy 3` = 'Cutting Tool' OR `Function` = 'Cutting Tool')
  AND (CAST(`Unit Cost` AS DECIMAL(10,4)) >= 0 OR CAST(`TXP1` AS DECIMAL(10,2)) >= 0)
  AND (CAST(`Unit Cost` AS DECIMAL(10,4)) <= 100 OR CAST(`TXP1` AS DECIMAL(10,2)) <= 100)
ORDER BY STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC
LIMIT 100
```

### 排查步骤

#### 1. 检查后端日志

查看控制台输出，确认：

```
[INFO] Processing chat message: AIR LIQUIDE + Cutting Tool + unit cost 0-100
[INFO] Detected intent: SEARCH_PRODUCTS
[INFO] Parsed search criteria: MaterialSearchCriteria{
  brandCode='AIR LIQUIDE',
  productHierarchy3='Cutting Tool',
  minPrice=0,
  maxPrice=100,
  searchType=COMBINED
}
[INFO] Performing combined search
[INFO] Total results found: X
```

#### 2. 检查数据库中的数据

直接在数据库中查询，确认数据是否存在：

```sql
-- 检查 AIR LIQUIDE 品牌的数据
SELECT `ItemName`, `Brand Code`, `Product Hierarchy 3`, `Function`, `Unit Cost`, `TXP1`
FROM ecoschema.sales_data
WHERE LOWER(`Brand Code`) = LOWER('AIR LIQUIDE')
LIMIT 10;

-- 检查 Cutting Tool 品类的数据
SELECT `ItemName`, `Brand Code`, `Product Hierarchy 3`, `Function`, `Unit Cost`, `TXP1`
FROM ecoschema.sales_data
WHERE `Product Hierarchy 3` = 'Cutting Tool' OR `Function` = 'Cutting Tool'
LIMIT 10;

-- 检查价格在 0-100 范围内的数据
SELECT `ItemName`, `Brand Code`, `Unit Cost`, `TXP1`
FROM ecoschema.sales_data
WHERE (CAST(`Unit Cost` AS DECIMAL(10,4)) BETWEEN 0 AND 100 
   OR CAST(`TXP1` AS DECIMAL(10,2)) BETWEEN 0 AND 100)
LIMIT 10;

-- 组合查询测试
SELECT `ItemName`, `Brand Code`, `Product Hierarchy 3`, `Function`, `Unit Cost`, `TXP1`
FROM ecoschema.sales_data
WHERE LOWER(`Brand Code`) = LOWER('AIR LIQUIDE')
  AND (`Product Hierarchy 3` = 'Cutting Tool' OR `Function` = 'Cutting Tool')
  AND (CAST(`Unit Cost` AS DECIMAL(10,4)) BETWEEN 0 AND 100 
   OR CAST(`TXP1` AS DECIMAL(10,2)) BETWEEN 0 AND 100)
LIMIT 10;
```

#### 3. 可能的问题

**问题 1: 品牌代码不匹配**
- 数据库中的 `Brand Code` 可能是 "AIR LIQUIDE"（带空格）
- 或者可能是 "AIRLIQUIDE"（无空格）
- 或者可能是其他变体

**解决方案：**
```sql
-- 查看实际的品牌代码格式
SELECT DISTINCT `Brand Code` 
FROM ecoschema.sales_data 
WHERE `Brand Code` LIKE '%AIR%' OR `Brand Code` LIKE '%LIQUIDE%';
```

**问题 2: 品类名称不匹配**
- 数据库中的 `Product Hierarchy 3` 可能不是 "Cutting Tool"
- 可能是 "Cutting Tools"（复数）
- 或者可能是其他名称

**解决方案：**
```sql
-- 查看实际的品类名称
SELECT DISTINCT `Product Hierarchy 3` 
FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` LIKE '%Cutting%' OR `Product Hierarchy 3` LIKE '%Tool%';
```

**问题 3: 功能名称不匹配**
- 数据库中的 `Function` 可能不是 "Cutting Tool"
- 可能是 "Cutting" 或其他名称

**解决方案：**
```sql
-- 查看实际的功能名称
SELECT DISTINCT `Function` 
FROM ecoschema.sales_data 
WHERE `Function` LIKE '%Cutting%' OR `Function` LIKE '%Tool%';
```

**问题 4: 价格字段为空或格式问题**
- `Unit Cost` 或 `TXP1` 可能为 NULL
- 可能包含非数字字符
- 可能超出 0-100 范围

**解决方案：**
```sql
-- 查看价格字段的分布
SELECT 
  MIN(CAST(`Unit Cost` AS DECIMAL(10,4))) as min_unit_cost,
  MAX(CAST(`Unit Cost` AS DECIMAL(10,4))) as max_unit_cost,
  MIN(CAST(`TXP1` AS DECIMAL(10,2))) as min_txp1,
  MAX(CAST(`TXP1` AS DECIMAL(10,2))) as max_txp1
FROM ecoschema.sales_data
WHERE `Brand Code` LIKE '%AIR%LIQUIDE%'
  AND (`Product Hierarchy 3` LIKE '%Cutting%' OR `Function` LIKE '%Cutting%');
```

### 测试建议

重启后端服务后，按以下顺序测试：

1. **单独测试品牌**
   ```
   AIR LIQUIDE
   ```

2. **单独测试品类**
   ```
   Cutting Tool
   ```

3. **单独测试价格**
   ```
   unit cost 0-100
   ```

4. **两两组合**
   ```
   AIR LIQUIDE + Cutting Tool
   AIR LIQUIDE + unit cost 0-100
   Cutting Tool + unit cost 0-100
   ```

5. **完整组合**
   ```
   AIR LIQUIDE + Cutting Tool + unit cost 0-100
   ```

### 如果仍然没有结果

1. **检查数据库中的实际值**
   - 品牌代码的确切格式
   - 品类的确切名称
   - 功能的确切名称
   - 价格字段的实际值

2. **调整关键词映射**
   - 根据实际数据更新 `LLMSearchParser` 中的关键词映射
   - 添加实际使用的品类和功能名称

3. **使用模糊匹配**
   - 如果精确匹配失败，可以考虑使用 `LIKE` 查询进行模糊匹配

### 调试日志位置

关键日志输出位置：
- `ChatService.processMessage()` - 处理消息
- `ChatService.analyzeIntent()` - 意图识别
- `LLMSearchParser.parseSearchQuery()` - 查询解析
- `LLMSearchParser.applyRuleBasedParsing()` - 规则匹配
- `MaterialSearchService.searchMaterials()` - 执行搜索
- `MaterialSearchService.performCombinedSearch()` - 组合搜索

### 快速检查清单

- [ ] 后端服务已重启
- [ ] 查看日志确认查询被正确解析
- [ ] 确认品牌代码格式匹配
- [ ] 确认品类/功能名称匹配
- [ ] 确认价格字段有值且在范围内
- [ ] 直接在数据库中测试 SQL 查询

按照这个指南排查，应该能找到问题所在！

