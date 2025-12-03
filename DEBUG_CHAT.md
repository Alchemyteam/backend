# Chat Service 调试指南

## 问题：tableData 和 actionData 为 null

### 可能的原因

1. **意图识别失败** - 消息没有被正确识别为 CREATE_REQUISITION
2. **产品搜索失败** - 没有找到匹配的产品
3. **异常被捕获** - 代码执行过程中抛出异常

### 调试步骤

1. **查看日志**
   - 检查 `Processing chat message:` 日志，确认消息被接收
   - 检查 `Detected intent:` 日志，确认意图识别结果
   - 检查 `Extracted product keyword:` 日志，确认关键词提取
   - 检查 `Found X products` 日志，确认产品搜索结果

2. **测试意图识别**
   - "Can you create a purchase requisition for battery?" 应该识别为 `CREATE_REQUISITION`
   - "create purchase requisition for battery" 应该识别为 `CREATE_REQUISITION`
   - "make a requisition for battery" 应该识别为 `CREATE_REQUISITION`

3. **测试产品搜索**
   - 确保数据库中有产品数据
   - 检查产品名称、描述、分类是否包含关键词

4. **检查 LLM API**
   - 确认 Google Gemini API Key 正确
   - 检查 API 调用是否成功
   - 查看是否有 API 错误日志

### 已添加的改进

1. ✅ 添加了详细的日志记录
2. ✅ 改进了意图识别（支持更多变体）
3. ✅ 即使没有找到产品，也返回 tableData 和 actionData
4. ✅ LLM 调用失败时使用默认回复，不影响 tableData 和 actionData
5. ✅ 一般查询如果找到相关产品，也会返回 tableData

### 测试请求示例

```json
POST /api/chat/message
Authorization: Bearer {token}

{
  "message": "Can you create a purchase requisition for battery?"
}
```

### 预期响应

```json
{
  "response": "...",
  "conversationId": "...",
  "tableData": {
    "title": "Purchase Requisition - battery",
    "headers": [...],
    "rows": [...],
    "description": "..."
  },
  "actionData": {
    "actionType": "CREATE_REQUISITION",
    "parameters": {...},
    "message": "..."
  }
}
```

### 如果仍然为 null

1. 检查后端日志，查看具体的错误信息
2. 确认数据库中是否有产品数据
3. 确认产品名称/描述中包含 "battery" 关键词
4. 检查 Google Gemini API 是否正常工作


