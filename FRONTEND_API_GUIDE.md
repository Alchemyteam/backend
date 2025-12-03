# 前端接口调用指南 - 销售数据 API

## 1. TypeScript 类型定义

### 类型定义文件：`types/salesData.ts`

```typescript
// 销售数据项类型
export interface SalesData {
  TXDate: string | null;              // 交易日期 "2024-01-20"
  TXNo: string | null;                 // 交易编号
  TXQty: number | null;                // 交易数量
  TXP1: number | null;                  // 交易价格
  BuyerCode: string | null;            // 买家代码
  BuyerName: string | null;             // 买家名称
  ItemCode: string | null;              // 产品代码
  ItemName: string | null;              // 产品名称
  "Product Hierarchy 3": string | null; // 产品分类层级3
  Function: string | null;              // 功能
  ItemType: string | null;              // 产品类型
  Model: string | null;                 // 型号
  Performance: string | null;           // 性能
  "Performance.1": string | null;       // 性能1
  Material: string | null;              // 材料
  UOM: string | null;                   // 单位
  "Brand Code": string | null;          // 品牌代码
  "Unit Cost": number | null;           // 单位成本
  Sector: string | null;                // 行业
  SubSector: string | null;             // 子行业
  Value: number | null;                 // 总价值
  Rationale: string | null;            // 理由
  www: string | null;                   // 网址
  Source: string | null;                // 来源
}

// 分页信息类型
export interface Pagination {
  page: number;        // 当前页码
  limit: number;       // 每页数量
  total: number;       // 总记录数
  totalPages: number;  // 总页数
}

// 销售数据列表响应类型
export interface SalesDataListResponse {
  data: SalesData[];
  pagination: Pagination;
}

// 查询参数类型
export interface SalesDataQueryParams {
  page?: number;      // 页码，从1开始，默认1
  limit?: number;     // 每页数量，默认20
  sort?: 'newest' | 'price_asc' | 'price_desc';  // 排序方式，默认newest
  category?: string;  // 产品分类过滤（当值为'all'时不传此参数）
}
```

## 2. API 服务函数

### API 服务文件：`services/salesDataApi.ts`

```typescript
import { SalesDataListResponse, SalesDataQueryParams } from '@/types/salesData';

const API_BASE_URL = 'http://localhost:8000/api';

/**
 * 获取销售数据列表
 * @param params 查询参数
 * @param token 认证 token
 * @returns 销售数据列表响应
 */
export async function getSalesData(
  params: SalesDataQueryParams = {},
  token: string
): Promise<SalesDataListResponse> {
  const { page = 1, limit = 20, sort = 'newest', category } = params;
  
  // 构建查询字符串
  const queryParams = new URLSearchParams();
  queryParams.append('page', page.toString());
  queryParams.append('limit', limit.toString());
  queryParams.append('sort', sort);
  
  // 只有当 category 存在且不为 'all' 时才添加
  if (category && category !== 'all') {
    queryParams.append('category', category);
  }
  
  const url = `${API_BASE_URL}/buyer/sales-data?${queryParams.toString()}`;
  
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });
  
  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('未授权：token 无效或已过期');
    }
    if (response.status === 500) {
      throw new Error('服务器错误：数据库连接失败');
    }
    throw new Error(`请求失败：${response.status} ${response.statusText}`);
  }
  
  return await response.json();
}
```

## 3. React Hook 示例

### Hook 文件：`hooks/useSalesData.ts`

```typescript
import { useState, useEffect } from 'react';
import { getSalesData } from '@/services/salesDataApi';
import { SalesDataListResponse, SalesDataQueryParams } from '@/types/salesData';

interface UseSalesDataReturn {
  data: SalesDataListResponse | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

export function useSalesData(
  params: SalesDataQueryParams,
  token: string
): UseSalesDataReturn {
  const [data, setData] = useState<SalesDataListResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  
  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await getSalesData(params, token);
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : '获取数据失败');
      setData(null);
    } finally {
      setLoading(false);
    }
  };
  
  useEffect(() => {
    fetchData();
  }, [params.page, params.limit, params.sort, params.category, token]);
  
  return {
    data,
    loading,
    error,
    refetch: fetchData,
  };
}
```

## 4. React 组件示例

### 组件文件：`components/SalesDataList.tsx`

```typescript
import React, { useState } from 'react';
import { useSalesData } from '@/hooks/useSalesData';
import { SalesDataQueryParams } from '@/types/salesData';
import { SalesData } from '@/types/salesData';

interface SalesDataListProps {
  token: string;
}

export const SalesDataList: React.FC<SalesDataListProps> = ({ token }) => {
  const [queryParams, setQueryParams] = useState<SalesDataQueryParams>({
    page: 1,
    limit: 20,
    sort: 'newest',
    category: undefined,
  });
  
  const { data, loading, error, refetch } = useSalesData(queryParams, token);
  
  // 处理分页
  const handlePageChange = (newPage: number) => {
    setQueryParams(prev => ({ ...prev, page: newPage }));
  };
  
  // 处理排序
  const handleSortChange = (newSort: 'newest' | 'price_asc' | 'price_desc') => {
    setQueryParams(prev => ({ ...prev, sort: newSort, page: 1 }));
  };
  
  // 处理分类过滤
  const handleCategoryChange = (category: string) => {
    setQueryParams(prev => ({
      ...prev,
      category: category === 'all' ? undefined : category,
      page: 1,
    }));
  };
  
  if (loading) {
    return <div>加载中...</div>;
  }
  
  if (error) {
    return <div>错误：{error}</div>;
  }
  
  if (!data || data.data.length === 0) {
    return <div>暂无数据</div>;
  }
  
  return (
    <div>
      {/* 排序和过滤控件 */}
      <div style={{ marginBottom: '20px' }}>
        <select
          value={queryParams.sort}
          onChange={(e) => handleSortChange(e.target.value as any)}
        >
          <option value="newest">最新</option>
          <option value="price_asc">价格升序</option>
          <option value="price_desc">价格降序</option>
        </select>
        
        <select
          value={queryParams.category || 'all'}
          onChange={(e) => handleCategoryChange(e.target.value)}
        >
          <option value="all">全部分类</option>
          <option value="formwork">Formwork</option>
          <option value="Construction">Construction</option>
          {/* 添加更多分类选项 */}
        </select>
      </div>
      
      {/* 数据表格 */}
      <table>
        <thead>
          <tr>
            <th>交易日期</th>
            <th>交易编号</th>
            <th>产品名称</th>
            <th>价格</th>
            <th>数量</th>
            <th>买家名称</th>
            <th>分类</th>
          </tr>
        </thead>
        <tbody>
          {data.data.map((item: SalesData) => (
            <tr key={item.TXNo}>
              <td>{item.TXDate || '-'}</td>
              <td>{item.TXNo || '-'}</td>
              <td>{item.ItemName || '-'}</td>
              <td>{item.TXP1 ? `$${item.TXP1.toFixed(2)}` : '-'}</td>
              <td>{item.TXQty || '-'}</td>
              <td>{item.BuyerName || '-'}</td>
              <td>{item['Product Hierarchy 3'] || item.Sector || '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      
      {/* 分页控件 */}
      <div style={{ marginTop: '20px' }}>
        <button
          disabled={queryParams.page === 1}
          onClick={() => handlePageChange(queryParams.page! - 1)}
        >
          上一页
        </button>
        <span>
          第 {data.pagination.page} 页 / 共 {data.pagination.totalPages} 页
          （共 {data.pagination.total} 条记录）
        </span>
        <button
          disabled={queryParams.page === data.pagination.totalPages}
          onClick={() => handlePageChange(queryParams.page! + 1)}
        >
          下一页
        </button>
      </div>
    </div>
  );
};
```

## 5. 使用 Axios 的版本（可选）

### API 服务文件：`services/salesDataApi.axios.ts`

```typescript
import axios from 'axios';
import { SalesDataListResponse, SalesDataQueryParams } from '@/types/salesData';

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
    const token = localStorage.getItem('token'); // 或从你的状态管理获取
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器：处理错误
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 处理未授权错误，例如跳转到登录页
      console.error('未授权：token 无效或已过期');
    }
    return Promise.reject(error);
  }
);

/**
 * 获取销售数据列表（使用 Axios）
 */
export async function getSalesData(
  params: SalesDataQueryParams = {},
  token?: string
): Promise<SalesDataListResponse> {
  const { page = 1, limit = 20, sort = 'newest', category } = params;
  
  const response = await apiClient.get<SalesDataListResponse>('/buyer/sales-data', {
    params: {
      page,
      limit,
      sort,
      ...(category && category !== 'all' && { category }),
    },
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  
  return response.data;
}
```

## 6. 错误处理示例

```typescript
import { getSalesData } from '@/services/salesDataApi';

async function fetchSalesDataWithErrorHandling(token: string) {
  try {
    const result = await getSalesData(
      { page: 1, limit: 20, sort: 'newest' },
      token
    );
    console.log('获取成功:', result);
    return result;
  } catch (error) {
    if (error instanceof Error) {
      if (error.message.includes('401')) {
        // 处理未授权错误
        console.error('请重新登录');
        // 跳转到登录页或刷新 token
      } else if (error.message.includes('500')) {
        // 处理服务器错误
        console.error('服务器错误，请稍后重试');
      } else {
        // 处理其他错误
        console.error('请求失败:', error.message);
      }
    }
    throw error;
  }
}
```

## 7. 关键注意事项

### 字段名处理
- 注意字段名中包含空格和特殊字符：
  - `"Product Hierarchy 3"` - 使用方括号访问：`item["Product Hierarchy 3"]`
  - `"Performance.1"` - 使用方括号访问：`item["Performance.1"]`
  - `"Brand Code"` - 使用方括号访问：`item["Brand Code"]`
  - `"Unit Cost"` - 使用方括号访问：`item["Unit Cost"]`

### 空值处理
- 所有字段都可能为 `null`，需要做好空值检查
- 使用可选链操作符：`item.ItemName?.toUpperCase()`
- 使用空值合并：`item.TXP1 ?? 0`

### 日期格式
- `TXDate` 返回格式为 `"YYYY-MM-DD"` 字符串
- 可以直接用于显示，或使用日期库（如 `date-fns`、`dayjs`）格式化

### 分页处理
- `page` 从 1 开始（不是 0）
- `totalPages` 是总页数，可用于分页控件
- 切换排序或分类时，记得重置 `page` 为 1

### 分类过滤
- 当 `category` 为 `"all"` 时，不传递该参数
- 分类匹配 `Product Hierarchy 3` 或 `Sector` 字段

## 8. 完整使用示例

```typescript
import React from 'react';
import { SalesDataList } from '@/components/SalesDataList';

function App() {
  // 从你的状态管理或 localStorage 获取 token
  const token = localStorage.getItem('token') || '';
  
  if (!token) {
    return <div>请先登录</div>;
  }
  
  return (
    <div>
      <h1>销售数据列表</h1>
      <SalesDataList token={token} />
    </div>
  );
}

export default App;
```

## 9. 测试调用示例

```typescript
// 测试 1: 基础查询
const result1 = await getSalesData(
  { page: 1, limit: 20 },
  token
);

// 测试 2: 带排序
const result2 = await getSalesData(
  { page: 1, limit: 20, sort: 'price_asc' },
  token
);

// 测试 3: 带分类过滤
const result3 = await getSalesData(
  { page: 1, limit: 20, category: 'formwork' },
  token
);

// 测试 4: 组合查询
const result4 = await getSalesData(
  { page: 2, limit: 10, sort: 'price_desc', category: 'Construction' },
  token
);
```

---

## 总结

1. **类型定义**：定义完整的 TypeScript 类型，包括带空格的字段名
2. **API 服务**：创建可复用的 API 调用函数
3. **React Hook**：封装数据获取逻辑
4. **组件**：实现完整的列表展示、排序、过滤、分页功能
5. **错误处理**：妥善处理各种错误情况
6. **空值处理**：所有字段都可能为 null，需要做好检查

按照这个指南修改你的前端代码，就能正确调用后端 API 了！

