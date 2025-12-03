# AI 搜索系统逻辑说明

## 📋 整体架构

AI 搜索系统采用**分层架构**，包含以下核心组件：

```
用户输入
  ↓
ChatService (意图识别 + 消息处理)
  ↓
LLMSearchParser (查询解析：规则匹配 + LLM)
  ↓
MaterialSearchService (搜索执行)
  ↓
SalesDataRepository (数据库查询)
  ↓
返回结果
```

---

## 🔄 完整工作流程

### 第一步：用户输入处理 (`ChatService.processMessage`)

用户发送消息后，系统首先进入 `ChatService.processMessage()`：

```java
用户输入: "AIR LIQUIDE + Cutting Tool + unit cost 0-100"
  ↓
1. 分析用户意图 (analyzeIntent)
2. 根据意图执行相应操作
```

### 第二步：意图识别 (`ChatService.analyzeIntent`)

系统分析用户意图，支持以下类型：

| 意图类型 | 触发关键词 | 说明 |
|---------|-----------|------|
| `CREATE_REQUISITION` | create/make + requisition/purchase/order | 创建采购申请 |
| `SEARCH_PRODUCTS` | search/find/show/list + 产品关键词 | **物料搜索** |
| `GET_PRODUCT_INFO` | info/detail/about/tell me | 获取产品详情 |
| `COMPARE_PRODUCTS` | compare/vs/versus | 产品对比 |
| `GENERAL` | 其他 | 通用查询 |

**识别逻辑：**
- 如果包含明确的搜索动词（search/find/show/list/查找/搜索）→ `SEARCH_PRODUCTS`
- 如果包含产品关键词（safety/shoe/equipment/filter等）→ `SEARCH_PRODUCTS`
- 如果查询很短（≤5个词）且不是问句 → `SEARCH_PRODUCTS`

**示例：**
```
"Safety Shoes" → SEARCH_PRODUCTS (包含产品关键词)
"查找 TI00040" → SEARCH_PRODUCTS (包含搜索动词)
"Site Safety Equipment" → SEARCH_PRODUCTS (包含产品关键词)
```

---

### 第三步：查询解析 (`LLMSearchParser.parseSearchQuery`)

当意图为 `SEARCH_PRODUCTS` 时，调用 `handleSearchProducts()` → `llmSearchParser.parseSearchQuery()`

#### 3.1 规则匹配 (`applyRuleBasedParsing`)

**优先级从高到低：**

##### 1️⃣ 物料编码识别（最高优先级）
```java
模式: \b([A-Z]{2,}\d{3,})\b
示例: "TI00040" → itemCode = "TI00040"
```
- 如果找到物料编码，**直接返回**，不再进行其他解析

##### 2️⃣ 品牌识别
```java
已知品牌列表: ["AIR LIQUIDE", "AET", "FLUKE", "3M", "HONEYWELL"]
规则1: 在 "brand/品牌" 关键词后查找
规则2: 匹配已知品牌列表
规则3: 提取所有大写字母组合（如 "AIR LIQUIDE"）
```

##### 3️⃣ 品类识别（Product Hierarchy 3）
```java
关键词映射:
  "site safety equipment" → "Site Safety Equipment"
  "site safety" → "Site Safety Equipment"
  "safety equipment" → "Site Safety Equipment"
  "filters" → "Filters"
  "cutting tool" → "Cutting Tool"
  ...
  
智能识别:
  - 如果查询格式像品类名称（首字母大写，多个单词）
  - 直接使用查询作为品类名称
```

##### 4️⃣ 功能识别（Function）
```java
关键词映射:
  "maintenance chemicals" → "Maintenance Chemicals"
  "safety" → "Safety"
  "cutting" → "Cutting"
  "cutting tool" → "Cutting Tool"
  ...
```

##### 5️⃣ 时间范围识别
```java
"去年" / "last year" → startDate = 去年1月1日, endDate = 去年12月31日
"今年" / "this year" → startDate = 今年1月1日, endDate = 今天
```

##### 6️⃣ 价格区间识别
```java
格式1: "unit cost 0-100" → minPrice = 0, maxPrice = 100
格式2: "0-100" → minPrice = 0, maxPrice = 100
格式3: "价格 100 到 500" → minPrice = 100, maxPrice = 500
```

##### 7️⃣ 物料名称关键字提取
```java
如果前面都没有匹配到:
  - 移除常见词汇（find/search/show等）
  - 提取剩余部分作为 itemNameKeyword
  - 例如: "Safety Shoes" → itemNameKeyword = "Safety Shoes"
```

##### 8️⃣ 组合查询处理
```java
如果识别到多个条件（品牌 + 品类 + 价格等）:
  - 移除已识别的部分（品牌、品类、功能、价格）
  - 提取剩余部分作为物料名称关键字
  - 例如: "AIR LIQUIDE + Cutting Tool + unit cost 0-100"
    → brandCode = "AIR LIQUIDE"
    → productHierarchy3 = "Cutting Tool"
    → minPrice = 0, maxPrice = 100
```

#### 3.2 LLM 解析（可选）

如果规则匹配不够充分，会调用 LLM 进行更复杂的解析：

```java
条件: needsLLMParsing(criteria)
  - 查询很复杂（多个条件）
  - 规则匹配没有找到主要条件

LLM 任务:
  - 提取结构化搜索条件
  - 返回 JSON 格式的 MaterialSearchCriteria
```

---

### 第四步：搜索执行 (`MaterialSearchService.searchMaterials`)

根据解析出的 `MaterialSearchCriteria`，执行相应的搜索：

#### 4.1 搜索类型判断

```java
优先级顺序:
1. 精确物料编码搜索 (hasItemCode)
2. 组合搜索 (isCombinedSearch)
3. 单一条件搜索:
   - 物料名称关键字 (hasItemNameKeyword)
   - 品类 (hasCategory)
   - 功能 (hasFunction)
   - 品牌 (hasBrand)
```

#### 4.2 精确物料编码搜索

```java
if (criteria.hasItemCode()) {
    // 直接查询，返回所有历史交易
    results = salesDataRepository.findByItemCode(itemCode);
    return results; // 直接返回，不进行其他搜索
}
```

#### 4.3 组合搜索

```java
if (criteria.isCombinedSearch()) {
    // 执行组合查询，同时匹配多个条件
    results = salesDataRepository.searchByCombinedCriteria(
        itemNameKeyword,      // 物料名称关键字（可选）
        productHierarchy3,    // 品类（可选）
        function,             // 功能（可选）
        brandCode,            // 品牌（可选）
        minPrice,             // 最低价格（可选）
        maxPrice,             // 最高价格（可选）
        startDate,            // 开始日期（可选）
        endDate,              // 结束日期（可选）
        limit                 // 结果限制
    );
}
```

**SQL 查询示例：**
```sql
SELECT * FROM ecoschema.sales_data
WHERE (:itemNameKeyword IS NULL OR LOWER(`ItemName`) LIKE LOWER(CONCAT('%', :itemNameKeyword, '%')))
  AND (:productHierarchy3 IS NULL OR LOWER(`Product Hierarchy 3`) = LOWER(:productHierarchy3))
  AND (:function IS NULL OR LOWER(`Function`) = LOWER(:function))
  AND (:brandCode IS NULL OR LOWER(`Brand Code`) = LOWER(:brandCode))
  AND (:minPrice IS NULL OR CAST(`Unit Cost` AS DECIMAL(10,4)) >= :minPrice OR CAST(`TXP1` AS DECIMAL(10,2)) >= :minPrice)
  AND (:maxPrice IS NULL OR CAST(`Unit Cost` AS DECIMAL(10,4)) <= :maxPrice OR CAST(`TXP1` AS DECIMAL(10,2)) <= :maxPrice)
ORDER BY STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC
LIMIT 100
```

#### 4.4 单一条件搜索

##### 物料名称关键字搜索
```java
1. 完整关键词搜索: "Safety Shoes" → 搜索 ItemName 包含 "Safety Shoes"
2. 如果无结果，拆分搜索: "Safety" + "Shoes" → 搜索包含任一关键词的记录
3. 去重并合并结果
```

##### 品类搜索
```java
results = salesDataRepository.findByProductHierarchy3("Site Safety Equipment", 100);
// SQL: WHERE LOWER(`Product Hierarchy 3`) = LOWER('Site Safety Equipment')
```

##### 功能搜索
```java
results = salesDataRepository.findByFunction("Cutting Tool", 100);
// SQL: WHERE LOWER(`Function`) = LOWER('Cutting Tool')
```

##### 品牌搜索
```java
results = salesDataRepository.findByBrandCode("AIR LIQUIDE", 100);
// SQL: WHERE LOWER(`Brand Code`) = LOWER('AIR LIQUIDE')
```

#### 4.5 价格和日期过滤

即使单一条件搜索，也会应用价格和日期过滤：

```java
if (criteria.hasPriceRange()) {
    // 优先使用 Unit Cost，如果没有则使用 TXP1
    // 过滤 minPrice <= price <= maxPrice
}

if (criteria.hasDateRange()) {
    // 过滤 startDate <= txDate <= endDate
}
```

---

### 第五步：结果构建 (`ChatService.handleSearchProducts`)

#### 5.1 构建表格数据

```java
TableData tableData = new TableData();
tableData.setTitle("Material Search Results");
tableData.setHeaders(["Item Code", "Item Name", "Price", "Date", "Category", "Brand", "Function"]);

// 将 SalesData 转换为表格行
rows = salesDataList.stream()
    .map(salesData -> {
        row.put("Item Code", salesData.getItemCode());
        row.put("Item Name", salesData.getItemName());
        row.put("Price", salesData.getTxP1());
        row.put("Date", salesData.getTxDate());
        row.put("Category", salesData.getProductHierarchy3());
        row.put("Brand", salesData.getBrandCode());
        row.put("Function", salesData.getFunction());
        return row;
    })
    .collect(Collectors.toList());
```

#### 5.2 生成响应文本

```java
如果是精确物料编码搜索:
    responseText = "Found X historical transactions for Item Code: XXX (ItemName). 
                   Price range: min - max, Average: avg. 
                   First transaction: date1, Last transaction: date2."
否则:
    responseText = "Found X material record(s) matching your search."
```

#### 5.3 返回响应

```java
ChatResponse response = new ChatResponse();
response.setResponse(responseText);
response.setTableData(tableData);
return response;
```

---

## 🎯 搜索类型总结

### 1. 精确物料编码搜索
```
输入: "TI00040"
解析: itemCode = "TI00040"
搜索: 直接查询 ItemCode = "TI00040"
返回: 所有历史交易 + 价格统计
```

### 2. 物料名称关键字搜索
```
输入: "Safety Shoes"
解析: itemNameKeyword = "Safety Shoes"
搜索: 
  1. ItemName LIKE '%Safety Shoes%'
  2. 如果无结果，拆分: ItemName LIKE '%Safety%' OR ItemName LIKE '%Shoes%'
返回: 匹配的物料记录
```

### 3. 品类搜索
```
输入: "Site Safety Equipment"
解析: productHierarchy3 = "Site Safety Equipment"
搜索: Product Hierarchy 3 = "Site Safety Equipment"
返回: 该品类下的所有物料
```

### 4. 功能搜索
```
输入: "Cutting Tool"
解析: function = "Cutting Tool"
搜索: Function = "Cutting Tool"
返回: 该功能下的所有物料
```

### 5. 品牌搜索
```
输入: "AIR LIQUIDE"
解析: brandCode = "AIR LIQUIDE"
搜索: Brand Code = "AIR LIQUIDE"
返回: 该品牌下的所有物料
```

### 6. 组合搜索
```
输入: "AIR LIQUIDE + Cutting Tool + unit cost 0-100"
解析:
  brandCode = "AIR LIQUIDE"
  productHierarchy3 = "Cutting Tool"
  minPrice = 0, maxPrice = 100
搜索: 
  Brand Code = "AIR LIQUIDE"
  AND Product Hierarchy 3 = "Cutting Tool"
  AND (Unit Cost BETWEEN 0 AND 100 OR TXP1 BETWEEN 0 AND 100)
返回: 同时满足所有条件的物料
```

---

## 🔍 关键特性

### 1. 大小写不敏感
- 所有字符串匹配都使用 `LOWER()` 函数
- 支持 "Site Safety Equipment" 和 "site safety equipment"

### 2. 模糊匹配
- 物料名称搜索使用 `LIKE '%keyword%'`
- 支持部分匹配

### 3. 关键词拆分
- 如果完整关键词无结果，自动拆分为单词搜索
- 例如: "Safety Shoes" → "Safety" + "Shoes"

### 4. 智能识别
- 自动识别品类名称格式（首字母大写，多个单词）
- 自动识别品牌名称（大写字母组合）

### 5. 价格字段支持
- 同时支持 `Unit Cost` 和 `TXP1` 字段
- 使用 OR 条件：任一字段在范围内即可

### 6. 去重机制
- 基于 `TXNo`（交易编号）去重
- 避免重复记录

---

## 📊 数据流示例

### 示例 1: 简单搜索
```
用户输入: "Safety Shoes"
  ↓
意图识别: SEARCH_PRODUCTS
  ↓
查询解析:
  - 规则匹配: itemNameKeyword = "Safety Shoes"
  - 搜索类型: ITEM_NAME_FUZZY
  ↓
搜索执行:
  - 搜索 ItemName LIKE '%Safety Shoes%'
  - 如果无结果，拆分搜索 "Safety" 和 "Shoes"
  ↓
结果返回:
  - 表格数据: 匹配的物料记录
  - 响应文本: "Found X material record(s) matching your search."
```

### 示例 2: 组合搜索
```
用户输入: "AIR LIQUIDE + Cutting Tool + unit cost 0-100"
  ↓
意图识别: SEARCH_PRODUCTS
  ↓
查询解析:
  - 品牌识别: brandCode = "AIR LIQUIDE"
  - 品类识别: productHierarchy3 = "Cutting Tool"
  - 价格识别: minPrice = 0, maxPrice = 100
  - 搜索类型: COMBINED
  ↓
搜索执行:
  - 组合查询: Brand Code = "AIR LIQUIDE"
              AND Product Hierarchy 3 = "Cutting Tool"
              AND (Unit Cost BETWEEN 0 AND 100 OR TXP1 BETWEEN 0 AND 100)
  ↓
结果返回:
  - 表格数据: 同时满足所有条件的物料
  - 响应文本: "Found X material record(s) matching your search."
```

### 示例 3: 精确编码搜索
```
用户输入: "TI00040"
  ↓
意图识别: SEARCH_PRODUCTS
  ↓
查询解析:
  - 物料编码识别: itemCode = "TI00040"
  - 搜索类型: EXACT_ITEM_CODE
  ↓
搜索执行:
  - 精确查询: ItemCode = "TI00040"
  - 获取历史统计: 价格区间、平均价格、交易日期范围
  ↓
结果返回:
  - 表格数据: 所有历史交易记录
  - 响应文本: "Found 15 historical transactions for Item Code: TI00040 (ItemName). 
               Price range: 100.00 - 500.00, Average: 250.00. 
               First transaction: 2023-01-15, Last transaction: 2024-12-20."
```

---

## 🛠️ 技术实现细节

### 1. 规则匹配 vs LLM 解析
- **规则匹配**: 快速、准确，优先使用
- **LLM 解析**: 用于复杂查询，作为补充

### 2. 搜索优先级
1. 精确物料编码（最高优先级，直接返回）
2. 组合搜索
3. 单一条件搜索（按顺序尝试）

### 3. 数据库查询优化
- 使用原生 SQL 查询，避免 Hibernate 命名策略问题
- 使用 `LOWER()` 进行大小写不敏感匹配
- 使用 `STR_TO_DATE()` 处理日期字段
- 使用 `CAST()` 处理价格字段类型转换

### 4. 错误处理
- 如果解析失败，回退到简单的关键词提取
- 如果搜索无结果，返回空表格和提示信息
- 所有异常都被捕获并记录日志

---

## 📝 总结

AI 搜索系统采用**分层解析 + 智能匹配**的策略：

1. **意图识别** → 确定用户想要做什么
2. **查询解析** → 提取结构化搜索条件（规则匹配 + LLM）
3. **搜索执行** → 根据条件执行相应的数据库查询
4. **结果构建** → 格式化返回结果

**核心优势：**
- ✅ 支持多种搜索方式（精确编码、关键字、品类、功能、品牌、组合）
- ✅ 智能识别和解析（规则匹配 + LLM）
- ✅ 大小写不敏感、模糊匹配
- ✅ 关键词拆分、去重机制
- ✅ 价格和日期过滤支持

这个系统能够处理从简单到复杂的各种搜索查询，为用户提供灵活且强大的物料搜索功能！

