# AI 物料搜索系统实现文档

## 概述

实现了一个强大的 AI 物料搜索系统，支持多种搜索方式，包括精确搜索、模糊搜索、分类搜索、品牌搜索和组合条件搜索。

## 架构设计

### 核心组件

1. **MaterialSearchCriteria** (`dto/chat/MaterialSearchCriteria.java`)
   - 搜索条件数据模型
   - 支持多种搜索类型和组合条件

2. **LLMSearchParser** (`service/LLMSearchParser.java`)
   - 使用规则匹配和 LLM 解析自然语言查询
   - 提取结构化搜索条件

3. **MaterialSearchService** (`service/MaterialSearchService.java`)
   - 执行物料搜索逻辑
   - 支持历史交易统计

4. **SalesDataRepository** (`repository/SalesDataRepository.java`)
   - 提供各种搜索方法
   - 支持原生 SQL 查询

5. **ChatService** (`service/ChatService.java`)
   - 集成搜索功能到聊天服务
   - 处理用户查询并返回结果

## 支持的搜索方式

### 1. 按物料编码精确搜索

**示例查询：**
- "TI00040"
- "查找物料编码 TI00040"
- "ItemCode = TI00040"

**实现：**
- 使用正则表达式识别物料编码格式（如 `TI00040`）
- 调用 `findByItemCode()` 方法
- 返回该编码的所有历史交易记录

**返回信息：**
- 所有历史交易记录
- 价格区间（最低价、最高价、平均价）
- 交易日期范围（最早、最晚）

### 2. 按物料名称关键字模糊搜索

**示例查询：**
- "安全鞋"
- "喷漆"
- "储物箱"
- "LEAKAGE CURRENT CLAMP METER"

**实现：**
- 在 `ItemName` 字段中进行全文检索
- 使用 `LIKE` 查询进行模糊匹配
- 调用 `searchByItemNameKeyword()` 方法

### 3. 按品类搜索

**示例查询：**
- "Site Safety Equipment"
- "安全设备"
- "Filters 类"

**实现：**
- 匹配 `Product Hierarchy 3` 字段
- 支持中英文关键词映射
- 调用 `findByProductHierarchy3()` 方法

### 4. 按功能搜索

**示例查询：**
- "Maintenance Chemicals"
- "维护化学品"

**实现：**
- 匹配 `Function` 字段
- 调用 `findByFunction()` 方法

### 5. 按品牌搜索

**示例查询：**
- "品牌 AET"
- "brand AET"
- "Air Liquide 品牌"

**实现：**
- 匹配 `Brand Code` 字段
- 使用正则表达式提取品牌代码
- 调用 `findByBrandCode()` 方法

### 6. 组合条件搜索

**示例查询：**
- "Site Safety Equipment + Air Liquide + 去年"
- "Filters 类 + AET + 单价 100 到 500"
- "安全设备 + 去年 + 价格区间 50-200"

**实现：**
- 使用 `LLMSearchParser` 解析复杂查询
- 提取多个搜索条件
- 调用 `searchByCombinedCriteria()` 方法
- 支持以下组合：
  - 品类 + 品牌 + 时间范围
  - 品类 + 品牌 + 价格区间
  - 物料名称 + 品类 + 功能
  - 任意条件的组合

## 工作流程

```
用户输入自然语言查询
    ↓
LLMSearchParser.parseSearchQuery()
    ↓
规则匹配（快速、准确）
    ↓
是否需要 LLM 解析？
    ↓ 是
调用 LLM API 解析复杂查询
    ↓
提取 MaterialSearchCriteria
    ↓
MaterialSearchService.searchMaterials()
    ↓
根据搜索类型选择查询方法
    ↓
执行数据库查询
    ↓
应用价格和日期过滤
    ↓
返回搜索结果
    ↓
ChatService 构建响应
    ↓
返回表格数据和统计信息
```

## 规则匹配逻辑

### 物料编码识别
```java
Pattern: \\b([A-Z]{2,}\\d{3,})\\b
示例: "TI00040" → itemCode = "TI00040"
```

### 品牌代码识别
```java
关键词: "brand", "品牌", "牌子"
Pattern: \\b([A-Z]{2,5})\\b
示例: "品牌 AET" → brandCode = "AET"
```

### 品类关键词映射
```java
"site safety" / "安全设备" → "Site Safety Equipment"
"filters" / "过滤器" → "Filters"
"maintenance chemicals" / "维护化学品" → "Maintenance Chemicals"
```

### 时间关键词识别
```java
"去年" / "last year" → 去年1月1日 到 去年12月31日
"今年" / "this year" → 今年1月1日 到 今天
```

### 价格区间识别
```java
Pattern: (\\d+(?:\\.\\d+)?)\\s*(?:到|-|~|至)\\s*(\\d+(?:\\.\\d+)?)
示例: "100 到 500" → minPrice = 100, maxPrice = 500
```

## LLM 解析

当规则匹配无法完全解析查询时，使用 LLM 进行智能解析：

**提示词结构：**
```
You are a search query parser for a construction materials database.
Parse the following user query and extract search criteria in JSON format.

User query: "{userQuery}"

Return a JSON object with the following structure:
{
  "itemCode": "exact item code if mentioned",
  "itemNameKeyword": "product name keywords",
  "productHierarchy3": "category name",
  "function": "function name",
  "brandCode": "brand code",
  "minPrice": minimum price or null,
  "maxPrice": maximum price or null,
  "startDate": "YYYY-MM-DD or null",
  "endDate": "YYYY-MM-DD or null"
}
```

## 数据库查询方法

### Repository 方法列表

1. `findByItemCode(String itemCode)` - 精确物料编码搜索
2. `searchByItemNameKeyword(String keyword, int limit)` - 物料名称模糊搜索
3. `findByProductHierarchy3(String category, int limit)` - 品类搜索
4. `findByFunction(String function, int limit)` - 功能搜索
5. `findByBrandCode(String brandCode, int limit)` - 品牌搜索
6. `searchByCombinedCriteria(...)` - 组合条件搜索

## 历史交易统计

当按物料编码精确搜索时，系统会返回：

- **总交易数**：该物料的所有历史交易记录数
- **价格统计**：
  - 最低价（Min Price）
  - 最高价（Max Price）
  - 平均价（Average Price）
- **时间范围**：
  - 首次交易日期（First Transaction Date）
  - 最后交易日期（Last Transaction Date）
- **完整历史记录**：所有交易记录的详细信息

## 使用示例

### 示例 1: 精确物料编码搜索
```
用户: "TI00040"
系统: 找到 15 条历史交易记录
      价格区间: 100.00 - 500.00, 平均: 250.00
      首次交易: 2023-01-15, 最后交易: 2024-12-20
```

### 示例 2: 物料名称模糊搜索
```
用户: "安全鞋"
系统: 找到 8 条记录
      显示: Item Code, Item Name, Price, Date, Category, Brand, Function
```

### 示例 3: 组合条件搜索
```
用户: "Site Safety Equipment + Air Liquide + 去年"
系统: 找到 5 条记录
      条件: Category = Site Safety Equipment
            Brand = Air Liquide
            Date Range = 2023-01-01 to 2023-12-31
```

### 示例 4: 价格区间搜索
```
用户: "Filters 类 + 单价 100 到 500"
系统: 找到 12 条记录
      条件: Category = Filters
            Price Range = 100.00 - 500.00
```

## 性能优化

1. **规则优先**：优先使用规则匹配，快速且准确
2. **LLM 备用**：仅在复杂查询时使用 LLM
3. **结果限制**：默认限制返回 100 条记录
4. **索引优化**：建议在以下字段创建索引：
   - `ItemCode`
   - `ItemName`
   - `Product Hierarchy 3`
   - `Function`
   - `Brand Code`
   - `TXDate`
   - `TXP1`

## 扩展建议

1. **全文搜索**：考虑使用 MySQL 的 FULLTEXT 索引
2. **缓存机制**：缓存常用搜索条件的结果
3. **搜索建议**：基于历史搜索提供自动补全
4. **多语言支持**：扩展关键词映射支持更多语言
5. **语义搜索**：使用向量数据库进行语义相似度搜索

## 测试用例

### 测试 1: 物料编码搜索
```
输入: "TI00040"
预期: 返回该编码的所有历史交易
```

### 测试 2: 中文物料名称搜索
```
输入: "安全鞋"
预期: 返回包含"安全鞋"的所有物料
```

### 测试 3: 品类搜索
```
输入: "Site Safety Equipment"
预期: 返回该品类下的所有物料
```

### 测试 4: 品牌搜索
```
输入: "品牌 AET"
预期: 返回 AET 品牌的所有产品
```

### 测试 5: 组合搜索
```
输入: "Site Safety Equipment + Air Liquide + 去年"
预期: 返回满足所有条件的物料记录
```

## 总结

这个 AI 搜索系统实现了：
- ✅ 多种搜索方式（编码、名称、品类、功能、品牌）
- ✅ 组合条件搜索
- ✅ 自然语言解析（规则 + LLM）
- ✅ 历史交易统计
- ✅ 价格和日期范围过滤
- ✅ 中英文支持

系统设计灵活、可扩展，能够满足复杂的物料搜索需求。

