# Punchout API 接口文档

## 📋 目录

- [概述](#概述)
- [基础信息](#基础信息)
- [接口列表](#接口列表)
- [数据模型](#数据模型)
- [错误处理](#错误处理)
- [前端集成指南](#前端集成指南)
- [示例代码](#示例代码)

---

## 概述

Punchout 是一种标准的 B2B 电子采购集成方式，允许采购系统的用户直接访问供应商的购物网站进行采购，然后将购物车返回给采购系统。

### 工作流程

1. **Setup（设置）**: 采购系统发起 Punchout 请求，供应商系统验证用户并生成会话
2. **Shopping（购物）**: 用户在前端购物网站浏览和选择商品
3. **Return（返回）**: 用户提交购物车，前端调用 Return 接口将购物车数据返回给采购系统

---

## 基础信息

### Base URL

```
开发环境: http://localhost:8000/api
生产环境: https://your-domain.com/api
```

### Content-Type

- JSON 接口: `application/json`
- cXML 接口: `application/xml` 或 `text/xml`

### 认证方式

Punchout 接口使用 **Session Token** 进行认证：

- Setup 成功后，后端会返回 `sessionToken`
- 前端需要将 `sessionToken` 存储在本地（localStorage/sessionStorage）
- 后续的 Return 和 Validate 请求都需要携带 `sessionToken`

### 会话管理

- 会话有效期：**30 分钟**
- 会话过期后需要重新发起 Setup 请求
- 前端可以通过 `/punchout/validate` 接口验证会话是否有效

---

## 接口列表

### 1. 验证会话

验证会话 token 是否有效。

**接口地址**

```
GET /punchout/validate
```

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionToken | string | 是 | 会话 token（从 Setup 响应中获取） |

**请求示例**

```http
GET /api/punchout/validate?sessionToken=abc123xyz456
```

**响应示例**

```json
{
  "valid": true
}
```

**响应字段**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| valid | boolean | 会话是否有效 |

**错误响应**

```json
{
  "valid": false,
  "error": "Session expired or invalid"
}
```

**使用场景**

- 页面加载时验证会话是否有效
- 定期检查会话状态（如每 5 分钟检查一次）
- 提交购物车前验证会话

---

### 2. 返回购物车（Return）

将购物车数据返回给采购系统。这是 Punchout 流程的核心接口。

**接口地址**

```
POST /punchout/return
```

**请求头**

```
Content-Type: application/json
```

**请求体**

```json
{
  "sessionToken": "abc123xyz456",
  "items": [
    {
      "productId": "PROD-001",
      "sku": "SKU-001",
      "productName": "产品名称",
      "quantity": 2,
      "unitPrice": 99.99,
      "supplierPartId": "SUPPLIER-PART-001",
      "unitOfMeasure": "EA"
    }
  ],
  "buyerOrderNumber": "PO-2024-001",
  "notes": "备注信息"
}
```

**请求字段说明**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sessionToken | string | 是 | 会话 token |
| items | array | 是 | 购物车商品列表（至少 1 项） |
| buyerOrderNumber | string | 否 | 采购订单号 |
| notes | string | 否 | 备注信息 |

**items 数组字段说明**

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| productId | string | 是 | 产品 ID（供应商系统中的产品 ID） |
| sku | string | 否 | 产品 SKU（用于匹配产品） |
| productName | string | 否 | 产品名称 |
| quantity | integer | 是 | 数量（必须 ≥ 1） |
| unitPrice | decimal | 否 | 单价 |
| supplierPartId | string | 否 | 供应商物料代码 |
| unitOfMeasure | string | 否 | 单位（UOM），如 "EA"、"BOX" 等 |

**成功响应**

```json
{
  "orderId": null,
  "orderNumber": null,
  "status": "returned",
  "success": true,
  "message": "Cart successfully returned to buyer system",
  "remainingAttempts": null
}
```

**响应字段说明**

| 字段名 | 类型 | 说明 |
|--------|------|------|
| orderId | string \| null | 订单 ID（标准 Punchout 流程中通常为 null） |
| orderNumber | string \| null | 订单号（标准 Punchout 流程中通常为 null） |
| status | string | 订单状态，成功时为 "returned" |
| success | boolean | 是否成功 |
| message | string | 响应消息 |
| remainingAttempts | integer \| null | 剩余重试次数（失败时返回，成功时为 null） |

**失败响应**

```json
{
  "orderId": null,
  "orderNumber": null,
  "status": null,
  "success": false,
  "message": "Session expired. Please restart punchout session.",
  "remainingAttempts": 0
}
```

**HTTP 状态码**

- `200 OK`: 请求成功（无论业务成功或失败）
- `400 Bad Request`: 请求参数错误
- `500 Internal Server Error`: 服务器内部错误

**重要说明**

1. **重试机制**: 如果返回失败，前端可以重试，但最多重试 **3 次**
2. **剩余次数**: 失败响应中的 `remainingAttempts` 字段表示剩余重试次数
3. **并发保护**: 后端已实现并发保护，同一会话同时只能有一个 Return 请求在处理
4. **业务状态**: 即使 HTTP 状态码是 200，也需要检查 `success` 字段判断业务是否成功

---

## 数据模型

### PunchoutCartItem（购物车商品）

```typescript
interface PunchoutCartItem {
  productId: string;        // 必填：产品 ID
  sku?: string;             // 可选：产品 SKU
  productName?: string;      // 可选：产品名称
  quantity: number;         // 必填：数量（≥ 1）
  unitPrice?: number;        // 可选：单价
  supplierPartId?: string;   // 可选：供应商物料代码
  unitOfMeasure?: string;    // 可选：单位（UOM）
}
```

### PunchoutReturnRequest（返回请求）

```typescript
interface PunchoutReturnRequest {
  sessionToken: string;                    // 必填：会话 token
  items: PunchoutCartItem[];               // 必填：购物车商品列表（至少 1 项）
  buyerOrderNumber?: string;                // 可选：采购订单号
  notes?: string;                          // 可选：备注信息
}
```

### PunchoutReturnResponse（返回响应）

```typescript
interface PunchoutReturnResponse {
  orderId: string | null;                  // 订单 ID（通常为 null）
  orderNumber: string | null;              // 订单号（通常为 null）
  status: string | null;                  // 订单状态
  success: boolean;                        // 是否成功
  message: string;                         // 响应消息
  remainingAttempts: number | null;        // 剩余重试次数
}
```

### ValidateResponse（验证响应）

```typescript
interface ValidateResponse {
  valid: boolean;                          // 会话是否有效
  error?: string;                          // 错误信息（如果有）
}
```

---

## 错误处理

### 常见错误场景

#### 1. 会话过期

**错误信息**: `"Session expired. Please restart punchout session."`

**处理方式**:
- 提示用户会话已过期
- 引导用户重新发起 Punchout 流程

#### 2. 会话无效

**错误信息**: `"Invalid or expired session token"`

**处理方式**:
- 清除本地存储的 sessionToken
- 提示用户重新登录

#### 3. 购物车为空

**错误信息**: `"Cart items are required"`

**处理方式**:
- 提示用户至少选择一件商品

#### 4. 重试次数超限

**错误信息**: `"Return attempts exceeded (3). Please restart punchout session."`

**处理方式**:
- 提示用户重试次数已用完
- 引导用户重新发起 Punchout 流程

#### 5. 正在处理中

**错误信息**: `"Return is already in progress. Please wait."`

**处理方式**:
- 提示用户请勿重复提交
- 等待当前请求完成

#### 6. 采购系统返回错误

**错误信息**: `"Buyer system returned error: 400 - Bad Request"`

**处理方式**:
- 检查购物车数据格式是否正确
- 查看 `remainingAttempts` 决定是否重试

### 错误处理最佳实践

1. **统一错误处理**: 创建统一的错误处理函数
2. **用户友好提示**: 将技术错误信息转换为用户友好的提示
3. **重试策略**: 根据 `remainingAttempts` 实现智能重试
4. **日志记录**: 记录错误信息以便排查问题

---

## 前端集成指南

### 1. 获取 Session Token

Session Token 通常通过 URL 参数传递到前端：

```
https://your-frontend.com/punchout?sessionToken=abc123xyz456
```

**前端代码示例**:

```javascript
// 从 URL 获取 sessionToken
const urlParams = new URLSearchParams(window.location.search);
const sessionToken = urlParams.get('sessionToken');

// 存储到 localStorage
if (sessionToken) {
  localStorage.setItem('punchoutSessionToken', sessionToken);
}
```

### 2. 验证会话

页面加载时验证会话是否有效：

```javascript
async function validateSession(sessionToken) {
  try {
    const response = await fetch(
      `${API_BASE_URL}/punchout/validate?sessionToken=${sessionToken}`
    );
    const data = await response.json();
    
    if (!data.valid) {
      // 会话无效，跳转到错误页面
      window.location.href = '/punchout/error?reason=session_expired';
      return false;
    }
    
    return true;
  } catch (error) {
    console.error('Session validation failed:', error);
    return false;
  }
}
```

### 3. 定期检查会话

建议每 5 分钟检查一次会话状态：

```javascript
// 定期检查会话（每 5 分钟）
setInterval(async () => {
  const sessionToken = localStorage.getItem('punchoutSessionToken');
  if (sessionToken) {
    const isValid = await validateSession(sessionToken);
    if (!isValid) {
      // 会话已过期，提示用户
      alert('您的会话已过期，请重新发起采购流程');
      window.location.href = '/punchout/error?reason=session_expired';
    }
  }
}, 5 * 60 * 1000); // 5 分钟
```

### 4. 提交购物车

用户点击"返回采购系统"按钮时调用 Return 接口：

```javascript
async function returnCart(cartItems) {
  const sessionToken = localStorage.getItem('punchoutSessionToken');
  
  if (!sessionToken) {
    throw new Error('Session token not found');
  }
  
  if (!cartItems || cartItems.length === 0) {
    throw new Error('Cart is empty');
  }
  
  const requestBody = {
    sessionToken: sessionToken,
    items: cartItems.map(item => ({
      productId: item.id,
      sku: item.sku,
      productName: item.name,
      quantity: item.quantity,
      unitPrice: item.price,
      supplierPartId: item.supplierPartId,
      unitOfMeasure: item.unitOfMeasure || 'EA'
    }))
  };
  
  try {
    const response = await fetch(`${API_BASE_URL}/punchout/return`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestBody)
    });
    
    const data = await response.json();
    
    if (data.success) {
      // 成功：显示成功提示，然后跳转或关闭窗口
      alert('购物车已成功返回采购系统');
      // 可以关闭窗口或跳转到采购系统
      window.close();
    } else {
      // 失败：显示错误信息
      throw new Error(data.message || 'Failed to return cart');
    }
    
    return data;
  } catch (error) {
    console.error('Return cart failed:', error);
    throw error;
  }
}
```

### 5. 重试机制

实现智能重试逻辑：

```javascript
async function returnCartWithRetry(cartItems, maxRetries = 3) {
  let lastError = null;
  let remainingAttempts = maxRetries;
  
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const response = await returnCart(cartItems);
      
      if (response.success) {
        return response;
      }
      
      // 业务失败，检查剩余重试次数
      remainingAttempts = response.remainingAttempts || 0;
      
      if (remainingAttempts <= 0) {
        throw new Error('Retry attempts exceeded');
      }
      
      // 等待后重试（指数退避）
      const delay = Math.min(1000 * Math.pow(2, attempt - 1), 10000);
      await new Promise(resolve => setTimeout(resolve, delay));
      
    } catch (error) {
      lastError = error;
      
      // 如果是最后一次尝试，抛出错误
      if (attempt === maxRetries) {
        throw error;
      }
      
      // 等待后重试
      const delay = Math.min(1000 * Math.pow(2, attempt - 1), 10000);
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
  
  throw lastError || new Error('Failed to return cart after retries');
}
```

### 6. 加载状态管理

在提交购物车时显示加载状态：

```javascript
async function handleReturnCart() {
  const cartItems = getCartItems(); // 获取购物车商品
  
  // 显示加载状态
  setLoading(true);
  setError(null);
  
  try {
    await returnCartWithRetry(cartItems);
    
    // 成功：显示成功提示
    setSuccess(true);
    
    // 3 秒后关闭窗口或跳转
    setTimeout(() => {
      window.close();
    }, 3000);
    
  } catch (error) {
    // 失败：显示错误信息
    setError(error.message);
    setLoading(false);
  }
}
```

---

## 示例代码

### React 示例

```tsx
import React, { useState, useEffect } from 'react';

const PunchoutReturn: React.FC = () => {
  const [sessionToken, setSessionToken] = useState<string | null>(null);
  const [cartItems, setCartItems] = useState<PunchoutCartItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    // 从 URL 获取 sessionToken
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('sessionToken');
    
    if (token) {
      setSessionToken(token);
      localStorage.setItem('punchoutSessionToken', token);
      
      // 验证会话
      validateSession(token);
    } else {
      // 从 localStorage 获取
      const storedToken = localStorage.getItem('punchoutSessionToken');
      if (storedToken) {
        setSessionToken(storedToken);
        validateSession(storedToken);
      }
    }
    
    // 加载购物车数据
    loadCartItems();
  }, []);

  const validateSession = async (token: string) => {
    try {
      const response = await fetch(
        `${API_BASE_URL}/punchout/validate?sessionToken=${token}`
      );
      const data = await response.json();
      
      if (!data.valid) {
        setError('会话已过期，请重新发起采购流程');
      }
    } catch (error) {
      console.error('Session validation failed:', error);
      setError('会话验证失败');
    }
  };

  const loadCartItems = () => {
    // 从你的购物车状态管理或 API 加载购物车数据
    // 示例：
    const items: PunchoutCartItem[] = [
      {
        productId: 'PROD-001',
        sku: 'SKU-001',
        productName: '产品名称',
        quantity: 2,
        unitPrice: 99.99,
        unitOfMeasure: 'EA'
      }
    ];
    setCartItems(items);
  };

  const handleReturnCart = async () => {
    if (!sessionToken) {
      setError('会话 token 不存在');
      return;
    }
    
    if (cartItems.length === 0) {
      setError('购物车为空，请至少选择一件商品');
      return;
    }
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(`${API_BASE_URL}/punchout/return`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          sessionToken: sessionToken,
          items: cartItems
        })
      });
      
      const data = await response.json();
      
      if (data.success) {
        setSuccess(true);
        // 3 秒后关闭窗口
        setTimeout(() => {
          window.close();
        }, 3000);
      } else {
        setError(data.message || '返回购物车失败');
        
        // 如果还有剩余重试次数，提示用户
        if (data.remainingAttempts && data.remainingAttempts > 0) {
          setError(`${data.message}（剩余重试次数：${data.remainingAttempts}）`);
        }
      }
    } catch (error) {
      console.error('Return cart failed:', error);
      setError('网络错误，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="punchout-return">
      <h1>返回采购系统</h1>
      
      {error && (
        <div className="error-message">
          {error}
        </div>
      )}
      
      {success && (
        <div className="success-message">
          购物车已成功返回采购系统，窗口将在 3 秒后关闭...
        </div>
      )}
      
      <div className="cart-items">
        <h2>购物车商品</h2>
        {cartItems.map((item, index) => (
          <div key={index} className="cart-item">
            <span>{item.productName}</span>
            <span>数量: {item.quantity}</span>
            <span>单价: ¥{item.unitPrice}</span>
          </div>
        ))}
      </div>
      
      <button 
        onClick={handleReturnCart} 
        disabled={loading || success || cartItems.length === 0}
      >
        {loading ? '提交中...' : '返回采购系统'}
      </button>
    </div>
  );
};

export default PunchoutReturn;
```

### Vue 示例

```vue
<template>
  <div class="punchout-return">
    <h1>返回采购系统</h1>
    
    <div v-if="error" class="error-message">
      {{ error }}
    </div>
    
    <div v-if="success" class="success-message">
      购物车已成功返回采购系统，窗口将在 3 秒后关闭...
    </div>
    
    <div class="cart-items">
      <h2>购物车商品</h2>
      <div v-for="(item, index) in cartItems" :key="index" class="cart-item">
        <span>{{ item.productName }}</span>
        <span>数量: {{ item.quantity }}</span>
        <span>单价: ¥{{ item.unitPrice }}</span>
      </div>
    </div>
    
    <button 
      @click="handleReturnCart" 
      :disabled="loading || success || cartItems.length === 0"
    >
      {{ loading ? '提交中...' : '返回采购系统' }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';

const API_BASE_URL = 'http://localhost:8000/api';

interface PunchoutCartItem {
  productId: string;
  sku?: string;
  productName?: string;
  quantity: number;
  unitPrice?: number;
  supplierPartId?: string;
  unitOfMeasure?: string;
}

const sessionToken = ref<string | null>(null);
const cartItems = ref<PunchoutCartItem[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const success = ref(false);

onMounted(() => {
  // 从 URL 获取 sessionToken
  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get('sessionToken');
  
  if (token) {
    sessionToken.value = token;
    localStorage.setItem('punchoutSessionToken', token);
    validateSession(token);
  } else {
    const storedToken = localStorage.getItem('punchoutSessionToken');
    if (storedToken) {
      sessionToken.value = storedToken;
      validateSession(storedToken);
    }
  }
  
  loadCartItems();
});

const validateSession = async (token: string) => {
  try {
    const response = await fetch(
      `${API_BASE_URL}/punchout/validate?sessionToken=${token}`
    );
    const data = await response.json();
    
    if (!data.valid) {
      error.value = '会话已过期，请重新发起采购流程';
    }
  } catch (err) {
    console.error('Session validation failed:', err);
    error.value = '会话验证失败';
  }
};

const loadCartItems = () => {
  // 从你的购物车状态管理或 API 加载购物车数据
  cartItems.value = [
    {
      productId: 'PROD-001',
      sku: 'SKU-001',
      productName: '产品名称',
      quantity: 2,
      unitPrice: 99.99,
      unitOfMeasure: 'EA'
    }
  ];
};

const handleReturnCart = async () => {
  if (!sessionToken.value) {
    error.value = '会话 token 不存在';
    return;
  }
  
  if (cartItems.value.length === 0) {
    error.value = '购物车为空，请至少选择一件商品';
    return;
  }
  
  loading.value = true;
  error.value = null;
  
  try {
    const response = await fetch(`${API_BASE_URL}/punchout/return`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        sessionToken: sessionToken.value,
        items: cartItems.value
      })
    });
    
    const data = await response.json();
    
    if (data.success) {
      success.value = true;
      setTimeout(() => {
        window.close();
      }, 3000);
    } else {
      error.value = data.message || '返回购物车失败';
      
      if (data.remainingAttempts && data.remainingAttempts > 0) {
        error.value = `${data.message}（剩余重试次数：${data.remainingAttempts}）`;
      }
    }
  } catch (err) {
    console.error('Return cart failed:', err);
    error.value = '网络错误，请稍后重试';
  } finally {
    loading.value = false;
  }
};
</script>
```

### 原生 JavaScript 示例

```javascript
// API 配置
const API_BASE_URL = 'http://localhost:8000/api';

// 从 URL 获取 sessionToken
function getSessionTokenFromURL() {
  const urlParams = new URLSearchParams(window.location.search);
  return urlParams.get('sessionToken');
}

// 验证会话
async function validateSession(sessionToken) {
  try {
    const response = await fetch(
      `${API_BASE_URL}/punchout/validate?sessionToken=${sessionToken}`
    );
    const data = await response.json();
    return data.valid;
  } catch (error) {
    console.error('Session validation failed:', error);
    return false;
  }
}

// 返回购物车
async function returnCart(sessionToken, cartItems) {
  const response = await fetch(`${API_BASE_URL}/punchout/return`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      sessionToken: sessionToken,
      items: cartItems
    })
  });
  
  return await response.json();
}

// 页面初始化
document.addEventListener('DOMContentLoaded', async () => {
  // 获取 sessionToken
  let sessionToken = getSessionTokenFromURL();
  
  if (sessionToken) {
    localStorage.setItem('punchoutSessionToken', sessionToken);
  } else {
    sessionToken = localStorage.getItem('punchoutSessionToken');
  }
  
  if (!sessionToken) {
    alert('会话 token 不存在，请重新发起采购流程');
    return;
  }
  
  // 验证会话
  const isValid = await validateSession(sessionToken);
  if (!isValid) {
    alert('会话已过期，请重新发起采购流程');
    return;
  }
  
  // 绑定返回按钮事件
  const returnButton = document.getElementById('return-button');
  returnButton.addEventListener('click', async () => {
    // 获取购物车数据
    const cartItems = getCartItems(); // 实现你的购物车数据获取逻辑
    
    if (cartItems.length === 0) {
      alert('购物车为空，请至少选择一件商品');
      return;
    }
    
    // 显示加载状态
    returnButton.disabled = true;
    returnButton.textContent = '提交中...';
    
    try {
      const result = await returnCart(sessionToken, cartItems);
      
      if (result.success) {
        alert('购物车已成功返回采购系统');
        // 关闭窗口或跳转
        window.close();
      } else {
        alert(`返回失败：${result.message}`);
        if (result.remainingAttempts && result.remainingAttempts > 0) {
          alert(`剩余重试次数：${result.remainingAttempts}`);
        }
      }
    } catch (error) {
      console.error('Return cart failed:', error);
      alert('网络错误，请稍后重试');
    } finally {
      returnButton.disabled = false;
      returnButton.textContent = '返回采购系统';
    }
  });
});
```

---

## 注意事项

### 1. 会话管理

- Session Token 应该安全存储（localStorage 或 sessionStorage）
- 会话有效期为 30 分钟，过期后需要重新发起 Setup
- 建议在页面加载时和提交前都验证会话

### 2. 错误处理

- 始终检查响应的 `success` 字段，即使 HTTP 状态码是 200
- 根据 `remainingAttempts` 实现智能重试
- 提供用户友好的错误提示

### 3. 并发控制

- 后端已实现并发保护，同一会话同时只能有一个 Return 请求
- 前端应该禁用提交按钮，防止用户重复点击

### 4. 数据格式

- 确保 `productId` 和 `quantity` 字段必填且格式正确
- `quantity` 必须 ≥ 1
- `unitPrice` 应该是数字类型（decimal）

### 5. 用户体验

- 提交时显示加载状态
- 成功后有明确的成功提示
- 失败时提供清晰的错误信息和重试选项

---

## 技术支持

如有问题，请联系开发团队或查看后端日志。

**API 版本**: v1.0  
**最后更新**: 2024-01-XX

