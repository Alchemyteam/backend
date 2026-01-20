# Punchout Postman 测试指南

## 📋 目录

- [准备工作](#准备工作)
- [测试流程](#测试流程)
- [接口测试步骤](#接口测试步骤)
- [常见问题](#常见问题)

---

## 准备工作

### 1. 确保后端服务运行

```bash
# 检查服务是否运行在 http://localhost:8000
# 查看后端日志确认服务已启动
```

### 2. 准备测试数据

确保数据库中有测试用户（用于 Setup 请求）：
- 邮箱：`test@example.com`（或使用你配置的邮箱）

---

## 测试流程

完整的 Punchout 测试流程：

1. **Setup** → 获取 `sessionToken`
2. **Validate** → 验证会话（可选）
3. **Return** → 返回购物车

---

## 接口测试步骤

### 步骤 1: 测试 Setup (JSON) - 最简单的方式

#### 1.1 创建新请求

1. 打开 Postman
2. 点击 **New** → **HTTP Request**
3. 设置请求名称：`Punchout Setup (JSON)`

#### 1.2 配置请求

**Method**: `POST`

**URL**: 
```
http://localhost:8000/api/punchout/setup
```

**Headers**:
- Key: `Content-Type`
- Value: `application/json`

**Body** (选择 `raw` → `JSON`):
```json
{
  "buyerCookie": "test-cookie-123",
  "buyerId": "buyer-001",
  "buyerName": "Test Buyer",
  "organizationId": "org-001",
  "returnUrl": "https://airliquideasia-test.coupahost.com/punchout/checkout?id=25",
  "operation": "create"
}
```

#### 1.3 发送请求

点击 **Send** 按钮

#### 1.4 查看响应

**成功响应** (200 OK):
```json
{
  "sessionToken": "abc123xyz456",
  "redirectUrl": "http://localhost:3000/punchout/shopping?sessionToken=abc123xyz456",
  "expiresIn": 1800
}
```

**重要**: 复制 `sessionToken` 的值，后续测试会用到！

---

### 步骤 2: 测试 Setup (cXML) - 标准方式

#### 2.1 创建新请求

1. 点击 **New** → **HTTP Request**
2. 设置请求名称：`Punchout Setup (cXML)`

#### 2.2 配置请求

**Method**: `POST`

**URL**: 
```
http://localhost:8000/api/punchout/setup
```

**Headers**:
- Key: `Content-Type`
- Value: `application/xml`

**Body** (选择 `raw` → `XML`):
```xml
<!DOCTYPE cXML SYSTEM "http://xml.cxml.org/schemas/cXML/1.2.014/cXML.dtd">
<cXML xml:lang="en-US" payloadID="test-123" timestamp="2024-01-01T12:00:00+08:00">
  <Header>
    <From>
      <Credential domain="NetworkID">
        <Identity>AIR_LIQUIDE</Identity>
      </Credential>
    </From>
    <To>
      <Credential domain="allinton.com.sg">
        <Identity>Allinton123</Identity>
      </Credential>
    </To>
    <Sender>
      <Credential domain="NetworkID">
        <Identity>AIR_LIQUIDE</Identity>
        <SharedSecret>s3cr3tk3y!</SharedSecret>
      </Credential>
      <UserAgent>Coupa Procurement 1.0</UserAgent>
    </Sender>
  </Header>
  <Request>
    <PunchOutSetupRequest operation="create">
      <BuyerCookie>test-cookie-123</BuyerCookie>
      <Extrinsic name="FirstName">Test</Extrinsic>
      <Extrinsic name="LastName">User</Extrinsic>
      <Extrinsic name="UserEmail">test@example.com</Extrinsic>
      <Extrinsic name="UniqueName">test@example.com</Extrinsic>
      <Extrinsic name="User">test@example.com</Extrinsic>
      <Extrinsic name="BusinessUnit">COUPA</Extrinsic>
      <BrowserFormPost>
        <URL>https://airliquideasia-test.coupahost.com/punchout/checkout?id=25</URL>
      </BrowserFormPost>
      <Contact role="endUser">
        <Name xml:lang="en-SG">Test User</Name>
        <Email>test@example.com</Email>
      </Contact>
      <SupplierSetup>
        <URL>https://allintonbeta.sana-cloud.net/punchout</URL>
      </SupplierSetup>
    </PunchOutSetupRequest>
  </Request>
</cXML>
```

**重要**: 确保 `UserEmail` 对应的用户存在于数据库中！

#### 2.3 发送请求

点击 **Send** 按钮

#### 2.4 查看响应

**成功响应** (200 OK, XML 格式):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<cXML xml:lang="en-US" payloadID="..." timestamp="...">
  <Response>
    <Status code="200" text="OK"/>
    <PunchOutSetupResponse>
      <StartPage>
        <URL>http://localhost:3000/punchout?sessionToken=abc123xyz456</URL>
      </StartPage>
    </PunchOutSetupResponse>
  </Response>
</cXML>
```

**提取 sessionToken**: 从 `<URL>` 标签的 query 参数中提取 `sessionToken`

---

### 步骤 3: 测试 Validate

#### 3.1 创建新请求

1. 点击 **New** → **HTTP Request**
2. 设置请求名称：`Punchout Validate`

#### 3.2 配置请求

**Method**: `GET`

**URL**: 
```
http://localhost:8000/api/punchout/validate
```

**Params** (Query Params 标签):
- Key: `sessionToken`
- Value: `YOUR_SESSION_TOKEN`（从 Setup 响应中获取）

**完整 URL 示例**:
```
http://localhost:8000/api/punchout/validate?sessionToken=abc123xyz456
```

#### 3.3 发送请求

点击 **Send** 按钮

#### 3.4 查看响应

**成功响应** (200 OK):
```json
{
  "valid": true
}
```

**失败响应** (200 OK):
```json
{
  "valid": false
}
```

---

### 步骤 4: 测试 Return

#### 4.1 创建新请求

1. 点击 **New** → **HTTP Request**
2. 设置请求名称：`Punchout Return`

#### 4.2 配置请求

**Method**: `POST`

**URL**: 
```
http://localhost:8000/api/punchout/return
```

**Headers**:
- Key: `Content-Type`
- Value: `application/json`

**Body** (选择 `raw` → `JSON`):
```json
{
  "sessionToken": "YOUR_SESSION_TOKEN",
  "items": [
    {
      "productId": "PROD-001",
      "sku": "SKU-001",
      "productName": "测试产品",
      "quantity": 2,
      "unitPrice": 99.99,
      "supplierPartId": "SUPPLIER-PART-001",
      "unitOfMeasure": "EA"
    },
    {
      "productId": "PROD-002",
      "quantity": 1,
      "unitPrice": 199.99
    }
  ],
  "buyerOrderNumber": "PO-2024-001",
  "notes": "测试备注"
}
```

**重要**: 
- 替换 `YOUR_SESSION_TOKEN` 为从 Setup 获取的实际 token
- `items` 数组至少需要 1 个元素
- 每个 item 必须包含 `productId` 和 `quantity`（≥ 1）

#### 4.3 发送请求

点击 **Send** 按钮

#### 4.4 查看响应

**成功响应** (200 OK):
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

**失败响应** (400 Bad Request):
```json
{
  "message": "Failed to process punchout return",
  "errors": null,
  "error": "Buyer system returned error: 400 - Bad Request"
}
```

**验证失败响应** (400 Bad Request):
```json
{
  "message": "Validation failed",
  "errors": {
    "sessionToken": ["Session token is required"],
    "items[0].productId": ["Product ID is required"],
    "items[0].quantity": ["Quantity is required"]
  },
  "error": null
}
```

---

## 📝 Postman 使用技巧

### 1. 使用环境变量

创建 Postman Environment 来管理变量：

1. 点击右上角 **Environments** → **+**
2. 创建新环境：`Local Development`
3. 添加变量：
   - `base_url`: `http://localhost:8000/api`
   - `session_token`: (留空，从 Setup 响应中自动设置)

4. 在请求中使用变量：
   - URL: `{{base_url}}/punchout/setup`
   - Body: `"sessionToken": "{{session_token}}"`

### 2. 使用 Tests 脚本自动提取 sessionToken

在 **Setup (JSON)** 请求的 **Tests** 标签中添加：

```javascript
// 解析响应
var jsonData = pm.response.json();

// 检查是否有 sessionToken
if (jsonData.sessionToken) {
    // 保存到环境变量
    pm.environment.set("session_token", jsonData.sessionToken);
    console.log("Session token saved: " + jsonData.sessionToken);
}
```

这样，后续的 Validate 和 Return 请求就可以自动使用这个 token 了。

### 3. 使用 Pre-request Script

在 **Return** 请求的 **Pre-request Script** 标签中添加：

```javascript
// 检查 sessionToken 是否存在
if (!pm.environment.get("session_token")) {
    console.error("Session token not found! Please run Setup request first.");
}
```

### 4. 保存请求到 Collection

1. 创建 Collection：`Punchout API Tests`
2. 将所有请求添加到 Collection
3. 可以按顺序运行所有请求（使用 Collection Runner）

---

## 🔍 调试技巧

### 1. 查看完整请求

在 Postman 中：
- 点击 **Code** 按钮（在 Send 按钮旁边）
- 可以查看完整的 cURL 命令

### 2. 查看响应详情

- **Status**: HTTP 状态码
- **Time**: 响应时间
- **Size**: 响应大小
- **Body**: 响应内容
- **Headers**: 响应头

### 3. 使用 Console

1. 点击 **View** → **Show Postman Console`
2. 可以看到所有请求和响应的详细信息

### 4. 检查后端日志

查看后端控制台日志，可以看到：
- 接收到的请求
- 处理过程
- 错误信息

---

## ⚠️ 常见问题

### 问题 1: 400 Bad Request - Validation failed

**原因**: 请求体缺少必需字段或格式错误

**解决方案**:
1. 检查 `sessionToken` 是否存在且不为空
2. 检查 `items` 数组是否不为空
3. 检查每个 item 是否包含 `productId` 和 `quantity`
4. 查看响应中的 `errors` 字段，了解具体哪些字段验证失败

### 问题 2: 401 Unauthorized - Authentication failed

**原因**: cXML Setup 请求中的认证信息不正确

**解决方案**:
1. 检查 `Sender.Identity` 是否为 `AIR_LIQUIDE`
2. 检查 `Sender.SharedSecret` 是否为 `s3cr3tk3y!`
3. 检查 `application.yml` 中的 `punchout.allowed-senders` 配置

### 问题 3: 404 Not Found - User not found

**原因**: Setup 请求中的 `UserEmail` 对应的用户不存在

**解决方案**:
1. 在数据库中创建对应的用户
2. 或修改测试请求中的 `UserEmail` 为已存在的用户邮箱

### 问题 4: 400 Bad Request - Session expired

**原因**: sessionToken 已过期（30 分钟）

**解决方案**:
1. 重新执行 Setup 请求获取新的 sessionToken
2. 使用新的 sessionToken 进行 Return 请求

### 问题 5: 400 Bad Request - Return URL host is not in allowed list

**原因**: `BrowserFormPost.URL` 的 host 不在允许列表中

**解决方案**:
1. 检查 `application.yml` 中的 `punchout.allowed-return-hosts` 配置
2. 确保测试 URL 的 host 在允许列表中

---

## 📋 完整测试 Checklist

- [ ] 后端服务已启动（http://localhost:8000）
- [ ] 数据库中有测试用户
- [ ] 测试 Setup (JSON) - 获取 sessionToken
- [ ] 测试 Validate - 验证会话
- [ ] 测试 Return - 返回购物车
- [ ] 检查所有响应状态码
- [ ] 检查错误处理（无效 token、空购物车等）

---

## 🎯 快速测试脚本

### Postman Collection JSON

你可以导入这个 Collection 到 Postman：

```json
{
  "info": {
    "name": "Punchout API Tests",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Setup (JSON)",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"buyerCookie\": \"test-cookie-123\",\n  \"buyerId\": \"buyer-001\",\n  \"returnUrl\": \"https://airliquideasia-test.coupahost.com/punchout/checkout?id=25\"\n}"
        },
        "url": {
          "raw": "http://localhost:8000/api/punchout/setup",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8000",
          "path": ["api", "punchout", "setup"]
        }
      },
      "event": [
        {
          "listen": "test",
          "script": {
            "exec": [
              "var jsonData = pm.response.json();",
              "if (jsonData.sessionToken) {",
              "    pm.environment.set(\"session_token\", jsonData.sessionToken);",
              "}"
            ]
          }
        }
      ]
    },
    {
      "name": "Validate",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8000/api/punchout/validate?sessionToken={{session_token}}",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8000",
          "path": ["api", "punchout", "validate"],
          "query": [
            {
              "key": "sessionToken",
              "value": "{{session_token}}"
            }
          ]
        }
      }
    },
    {
      "name": "Return",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"sessionToken\": \"{{session_token}}\",\n  \"items\": [\n    {\n      \"productId\": \"PROD-001\",\n      \"quantity\": 2,\n      \"unitPrice\": 99.99\n    }\n  ]\n}"
        },
        "url": {
          "raw": "http://localhost:8000/api/punchout/return",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8000",
          "path": ["api", "punchout", "return"]
        }
      }
    }
  ]
}
```

**导入方法**:
1. 打开 Postman
2. 点击 **Import** 按钮
3. 选择 **Raw text**
4. 粘贴上面的 JSON
5. 点击 **Import**

---

## 📚 相关文档

- [Punchout API 文档](./PUNCHOUT_API_DOCUMENTATION.md)
- [Punchout 测试指南](./PUNCHOUT_TESTING_GUIDE.md)

---

**提示**: 如果遇到问题，查看后端日志和 Postman Console 获取详细错误信息。

