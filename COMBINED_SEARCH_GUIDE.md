# 组合搜索功能说明

## 功能概述

系统现在支持**任意两个或多个字段**的内容用 "+" 连接进行组合搜索，返回同时满足所有条件的数据。

## 支持的字段类型

### 1. 买家名称 (BuyerName)
- **识别规则**: 包含公司相关词汇（LIMITED, PRIVATE, COMPANY, SINGAPORE 等）或长度 > 15 个字符
- **示例**: `AIR LIQUIDE SINGAPORE PRIVATE LIMITED`

### 2. 买家代码 (BuyerCode)
- **识别规则**: 通常是大写字母组合
- **示例**: `BUY001`, `ABC123`

### 3. 品类 (Product Hierarchy 3)
- **识别规则**: 
  - 关键词映射（如 "Site Safety Equipment", "Electrical Accessories"）
  - 首字母大写，多个单词的格式
- **示例**: `Site Safety Equipment`, `Electrical Accessories`, `Filters`

### 4. 功能 (Function)
- **识别规则**: 关键词映射（如 "Maintenance Chemicals", "Cutting Tool"）
- **示例**: `Maintenance Chemicals`, `Cutting Tool`

### 5. 品牌 (Brand Code)
- **识别规则**: 
  - 已知品牌列表（AIR LIQUIDE, AET, FLUKE 等）
  - 大写字母组合
- **示例**: `AIR LIQUIDE`, `AET`, `FLUKE`

### 6. 物料名称关键字 (ItemName)
- **识别规则**: 如果无法识别为其他类型，作为物料名称关键字
- **示例**: `Safety Shoes`, `Steel Formwork`

### 7. 物料编码 (ItemCode)
- **识别规则**: 字母+数字格式（如 TI00040）
- **示例**: `TI00040`, `ABC123`

### 8. 价格区间 (Price Range)
- **识别规则**: "unit cost 0-100", "价格 100 到 500" 等
- **示例**: `unit cost 0-100`, `价格 100-500`

### 9. 日期范围 (Date Range)
- **识别规则**: "去年", "今年", "2023年" 等
- **示例**: `去年`, `2023年`

## 组合搜索语法

### 基本语法

使用 `+` 连接多个条件：

```
条件1 + 条件2 + 条件3
```

### 支持的连接符

- `+` (加号)
- `and` (英文)
- `和` (中文)

## 使用示例

### 示例 1: 买家名称 + 品类

```
AIR LIQUIDE SINGAPORE PRIVATE LIMITED + Electrical Accessories
```

**解析结果**:
- `buyerName = "AIR LIQUIDE SINGAPORE PRIVATE LIMITED"`
- `productHierarchy3 = "Electrical Accessories"`

**SQL 查询**:
```sql
WHERE BuyerName LIKE '%AIR LIQUIDE SINGAPORE PRIVATE LIMITED%'
  AND Product Hierarchy 3 LIKE '%Electrical Accessories%'
```

### 示例 2: 品牌 + 品类

```
AIR LIQUIDE + Site Safety Equipment
```

**解析结果**:
- `brandCode = "AIR LIQUIDE"`
- `productHierarchy3 = "Site Safety Equipment"`

**SQL 查询**:
```sql
WHERE Brand Code LIKE '%AIR LIQUIDE%'
  AND Product Hierarchy 3 LIKE '%Site Safety Equipment%'
```

### 示例 3: 品类 + 功能

```
Site Safety Equipment + Maintenance Chemicals
```

**解析结果**:
- `productHierarchy3 = "Site Safety Equipment"`
- `function = "Maintenance Chemicals"`

**SQL 查询**:
```sql
WHERE Product Hierarchy 3 LIKE '%Site Safety Equipment%'
  AND Function LIKE '%Maintenance Chemicals%'
```

### 示例 4: 买家名称 + 品牌 + 品类

```
AIR LIQUIDE SINGAPORE PRIVATE LIMITED + AIR LIQUIDE + Electrical Accessories
```

**解析结果**:
- `buyerName = "AIR LIQUIDE SINGAPORE PRIVATE LIMITED"`
- `brandCode = "AIR LIQUIDE"`
- `productHierarchy3 = "Electrical Accessories"`

**SQL 查询**:
```sql
WHERE BuyerName LIKE '%AIR LIQUIDE SINGAPORE PRIVATE LIMITED%'
  AND Brand Code LIKE '%AIR LIQUIDE%'
  AND Product Hierarchy 3 LIKE '%Electrical Accessories%'
```

### 示例 5: 物料名称 + 品类

```
Safety Shoes + Site Safety Equipment
```

**解析结果**:
- `itemNameKeyword = "Safety Shoes"`
- `productHierarchy3 = "Site Safety Equipment"`

**SQL 查询**:
```sql
WHERE ItemName LIKE '%Safety Shoes%'
  AND Product Hierarchy 3 LIKE '%Site Safety Equipment%'
```

### 示例 6: 品牌 + 价格区间

```
AIR LIQUIDE + unit cost 0-100
```

**解析结果**:
- `brandCode = "AIR LIQUIDE"`
- `minPrice = 0`, `maxPrice = 100`

**SQL 查询**:
```sql
WHERE Brand Code LIKE '%AIR LIQUIDE%'
  AND (Unit Cost BETWEEN 0 AND 100 OR TXP1 BETWEEN 0 AND 100)
```

### 示例 7: 买家名称 + 品类 + 价格区间

```
AIR LIQUIDE SINGAPORE PRIVATE LIMITED + Electrical Accessories + unit cost 0-100
```

**解析结果**:
- `buyerName = "AIR LIQUIDE SINGAPORE PRIVATE LIMITED"`
- `productHierarchy3 = "Electrical Accessories"`
- `minPrice = 0`, `maxPrice = 100`

**SQL 查询**:
```sql
WHERE BuyerName LIKE '%AIR LIQUIDE SINGAPORE PRIVATE LIMITED%'
  AND Product Hierarchy 3 LIKE '%Electrical Accessories%'
  AND (Unit Cost BETWEEN 0 AND 100 OR TXP1 BETWEEN 0 AND 100)
```

## 识别优先级

在组合查询中，每个部分的识别优先级：

1. **物料编码** (最高优先级，如果识别到，直接返回)
2. **买家名称** (优先检查，避免误识别为品类)
3. **品牌** (已知品牌列表 → 大写字母组合)
4. **品类** (关键词映射 → 格式匹配)
5. **功能** (关键词映射)
6. **物料名称关键字** (如果无法识别为其他类型)

## 搜索逻辑

### 组合搜索执行流程

```
用户输入: "条件1 + 条件2 + 条件3"
  ↓
1. 检测 "+" 分隔符
  ↓
2. 拆分为多个部分
  ↓
3. 对每个部分单独解析
  ↓
4. 合并所有识别到的条件
  ↓
5. 执行组合搜索 SQL
  WHERE 条件1 AND 条件2 AND 条件3
  ↓
6. 返回同时满足所有条件的数据
```

### SQL 查询特点

- **模糊匹配**: 所有字符串字段使用 `LIKE '%keyword%'`
- **大小写不敏感**: 使用 `LOWER()` 函数
- **AND 逻辑**: 所有条件必须同时满足
- **NULL 处理**: 如果某个条件为 NULL，该条件不参与过滤

## 支持的组合类型

### 任意字段组合

系统支持以下任意组合：

| 组合类型 | 示例 |
|---------|------|
| 买家名称 + 品类 | `AIR LIQUIDE SINGAPORE + Electrical Accessories` |
| 买家名称 + 品牌 | `AIR LIQUIDE SINGAPORE + AIR LIQUIDE` |
| 买家名称 + 功能 | `AIR LIQUIDE SINGAPORE + Maintenance Chemicals` |
| 买家名称 + 物料名称 | `AIR LIQUIDE SINGAPORE + Safety Shoes` |
| 品牌 + 品类 | `AIR LIQUIDE + Site Safety Equipment` |
| 品牌 + 功能 | `AIR LIQUIDE + Cutting Tool` |
| 品牌 + 价格 | `AIR LIQUIDE + unit cost 0-100` |
| 品类 + 功能 | `Site Safety Equipment + Maintenance Chemicals` |
| 品类 + 价格 | `Site Safety Equipment + unit cost 0-100` |
| 三个条件 | `买家名称 + 品类 + 价格` |
| 四个条件 | `买家名称 + 品牌 + 品类 + 价格` |
| ... | 任意组合 |

## 注意事项

### 1. 识别准确性

- 系统会尽可能准确地识别每个部分的类型
- 如果无法识别为特定类型，会作为物料名称关键字
- 买家名称格式优先识别，避免误识别为品类

### 2. 大小写

- 所有搜索都是大小写不敏感的
- "AIR LIQUIDE" 和 "air liquide" 都能匹配

### 3. 部分匹配

- 所有字符串字段都支持部分匹配
- "AIR LIQUIDE" 可以匹配 "AIR LIQUIDE SINGAPORE PRIVATE LIMITED"

### 4. 空格处理

- "+" 前后的空格会被自动忽略
- `条件1 + 条件2` 和 `条件1+条件2` 效果相同

## 日志输出

当使用组合搜索时，会看到以下日志：

```
[INFO] Detected combined query with separator, splitting and parsing each part
[INFO] Split query into 2 parts: [AIR LIQUIDE SINGAPORE PRIVATE LIMITED, Electrical Accessories]
[INFO] Detected BuyerName from part (before parsing): AIR LIQUIDE SINGAPORE PRIVATE LIMITED
[INFO] Detected Category: 'Electrical Accessories' (matched keyword: 'electrical accessories')
[INFO] Search type set to COMBINED. Conditions: itemCode=false, category=true, function=false, brand=false, buyerName=true, buyerCode=false, itemNameKeyword=false
[INFO] Performing combined search with multiple conditions
[INFO] Performing combined search with criteria: buyerName=..., productHierarchy3=...
```

## 测试建议

测试以下组合查询，确认功能正常：

```
# 两个条件
AIR LIQUIDE SINGAPORE PRIVATE LIMITED + Electrical Accessories
AIR LIQUIDE + Site Safety Equipment
Site Safety Equipment + Maintenance Chemicals
Safety Shoes + Site Safety Equipment

# 三个条件
AIR LIQUIDE SINGAPORE + AIR LIQUIDE + Electrical Accessories
AIR LIQUIDE + Site Safety Equipment + unit cost 0-100

# 四个条件
AIR LIQUIDE SINGAPORE + AIR LIQUIDE + Electrical Accessories + unit cost 0-100
```

所有查询都应该能返回同时满足所有条件的数据！

