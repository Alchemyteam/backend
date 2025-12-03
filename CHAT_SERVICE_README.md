# Chat Service 聊天服务

基于大语言模型（LLM）的智能聊天服务，用于处理产品相关的查询并返回结构化 JSON 数据。

## 功能特性

### ✅ 支持的查询类型

1. **创建采购需求** (CREATE_REQUISITION)
   - 示例: "Can you create a purchase requisition for battery?"
   - 返回: 产品列表表格 + 操作数据

2. **搜索产品** (SEARCH_PRODUCTS)
   - 示例: "Search for steel formwork"
   - 返回: 产品搜索结果表格

3. **获取产品信息** (GET_PRODUCT_INFO)
   - 示例: "Tell me about battery products"
   - 返回: 产品详细信息表格

4. **比较产品** (COMPARE_PRODUCTS)
   - 示例: "Compare battery and steel products"
   - 返回: 产品对比表格

5. **通用查询** (GENERAL)
   - 其他产品相关问题
   - 返回: LLM 生成的文本回复

## API 接口

### 发送消息

**POST** `/api/chat/message`

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
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

### 健康检查

**GET** `/api/chat/health`

**响应**:
```json
{
  "status": "ok",
  "service": "chat"
}
```

## 配置说明

### LLM API 配置

在 `application.yml` 中配置：

```yaml
llm:
  api:
    url: https://generativelanguage.googleapis.com/v1beta/models
    key: AIzaSyCFwNSU1VL4LqGTbFMpK_6N0VOpAJnTwxw  # Google AI Studio API Key
  model: gemini-pro  # 或 gemini-pro-vision
```

### 环境变量（可选）

如果需要从环境变量读取 API Key：

```yaml
llm:
  api:
    key: ${GOOGLE_AI_API_KEY:AIzaSyCFwNSU1VL4LqGTbFMpK_6N0VOpAJnTwxw}
```

```bash
# Windows
set GOOGLE_AI_API_KEY=your-api-key-here

# Linux/Mac
export GOOGLE_AI_API_KEY=your-api-key-here
```

### 支持的 LLM 服务

- **Google AI Studio (Gemini)**: 当前配置
  - API 地址: `https://generativelanguage.googleapis.com/v1beta/models`
  - 模型: `gemini-pro`, `gemini-pro-vision`
  - 获取 API Key: https://aistudio.google.com/app/apikey

## 响应格式说明

### TableData（表格数据）

用于前端生成表格：

```json
{
  "title": "表格标题",
  "headers": ["列1", "列2", "列3"],
  "rows": [
    {"列1": "值1", "列2": "值2", "列3": "值3"},
    {"列1": "值4", "列2": "值5", "列3": "值6"}
  ],
  "description": "表格描述"
}
```

### ActionData（操作数据）

用于前端执行特定操作：

```json
{
  "actionType": "CREATE_REQUISITION",
  "parameters": {
    "productKeyword": "battery",
    "productCount": 3
  },
  "message": "操作结果消息"
}
```

## 使用示例

### 示例 1: 创建采购需求

**请求**:
```json
{
  "message": "Can you create a purchase requisition for battery?"
}
```

**响应**: 包含匹配的电池产品表格和创建采购需求的操作数据。

### 示例 2: 搜索产品

**请求**:
```json
{
  "message": "Show me steel products"
}
```

**响应**: 包含搜索结果表格。

### 示例 3: 获取产品信息

**请求**:
```json
{
  "message": "Tell me about product prod_001"
}
```

**响应**: 包含产品详细信息表格。

## 注意事项

1. **API Key 安全**: 不要将 API Key 提交到代码仓库，使用环境变量。

2. **费用控制**: LLM API 调用会产生费用，建议：
   - 设置合理的 `max_tokens` 限制
   - 实现请求频率限制
   - 监控 API 使用量

3. **错误处理**: 如果 LLM API 调用失败，服务会返回默认回复，不会中断请求。

4. **产品搜索**: 当前使用简单的关键词匹配，可以改进为：
   - 全文搜索
   - 模糊匹配
   - 语义搜索

## 扩展建议

1. **对话历史**: 实现多轮对话上下文
2. **缓存机制**: 缓存常见查询结果
3. **意图识别优化**: 使用更智能的 NLP 模型
4. **产品推荐**: 基于用户查询推荐相关产品
5. **多语言支持**: 支持多种语言的查询

