# 字段映射说明

## 数据库字段与前端字段的映射关系

### Product Hierarchy 3 → Category

#### 数据库层面
- **数据库字段名**: `Product Hierarchy 3` (带空格，需要反引号)
- **实体类字段**: `productHierarchy3` (Java 驼峰命名)
- **实体类注解**: `@Column(name = "`Product Hierarchy 3`")`

#### 后端处理层面
- **搜索条件**: `MaterialSearchCriteria.productHierarchy3`
- **搜索方法**: `findByProductHierarchy3(String productHierarchy3)`
- **SQL 查询**: `WHERE LOWER(\`Product Hierarchy 3\`) = LOWER(:productHierarchy3)`

#### 前端返回层面
- **表格列名**: `"Category"` (用户友好的名称)
- **数据值**: `salesData.getProductHierarchy3()` 的值
- **JSON 字段**: `"Category": "Site Safety Equipment"`

### 完整映射流程

```
数据库: Product Hierarchy 3 = "Site Safety Equipment"
  ↓
实体类: productHierarchy3 = "Site Safety Equipment"
  ↓
搜索条件: criteria.productHierarchy3 = "Site Safety Equipment"
  ↓
SQL 查询: WHERE LOWER(`Product Hierarchy 3`) = LOWER('Site Safety Equipment')
  ↓
查询结果: SalesData.productHierarchy3 = "Site Safety Equipment"
  ↓
表格数据: row.put("Category", "Site Safety Equipment")
  ↓
前端接收: { "Category": "Site Safety Equipment" }
```

## 代码位置

### 1. 实体类定义
**文件**: `src/main/java/com/ecosystem/entity/SalesData.java`
```java
@Column(name = "`Product Hierarchy 3`", length = 255)
private String productHierarchy3;
```

### 2. DTO 定义
**文件**: `src/main/java/com/ecosystem/dto/buyer/SalesDataResponse.java`
```java
@JsonProperty("Product Hierarchy 3")
private String productHierarchy3;
```

### 3. 搜索条件
**文件**: `src/main/java/com/ecosystem/dto/chat/MaterialSearchCriteria.java`
```java
private String productHierarchy3;  // 产品分类层级3
```

### 4. 表格数据构建
**文件**: `src/main/java/com/ecosystem/service/ChatService.java`
```java
// 表格列名使用 "Category"（用户友好）
tableData.setHeaders(Arrays.asList("Item Code", "Item Name", "Price", "Date", "Category", "Brand", "Function"));

// 数据值来自 productHierarchy3
row.put("Category", salesData.getProductHierarchy3() != null ? salesData.getProductHierarchy3() : "N/A");
```

### 5. 数据库查询
**文件**: `src/main/java/com/ecosystem/repository/SalesDataRepository.java`
```java
@Query(value = "SELECT ... FROM ecoschema.sales_data " +
       "WHERE LOWER(`Product Hierarchy 3`) = LOWER(:productHierarchy3) ...")
List<SalesData> findByProductHierarchy3(@Param("productHierarchy3") String productHierarchy3, @Param("limit") int limit);
```

## 前端使用

### 接收数据
```typescript
interface TableData {
  headers: string[];  // ["Item Code", "Item Name", "Price", "Date", "Category", "Brand", "Function"]
  rows: Array<{
    "Item Code": string;
    "Item Name": string;
    "Price": string;
    "Date": string;
    "Category": string;  // ← 这里使用 "Category"
    "Brand": string;
    "Function": string;
  }>;
}
```

### 显示数据
```typescript
// 前端可以直接使用 "Category" 字段
const category = row["Category"];  // "Site Safety Equipment"
```

## 搜索时的处理

### 用户输入
```
"Site Safety Equipment"
"category: Site Safety Equipment"
"查找 Site Safety Equipment 类的产品"
```

### 后端解析
1. **关键词映射**: `"site safety equipment"` → `productHierarchy3 = "Site Safety Equipment"`
2. **智能识别**: 如果查询格式像品类名称 → `productHierarchy3 = "Site Safety Equipment"`
3. **搜索执行**: `WHERE LOWER(\`Product Hierarchy 3\`) = LOWER('Site Safety Equipment')`

### 返回结果
```json
{
  "tableData": {
    "headers": ["Item Code", "Item Name", "Price", "Date", "Category", "Brand", "Function"],
    "rows": [
      {
        "Item Code": "TI00040",
        "Item Name": "LEAKAGE CURRENT CLAMP METER",
        "Price": "299.99",
        "Date": "2024-01-20",
        "Category": "Site Safety Equipment",  // ← 数据库中的 Product Hierarchy 3 值
        "Brand": "AET",
        "Function": "Measurement"
      }
    ]
  }
}
```

## 总结

✅ **映射关系是正确的**：
- 数据库字段：`Product Hierarchy 3`
- 后端实体：`productHierarchy3`
- 前端列名：`Category`（用户友好）
- 数据值：来自 `Product Hierarchy 3` 字段

✅ **搜索功能正常**：
- 用户输入品类名称 → 识别为品类搜索
- 查询 `Product Hierarchy 3` 字段
- 返回结果中显示为 `Category` 列

✅ **前后端一致**：
- 前端接收的字段名是 `Category`
- 后端返回的字段名也是 `Category`
- 数据值来自数据库的 `Product Hierarchy 3` 字段

## 注意事项

1. **数据库字段名**: 使用反引号包裹，因为包含空格
   ```sql
   `Product Hierarchy 3`
   ```

2. **大小写不敏感**: 所有查询都使用 `LOWER()` 函数
   ```sql
   WHERE LOWER(`Product Hierarchy 3`) = LOWER(:productHierarchy3)
   ```

3. **前端显示**: 使用 `Category` 作为列名，更符合用户习惯

4. **搜索识别**: 支持多种方式识别品类搜索
   - 关键词映射（"site safety equipment"）
   - 智能识别（首字母大写，多个单词）

