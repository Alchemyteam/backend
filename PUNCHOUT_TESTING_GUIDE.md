# Punchout 测试指南

## 📍 测试 URL

### 基础配置

- **服务器地址**: `http://localhost:8000`
- **Context Path**: `/api`
- **完整 Base URL**: `http://localhost:8000/api`

### 接口列表

| 接口 | 方法 | 完整 URL | Content-Type |
|------|------|----------|--------------|
| Setup (cXML) | POST | `http://localhost:8000/api/punchout/setup` | `application/xml` 或 `text/xml` |
| Setup (JSON) | POST | `http://localhost:8000/api/punchout/setup` | `application/json` |
| Return | POST | `http://localhost:8000/api/punchout/return` | `application/json` |
| Validate | GET | `http://localhost:8000/api/punchout/validate?sessionToken=xxx` | - |

---

## 🧪 测试方法

### 方法 1: 使用 cURL（命令行）

#### 1. 测试 Setup (cXML)

```bash
curl -X POST http://localhost:8000/api/punchout/setup \
  -H "Content-Type: application/xml" \
  -d @setup-request.xml
```

**setup-request.xml** 文件内容：

```xml
<!DOCTYPE cXML SYSTEM "http://xml.cxml.org/schemas/cXML/1.2.014/cXML.dtd">
<cXML xml:lang="en-US" payloadID="1631508858.3876817@stg485app17.int.coupahost.com" timestamp="2021-09-13T12:54:18+08:00">
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
      <BuyerCookie>40744c851db9b4c8f63ff2312ce5c389</BuyerCookie>
      <Extrinsic name="FirstName">Rashmi</Extrinsic>
      <Extrinsic name="LastName">M</Extrinsic>
      <Extrinsic name="UniqueName">rashmi.m-sc@airliquide.com</Extrinsic>
      <Extrinsic name="UserEmail">rashmi.m-sc@airliquide.com</Extrinsic>
      <Extrinsic name="User">rashmi.m-sc@airliquide.com</Extrinsic>
      <Extrinsic name="company"></Extrinsic>
      <Extrinsic name="BusinessUnit">COUPA</Extrinsic>
      <BrowserFormPost>
        <URL>https://airliquideasia-test.coupahost.com/punchout/checkout?id=25</URL>
      </BrowserFormPost>
      <Contact role="endUser">
        <Name xml:lang="en-SG">rashmi.m-sc@airliquide.com</Name>
        <Email>rashmi.m-sc@airliquide.com</Email>
      </Contact>
      <SupplierSetup>
        <URL>https://allintonbeta.sana-cloud.net/punchout</URL>
      </SupplierSetup>
    </PunchOutSetupRequest>
  </Request>
</cXML>
```

#### 2. 测试 Setup (JSON)

```bash
curl -X POST http://localhost:8000/api/punchout/setup \
  -H "Content-Type: application/json" \
  -d '{
    "buyerCookie": "test-cookie-123",
    "buyerId": "buyer-001",
    "buyerName": "Test Buyer",
    "organizationId": "org-001",
    "returnUrl": "https://airliquideasia-test.coupahost.com/punchout/checkout?id=25",
    "operation": "create"
  }'
```

#### 3. 测试 Validate

```bash
# 替换 YOUR_SESSION_TOKEN 为 Setup 返回的 sessionToken
curl -X GET "http://localhost:8000/api/punchout/validate?sessionToken=YOUR_SESSION_TOKEN"
```

#### 4. 测试 Return

```bash
# 替换 YOUR_SESSION_TOKEN 为 Setup 返回的 sessionToken
curl -X POST http://localhost:8000/api/punchout/return \
  -H "Content-Type: application/json" \
  -d '{
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
      }
    ],
    "buyerOrderNumber": "PO-2024-001",
    "notes": "测试备注"
  }'
```

---

### 方法 2: 使用 Postman

#### 1. 测试 Setup (cXML)

1. **创建新请求**
   - Method: `POST`
   - URL: `http://localhost:8000/api/punchout/setup`

2. **设置 Headers**
   - `Content-Type`: `application/xml`

3. **设置 Body**
   - 选择 `raw`
   - 选择 `XML`
   - 粘贴上面的 XML 内容

4. **发送请求**

**预期响应**（成功）:

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

从响应中提取 `sessionToken`（在 StartPage URL 的 query 参数中）。

#### 2. 测试 Setup (JSON)

1. **创建新请求**
   - Method: `POST`
   - URL: `http://localhost:8000/api/punchout/setup`

2. **设置 Headers**
   - `Content-Type`: `application/json`

3. **设置 Body**
   - 选择 `raw`
   - 选择 `JSON`
   - 粘贴以下 JSON：

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

**预期响应**（成功）:

```json
{
  "sessionToken": "abc123xyz456",
  "redirectUrl": "http://localhost:3000/punchout?sessionToken=abc123xyz456",
  "expiresIn": 1800
}
```

#### 3. 测试 Validate

1. **创建新请求**
   - Method: `GET`
   - URL: `http://localhost:8000/api/punchout/validate`
   - Params:
     - Key: `sessionToken`
     - Value: `YOUR_SESSION_TOKEN`（从 Setup 响应中获取）

**预期响应**:

```json
{
  "valid": true
}
```

#### 4. 测试 Return

1. **创建新请求**
   - Method: `POST`
   - URL: `http://localhost:8000/api/punchout/return`

2. **设置 Headers**
   - `Content-Type`: `application/json`

3. **设置 Body**
   - 选择 `raw`
   - 选择 `JSON`
   - 粘贴以下 JSON（替换 `YOUR_SESSION_TOKEN`）：

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
    }
  ],
  "buyerOrderNumber": "PO-2024-001",
  "notes": "测试备注"
}
```

**预期响应**（成功）:

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

---

### 方法 3: 使用 HTTPie

#### 1. 测试 Setup (cXML)

```bash
echo '<!DOCTYPE cXML SYSTEM "http://xml.cxml.org/schemas/cXML/1.2.014/cXML.dtd">
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
      <Extrinsic name="UserEmail">test@example.com</Extrinsic>
      <BrowserFormPost>
        <URL>https://airliquideasia-test.coupahost.com/punchout/checkout?id=25</URL>
      </BrowserFormPost>
      <SupplierSetup>
        <URL>https://allintonbeta.sana-cloud.net/punchout</URL>
      </SupplierSetup>
    </PunchOutSetupRequest>
  </Request>
</cXML>' | http POST http://localhost:8000/api/punchout/setup Content-Type:application/xml
```

#### 2. 测试 Setup (JSON)

```bash
http POST http://localhost:8000/api/punchout/setup \
  Content-Type:application/json \
  buyerCookie="test-cookie-123" \
  buyerId="buyer-001" \
  buyerName="Test Buyer" \
  returnUrl="https://airliquideasia-test.coupahost.com/punchout/checkout?id=25"
```

#### 3. 测试 Validate

```bash
http GET http://localhost:8000/api/punchout/validate sessionToken==YOUR_SESSION_TOKEN
```

#### 4. 测试 Return

```bash
http POST http://localhost:8000/api/punchout/return \
  Content-Type:application/json \
  sessionToken="YOUR_SESSION_TOKEN" \
  items:='[{"productId":"PROD-001","quantity":2,"unitPrice":99.99}]'
```

---

## 🔑 重要配置信息

### 认证配置

在 `application.yml` 中配置的认证信息：

```yaml
punchout:
  allowed-senders: AIR_LIQUIDE:s3cr3tk3y!
```

**测试时使用的认证信息**:
- **Identity**: `AIR_LIQUIDE`
- **SharedSecret**: `s3cr3tk3y!`

### 用户配置

**重要**: Setup 请求中的 `UserEmail` 必须对应数据库中存在的用户邮箱。

**检查用户是否存在**:

```sql
SELECT * FROM users WHERE email = 'rashmi.m-sc@airliquide.com';
```

如果用户不存在，需要先创建用户，或者修改测试请求中的 `UserEmail` 为已存在的用户邮箱。

### 前端 URL 配置

在 `application.yml` 中配置：

```yaml
punchout:
  frontend-url: http://localhost:3000
```

Setup 成功后，返回的 `redirectUrl` 会使用这个配置。

---

## 📝 完整测试流程

### 步骤 1: 启动后端服务

```bash
# 确保后端服务正在运行
# 检查日志确认服务已启动在 http://localhost:8000
```

### 步骤 2: 准备测试数据

1. **确保数据库中有测试用户**
   - 邮箱: `test@example.com`（或使用你配置的邮箱）
   - 如果使用 cXML 测试，确保 `UserEmail` 对应的用户存在

2. **准备 cXML 请求文件**（如果使用 cXML 测试）
   - 创建 `setup-request.xml` 文件
   - 确保 `UserEmail` 对应数据库中的用户

### 步骤 3: 执行 Setup 请求

**使用 cXML**:

```bash
curl -X POST http://localhost:8000/api/punchout/setup \
  -H "Content-Type: application/xml" \
  -d @setup-request.xml
```

**使用 JSON**:

```bash
curl -X POST http://localhost:8000/api/punchout/setup \
  -H "Content-Type: application/json" \
  -d '{
    "buyerCookie": "test-cookie-123",
    "buyerId": "buyer-001",
    "returnUrl": "https://airliquideasia-test.coupahost.com/punchout/checkout?id=25"
  }'
```

**从响应中提取 `sessionToken`**:

- cXML 响应: 从 `<StartPage><URL>...</URL></StartPage>` 中提取 query 参数 `sessionToken`
- JSON 响应: 直接使用 `sessionToken` 字段

### 步骤 4: 验证会话

```bash
curl -X GET "http://localhost:8000/api/punchout/validate?sessionToken=YOUR_SESSION_TOKEN"
```

预期响应: `{"valid": true}`

### 步骤 5: 返回购物车

```bash
curl -X POST http://localhost:8000/api/punchout/return \
  -H "Content-Type: application/json" \
  -d '{
    "sessionToken": "YOUR_SESSION_TOKEN",
    "items": [
      {
        "productId": "PROD-001",
        "quantity": 2,
        "unitPrice": 99.99
      }
    ]
  }'
```

预期响应: `{"success": true, "status": "returned", ...}`

---

## ⚠️ 常见问题

### 1. 认证失败

**错误**: `Authentication failed for sender: AIR_LIQUIDE`

**解决方案**:
- 检查 `application.yml` 中的 `punchout.allowed-senders` 配置
- 确保 cXML 中的 `Sender.Identity` 和 `Sender.SharedSecret` 与配置匹配

### 2. 用户不存在

**错误**: `User not found with email: xxx@example.com`

**解决方案**:
- 在数据库中创建对应的用户
- 或修改测试请求中的 `UserEmail` 为已存在的用户邮箱

### 3. 会话过期

**错误**: `Session expired or invalid`

**解决方案**:
- 会话有效期为 30 分钟
- 重新执行 Setup 请求获取新的 `sessionToken`

### 4. 购物车为空

**错误**: `Cart items are required`

**解决方案**:
- 确保 `items` 数组不为空
- 确保每个 item 都有 `productId` 和 `quantity`（≥ 1）

### 5. Return URL 验证失败

**错误**: `Return URL host is not in allowed list`

**解决方案**:
- 检查 `application.yml` 中的 `punchout.allowed-return-hosts` 配置
- 确保 `BrowserFormPost.URL` 的 host 在允许列表中

---

## 🔍 调试技巧

### 1. 查看后端日志

后端会输出详细的日志信息，包括：
- 接收到的请求
- 解析的 cXML 内容（已脱敏）
- 处理过程
- 错误信息

### 2. 使用 Postman Console

在 Postman 中查看：
- 请求和响应的完整内容
- HTTP 状态码
- 响应时间

### 3. 验证 cXML 格式

使用在线 XML 验证工具验证 cXML 格式是否正确。

### 4. 检查数据库

```sql
-- 检查用户是否存在
SELECT * FROM users WHERE email = 'test@example.com';

-- 检查会话（如果存储在数据库中）
-- 注意：当前实现使用内存存储，重启服务后会话会丢失
```

---

## 📚 相关文档

- [Punchout API 文档](./PUNCHOUT_API_DOCUMENTATION.md)
- [cXML 标准文档](http://xml.cxml.org/)

---

## 🎯 快速测试脚本

创建一个 `test-punchout.sh` 脚本：

```bash
#!/bin/bash

BASE_URL="http://localhost:8000/api"

echo "=== 1. Testing Setup (JSON) ==="
SETUP_RESPONSE=$(curl -s -X POST "$BASE_URL/punchout/setup" \
  -H "Content-Type: application/json" \
  -d '{
    "buyerCookie": "test-cookie-123",
    "buyerId": "buyer-001",
    "returnUrl": "https://airliquideasia-test.coupahost.com/punchout/checkout?id=25"
  }')

echo "$SETUP_RESPONSE" | jq '.'

# 提取 sessionToken（需要安装 jq）
SESSION_TOKEN=$(echo "$SETUP_RESPONSE" | jq -r '.sessionToken')

if [ "$SESSION_TOKEN" == "null" ] || [ -z "$SESSION_TOKEN" ]; then
  echo "❌ Failed to get sessionToken"
  exit 1
fi

echo ""
echo "Session Token: $SESSION_TOKEN"
echo ""

echo "=== 2. Testing Validate ==="
curl -s -X GET "$BASE_URL/punchout/validate?sessionToken=$SESSION_TOKEN" | jq '.'

echo ""
echo "=== 3. Testing Return ==="
curl -s -X POST "$BASE_URL/punchout/return" \
  -H "Content-Type: application/json" \
  -d "{
    \"sessionToken\": \"$SESSION_TOKEN\",
    \"items\": [
      {
        \"productId\": \"PROD-001\",
        \"quantity\": 2,
        \"unitPrice\": 99.99
      }
    ]
  }" | jq '.'

echo ""
echo "=== Test Complete ==="
```

使用方法：

```bash
chmod +x test-punchout.sh
./test-punchout.sh
```

---

**提示**: 确保后端服务正在运行，并且数据库连接正常。

