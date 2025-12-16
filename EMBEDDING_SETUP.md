# Embedding 和向量数据库设置指南

本文档说明如何设置和使用 Cohere Embedding 和 Qdrant 向量数据库。

## 前置要求

1. **Qdrant 服务**
   - 安装并启动 Qdrant（默认端口：6333）
   - 可以使用 Docker：`docker run -p 6333:6333 qdrant/qdrant`

2. **Cohere API Key**
   - 在 [Cohere 官网](https://cohere.com/) 注册账号
   - 获取 API Key

## 配置步骤

### 1. 更新 application.yml

在 `src/main/resources/application.yml` 中配置：

```yaml
# Cohere Embedding 配置
cohere:
  api:
    key: YOUR_COHERE_API_KEY  # 替换为你的 Cohere API Key
    model: embed-english-v3.0  # 使用的模型
    input-type: search_document  # 输入类型
  enabled: true

# Qdrant 向量数据库配置
qdrant:
  host: localhost
  port: 6333
  collection: sales_data_vectors
  vector-size: 1024  # Cohere embed-english-v3.0 的向量维度
  enabled: true
```

### 2. 运行数据库迁移

确保已运行数据库迁移脚本 `V10__add_embedding_fields.sql`，添加 `embedding_text` 和 `embedding_hash` 字段。

### 3. 生成 Embedding Text

首先需要为所有 SalesData 记录生成 `embedding_text` 和 `embedding_hash`：

```bash
POST http://localhost:8000/api/admin/embedding/generate-all?batchSize=100
```

或者仅为缺失的记录生成：

```bash
POST http://localhost:8000/api/admin/embedding/generate-missing?batchSize=100
```

### 4. 生成向量并存储到 Qdrant

为所有有 `embedding_text` 的记录生成向量并存储到 Qdrant：

```bash
POST http://localhost:8000/api/admin/embedding/generate-vectors?batchSize=100
```

## API 端点

### 1. 生成 Embedding Text

**生成所有记录的 embedding_text**
```
POST /api/admin/embedding/generate-all
参数: batchSize (可选, 默认: 100)
```

**仅为缺失的记录生成 embedding_text**
```
POST /api/admin/embedding/generate-missing
参数: batchSize (可选, 默认: 100)
```

### 2. 生成向量并存储到 Qdrant

```
POST /api/admin/embedding/generate-vectors
参数: batchSize (可选, 默认: 100)
```

### 3. 向量相似度搜索

```
GET /api/admin/embedding/search?query=your search query&topK=10
参数:
  - query: 搜索查询文本（必需）
  - topK: 返回结果数量（可选, 默认: 10）
```

## 工作流程

1. **第一步：生成 embedding_text**
   - 调用 `/generate-all` 或 `/generate-missing`
   - 这会为每个 SalesData 记录生成英文描述文本和 SHA256 哈希

2. **第二步：生成向量并存储**
   - 调用 `/generate-vectors`
   - 这会使用 Cohere API 为每个 `embedding_text` 生成向量
   - 然后将向量存储到 Qdrant

3. **第三步：使用向量搜索**
   - 调用 `/search` 进行相似度搜索
   - 系统会为查询文本生成向量，然后在 Qdrant 中搜索相似的产品

## Embedding Text 生成规则

系统会按照以下规则生成英文描述文本（空字段自动跳过）：

```
ItemName: {ItemName}; Model: {Model}; Performance: {Performance}; 
Performance2: {Performance.1}; Material: {Material}; Brand: {Brand Code}; 
UOM: {UOM}; Function: {Function}; ItemType: {ItemType}; 
Hierarchy: {Product Hierarchy 3}
```

## 注意事项

1. **Cohere API 限制**
   - 注意 API 调用频率限制
   - 大批量处理时建议使用较小的 batchSize

2. **Qdrant 连接**
   - 确保 Qdrant 服务正在运行
   - 检查端口和主机配置是否正确

3. **向量维度**
   - Cohere `embed-english-v3.0` 的向量维度是 1024
   - 如果使用其他模型，需要相应调整 `vector-size` 配置

4. **性能优化**
   - 大批量处理时建议分批进行
   - 可以设置较小的 batchSize 避免内存问题

## 故障排查

### Qdrant 连接失败
- 检查 Qdrant 服务是否运行：`curl http://localhost:6333/health`
- 检查配置中的 host 和 port

### Cohere API 错误
- 检查 API Key 是否正确
- 检查网络连接
- 查看日志中的详细错误信息

### 向量维度不匹配
- 确认使用的 Cohere 模型的向量维度
- 更新 `application.yml` 中的 `vector-size` 配置

