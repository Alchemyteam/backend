# Ecosystem Backend API

基于 Spring Boot 的后端 API 服务，提供用户认证和授权功能。

## 技术栈

- **Spring Boot 3.2.0**
- **Spring Security** - 安全认证
- **Spring Data JPA** - 数据持久化
- **JWT (jjwt)** - Token 认证
- **H2 Database** - 开发环境数据库（可切换为 MySQL）
- **Lombok** - 简化代码

## 项目结构

```
src/main/java/com/ecosystem/
├── config/              # 配置类
│   ├── CorsConfig.java          # CORS 配置
│   └── SecurityConfig.java      # Spring Security 配置
├── controller/          # 控制器层
│   └── AuthController.java      # 认证相关接口
├── dto/                 # 数据传输对象
│   ├── AuthResponse.java
│   ├── ErrorResponse.java
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   ├── UserResponse.java
│   └── VerifyResponse.java
├── entity/              # 实体类
│   └── User.java
├── exception/           # 异常处理
│   ├── EmailAlreadyExistsException.java
│   ├── GlobalExceptionHandler.java
│   └── InvalidCredentialsException.java
├── repository/          # 数据访问层
│   └── UserRepository.java
├── security/            # 安全相关
│   └── JwtAuthenticationFilter.java
├── service/             # 业务逻辑层
│   ├── AuthService.java
│   └── UserService.java
└── util/                # 工具类
    └── JwtUtil.java
```

## 快速开始

### 1. 环境要求

- JDK 17 或更高版本
- Maven 3.6 或更高版本

### 2. 配置

编辑 `src/main/resources/application.yml` 文件，根据需要修改配置：

```yaml
server:
  port: 8000
  servlet:
    context-path: /api

jwt:
  secret: your-secret-key-change-in-production  # 生产环境请修改为更安全的密钥
  expiration: 86400000  # Token 过期时间（毫秒），默认 24 小时

cors:
  allowed-origins: http://localhost:3000,http://localhost:5173  # 允许的前端域名
```

### 3. 运行项目

```bash
# 使用 Maven 运行
mvn spring-boot:run

# 或先编译再运行
mvn clean package
java -jar target/ecosystem-backend-1.0.0.jar
```

项目启动后，API 基础地址为：`http://localhost:8000/api`

### 4. 数据库

开发环境使用 H2 内存数据库，启动后可通过以下地址访问 H2 Console：
- URL: `http://localhost:8000/api/h2-console`
- JDBC URL: `jdbc:h2:mem:ecosystemdb`
- Username: `sa`
- Password: (留空)

## API 接口

### 1. 用户登录

**POST** `/api/auth/login`

请求体：
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

响应：
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

### 2. 用户注册

**POST** `/api/auth/register`

请求体：
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "User Name"  // 可选
}
```

响应：同登录接口

### 3. 获取当前用户信息

**GET** `/api/auth/me`

请求头：
```
Authorization: Bearer {token}
```

响应：
```json
{
  "id": "user_id_123",
  "email": "user@example.com",
  "name": "User Name"
}
```

### 4. 验证 Token

**GET** `/api/auth/verify`

请求头：
```
Authorization: Bearer {token}
```

响应：
```json
{
  "valid": true
}
```

## 错误处理

所有错误响应遵循统一格式：

```json
{
  "message": "主要错误信息",
  "errors": {
    "field_name": ["错误信息1", "错误信息2"]
  }
}
```

### HTTP 状态码

- **200**: 请求成功
- **400**: 请求参数错误（如验证失败）
- **401**: 未授权（token 无效或过期）
- **404**: 资源不存在
- **500**: 服务器内部错误

## 生产环境部署

### 1. 切换到 MySQL 数据库

修改 `application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ecosystemdb
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
```

### 2. 修改 JWT 密钥

生产环境必须使用足够长的随机密钥（至少 256 位）：

```yaml
jwt:
  secret: your-very-long-and-random-secret-key-minimum-256-bits
```

### 3. 配置 CORS

根据实际的前端域名配置：

```yaml
cors:
  allowed-origins: https://your-frontend-domain.com
```

## 测试

使用 Postman 或类似工具测试接口，或运行单元测试：

```bash
mvn test
```

## 注意事项

1. **JWT 密钥安全**: 生产环境必须使用强随机密钥
2. **密码加密**: 使用 BCrypt 加密存储密码
3. **CORS 配置**: 根据实际需求配置允许的源
4. **数据库**: 开发环境使用 H2，生产环境建议使用 MySQL 或 PostgreSQL

## 许可证

MIT License

