# 全文搜索功能说明

## 功能概述

现在系统支持**通用全文搜索**功能，可以在所有相关字段中搜索任意内容。

## 搜索字段

全文搜索会在以下字段中搜索：

1. **ItemName** - 物料名称
2. **ItemCode** - 物料编码
3. **BuyerName** - 买家名称 ⭐
4. **BuyerCode** - 买家代码
5. **Product Hierarchy 3** - 产品分类层级3
6. **Function** - 功能
7. **Brand Code** - 品牌代码
8. **Model** - 型号
9. **ItemType** - 产品类型
10. **Material** - 材料
11. **Sector** - 行业
12. **SubSector** - 子行业

## 工作流程

### 1. 优先特定搜索

系统首先尝试识别查询类型并执行特定搜索：
- 物料编码搜索（如 "TI00040"）
- 品类搜索（如 "Site Safety Equipment"）
- 功能搜索（如 "Cutting Tool"）
- 品牌搜索（如 "AIR LIQUIDE"）
- 物料名称关键字搜索

### 2. 全文搜索回退

如果特定搜索**无结果**，系统会自动回退到全文搜索：

```
用户输入: "AIR LIQUIDE SINGAPORE PRIVATE LIMITED"
  ↓
特定搜索: 尝试识别为品牌、品类等 → 无结果
  ↓
全文搜索: 在所有字段中搜索 "AIR LIQUIDE SINGAPORE PRIVATE LIMITED"
  ↓
返回结果: 所有包含该文本的记录（可能在 BuyerName、Brand Code 等字段中）
```

## 使用示例

### 示例 1: 搜索买家名称

```
输入: AIR LIQUIDE SINGAPORE PRIVATE LIMITED
  ↓
特定搜索: 无结果（无法识别为特定类型）
  ↓
全文搜索: 在 BuyerName 字段中找到匹配
  ↓
返回: 所有 BuyerName 包含 "AIR LIQUIDE SINGAPORE PRIVATE LIMITED" 的记录
```

### 示例 2: 搜索物料名称

```
输入: Safety Shoes
  ↓
特定搜索: 尝试物料名称关键字搜索 → 可能无结果
  ↓
全文搜索: 在 ItemName 字段中找到匹配
  ↓
返回: 所有 ItemName 包含 "Safety Shoes" 的记录
```

### 示例 3: 搜索品牌代码

```
输入: AET
  ↓
特定搜索: 识别为品牌搜索 → 找到结果
  ↓
返回: 所有 Brand Code = "AET" 的记录
```

## SQL 查询

全文搜索使用的 SQL 查询：

```sql
SELECT * FROM ecoschema.sales_data
WHERE LOWER(`ItemName`) LIKE LOWER(CONCAT('%', :keyword, '%'))
   OR LOWER(`ItemCode`) LIKE LOWER(CONCAT('%', :keyword, '%'))
   OR LOWER(`BuyerName`) LIKE LOWER(CONCAT('%', :keyword, '%'))
   OR LOWER(`BuyerCode`) LIKE LOWER(CONCAT('%', :keyword, '%'))
   OR LOWER(`Product Hierarchy 3`) LIKE LOWER(CONCAT('%', :keyword, '%'))
   OR LOWER(`Function`) LIKE LOWER(CONCAT('%', :keyword, '%'))
   OR LOWER(`Brand Code`) LIKE LOWER(CONCAT('%', :keyword, '%'))
   OR LOWER(`Model`) LIKE LOWER(CONCAT('%', :keyword, '%'))
   OR LOWER(`ItemType`) LIKE LOWER(CONCAT('%', :keyword, '%'))
   OR LOWER(`Material`) LIKE LOWER(CONCAT('%', :keyword, '%'))
   OR LOWER(`Sector`) LIKE LOWER(CONCAT('%', :keyword, '%'))
   OR LOWER(`SubSector`) LIKE LOWER(CONCAT('%', :keyword, '%'))
ORDER BY STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC
LIMIT 100
```

## 特性

### ✅ 大小写不敏感
- 所有搜索都使用 `LOWER()` 函数
- "AIR LIQUIDE" 和 "air liquide" 都能找到结果

### ✅ 部分匹配
- 使用 `LIKE '%keyword%'` 进行模糊匹配
- "AIR LIQUIDE" 可以匹配 "AIR LIQUIDE SINGAPORE PRIVATE LIMITED"

### ✅ 多字段搜索
- 在 12 个相关字段中同时搜索
- 只要任一字段包含关键词，就会返回该记录

### ✅ 自动回退
- 如果特定搜索无结果，自动使用全文搜索
- 用户无需指定搜索类型

## 搜索优先级

1. **精确物料编码搜索**（最高优先级）
   - 如果识别为物料编码，直接返回，不进行全文搜索

2. **特定搜索**
   - 品类、功能、品牌、物料名称关键字搜索

3. **全文搜索**（回退）
   - 如果特定搜索无结果，自动使用全文搜索

## 日志输出

当使用全文搜索时，会看到以下日志：

```
[INFO] No results from specific search, trying full-text search with keyword: 'AIR LIQUIDE SINGAPORE PRIVATE LIMITED'
[INFO] Full-text search found X results
```

## 注意事项

1. **性能考虑**
   - 全文搜索会在多个字段中使用 `LIKE` 查询
   - 如果数据量很大，可能需要较长时间
   - 建议在相关字段上创建索引

2. **结果限制**
   - 默认限制返回 100 条记录
   - 按交易日期降序排序

3. **搜索关键词**
   - 优先使用原始查询文本
   - 如果原始查询不可用，使用提取的关键词

## 测试建议

测试以下查询，确认全文搜索功能：

```
AIR LIQUIDE SINGAPORE PRIVATE LIMITED
AIR LIQUIDE
SINGAPORE
PRIVATE LIMITED
Safety Shoes
TI00040
Site Safety Equipment
```

所有查询都应该能返回相关结果！

