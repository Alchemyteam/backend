# 产品详情接口调用指南

## 接口概述

获取产品详情信息，基于 `sales_data` 表查询产品数据。

---

## 接口信息

### 接口地址

```
GET /api/buyer/products/{productId}
```

### 请求方法

`GET`

### 请求头

```
Authorization: Bearer {token}
Content-Type: application/json
```

**说明**：
- `Authorization`: 必需，JWT 认证 token
- `Content-Type`: 可选，但建议设置为 `application/json`

---

## 路径参数

| 参数名 | 类型 | 必需 | 说明 |
|--------|------|------|------|
| `productId` | string | 是 | 产品ID，支持两种格式：<br>1. **数字**（推荐）：数据库主键 `id`，如 `1234`<br>2. **ItemCode**（向后兼容）：产品代码，如 `TI00040` |

### 参数说明

- **数字格式**（推荐）：使用数据库的 `id` 字段，这是唯一的主键标识符
- **ItemCode 格式**：如果传入的是 ItemCode，系统会查找该 ItemCode 的第一条记录

---

## 响应格式

### 成功响应 (HTTP 200)

```json
{
  "id": "1234",
  "name": "LEAKAGE CURRENT CLAMP METER FLUKE 368 FC",
  "description": "Safety Equipment - Model XYZ - Plastic",
  "price": 299.99,
  "currency": "USD",
  "image": null,
  "images": null,
  "seller": {
    "id": "BUYER001",
    "name": "ABC Construction",
    "verified": false,
    "rating": null
  },
  "certification": null,
  "specifications": null,
  "stock": null,
  "rating": null,
  "reviewsCount": null,
  "category": "Site Safety Equipment",
  "tags": ["Safety Equipment", "AET", "Construction", "Protection"],
  "createdAt": "2024-01-20T10:30:00",
  "updatedAt": "2024-01-20T10:30:00",
  "historicalLowPrice": 250.00,
  "lastTransactionPrice": 299.99,
  "inWishlist": false
}
```

### 响应字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `id` | string | 产品ID（数据库主键，数字字符串） |
| `name` | string | 产品名称 |
| `description` | string \| null | 产品描述（由 ItemType、Model、Material 组合） |
| `price` | number | 当前价格（最新交易价格，默认 0） |
| `currency` | string | 货币单位（默认 "USD"） |
| `image` | string \| null | 主图片URL（当前为 null） |
| `images` | string[] \| null | 多图片URL数组（当前为 null） |
| `seller` | object \| null | 卖家信息 |
| `seller.id` | string | 卖家ID（BuyerCode） |
| `seller.name` | string | 卖家名称（BuyerName） |
| `seller.verified` | boolean | 是否认证（默认 false） |
| `seller.rating` | number \| null | 卖家评分 |
| `certification` | object \| null | 认证信息（当前为 null） |
| `specifications` | object \| null | 规格信息（当前为 null） |
| `stock` | number \| null | 库存数量（当前为 null） |
| `rating` | number \| null | 产品评分（当前为 null） |
| `reviewsCount` | number \| null | 评论数量（当前为 null） |
| `category` | string \| null | 产品分类（Product Hierarchy 3） |
| `tags` | string[] \| null | 标签数组（ItemType、Brand Code、Sector、Function） |
| `createdAt` | string | 创建时间（ISO 8601 格式） |
| `updatedAt` | string | 更新时间（ISO 8601 格式） |
| `historicalLowPrice` | number | 历史最低价（默认使用当前价格） |
| `lastTransactionPrice` | number | 最近交易价（最新交易价格） |
| `inWishlist` | boolean | 是否在愿望清单中（当前为 false） |

---

## 错误响应

### 404 - 产品不存在

```json
{
  "message": "Product not found with ID: 9999. Please use a valid product ID (number) or item code.",
  "errors": null,
  "error": null
}
```

### 401 - 未授权

```json
{
  "message": "Unauthorized",
  "errors": null,
  "error": null
}
```

### 400 - 请求参数错误

```json
{
  "message": "Product ID cannot be empty",
  "errors": null,
  "error": null
}
```

---

## TypeScript 类型定义

### 类型定义文件：`types/product.ts`

```typescript
// 产品详情响应类型
export interface ProductResponse {
  id: string;
  name: string;
  description: string | null;
  price: number;
  currency: string;
  image: string | null;
  images: string[] | null;
  seller: SellerResponse | null;
  certification: CertificationResponse | null;
  specifications: Record<string, any> | null;
  stock: number | null;
  rating: number | null;
  reviewsCount: number | null;
  category: string | null;
  tags: string[] | null;
  createdAt: string;
  updatedAt: string;
  historicalLowPrice: number;
  lastTransactionPrice: number;
  inWishlist: boolean;
}

// 卖家信息类型
export interface SellerResponse {
  id: string;
  name: string;
  verified: boolean;
  rating: number | null;
}

// 认证信息类型
export interface CertificationResponse {
  peCertified: boolean;
  certificateNumber?: string;
  certifiedBy?: string;
  certifiedDate?: string;
}
```

---

## 前端调用示例

### 1. 使用 Fetch API

```typescript
async function getProductDetail(productId: string, token: string): Promise<ProductResponse> {
  try {
    const response = await fetch(`http://localhost:8000/api/buyer/products/${productId}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('Product not found');
      }
      if (response.status === 401) {
        throw new Error('Unauthorized - Please login again');
      }
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const product: ProductResponse = await response.json();
    return product;
  } catch (error) {
    console.error('Error fetching product detail:', error);
    throw error;
  }
}

// 使用示例
const product = await getProductDetail('1234', userToken);
console.log('Product:', product.name);
console.log('Price:', product.price);
```

### 2. 使用 Axios

```typescript
import axios from 'axios';

async function getProductDetail(productId: string, token: string): Promise<ProductResponse> {
  try {
    const response = await axios.get<ProductResponse>(
      `http://localhost:8000/api/buyer/products/${productId}`,
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    );
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      if (error.response?.status === 404) {
        throw new Error('Product not found');
      }
      if (error.response?.status === 401) {
        throw new Error('Unauthorized - Please login again');
      }
    }
    throw error;
  }
}

// 使用示例
const product = await getProductDetail('1234', userToken);
```

### 3. React Hook 示例

```typescript
import { useState, useEffect } from 'react';

interface UseProductDetailResult {
  product: ProductResponse | null;
  loading: boolean;
  error: string | null;
}

function useProductDetail(productId: string | null, token: string): UseProductDetailResult {
  const [product, setProduct] = useState<ProductResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!productId || !token) {
      return;
    }

    const fetchProduct = async () => {
      setLoading(true);
      setError(null);
      
      try {
        const response = await fetch(
          `http://localhost:8000/api/buyer/products/${productId}`,
          {
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            }
          }
        );

        if (!response.ok) {
          if (response.status === 404) {
            throw new Error('Product not found');
          }
          throw new Error(`Failed to fetch product: ${response.status}`);
        }

        const data: ProductResponse = await response.json();
        setProduct(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Unknown error');
      } finally {
        setLoading(false);
      }
    };

    fetchProduct();
  }, [productId, token]);

  return { product, loading, error };
}

// 在组件中使用
function ProductDetailPage({ productId }: { productId: string }) {
  const token = localStorage.getItem('token') || '';
  const { product, loading, error } = useProductDetail(productId, token);

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;
  if (!product) return <div>Product not found</div>;

  return (
    <div>
      <h1>{product.name}</h1>
      <p>Price: ${product.price.toFixed(2)}</p>
      <p>Category: {product.category}</p>
      {product.tags && (
        <div>
          Tags: {product.tags.join(', ')}
        </div>
      )}
    </div>
  );
}
```

### 4. React Router 集成示例

```typescript
import { useParams } from 'react-router-dom';

function ProductDetailPage() {
  const { productId } = useParams<{ productId: string }>();
  const token = localStorage.getItem('token') || '';
  const { product, loading, error } = useProductDetail(productId || null, token);

  // ... 渲染逻辑
}
```

---

## 使用场景

### 场景 1：从产品列表跳转到详情页

```typescript
// 产品列表页面
function ProductListPage() {
  const products = [
    { id: '1234', name: 'Product 1' },
    { id: '5678', name: 'Product 2' }
  ];

  const handleProductClick = (productId: string) => {
    // 使用产品 ID 导航到详情页
    window.location.href = `/buyer/products/${productId}`;
    // 或使用 React Router
    // navigate(`/buyer/products/${productId}`);
  };

  return (
    <div>
      {products.map(product => (
        <div key={product.id} onClick={() => handleProductClick(product.id)}>
          {product.name}
        </div>
      ))}
    </div>
  );
}
```

### 场景 2：从 AI Search 表格跳转到详情页

```typescript
// AI Search 结果表格
function AISearchResults({ results }: { results: any[] }) {
  const handleRowClick = (itemCode: string) => {
    // 如果表格显示的是 ItemCode，可以直接使用
    // 系统会通过 ItemCode 查找对应的产品
    navigate(`/buyer/products/${itemCode}`);
  };

  return (
    <table>
      {results.map((row, index) => (
        <tr key={index} onClick={() => handleRowClick(row.ItemCode)}>
          <td>{row.ItemCode}</td>
          <td>{row.ItemName}</td>
          <td>{row.Price}</td>
        </tr>
      ))}
    </table>
  );
}
```

---

## 注意事项

### 1. 产品 ID 格式

- ✅ **推荐使用数字格式**：`1234`、`5678` 等（数据库主键 `id`）
- ✅ **支持 ItemCode 格式**：`TI00040` 等（向后兼容）
- ❌ **不要使用纯数字但无意义的数字**：如 `1000`、`9999` 等（除非确实是有效的产品 ID）

### 2. 价格字段处理

所有价格字段（`price`、`historicalLowPrice`、`lastTransactionPrice`）都保证不为 `null`，可以直接使用：

```typescript
// ✅ 安全：价格字段不会为 null
const price = product.price.toFixed(2);
const lowPrice = product.historicalLowPrice.toFixed(2);

// ❌ 不需要这样检查（但为了代码健壮性，建议还是检查）
if (product.price !== null) {
  const price = product.price.toFixed(2);
}
```

### 3. 可选字段处理

某些字段可能为 `null`，使用前需要检查：

```typescript
// 检查可选字段
if (product.description) {
  console.log(product.description);
}

if (product.seller) {
  console.log('Seller:', product.seller.name);
}

if (product.tags && product.tags.length > 0) {
  product.tags.forEach(tag => console.log(tag));
}
```

### 4. 错误处理

建议实现完善的错误处理：

```typescript
try {
  const product = await getProductDetail(productId, token);
  // 处理产品数据
} catch (error) {
  if (error.message === 'Product not found') {
    // 显示 404 页面
    showNotFoundPage();
  } else if (error.message.includes('Unauthorized')) {
    // 重新登录
    redirectToLogin();
  } else {
    // 显示通用错误
    showError('Failed to load product details');
  }
}
```

### 5. 认证 Token

确保在每次请求中都包含有效的 JWT token：

```typescript
// 从 localStorage 或状态管理获取 token
const token = localStorage.getItem('token');

// 如果 token 过期，需要重新登录
if (!token) {
  redirectToLogin();
}
```

---

## 完整示例：产品详情页面

```typescript
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

interface ProductDetailPageProps {}

const ProductDetailPage: React.FC<ProductDetailPageProps> = () => {
  const { productId } = useParams<{ productId: string }>();
  const navigate = useNavigate();
  const [product, setProduct] = useState<ProductResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProduct = async () => {
      if (!productId) {
        setError('Product ID is required');
        setLoading(false);
        return;
      }

      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/login');
        return;
      }

      try {
        setLoading(true);
        const response = await fetch(
          `http://localhost:8000/api/buyer/products/${productId}`,
          {
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            }
          }
        );

        if (response.status === 404) {
          setError('Product not found');
          return;
        }

        if (response.status === 401) {
          localStorage.removeItem('token');
          navigate('/login');
          return;
        }

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data: ProductResponse = await response.json();
        setProduct(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Unknown error');
      } finally {
        setLoading(false);
      }
    };

    fetchProduct();
  }, [productId, navigate]);

  if (loading) {
    return <div className="loading">Loading product details...</div>;
  }

  if (error) {
    return (
      <div className="error">
        <h2>Error</h2>
        <p>{error}</p>
        <button onClick={() => navigate('/buyer/products')}>
          Back to Products
        </button>
      </div>
    );
  }

  if (!product) {
    return <div>Product not found</div>;
  }

  return (
    <div className="product-detail">
      <h1>{product.name}</h1>
      
      {product.description && (
        <p className="description">{product.description}</p>
      )}

      <div className="price-info">
        <div className="current-price">
          <span className="label">Price:</span>
          <span className="value">${product.price.toFixed(2)}</span>
        </div>
        
        {product.historicalLowPrice && (
          <div className="low-price">
            <span className="label">Historical Low:</span>
            <span className="value">${product.historicalLowPrice.toFixed(2)}</span>
          </div>
        )}
      </div>

      {product.category && (
        <div className="category">
          <span className="label">Category:</span>
          <span>{product.category}</span>
        </div>
      )}

      {product.tags && product.tags.length > 0 && (
        <div className="tags">
          {product.tags.map((tag, index) => (
            <span key={index} className="tag">{tag}</span>
          ))}
        </div>
      )}

      {product.seller && (
        <div className="seller">
          <span className="label">Seller:</span>
          <span>{product.seller.name}</span>
          {product.seller.verified && (
            <span className="verified">✓ Verified</span>
          )}
        </div>
      )}

      <button onClick={() => navigate('/buyer/products')}>
        Back to Products
      </button>
    </div>
  );
};

export default ProductDetailPage;
```

---

## 测试示例

### 使用 curl 测试

```bash
# 通过数字 ID 查询
curl -X GET "http://localhost:8000/api/buyer/products/1234" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json"

# 通过 ItemCode 查询
curl -X GET "http://localhost:8000/api/buyer/products/TI00040" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json"
```

---

## 常见问题

### Q1: 我应该使用数字 ID 还是 ItemCode？

**A**: 推荐使用数字 ID（数据库主键），因为：
- 唯一性保证
- 查询性能更好
- 是标准的 RESTful API 实践

### Q2: 如果产品不存在会返回什么？

**A**: 返回 HTTP 404 状态码，响应体包含错误信息：
```json
{
  "message": "Product not found with ID: 9999. Please use a valid product ID (number) or item code."
}
```

### Q3: 价格字段可能为 null 吗？

**A**: 不会。所有价格字段都有默认值（至少为 0），可以直接使用 `toFixed()` 等方法。

### Q4: 如何获取产品列表中的产品 ID？

**A**: 产品列表接口返回的产品对象中包含 `id` 字段，直接使用即可：
```typescript
products.map(product => (
  <Link to={`/buyer/products/${product.id}`}>
    {product.name}
  </Link>
))
```

---

## 更新日志

- **2024-01-20**: 初始版本，支持通过数字 ID 和 ItemCode 查询产品详情

