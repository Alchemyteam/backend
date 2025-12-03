# AI 物料搜索 - 快速参考

## 🚀 快速开始

**接口**: `POST /api/chat/message`  
**认证**: `Authorization: Bearer {token}`

```typescript
const response = await fetch('/api/chat/message', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    message: '你的查询',
  }),
});
```

---

## 📋 可以问的问题类型

### 1️⃣ 按物料编码精确搜索

**返回：所有历史交易 + 价格统计**

```
TI00040
查找物料编码 TI00040
ItemCode = TI00040
显示 TI00040 的所有历史交易
TI00040 的价格区间是多少
物料编码 TI00040 的历史记录
```

**返回信息：**
- ✅ 所有历史交易记录
- ✅ 价格区间（最低价、最高价、平均价）
- ✅ 交易日期范围（最早、最晚）

---

### 2️⃣ 按物料名称关键字模糊搜索

**支持中文和英文**

```
安全鞋
喷漆
储物箱
LEAKAGE CURRENT CLAMP METER
FLUKE 368 FC
安全帽
防护手套
测量仪器
```

---

### 3️⃣ 按品类搜索

```
Site Safety Equipment
安全设备
Filters
过滤器
Maintenance Chemicals
维护化学品
Site Safety Equipment 类的所有产品
显示所有安全设备
```

---

### 4️⃣ 按功能搜索

```
Maintenance Chemicals
维护化学品
Safety
安全
Protection
防护
```

---

### 5️⃣ 按品牌搜索

```
品牌 AET
brand AET
Air Liquide 品牌
显示 AET 的所有产品
AET 品牌的产品有哪些
查找品牌代码 AET
```

---

### 6️⃣ 组合条件搜索

#### 品类 + 品牌 + 时间

```
Site Safety Equipment + Air Liquide + 去年
安全设备 + AET + 去年
Filters 类 + AET + 今年
Site Safety Equipment + Air Liquide + 2023年
```

#### 品类 + 品牌 + 价格区间

```
Filters 类 + AET + 单价 100 到 500
Site Safety Equipment + Air Liquide + 价格 50-200
安全设备 + 品牌 AET + 100元到500元
```

#### 物料名称 + 品类

```
安全鞋 + Site Safety Equipment
喷漆 + Maintenance Chemicals
储物箱 + Filters
```

#### 品类 + 功能

```
Site Safety Equipment + Safety
Filters + Protection
安全设备 + 安全功能
```

#### 多条件组合

```
Site Safety Equipment + Air Liquide + 去年 + 价格 100-500
Filters 类 + AET + 今年 + 单价区间 50-200
安全设备 + 品牌 AET + 2023年 + 100到500元
```

---

### 7️⃣ 时间范围搜索

```
去年的交易
今年的数据
2023年的记录
最近一年的交易
```

---

### 8️⃣ 价格区间搜索

```
价格 100 到 500
单价区间 50-200
100元到500元的产品
价格范围 50 至 200
```

---

### 9️⃣ 复杂组合查询

```
查找 Site Safety Equipment 类别下，Air Liquide 品牌，去年，价格在 100-500 之间的所有产品
显示 Filters 类，AET 品牌，今年，单价 50-200 的所有物料
安全设备 + Air Liquide + 去年 + 价格区间 100-500
```

---

## 💡 搜索技巧

### ✅ 推荐做法

1. **使用物料编码**（最准确）
   - ✅ `TI00040`
   - ❌ `ti00040`（建议大写）

2. **使用完整关键词**
   - ✅ `LEAKAGE CURRENT CLAMP METER FLUKE 368 FC`
   - ✅ `安全鞋`

3. **组合搜索使用分隔符**
   - ✅ `Site Safety Equipment + Air Liquide + 去年`
   - ✅ `Filters 类 + AET + 价格 100-500`

4. **使用标准品类名称**
   - ✅ `Site Safety Equipment`
   - ✅ `Maintenance Chemicals`

### ❌ 避免的做法

- ❌ 拼写错误
- ❌ 使用不存在的品类名称
- ❌ 品牌代码拼写错误

---

## 📊 响应数据示例

### 精确物料编码搜索响应

```json
{
  "response": "Found 15 historical transactions for Item Code: TI00040 (LEAKAGE CURRENT CLAMP METER). Price range: 100.00 - 500.00, Average: 250.00. First transaction: 2023-01-15, Last transaction: 2024-12-20.",
  "tableData": {
    "title": "Material Search Results",
    "headers": ["Item Code", "Item Name", "Price", "Date", "Category", "Brand", "Function"],
    "rows": [
      {
        "Item Code": "TI00040",
        "Item Name": "LEAKAGE CURRENT CLAMP METER FLUKE 368 FC",
        "Price": "299.99",
        "Date": "2024-01-20",
        "Category": "Site Safety Equipment",
        "Brand": "AET",
        "Function": "Measurement"
      }
    ],
    "description": "Found 15 material record(s)."
  }
}
```

### 模糊搜索响应

```json
{
  "response": "Found 8 material record(s) matching your search.",
  "tableData": {
    "title": "Material Search Results",
    "headers": ["Item Code", "Item Name", "Price", "Date", "Category", "Brand", "Function"],
    "rows": [
      {
        "Item Code": "TI00040",
        "Item Name": "LEAKAGE CURRENT CLAMP METER FLUKE 368 FC",
        "Price": "299.99",
        "Date": "2024-01-20",
        "Category": "Site Safety Equipment",
        "Brand": "AET",
        "Function": "Measurement"
      }
    ],
    "description": "Found 8 material record(s)."
  }
}
```

---

## 🔧 前端代码示例

### 最简单的调用

```typescript
const response = await fetch('/api/chat/message', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    message: 'TI00040',
  }),
});

const data = await response.json();
console.log('AI回复:', data.response);
console.log('表格数据:', data.tableData);
```

### 使用 Hook

```typescript
import { useChatSearch } from '@/hooks/useChatSearch';

function SearchComponent() {
  const token = localStorage.getItem('token') || '';
  const { sendMessage, response, loading } = useChatSearch(token);
  
  const handleSearch = async () => {
    await sendMessage('TI00040');
  };
  
  return (
    <div>
      <button onClick={handleSearch} disabled={loading}>
        搜索
      </button>
      {response?.tableData && (
        <table>
          {/* 渲染表格 */}
        </table>
      )}
    </div>
  );
}
```

---

## ❓ 常见问题

**Q: 搜索返回空结果怎么办？**  
A: 检查拼写，尝试使用物料编码，或使用更通用的关键词。

**Q: 如何获取历史统计？**  
A: 使用物料编码进行精确搜索，系统会自动返回统计信息。

**Q: 支持哪些语言？**  
A: 中文（简体）和英文，可以混合使用。

---

## 📚 完整文档

详细文档请参考：`FRONTEND_AI_SEARCH_GUIDE.md`

