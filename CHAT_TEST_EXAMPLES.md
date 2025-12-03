# 聊天服务测试示例

## 会返回 tableData 和 actionData 的查询类型

### 1. 创建采购需求 (CREATE_REQUISITION)

这些查询会返回 **tableData** 和 **actionData**：

✅ **推荐测试问题**：
- "Can you create a purchase requisition for battery?"
- "Create a purchase requisition for steel"
- "Make a requisition for formwork"
- "I need to create a purchase requisition for concrete"
- "Can you make a purchase order for battery?"

**返回的数据**：
- `tableData`: 包含匹配产品的表格（产品名称、价格、库存、卖家、分类）
- `actionData`: 包含操作类型和产品信息

---

### 2. 搜索产品 (SEARCH_PRODUCTS)

这些查询会返回 **tableData**：

✅ **推荐测试问题**：
- "Search for battery"
- "Find steel products"
- "Show me formwork"
- "List concrete products"
- "Search products for construction"

**返回的数据**：
- `tableData`: 包含搜索结果的表格（ID、名称、价格、库存、评分、分类）

---

### 3. 获取产品信息 (GET_PRODUCT_INFO)

这些查询会返回 **tableData**：

✅ **推荐测试问题**：
- "Tell me about battery"
- "What is the info about steel formwork?"
- "Details about concrete mixer"
- "Tell me about product battery"

**返回的数据**：
- `tableData`: 包含产品详细信息的表格（字段-值对）

---

### 4. 比较产品 (COMPARE_PRODUCTS)

这些查询会返回 **tableData**：

✅ **推荐测试问题**：
- "Compare battery and steel"
- "Compare formwork vs concrete"
- "Compare products battery versus steel"

**返回的数据**：
- `tableData`: 包含产品对比表格

---

## 数据库要求

### 确保数据库中有产品数据

你的数据库中需要有产品，产品名称、描述或分类中包含以下关键词：

**测试关键词**：
- `battery` - 电池相关产品
- `steel` - 钢材相关产品
- `formwork` - 模板相关产品
- `concrete` - 混凝土相关产品
- `construction` - 建筑相关产品

### 检查数据库

执行以下 SQL 查询检查是否有产品：

```sql
USE ecoschema;

-- 查看所有产品
SELECT id, name, category, stock FROM products;

-- 查看包含 "battery" 的产品
SELECT * FROM products 
WHERE name LIKE '%battery%' 
   OR description LIKE '%battery%' 
   OR category LIKE '%battery%';

-- 查看包含 "steel" 的产品
SELECT * FROM products 
WHERE name LIKE '%steel%' 
   OR description LIKE '%steel%' 
   OR category LIKE '%steel%';
```

### 如果没有产品数据

执行以下 SQL 插入测试数据：

```sql
USE ecoschema;

-- 确保有卖家
INSERT INTO sellers (id, name, verified, rating) VALUES
('seller_001', 'ABC Construction Supplies', 1, 4.8),
('seller_002', 'XYZ Building Materials', 1, 4.5)
ON DUPLICATE KEY UPDATE name=name;

-- 插入测试产品
INSERT INTO products (id, name, description, price, currency, image, seller_id, category, stock, rating, reviews_count) VALUES
('prod_battery_001', 'Battery Type A', 'High-quality construction battery', 99.99, 'USD', 'https://example.com/battery1.jpg', 'seller_001', 'electronics', 50, 4.5, 23),
('prod_battery_002', 'Battery Type B', 'Professional grade battery for construction equipment', 149.99, 'USD', 'https://example.com/battery2.jpg', 'seller_002', 'electronics', 30, 4.7, 15),
('prod_steel_001', 'Steel Formwork System', 'High-quality steel formwork system for construction', 299.99, 'USD', 'https://example.com/steel1.jpg', 'seller_001', 'steel', 50, 4.5, 23),
('prod_steel_002', 'Steel Rebar', 'High-strength steel rebar', 89.99, 'USD', 'https://example.com/steel2.jpg', 'seller_001', 'steel', 100, 4.6, 31),
('prod_concrete_001', 'Concrete Mixer', 'Professional grade concrete mixer', 599.99, 'USD', 'https://example.com/concrete1.jpg', 'seller_002', 'equipment', 30, 4.7, 15)
ON DUPLICATE KEY UPDATE name=name;
```

---

## 测试步骤

### 1. 确保数据库有数据

```sql
SELECT COUNT(*) FROM products;
-- 应该返回 > 0
```

### 2. 测试创建采购需求

```bash
POST /api/chat/message
Authorization: Bearer {token}

{
  "message": "Can you create a purchase requisition for battery?"
}
```

**预期响应**：
```json
{
  "response": "...",
  "tableData": {
    "title": "Purchase Requisition - battery",
    "headers": ["Product Name", "Price", "Currency", "Stock", "Seller", "Category"],
    "rows": [
      {
        "Product Name": "Battery Type A",
        "Price": 99.99,
        "Currency": "USD",
        "Stock": 50,
        "Seller": "ABC Construction Supplies",
        "Category": "electronics"
      }
    ]
  },
  "actionData": {
    "actionType": "CREATE_REQUISITION",
    "parameters": {...}
  }
}
```

### 3. 测试搜索产品

```bash
POST /api/chat/message
Authorization: Bearer {token}

{
  "message": "Search for steel products"
}
```

---

## 常见问题

### Q: 为什么 tableData 是 null？

**A**: 可能的原因：
1. 数据库中没有产品数据
2. 产品名称/描述/分类中不包含查询的关键词
3. 意图识别失败（消息格式不匹配）

**解决方法**：
- 检查数据库是否有产品
- 使用明确的关键词（battery, steel, formwork, concrete）
- 使用推荐的测试问题格式

### Q: 为什么 actionData 是 null？

**A**: actionData 只在以下情况返回：
- CREATE_REQUISITION 意图（创建采购需求）
- 其他意图（SEARCH, INFO, COMPARE）不返回 actionData

**解决方法**：
- 使用 "create purchase requisition for X" 格式的问题

### Q: 如何知道意图识别是否成功？

**A**: 查看后端日志：
```
Detected intent: CREATE_REQUISITION
Extracted product keyword: battery
Found 2 products for keyword: battery
```

---

## 快速测试清单

✅ 数据库中有产品数据
✅ 产品名称/描述包含测试关键词（battery, steel等）
✅ 使用推荐的测试问题格式
✅ 检查后端日志确认意图识别和产品搜索
✅ 确认 API Key 配置正确

---

## 推荐的测试问题（按优先级）

1. **"Can you create a purchase requisition for battery?"** ⭐⭐⭐
   - 最可能返回完整数据（tableData + actionData）

2. **"Search for steel products"** ⭐⭐
   - 返回 tableData

3. **"Show me formwork products"** ⭐⭐
   - 返回 tableData

4. **"Tell me about battery"** ⭐
   - 返回 tableData（产品详情）

5. **"Compare battery and steel"** ⭐
   - 返回 tableData（对比表格）


