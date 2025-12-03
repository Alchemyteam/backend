# LLM API 调用诊断指南

## 📍 LLM API 调用位置

LLM API 调用代码位于：
- **文件**: `src/main/java/com/ecosystem/service/LLMSearchParser.java`
- **方法**: `callLLM(String prompt)` (第 691 行)
- **调用位置**: `applyLLMParsing()` 方法中 (第 595 行)

## 🔧 配置位置

LLM API 配置在 `src/main/resources/application.yml`:

```yaml
llm:
  api:
    url: https://generativelanguage.googleapis.com/v1beta/models
    key: AIzaSyCFwNSU1VL4LqGTbFMpK_6N0VOpAJnTwxw
  model: gemini-pro
```

## 🔍 如何诊断 LLM API 调用问题

### 1. 查看日志

重启后端服务后，当你发送查询时，查看日志中的以下信息：

#### ✅ 正常调用应该看到：

```
[INFO] 🔵 Calling LLM API: https://generativelanguage.googleapis.com/v1beta/models with model: gemini-pro
[INFO] 🔵 API Key configured: Yes (length: 39)
[INFO] 🔵 LLM API URL: https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=***
[INFO] 🔵 Sending HTTP POST request to LLM API...
[INFO] LLM API response status: 200 OK
[INFO] LLM API response body keys: [candidates]
[INFO] LLM API call successful, response length: 250
```

#### ❌ 如果 API 调用失败，可能看到：

**情况 1: API Key 未配置**
```
[ERROR] LLM API key is not configured! Please set llm.api.key in application.yml
```

**情况 2: 认证失败 (401)**
```
[ERROR] ❌ LLM API HTTP client error (4xx): Status=401
[ERROR] ⚠️ LLM API authentication failed! Please check your API key in application.yml
```

**情况 3: 权限不足或额度用完 (403)**
```
[ERROR] ❌ LLM API HTTP client error (4xx): Status=403
[ERROR] ⚠️ LLM API access forbidden! Your API key may not have permission or quota may be exceeded.
```

**情况 4: 速率限制 (429) - 免费额度用完**
```
[ERROR] ❌ LLM API HTTP client error (4xx): Status=429
[ERROR] ⚠️ LLM API rate limit exceeded! You may have reached your free quota. Please check Google AI Studio.
```

**情况 5: 网络问题**
```
[ERROR] ❌ LLM API resource access error (network/timeout): ...
[ERROR] This could be a network issue or the API endpoint is unreachable.
```

**情况 6: API 返回错误**
```
[ERROR] LLM API returned error: {code=400, message=...}
[ERROR] LLM API error message: ...
[ERROR] LLM API error code: ...
```

### 2. 检查 API Key

1. **确认 API Key 是否正确配置**:
   - 打开 `src/main/resources/application.yml`
   - 检查 `llm.api.key` 是否设置
   - 确认 API Key 长度是否正确（Google AI Studio API Key 通常是 39 个字符）

2. **验证 API Key 是否有效**:
   - 访问 [Google AI Studio](https://makersuite.google.com/app/apikey)
   - 检查 API Key 是否仍然有效
   - 检查是否有使用限制或配额限制

### 3. 检查免费额度

Google AI Studio (Gemini API) 的免费额度：
- **免费层**: 每分钟 15 次请求，每天 1500 次请求
- 如果超过限制，会返回 429 错误

**如何检查额度**:
1. 访问 [Google AI Studio](https://makersuite.google.com/app/apikey)
2. 查看 API 使用情况
3. 检查是否达到每日/每分钟限制

### 4. 测试 API Key

你可以使用 curl 命令直接测试 API Key：

```bash
curl -X POST \
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "contents": [{
      "parts": [{
        "text": "Hello"
      }]
    }]
  }'
```

**如果成功**，会返回 JSON 响应，包含 `candidates` 字段。

**如果失败**，会返回错误信息，例如：
- `401`: API Key 无效
- `403`: 权限不足或额度用完
- `429`: 速率限制

### 5. 常见问题解决

#### 问题 1: API Key 无效
**解决方案**:
1. 在 [Google AI Studio](https://makersuite.google.com/app/apikey) 生成新的 API Key
2. 更新 `application.yml` 中的 `llm.api.key`
3. 重启后端服务

#### 问题 2: 免费额度用完
**解决方案**:
1. 等待配额重置（通常是每天重置）
2. 或者升级到付费计划
3. 或者减少 API 调用频率

#### 问题 3: 网络连接问题
**解决方案**:
1. 检查网络连接
2. 检查防火墙设置
3. 确认可以访问 `https://generativelanguage.googleapis.com`

#### 问题 4: API URL 错误
**解决方案**:
- 确认 `llm.api.url` 是: `https://generativelanguage.googleapis.com/v1beta/models`
- 确认 `llm.model` 是: `gemini-pro` 或 `gemini-1.5-pro`

## 📊 当前状态检查

根据你的日志，我看到：
```
[WARN] LLM API call returned null
[INFO] LLM expert response received: null
```

这说明：
1. ✅ API Key 已配置（否则会看到 "API key is not configured" 错误）
2. ❌ API 调用返回了 null（可能是网络问题、API 错误或额度问题）

**下一步**:
1. 重启后端服务
2. 发送一个查询（如 "Show all products from AET"）
3. 查看详细的日志输出，应该能看到：
   - HTTP 状态码
   - 错误信息（如果有）
   - API 响应内容

## 🔄 回退机制

即使 LLM API 调用失败，系统也有回退机制：

1. **规则匹配**: 如果 LLM 失败，会自动使用规则匹配提取关键词
2. **全文搜索**: 如果规则匹配也失败，会使用全文搜索

所以即使 LLM API 不可用，搜索功能仍然可以工作（只是可能不够智能）。

## 📝 日志级别

如果你想看到更详细的日志，可以在 `application.yml` 中设置：

```yaml
logging:
  level:
    com.ecosystem.service.LLMSearchParser: DEBUG
```

这样可以看到完整的 API 请求和响应内容。

