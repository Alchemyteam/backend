# API 接口文档

## 基础信息

- **基础URL**: `http://localhost:8000/api`
- **Content-Type**: `application/json`
- **认证方式**: `Authorization: Bearer {token}` (除认证接口外，所有接口都需要认证)

---

## 一、认证相关接口 (Auth)

### 1. 用户登录

**POST** `/api/auth/login`

**请求头**: 无需认证

**请求体**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**成功响应** (HTTP 200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "user_id_123",
    "email": "user@example.com",
    "name": "User Name"
  }
}
```

**失败响应** (HTTP 401):
```json
{
  "message": "Invalid email or password",
  "errors": null,
  "error": null
}
```

---

### 2. 用户注册

**POST** `/api/auth/register`

**请求头**: 无需认证

**请求体**:
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "User Name"
}
```

**注意**: `name` 字段是可选的

**成功响应** (HTTP 200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "user_id_123",
    "email": "user@example.com",
    "name": "User Name"
  }
}
```

**失败响应** (HTTP 400):
```json
{
  "message": "Registration failed",
  "errors": {
    "email": ["Email already exists"]
  },
  "error": null
}
```

---

### 3. 获取当前用户信息

**GET** `/api/auth/me`

**请求头**: 
```
Authorization: Bearer {token}
```

**成功响应** (HTTP 200):
```json
{
  "id": "user_id_123",
  "email": "user@example.com",
  "name": "User Name"
}
```

**失败响应** (HTTP 401):
```json
{
  "message": "Unauthorized",
  "errors": null,
  "error": "Invalid or expired token"
}
```

---

### 4. 验证 Token

**GET** `/api/auth/verify`

**请求头**: 
```
Authorization: Bearer {token}
```

**成功响应** (HTTP 200):
```json
{
  "valid": true
}
```

**失败响应** (HTTP 401):
```json
{
  "message": "Token is invalid or expired",
  "errors": null,
  "error": null
}
```

---

## 二、买家门户接口 (Buyer)

### 1. 搜索产品

**GET** `/api/buyer/products/search`

**请求头**: 
```
Authorization: Bearer {token}
```

**查询参数**:
- `keyword` (string, 可选) - 搜索关键词
- `category` (string, 可选) - 产品分类
- `minPrice` (number, 可选) - 最低价格
- `maxPrice` (number, 可选) - 最高价格
- `page` (number, 可选) - 页码，默认1
- `limit` (number, 可选) - 每页数量，默认20

**示例**: `/api/buyer/products/search?keyword=formwork&category=steel&page=1&limit=20`

**成功响应** (HTTP 200):
```json
{
  "products": [
    {
      "id": "prod_123",
      "name": "Steel Formwork System",
      "description": "High-quality steel formwork system",
      "price": 299.99,
      "currency": "USD",
      "image": "https://example.com/image.jpg",
      "images": ["url1", "url2"],
      "seller": {
        "id": "seller_456",
        "name": "ABC Construction Supplies",
        "verified": true,
        "rating": 4.8
      },
      "certification": {
        "peCertified": true,
        "certificateNumber": "CERT-001",
        "certifiedBy": "PE-123",
        "certifiedDate": "2024-01-15"
      },
      "specifications": {
        "material": "Steel",
        "dimensions": "2m x 1m"
      },
      "stock": 50,
      "rating": 4.5,
      "reviewsCount": 23,
      "category": "steel",
      "tags": ["construction", "formwork"],
      "createdAt": "2024-01-20T10:30:00",
      "updatedAt": "2024-01-20T10:30:00",
      "historicalLowPrice": 250.00,
      "lastTransactionPrice": 299.99,
      "inWishlist": false
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 156,
    "totalPages": 8
  }
}
```

---

### 2. 获取特色产品

**GET** `/api/buyer/products/featured`

**请求头**: 
```
Authorization: Bearer {token}
```

**查询参数**:
- `limit` (number, 可选) - 返回数量，默认3

**成功响应** (HTTP 200):
```json
{
  "products": [
    {
      "id": "prod_123",
      "name": "Product 1",
      "description": "Product description",
      "price": 99.99,
      "currency": "USD",
      "image": "https://example.com/image.jpg",
      "images": ["url1"],
      "seller": {
        "id": "seller_456",
        "name": "Seller Name",
        "verified": true,
        "rating": 4.8
      },
      "certification": {
        "peCertified": true,
        "certificateNumber": "CERT-001"
      },
      "stock": 50,
      "rating": 4.5,
      "category": "steel",
      "tags": ["tag1"],
      "createdAt": "2024-01-20T10:30:00",
      "updatedAt": "2024-01-20T10:30:00",
      "historicalLowPrice": 90.00,
      "lastTransactionPrice": 99.99,
      "inWishlist": false
    }
  ],
  "pagination": null
}
```

---

### 3. 获取所有产品（浏览目录）

**GET** `/api/buyer/products`

**请求头**: 
```
Authorization: Bearer {token}
```

**查询参数**:
- `page` (number, 可选) - 页码，默认1
- `limit` (number, 可选) - 每页数量，默认20
- `sort` (string, 可选) - 排序方式：`price_asc`, `price_desc`, `rating_desc`, `newest`
- `category` (string, 可选) - 产品分类

**成功响应** (HTTP 200):
```json
{
  "products": [...],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 500,
    "totalPages": 25
  }
}
```

---

### 4. 获取产品详情

**GET** `/api/buyer/products/{productId}`

**请求头**: 
```
Authorization: Bearer {token}
```

**成功响应** (HTTP 200):
```json
{
  "id": "prod_123",
  "name": "Steel Formwork System",
  "description": "Detailed product description",
  "price": 299.99,
  "currency": "USD",
  "image": "https://example.com/image.jpg",
  "images": ["url1", "url2"],
  "seller": {
    "id": "seller_456",
    "name": "ABC Construction Supplies",
    "verified": true,
    "rating": 4.8
  },
  "certification": {
    "peCertified": true,
    "certificateNumber": "CERT-001",
    "certifiedBy": "PE-123",
    "certifiedDate": "2024-01-15"
  },
  "specifications": {
    "material": "Steel",
    "dimensions": "2m x 1m",
    "weight": "50kg"
  },
  "stock": 50,
  "rating": 4.5,
  "reviewsCount": 23,
  "category": "steel",
  "tags": ["construction", "formwork"],
  "createdAt": "2024-01-20T10:30:00",
  "updatedAt": "2024-01-20T10:30:00",
  "historicalLowPrice": 250.00,
  "lastTransactionPrice": 299.99,
  "inWishlist": false
}
```

---

### 5. 添加到购物车

**POST** `/api/buyer/cart/add`

**请求头**: 
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "productId": "prod_123",
  "quantity": 2
}
```

**成功响应** (HTTP 200):
```json
{
  "id": "cart_item_789",
  "product": null,
  "quantity": 2,
  "subtotal": 599.98
}
```

---

### 6. 获取购物车

**GET** `/api/buyer/cart`

**请求头**: 
```
Authorization: Bearer {token}
```

**成功响应** (HTTP 200):
```json
{
  "items": [
    {
      "id": "cart_item_789",
      "product": {
        "id": "prod_123",
        "name": "Steel Formwork System",
        "description": "Product description",
        "price": 299.99,
        "currency": "USD",
        "image": "url",
        "images": ["url1"],
        "seller": {
          "id": "seller_456",
          "name": "ABC Supplies",
          "verified": true,
          "rating": 4.8
        },
        "stock": 50,
        "rating": 4.5,
        "category": "steel",
        "tags": ["tag1"],
        "createdAt": "2024-01-20T10:30:00",
        "updatedAt": "2024-01-20T10:30:00",
        "historicalLowPrice": 250.00,
        "lastTransactionPrice": 299.99,
        "inWishlist": false
      },
      "quantity": 2,
      "subtotal": 599.98
    }
  ],
  "total": 599.98,
  "itemCount": 2
}
```

---

### 7. 从购物车移除

**DELETE** `/api/buyer/cart/{cartItemId}`

**请求头**: 
```
Authorization: Bearer {token}
```

**成功响应** (HTTP 200):
```json
{
  "message": "Product removed from cart",
  "errors": null,
  "error": null
}
```

---

### 8. 创建订单

**POST** `/api/buyer/orders`

**请求头**: 
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "items": [
    {
      "productId": "prod_123",
      "quantity": 2
    }
  ],
  "shippingAddress": {
    "street": "123 Main St",
    "city": "Singapore",
    "postalCode": "123456",
    "country": "Singapore"
  },
  "paymentMethod": "credit_card"
}
```

**成功响应** (HTTP 201):
```json
{
  "id": "order_456",
  "orderNumber": "ORD-2024-001",
  "status": "pending",
  "items": [
    {
      "productId": "prod_123",
      "productName": "Steel Formwork System",
      "quantity": 2,
      "price": 299.99,
      "subtotal": 599.98,
      "image": "url"
    }
  ],
  "shippingAddress": {
    "street": "123 Main St",
    "city": "Singapore",
    "postalCode": "123456",
    "country": "Singapore"
  },
  "subtotal": 599.98,
  "shipping": 50.00,
  "tax": 45.00,
  "total": 694.98,
  "currency": "USD",
  "trackingNumber": null,
  "createdAt": "2024-01-20T10:30:00",
  "updatedAt": "2024-01-20T10:30:00",
  "estimatedDelivery": null,
  "itemCount": 2
}
```

---

### 9. 获取订单列表

**GET** `/api/buyer/orders`

**请求头**: 
```
Authorization: Bearer {token}
```

**查询参数**:
- `status` (string, 可选) - 订单状态：`all`, `pending`, `processing`, `shipped`, `delivered`, `cancelled`
- `page` (number, 可选) - 页码，默认1
- `limit` (number, 可选) - 每页数量，默认20

**成功响应** (HTTP 200):
```json
{
  "orders": [
    {
      "id": "order_456",
      "orderNumber": "ORD-2024-001",
      "status": "shipped",
      "items": [
        {
          "productId": "prod_123",
          "productName": "Steel Formwork System",
          "quantity": 2,
          "price": 299.99,
          "subtotal": 599.98,
          "image": "url"
        }
      ],
      "shippingAddress": {
        "street": "123 Main St",
        "city": "Singapore",
        "postalCode": "123456",
        "country": "Singapore"
      },
      "subtotal": 599.98,
      "shipping": 50.00,
      "tax": 45.00,
      "total": 694.98,
      "currency": "USD",
      "trackingNumber": "TRACK-123456",
      "createdAt": "2024-01-20T10:30:00",
      "updatedAt": "2024-01-22T10:30:00",
      "estimatedDelivery": "2024-01-25T10:30:00",
      "itemCount": 2
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 24,
    "totalPages": 2
  }
}
```

---

### 10. 获取订单详情

**GET** `/api/buyer/orders/{orderId}`

**请求头**: 
```
Authorization: Bearer {token}
```

**成功响应** (HTTP 200):
```json
{
  "id": "order_456",
  "orderNumber": "ORD-2024-001",
  "status": "shipped",
  "items": [
    {
      "productId": "prod_123",
      "productName": "Steel Formwork System",
      "quantity": 2,
      "price": 299.99,
      "subtotal": 599.98,
      "image": "url"
    }
  ],
  "shippingAddress": {
    "street": "123 Main St",
    "city": "Singapore",
    "postalCode": "123456",
    "country": "Singapore"
  },
  "subtotal": 599.98,
  "shipping": 50.00,
  "tax": 45.00,
  "total": 694.98,
  "currency": "USD",
  "trackingNumber": "TRACK-123456",
  "createdAt": "2024-01-20T10:30:00",
  "updatedAt": "2024-01-22T10:30:00",
  "estimatedDelivery": "2024-01-25T10:30:00",
  "itemCount": 2
}
```

---

### 11. 添加到愿望清单

**POST** `/api/buyer/wishlist/add`

**请求头**: 
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "productId": "prod_123"
}
```

**成功响应** (HTTP 200):
```json
{
  "id": "wish_789",
  "product": {
    "id": "prod_123",
    "name": "Steel Formwork System",
    "description": "Product description",
    "price": 299.99,
    "currency": "USD",
    "image": "url",
    "images": ["url1"],
    "seller": {
      "id": "seller_456",
      "name": "ABC Supplies",
      "verified": true,
      "rating": 4.8
    },
    "stock": 50,
    "rating": 4.5,
    "category": "steel",
    "tags": ["tag1"],
    "createdAt": "2024-01-20T10:30:00",
    "updatedAt": "2024-01-20T10:30:00",
    "historicalLowPrice": 250.00,
    "lastTransactionPrice": 299.99,
    "inWishlist": true
  },
  "addedAt": "2024-01-20T10:30:00"
}
```

---

### 12. 获取愿望清单

**GET** `/api/buyer/wishlist`

**请求头**: 
```
Authorization: Bearer {token}
```

**查询参数**:
- `page` (number, 可选) - 页码，默认1
- `limit` (number, 可选) - 每页数量，默认20

**成功响应** (HTTP 200):
```json
{
  "items": [
    {
      "id": "wish_789",
      "product": {
        "id": "prod_123",
        "name": "Steel Formwork System",
        "description": "Product description",
        "price": 299.99,
        "currency": "USD",
        "image": "url",
        "images": ["url1"],
        "seller": {
          "id": "seller_456",
          "name": "ABC Supplies",
          "verified": true,
          "rating": 4.8
        },
        "stock": 50,
        "rating": 4.5,
        "category": "steel",
        "tags": ["tag1"],
        "createdAt": "2024-01-20T10:30:00",
        "updatedAt": "2024-01-20T10:30:00",
        "historicalLowPrice": 250.00,
        "lastTransactionPrice": 299.99,
        "inWishlist": true
      },
      "addedAt": "2024-01-20T10:30:00"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 12,
    "totalPages": 1
  }
}
```

---

### 13. 从愿望清单移除

**DELETE** `/api/buyer/wishlist/{wishlistItemId}`

**请求头**: 
```
Authorization: Bearer {token}
```

**成功响应** (HTTP 200):
```json
{
  "message": "Product removed from wishlist",
  "errors": null,
  "error": null
}
```

---

### 14. 获取买家统计数据

**GET** `/api/buyer/statistics`

**请求头**: 
```
Authorization: Bearer {token}
```

**查询参数**:
- `period` (string, 可选) - 统计周期：`week`, `month`, `year`，默认`month`

**成功响应** (HTTP 200):
```json
{
  "totalOrders": 24,
  "activeOrders": 5,
  "wishlistItems": 12,
  "totalSpent": 12500.50,
  "currency": "USD",
  "period": "month",
  "orderStatusBreakdown": {
    "pending": 2,
    "processing": 1,
    "shipped": 2,
    "delivered": 19,
    "cancelled": 0
  }
}
```

---

## 三、聊天服务接口 (Chat)

### 1. 发送聊天消息

**POST** `/api/chat/message`

**请求头**: 
```
Authorization: Bearer {token}
```

**请求体**:
```json
{
  "message": "Can you create a purchase requisition for battery?",
  "conversationId": "optional-conversation-id"
}
```

**成功响应** (HTTP 200):
```json
{
  "response": "I found 3 battery products. Here's the purchase requisition:",
  "conversationId": "uuid-here",
  "tableData": {
    "title": "Purchase Requisition - battery",
    "headers": ["Product Name", "Price", "Currency", "Stock", "Seller", "Category"],
    "rows": [
      {
        "Product Name": "Battery Type A",
        "Price": 99.99,
        "Currency": "USD",
        "Stock": 50,
        "Seller": "ABC Supplies",
        "Category": "electronics"
      },
      {
        "Product Name": "Battery Type B",
        "Price": 149.99,
        "Currency": "USD",
        "Stock": 30,
        "Seller": "XYZ Electronics",
        "Category": "electronics"
      }
    ],
    "description": "Found 3 product(s) matching your search."
  },
  "actionData": {
    "actionType": "CREATE_REQUISITION",
    "parameters": {
      "productKeyword": "battery",
      "productCount": 3,
      "productIds": ["prod_001", "prod_002", "prod_003"]
    },
    "message": "Purchase requisition created for 3 product(s)."
  }
}
```

**响应字段说明**:
- `response`: LLM生成的文本回复
- `conversationId`: 对话ID，用于多轮对话
- `tableData`: 表格数据（可选）
  - `title`: 表格标题
  - `headers`: 表头数组
  - `rows`: 表格行数据数组（每行是一个对象，key为表头，value为数据）
  - `description`: 表格描述
- `actionData`: 操作数据（可选）
  - `actionType`: 操作类型（CREATE_REQUISITION, SEARCH_PRODUCTS等）
  - `parameters`: 操作参数
  - `message`: 操作结果消息

---

### 2. 健康检查

**GET** `/api/chat/health`

**请求头**: 无需认证

**成功响应** (HTTP 200):
```json
{
  "status": "ok",
  "service": "chat"
}
```

---

## 错误响应格式

所有错误响应都遵循统一格式：

```json
{
  "message": "主要错误信息",
  "errors": {
    "field_name": ["错误信息1", "错误信息2"]
  },
  "error": "详细错误信息（用于401错误）"
}
```

### HTTP 状态码

- **200**: 请求成功
- **201**: 创建成功
- **400**: 请求参数错误（如验证失败）
- **401**: 未授权（token无效或过期）
- **404**: 资源不存在
- **500**: 服务器内部错误

---

## 完整接口列表

### 认证接口 (无需token)
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册
- `GET /api/auth/verify` - 验证Token
- `GET /api/auth/me` - 获取当前用户信息（需要token）

### 买家接口 (需要token)
- `GET /api/buyer/products/search` - 搜索产品
- `GET /api/buyer/products/featured` - 获取特色产品
- `GET /api/buyer/products` - 获取所有产品
- `GET /api/buyer/products/{productId}` - 获取产品详情
- `POST /api/buyer/cart/add` - 添加到购物车
- `GET /api/buyer/cart` - 获取购物车
- `DELETE /api/buyer/cart/{cartItemId}` - 从购物车移除
- `POST /api/buyer/orders` - 创建订单
- `GET /api/buyer/orders` - 获取订单列表
- `GET /api/buyer/orders/{orderId}` - 获取订单详情
- `POST /api/buyer/wishlist/add` - 添加到愿望清单
- `GET /api/buyer/wishlist` - 获取愿望清单
- `DELETE /api/buyer/wishlist/{wishlistItemId}` - 从愿望清单移除
- `GET /api/buyer/statistics` - 获取买家统计数据

### 聊天接口 (需要token)
- `POST /api/chat/message` - 发送聊天消息
- `GET /api/chat/health` - 健康检查（无需token）

---

## 使用示例

### 完整流程示例

1. **登录获取Token**:
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

2. **搜索产品**:
```http
GET /api/buyer/products/search?keyword=battery&page=1&limit=20
Authorization: Bearer {token}
```

3. **添加到购物车**:
```http
POST /api/buyer/cart/add
Authorization: Bearer {token}
Content-Type: application/json

{
  "productId": "prod_123",
  "quantity": 2
}
```

4. **创建订单**:
```http
POST /api/buyer/orders
Authorization: Bearer {token}
Content-Type: application/json

{
  "items": [{"productId": "prod_123", "quantity": 2}],
  "shippingAddress": {
    "street": "123 Main St",
    "city": "Singapore",
    "postalCode": "123456",
    "country": "Singapore"
  },
  "paymentMethod": "credit_card"
}
```

5. **使用聊天服务**:
```http
POST /api/chat/message
Authorization: Bearer {token}
Content-Type: application/json

{
  "message": "Can you create a purchase requisition for battery?"
}
```


