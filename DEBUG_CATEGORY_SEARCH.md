# 品类搜索问题调试

## 问题现象

- ✅ "Maintenance Chemicals" 可以返回结果
- ❌ "Site Safety Equipment" 返回空结果

## 可能的原因

### 1. 关键词映射差异

**品类关键词映射：**
```java
"maintenance chemicals" → "Maintenance Chemicals"  ✅
"site safety equipment" → "Site Safety Equipment"  ✅
```

两者都有映射，应该都能识别。

### 2. 识别顺序问题

**识别顺序：**
1. 品类识别（第 139-175 行）
2. 功能识别（第 177-192 行）

品类识别在功能识别之前，所以应该先匹配到品类。

### 3. 搜索执行差异

**搜索优先级：**
```java
if (criteria.hasCategory()) {
    // 品类搜索
} else if (criteria.hasFunction()) {
    // 功能搜索
}
```

品类搜索优先于功能搜索。

### 4. 数据库字段差异

**可能的情况：**
- "Maintenance Chemicals" 在 `Product Hierarchy 3` 字段中 ✅
- "Site Safety Equipment" 在 `Product Hierarchy 3` 字段中 ❓
- 或者 "Maintenance Chemicals" 在 `Function` 字段中，也能搜索到

## 调试步骤

### 1. 检查后端日志

查看两个搜索的日志输出：

**Maintenance Chemicals:**
```
[INFO] Parsing search query: Maintenance Chemicals
[INFO] Detected Category: Maintenance Chemicals (matched keyword: maintenance chemicals)
或
[INFO] Using query as category name: Maintenance Chemicals
[INFO] Searching by Product Hierarchy 3: Maintenance Chemicals
[INFO] Found X results
```

**Site Safety Equipment:**
```
[INFO] Parsing search query: Site Safety Equipment
[INFO] Detected Category: Site Safety Equipment (matched keyword: site safety equipment)
或
[INFO] Using query as category name: Site Safety Equipment
[INFO] Searching by Product Hierarchy 3: Site Safety Equipment
[INFO] Found X results
```

### 2. 检查数据库中的实际值

```sql
-- 检查 Maintenance Chemicals 的数据
SELECT DISTINCT `Product Hierarchy 3`, `Function`
FROM ecoschema.sales_data 
WHERE LOWER(`Product Hierarchy 3`) = LOWER('Maintenance Chemicals')
   OR LOWER(`Function`) = LOWER('Maintenance Chemicals')
LIMIT 10;

-- 检查 Site Safety Equipment 的数据
SELECT DISTINCT `Product Hierarchy 3`, `Function`
FROM ecoschema.sales_data 
WHERE LOWER(`Product Hierarchy 3`) = LOWER('Site Safety Equipment')
   OR LOWER(`Function`) = LOWER('Site Safety Equipment')
LIMIT 10;

-- 检查数量
SELECT 
  COUNT(*) as maintenance_chemicals_count
FROM ecoschema.sales_data 
WHERE LOWER(`Product Hierarchy 3`) = LOWER('Maintenance Chemicals');

SELECT 
  COUNT(*) as site_safety_equipment_count
FROM ecoschema.sales_data 
WHERE LOWER(`Product Hierarchy 3`) = LOWER('Site Safety Equipment');
```

### 3. 检查识别逻辑

**关键词匹配：**
- "maintenance chemicals" (21个字符) - 会优先匹配
- "site safety equipment" (22个字符) - 会优先匹配

两者都应该能匹配到。

**智能识别：**
- "Maintenance Chemicals" - 匹配正则 `^[A-Z][a-zA-Z\\s]+$` ✅
- "Site Safety Equipment" - 匹配正则 `^[A-Z][a-zA-Z\\s]+$` ✅

两者都应该能通过智能识别。

## 可能的问题

### 问题 1: 数据库中的值格式不同

**Maintenance Chemicals:**
- 数据库中可能是 "Maintenance Chemicals"（精确匹配）
- 或者 "Maintenance Chemicals "（末尾有空格）

**Site Safety Equipment:**
- 数据库中可能是 "Site Safety Equipment"（精确匹配）
- 或者 "Site Safety  Equipment"（中间有多个空格）
- 或者 "SiteSafetyEquipment"（没有空格）

### 问题 2: 识别为功能而不是品类

如果 "Maintenance Chemicals" 被识别为功能，搜索会使用 `findByFunction()`，可能也能找到数据。

但 "Site Safety Equipment" 如果被识别为功能，可能数据库中的 `Function` 字段不是这个值。

### 问题 3: 搜索字段不同

- "Maintenance Chemicals" 可能在 `Function` 字段中，也能搜索到
- "Site Safety Equipment" 只在 `Product Hierarchy 3` 字段中，但搜索逻辑有问题

## 解决方案

### 方案 1: 同时搜索品类和功能字段

修改搜索逻辑，如果品类搜索无结果，尝试功能搜索：

```java
if (criteria.hasCategory()) {
    results = salesDataRepository.findByProductHierarchy3(criteria.getProductHierarchy3(), 100);
    // 如果无结果，尝试功能搜索
    if (results.isEmpty()) {
        results = salesDataRepository.findByFunction(criteria.getProductHierarchy3(), 100);
    }
}
```

### 方案 2: 检查数据库中的实际值格式

直接查询数据库，确认实际的值格式：

```sql
SELECT 
  `Product Hierarchy 3`,
  LENGTH(`Product Hierarchy 3`) as length,
  HEX(`Product Hierarchy 3`) as hex
FROM ecoschema.sales_data 
WHERE `Product Hierarchy 3` LIKE '%Safety%' OR `Product Hierarchy 3` LIKE '%Equipment%'
LIMIT 10;
```

### 方案 3: 使用模糊匹配

如果精确匹配失败，使用 `LIKE` 进行模糊匹配：

```sql
WHERE `Product Hierarchy 3` LIKE '%Site Safety Equipment%'
```

## 建议的调试步骤

1. **查看后端日志**，确认两个搜索的识别结果
2. **查询数据库**，确认实际的值格式
3. **检查搜索逻辑**，确认是否正确执行
4. **如果数据库中的值格式不同**，调整匹配逻辑

