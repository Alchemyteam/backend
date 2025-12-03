# 前端 AI 物料搜索调用指南

## 概述

本文档说明如何在前端调用 AI 物料搜索功能。系统支持多种搜索方式，包括物料编码、物料名称、品类、功能、品牌以及组合条件搜索。

## API 接口

### 基础信息

- **接口地址**: `POST /api/chat/message`
- **Content-Type**: `application/json`
- **认证方式**: `Authorization: Bearer {token}`

### 请求格式

```typescript
POST /api/chat/message
Authorization: Bearer {token}
Content-Type: application/json

{
  "message": "用户查询文本",
  "conversationId": "可选，会话ID"
}
```

### 响应格式

```typescript
{
  "response": "AI生成的回复文本",
  "conversationId": "会话ID",
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
  },
  "actionData": {
    "actionType": "SEARCH",
    "parameters": {},
    "message": "Search completed"
  }
}
```

## TypeScript 类型定义

### 类型定义文件：`types/chat.ts`

```typescript
// 聊天请求
export interface ChatRequest {
  message: string;
  conversationId?: string;
}

// 聊天响应
export interface ChatResponse {
  response: string;
  conversationId: string;
  tableData?: TableData;
  actionData?: ActionData;
}

// 表格数据
export interface TableData {
  title: string;
  headers: string[];
  rows: Array<Record<string, any>>;
  description?: string;
}

// 操作数据
export interface ActionData {
  actionType: string;
  parameters: Record<string, any>;
  message?: string;
}
```

## API 服务函数

### API 服务文件：`services/chatApi.ts`

```typescript
import { ChatRequest, ChatResponse } from '@/types/chat';

const API_BASE_URL = 'http://localhost:8000/api';

/**
 * 发送聊天消息（物料搜索）
 * @param message 用户查询文本
 * @param conversationId 可选，会话ID
 * @param token 认证token
 * @returns 聊天响应
 */
export async function sendChatMessage(
  message: string,
  token: string,
  conversationId?: string
): Promise<ChatResponse> {
  const url = `${API_BASE_URL}/chat/message`;
  
  const requestBody: ChatRequest = {
    message,
    ...(conversationId && { conversationId }),
  };
  
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(requestBody),
  });
  
  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('未授权：token 无效或已过期');
    }
    if (response.status === 500) {
      throw new Error('服务器错误');
    }
    throw new Error(`请求失败：${response.status} ${response.statusText}`);
  }
  
  return await response.json();
}
```

### 使用 Axios 的版本

```typescript
import axios from 'axios';
import { ChatRequest, ChatResponse } from '@/types/chat';

const API_BASE_URL = 'http://localhost:8000/api';

// 创建 axios 实例
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器：自动添加 token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/**
 * 发送聊天消息（使用 Axios）
 */
export async function sendChatMessage(
  message: string,
  conversationId?: string
): Promise<ChatResponse> {
  const response = await apiClient.post<ChatResponse>('/chat/message', {
    message,
    conversationId,
  });
  
  return response.data;
}
```

## React Hook 示例

### Hook 文件：`hooks/useChatSearch.ts`

```typescript
import { useState, useCallback } from 'react';
import { sendChatMessage } from '@/services/chatApi';
import { ChatResponse } from '@/types/chat';

interface UseChatSearchReturn {
  sendMessage: (message: string) => Promise<void>;
  response: ChatResponse | null;
  loading: boolean;
  error: string | null;
  conversationId: string | null;
}

export function useChatSearch(token: string): UseChatSearchReturn {
  const [response, setResponse] = useState<ChatResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [conversationId, setConversationId] = useState<string | null>(null);
  
  const sendMessage = useCallback(async (message: string) => {
    try {
      setLoading(true);
      setError(null);
      
      const result = await sendChatMessage(message, token, conversationId || undefined);
      
      setResponse(result);
      if (result.conversationId) {
        setConversationId(result.conversationId);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '发送消息失败');
      setResponse(null);
    } finally {
      setLoading(false);
    }
  }, [token, conversationId]);
  
  return {
    sendMessage,
    response,
    loading,
    error,
    conversationId,
  };
}
```

## React 组件示例

### 组件文件：`components/MaterialSearchChat.tsx`

```typescript
import React, { useState } from 'react';
import { useChatSearch } from '@/hooks/useChatSearch';
import { ChatResponse } from '@/types/chat';

interface MaterialSearchChatProps {
  token: string;
}

export const MaterialSearchChat: React.FC<MaterialSearchChatProps> = ({ token }) => {
  const [inputMessage, setInputMessage] = useState('');
  const { sendMessage, response, loading, error } = useChatSearch(token);
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputMessage.trim()) return;
    
    await sendMessage(inputMessage);
    setInputMessage('');
  };
  
  return (
    <div className="material-search-chat">
      <div className="chat-container">
        {/* 消息显示区域 */}
        <div className="messages">
          {response && (
            <div className="message response">
              <div className="message-text">{response.response}</div>
              
              {/* 表格数据 */}
              {response.tableData && (
                <div className="table-container">
                  <h3>{response.tableData.title}</h3>
                  <p>{response.tableData.description}</p>
                  
                  <table>
                    <thead>
                      <tr>
                        {response.tableData.headers.map((header, index) => (
                          <th key={index}>{header}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {response.tableData.rows.map((row, rowIndex) => (
                        <tr key={rowIndex}>
                          {response.tableData!.headers.map((header, colIndex) => (
                            <td key={colIndex}>{row[header] || 'N/A'}</td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
          
          {error && (
            <div className="message error">
              错误：{error}
            </div>
          )}
        </div>
        
        {/* 输入区域 */}
        <form onSubmit={handleSubmit} className="input-form">
          <input
            type="text"
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            placeholder="输入搜索查询，例如：TI00040 或 安全鞋"
            disabled={loading}
          />
          <button type="submit" disabled={loading || !inputMessage.trim()}>
            {loading ? '搜索中...' : '搜索'}
          </button>
        </form>
      </div>
    </div>
  );
};
```

## 可以问的问题示例

### 1. 按物料编码精确搜索

这些查询会返回该物料的所有历史交易记录和统计信息：

```
✅ TI00040
✅ 查找物料编码 TI00040
✅ ItemCode = TI00040
✅ 显示 TI00040 的所有历史交易
✅ TI00040 的价格区间是多少
✅ 物料编码 TI00040 的历史记录
```

**返回信息包括：**
- 所有历史交易记录
- 价格区间（最低价、最高价、平均价）
- 交易日期范围（最早、最晚）

### 2. 按物料名称关键字模糊搜索

支持中文和英文：

```
✅ 安全鞋
✅ 喷漆
✅ 储物箱
✅ LEAKAGE CURRENT CLAMP METER
✅ FLUKE 368 FC
✅ 安全帽
✅ 防护手套
✅ 测量仪器
```

**返回：** 包含该关键词的所有物料记录

### 3. 按品类搜索

```
✅ Site Safety Equipment
✅ 安全设备
✅ Filters
✅ 过滤器
✅ Maintenance Chemicals
✅ 维护化学品
✅ Site Safety Equipment 类的所有产品
✅ 显示所有安全设备
```

**返回：** 该品类下的所有物料

### 4. 按功能搜索

```
✅ Maintenance Chemicals
✅ 维护化学品
✅ Safety
✅ 安全
✅ Protection
✅ 防护
```

**返回：** 该功能分类下的所有物料

### 5. 按品牌搜索

```
✅ 品牌 AET
✅ brand AET
✅ Air Liquide 品牌
✅ 显示 AET 的所有产品
✅ AET 品牌的产品有哪些
✅ 查找品牌代码 AET
```

**返回：** 该品牌的所有产品

### 6. 组合条件搜索

#### 品类 + 品牌 + 时间

```
✅ Site Safety Equipment + Air Liquide + 去年
✅ 安全设备 + AET + 去年
✅ Filters 类 + AET + 今年
✅ Site Safety Equipment + Air Liquide + 2023年
```

#### 品类 + 品牌 + 价格区间

```
✅ Filters 类 + AET + 单价 100 到 500
✅ Site Safety Equipment + Air Liquide + 价格 50-200
✅ 安全设备 + 品牌 AET + 100元到500元
```

#### 物料名称 + 品类

```
✅ 安全鞋 + Site Safety Equipment
✅ 喷漆 + Maintenance Chemicals
✅ 储物箱 + Filters
```

#### 品类 + 功能

```
✅ Site Safety Equipment + Safety
✅ Filters + Protection
✅ 安全设备 + 安全功能
```

#### 多条件组合

```
✅ Site Safety Equipment + Air Liquide + 去年 + 价格 100-500
✅ Filters 类 + AET + 今年 + 单价区间 50-200
✅ 安全设备 + 品牌 AET + 2023年 + 100到500元
```

### 7. 时间范围搜索

```
✅ 去年的交易
✅ 今年的数据
✅ 2023年的记录
✅ 最近一年的交易
```

### 8. 价格区间搜索

```
✅ 价格 100 到 500
✅ 单价区间 50-200
✅ 100元到500元的产品
✅ 价格范围 50 至 200
```

### 9. 复杂组合查询

```
✅ 查找 Site Safety Equipment 类别下，Air Liquide 品牌，去年，价格在 100-500 之间的所有产品
✅ 显示 Filters 类，AET 品牌，今年，单价 50-200 的所有物料
✅ 安全设备 + Air Liquide + 去年 + 价格区间 100-500
```

## 完整使用示例

### 示例 1: 基础搜索

```typescript
import { sendChatMessage } from '@/services/chatApi';

async function searchMaterial() {
  const token = localStorage.getItem('token') || '';
  
  try {
    const response = await sendChatMessage('TI00040', token);
    
    console.log('AI回复:', response.response);
    console.log('表格数据:', response.tableData);
    
    if (response.tableData) {
      response.tableData.rows.forEach((row) => {
        console.log('物料:', row['Item Name'], '价格:', row['Price']);
      });
    }
  } catch (error) {
    console.error('搜索失败:', error);
  }
}
```

### 示例 2: 组合搜索

```typescript
async function complexSearch() {
  const token = localStorage.getItem('token') || '';
  
  const query = 'Site Safety Equipment + Air Liquide + 去年';
  const response = await sendChatMessage(query, token);
  
  // 处理响应
  if (response.tableData && response.tableData.rows.length > 0) {
    console.log(`找到 ${response.tableData.rows.length} 条记录`);
  }
}
```

### 示例 3: React 组件完整示例

```typescript
import React, { useState } from 'react';
import { MaterialSearchChat } from '@/components/MaterialSearchChat';

function MaterialSearchPage() {
  const token = localStorage.getItem('token') || '';
  
  if (!token) {
    return <div>请先登录</div>;
  }
  
  return (
    <div>
      <h1>AI 物料搜索</h1>
      <MaterialSearchChat token={token} />
    </div>
  );
}

export default MaterialSearchPage;
```

## 响应数据说明

### 表格数据结构

```typescript
tableData: {
  title: "Material Search Results",           // 表格标题
  headers: [                                  // 表头
    "Item Code",      // 物料编码
    "Item Name",      // 物料名称
    "Price",          // 价格
    "Date",           // 交易日期
    "Category",       // 品类
    "Brand",          // 品牌
    "Function"        // 功能
  ],
  rows: [                                     // 数据行
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
  description: "Found 15 material record(s)." // 描述信息
}
```

### 精确物料编码搜索的特殊响应

当搜索物料编码（如 `TI00040`）时，`response` 字段会包含统计信息：

```
Found 15 historical transactions for Item Code: TI00040 (LEAKAGE CURRENT CLAMP METER).
Price range: 100.00 - 500.00, Average: 250.00.
First transaction: 2023-01-15, Last transaction: 2024-12-20.
```

## 错误处理

```typescript
try {
  const response = await sendChatMessage('TI00040', token);
  // 处理成功响应
} catch (error) {
  if (error.message.includes('401')) {
    // 处理未授权错误
    console.error('请重新登录');
    // 跳转到登录页
  } else if (error.message.includes('500')) {
    // 处理服务器错误
    console.error('服务器错误，请稍后重试');
  } else {
    // 处理其他错误
    console.error('请求失败:', error.message);
  }
}
```

## 最佳实践

### 1. 会话管理

```typescript
// 保存 conversationId 以维持会话上下文
const [conversationId, setConversationId] = useState<string | null>(null);

const sendMessage = async (message: string) => {
  const response = await sendChatMessage(message, token, conversationId || undefined);
  if (response.conversationId) {
    setConversationId(response.conversationId);
  }
};
```

### 2. 加载状态

```typescript
const [loading, setLoading] = useState(false);

const sendMessage = async (message: string) => {
  setLoading(true);
  try {
    const response = await sendChatMessage(message, token);
    // 处理响应
  } finally {
    setLoading(false);
  }
};
```

### 3. 空结果处理

```typescript
if (response.tableData && response.tableData.rows.length === 0) {
  return <div>未找到匹配的物料，请尝试其他搜索条件</div>;
}
```

### 4. 表格渲染优化

```typescript
{response.tableData && response.tableData.rows.length > 0 && (
  <div className="table-wrapper">
    <table>
      <thead>
        <tr>
          {response.tableData.headers.map((header, index) => (
            <th key={index}>{header}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {response.tableData.rows.map((row, rowIndex) => (
          <tr key={rowIndex}>
            {response.tableData.headers.map((header, colIndex) => (
              <td key={colIndex}>
                {row[header] || 'N/A'}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  </div>
)}
```

## 搜索提示

### 提高搜索准确性的建议

1. **使用物料编码**：最准确的搜索方式
   - ✅ `TI00040`
   - ❌ `ti00040`（建议使用大写）

2. **使用完整关键词**：提高匹配率
   - ✅ `LEAKAGE CURRENT CLAMP METER FLUKE 368 FC`
   - ✅ `安全鞋`

3. **组合搜索时使用明确的分隔符**
   - ✅ `Site Safety Equipment + Air Liquide + 去年`
   - ✅ `Filters 类 + AET + 价格 100-500`

4. **使用标准品类名称**
   - ✅ `Site Safety Equipment`
   - ✅ `Maintenance Chemicals`

## 常见问题

### Q: 为什么搜索返回空结果？

A: 可能的原因：
1. 物料编码不存在
2. 关键词拼写错误
3. 品类名称不匹配
4. 品牌代码不正确

**解决方案：**
- 检查拼写
- 尝试使用更通用的关键词
- 使用物料编码进行精确搜索

### Q: 如何获取物料的历史统计信息？

A: 使用物料编码进行精确搜索，系统会自动返回：
- 总交易数
- 价格区间（最低、最高、平均）
- 交易日期范围

### Q: 支持哪些语言？

A: 目前支持：
- 中文（简体）
- 英文

关键词可以混合使用，例如：`安全设备 + AET + 去年`

## 总结

AI 物料搜索系统支持：
- ✅ 6 种搜索方式（编码、名称、品类、功能、品牌、组合）
- ✅ 自然语言查询
- ✅ 中英文支持
- ✅ 历史交易统计
- ✅ 价格和日期范围过滤

按照本文档的说明，你可以轻松集成 AI 搜索功能到前端应用中！

