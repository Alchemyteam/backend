# SalesData 表 id 字段更新总结

## 更新内容

已将所有相关代码更新为使用新的 `id` 自增主键字段。

---

## 1. 实体类 (SalesData.java)

### 已更新
- ✅ `id` 字段已配置为主键（`@Id`）
- ✅ 使用 `@GeneratedValue(strategy = GenerationType.IDENTITY)` 自增
- ✅ 字段类型为 `Long`

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "id")
private Long id;
```

---

## 2. Repository (SalesDataRepository.java)

### 已更新
- ✅ 泛型类型从 `JpaRepository<SalesData, String>` 改为 `JpaRepository<SalesData, Long>`
- ✅ 所有返回 `SalesData` 的查询方法都已更新，在 SELECT 中包含 `id` 字段
- ✅ 删除了 `findByIdNative` 方法（不再需要，直接使用 JpaRepository 的 `findById`）

### 更新的查询方法列表

以下查询方法已更新，SELECT 语句中包含 `id` 字段：

1. `findByCategoryOrderByTxDateDesc` - 按分类和日期排序
2. `findByCategoryOrderByTxP1Asc` - 按分类和价格升序
3. `findByCategoryOrderByTxP1Desc` - 按分类和价格降序
4. `findAllByOrderByTxDateDesc` - 按日期降序
5. `findAllByOrderByTxP1Asc` - 按价格升序
6. `findAllByOrderByTxP1Desc` - 按价格降序
7. `searchByItemName` - 按产品名称搜索
8. `findByItemCode` - 按 ItemCode 查询
9. `searchByItemNameKeyword` - 按关键词搜索
10. `findByProductHierarchy3` - 按分类查询
11. `findByFunction` - 按功能查询
12. `findByBrandCode` - 按品牌查询
13. `searchByCombinedCriteria` - 组合条件搜索
14. `fullTextSearch` - 全文搜索
15. `findAllWithFilters` - 完整过滤查询
16. `findFirstByItemCode` - 按 ItemCode 查询第一条（向后兼容）

### 保留的方法

- `findByIdNative` - **已删除**（使用 JpaRepository 的 `findById` 即可）
- `getPriceStatisticsByItemCode` - 价格统计查询（不返回 SalesData，无需更新）

---

## 3. Service (BuyerProductService.java)

### 已更新
- ✅ `getProductDetail()` 方法已更新
- ✅ 优先使用 `salesDataRepository.findById(id)` 查询（JpaRepository 提供的方法）
- ✅ 向后兼容：如果 `productId` 不是数字，尝试通过 `ItemCode` 查询
- ✅ 返回的 `id` 字段使用数据库的 `id`（数字字符串）

### 查询逻辑

```java
// 1. 优先通过 id（数字）查询
Long id = Long.parseLong(productId);
salesData = salesDataRepository.findById(id).orElse(null);

// 2. 如果失败，通过 ItemCode 查询（向后兼容）
if (salesData == null) {
    salesData = salesDataRepository.findFirstByItemCode(productId);
}

// 3. 返回的 id 使用数据库的 id
response.setId(salesData.getId().toString());
```

---

## 4. Service (SalesDataService.java)

### 已更新
- ✅ `updateSalesData()` 方法已更新，支持通过 `id` 查询
- ✅ `deleteSalesData()` 方法已更新，支持通过 `id` 查询

---

## 5. 异常处理

### 已创建
- ✅ `ProductNotFoundException` 异常类
- ✅ `GlobalExceptionHandler` 已更新，处理 `ProductNotFoundException`，返回 404 状态码

---

## 使用方式

### 通过 id 查询（推荐）

```java
// 使用 JpaRepository 的 findById 方法
Optional<SalesData> salesData = salesDataRepository.findById(1234L);
```

### 通过 ItemCode 查询（向后兼容）

```java
// 使用自定义查询方法
SalesData salesData = salesDataRepository.findFirstByItemCode("TI00040");
```

---

## API 接口使用

### 产品详情接口

**接口地址**: `GET /api/buyer/products/{productId}`

**支持的 productId 格式**：
1. **数字格式**（推荐）：`1234` - 使用数据库主键 `id`
2. **ItemCode 格式**（向后兼容）：`TI00040` - 使用 ItemCode 查询

**返回的 id 字段**：
- 始终返回数据库的 `id` 字段（数字字符串）
- 例如：`"id": "1234"`

---

## 验证清单

- [x] 实体类 `SalesData` 有 `id` 字段（Long 类型，主键）
- [x] Repository 泛型类型为 `JpaRepository<SalesData, Long>`
- [x] 所有返回 `SalesData` 的查询都包含 `id` 字段
- [x] Service 使用 `findById()` 方法查询
- [x] 产品详情接口支持通过数字 id 查询
- [x] 产品详情接口向后兼容 ItemCode 查询
- [x] 返回的响应包含正确的 `id` 字段

---

## 注意事项

1. **数据库表结构**：确保 `sales_data` 表有 `id` 字段，类型为 `BIGINT AUTO_INCREMENT`，并且是主键

2. **现有数据**：如果表中已有数据，`id` 字段应该已经有值（自增生成）

3. **查询性能**：使用 `id` 查询比使用 `ItemCode` 查询性能更好，因为 `id` 是主键

4. **唯一性**：`id` 字段保证唯一性，适合作为产品标识符

---

## 测试建议

### 1. 测试通过 id 查询

```bash
curl -X GET "http://localhost:8000/api/buyer/products/1234" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 2. 测试通过 ItemCode 查询（向后兼容）

```bash
curl -X GET "http://localhost:8000/api/buyer/products/TI00040" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 3. 验证返回的 id 字段

响应中应该包含：
```json
{
  "id": "1234",  // 数据库的 id 字段
  "name": "...",
  ...
}
```

---

## 更新完成

所有相关代码已更新完成，现在系统使用 `id` 字段作为 `sales_data` 表的主键。

