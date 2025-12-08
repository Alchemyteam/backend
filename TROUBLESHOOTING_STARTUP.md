# 应用启动故障排除指南

## 问题：运行命令后应用立即退出

如果运行 `mvn spring-boot:run` 或 `java -jar` 后应用立即退出，请按照以下步骤排查：

---

## 1. 检查错误日志

### 查看完整错误信息

运行命令时，**不要关闭终端窗口**，查看完整的错误输出。常见错误包括：

#### 错误 1: 数据库连接失败

```
Caused by: java.sql.SQLException: Access denied for user 'root'@'localhost'
或
Communications link failure
```

**解决方法**：
1. 检查 MySQL 服务是否启动
   ```bash
   # Windows
   net start MySQL
   
   # 或检查服务状态
   services.msc
   ```

2. 检查数据库连接信息（`application.yml`）：
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/ecoschema
       username: root  # 确认用户名正确
       password: allinton  # 确认密码正确
   ```

3. 确认数据库 `ecoschema` 已创建：
   ```sql
   CREATE DATABASE IF NOT EXISTS ecoschema 
       DEFAULT CHARACTER SET utf8mb4 
       DEFAULT COLLATE utf8mb4_unicode_ci;
   ```

#### 错误 2: Flyway 迁移失败

```
FlywayException: Validate failed
或
Migration checksum mismatch
```

**解决方法**：
1. 检查迁移脚本是否有语法错误
2. 如果迁移已部分执行，可能需要手动修复数据库
3. 临时禁用 Flyway 验证（仅用于开发环境）：
   ```yaml
   spring:
     flyway:
       validate-on-migrate: false  # 临时禁用验证
   ```

#### 错误 3: 端口被占用

```
Port 8000 is already in use
```

**解决方法**：
1. 查找占用端口的进程：
   ```bash
   # Windows
   netstat -ano | findstr :8000
   
   # 查看进程详情
   tasklist | findstr <PID>
   ```

2. 关闭占用端口的程序，或修改端口：
   ```yaml
   server:
     port: 8001  # 改为其他端口
   ```

#### 错误 4: 编译错误

```
[ERROR] COMPILATION ERROR
```

**解决方法**：
1. 清理并重新编译：
   ```bash
   mvn clean compile
   ```

2. 检查 Java 版本（需要 JDK 17+）：
   ```bash
   java -version
   ```

---

## 2. 在 Windows 命令行中保持窗口打开

### 问题：命令执行后窗口立即关闭

如果你双击运行 `.bat` 文件或使用某些 IDE 运行，窗口可能会立即关闭。

**解决方法**：

#### 方法 1: 在命令行中运行

1. 打开 **命令提示符**（cmd）或 **PowerShell**
2. 导航到项目目录：
   ```cmd
   cd C:\Users\yiwen.li\ecosystem_backend
   ```
3. 运行命令：
   ```cmd
   mvn spring-boot:run
   ```
4. **不要关闭窗口**，查看完整输出

#### 方法 2: 创建批处理文件（保持窗口打开）

创建 `start.bat` 文件：

```batch
@echo off
cd /d "%~dp0"
echo Starting Spring Boot Application...
mvn spring-boot:run
pause
```

#### 方法 3: 使用 PowerShell

```powershell
cd C:\Users\yiwen.li\ecosystem_backend
mvn spring-boot:run
# 窗口会保持打开，显示输出
```

---

## 3. 逐步诊断步骤

### 步骤 1: 检查 Java 环境

```bash
java -version
# 应该显示 Java 17 或更高版本

mvn -version
# 应该显示 Maven 版本信息
```

### 步骤 2: 检查数据库连接

```bash
# 测试 MySQL 连接
mysql -u root -p
# 输入密码后，尝试连接
```

### 步骤 3: 清理并重新编译

```bash
mvn clean
mvn compile
```

### 步骤 4: 运行并查看完整日志

```bash
mvn spring-boot:run
# 等待完整输出，不要提前关闭
```

### 步骤 5: 检查应用日志

查看控制台输出的最后几行，寻找：
- `Started EcosystemBackendApplication` - 启动成功
- `Tomcat started on port(s): 8000` - 服务器启动成功
- 任何 `ERROR` 或 `Exception` 信息

---

## 4. 常见启动失败原因

### 原因 1: 数据库表结构不匹配

**症状**：Flyway 迁移失败

**解决**：
1. 检查 `sales_data` 表是否有 `id` 字段
2. 如果没有，手动添加：
   ```sql
   ALTER TABLE ecoschema.sales_data 
   ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
   ADD PRIMARY KEY (id);
   ```

### 原因 2: 实体类与数据库不匹配

**症状**：JPA 启动失败

**解决**：
1. 确认 `SalesData` 实体类的 `id` 字段配置正确
2. 确认数据库表结构与实体类匹配

### 原因 3: 依赖缺失

**症状**：`ClassNotFoundException`

**解决**：
```bash
mvn clean install
mvn dependency:resolve
```

### 原因 4: 配置文件错误

**症状**：`YAML parse error`

**解决**：
1. 检查 `application.yml` 语法
2. 确认缩进正确（使用空格，不是 Tab）
3. 确认所有引号匹配

---

## 5. 调试模式启动

### 启用详细日志

在 `application.yml` 中添加：

```yaml
logging:
  level:
    root: INFO
    com.ecosystem: DEBUG
    org.springframework: DEBUG
    org.flywaydb: DEBUG
```

### 使用调试模式运行

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

---

## 6. 验证启动成功

### 启动成功的标志

看到以下信息表示启动成功：

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

... (各种初始化日志)

Tomcat started on port(s): 8000 (http) with context path '/api'
Started EcosystemBackendApplication in X.XXX seconds
```

### 测试 API 是否可用

启动成功后，测试接口：

```bash
# 测试健康检查（如果有）
curl http://localhost:8000/api/auth/verify

# 或使用浏览器访问
# http://localhost:8000/api/auth/verify
```

---

## 7. 快速修复脚本

### Windows 批处理文件：`fix-and-run.bat`

```batch
@echo off
echo ====================================
echo Ecosystem Backend - Fix and Run
echo ====================================
echo.

echo [1/4] Cleaning project...
call mvn clean
if %errorlevel% neq 0 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)

echo.
echo [2/4] Compiling project...
call mvn compile
if %errorlevel% neq 0 (
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

echo.
echo [3/4] Resolving dependencies...
call mvn dependency:resolve
if %errorlevel% neq 0 (
    echo WARNING: Dependency resolution had issues
)

echo.
echo [4/4] Starting application...
echo.
echo ====================================
echo Application is starting...
echo Press Ctrl+C to stop
echo ====================================
echo.

call mvn spring-boot:run

pause
```

---

## 8. 如果问题仍然存在

### 收集信息

1. **完整的错误日志**（从启动到退出的所有输出）
2. **Java 版本**：`java -version`
3. **Maven 版本**：`mvn -version`
4. **操作系统**：Windows 版本
5. **数据库状态**：MySQL 是否运行
6. **配置文件**：`application.yml` 内容（隐藏密码）

### 检查清单

- [ ] Java 17+ 已安装
- [ ] Maven 已安装并配置
- [ ] MySQL 服务正在运行
- [ ] 数据库 `ecoschema` 已创建
- [ ] 数据库用户名和密码正确
- [ ] 端口 8000 未被占用
- [ ] 项目已成功编译（`mvn compile`）
- [ ] 没有编译错误

---

## 9. 替代启动方式

### 方式 1: 使用 IDE 启动

**IntelliJ IDEA**:
1. 右键 `EcosystemBackendApplication.java`
2. 选择 "Run 'EcosystemBackendApplication.main()'"
3. 查看 "Run" 窗口的输出

**Eclipse**:
1. 右键 `EcosystemBackendApplication.java`
2. 选择 "Run As" -> "Java Application"
3. 查看 "Console" 视图

### 方式 2: 打包后运行

```bash
# 打包
mvn clean package

# 运行 JAR
java -jar target/ecosystem-backend-1.0.0.jar
```

### 方式 3: 使用 Spring Boot Maven 插件

```bash
mvn spring-boot:run -X
# -X 参数显示详细调试信息
```

---

## 10. 联系支持

如果以上方法都无法解决问题，请提供：

1. 完整的错误日志
2. `application.yml` 配置（隐藏敏感信息）
3. 数据库表结构（`DESCRIBE sales_data;`）
4. 执行的命令和输出

---

## 快速参考

### 常用命令

```bash
# 清理项目
mvn clean

# 编译项目
mvn compile

# 运行测试
mvn test

# 打包项目
mvn package

# 运行应用
mvn spring-boot:run

# 查看依赖树
mvn dependency:tree

# 检查 Flyway 状态
mvn flyway:info
```

### 常用 SQL 检查

```sql
-- 检查数据库是否存在
SHOW DATABASES LIKE 'ecoschema';

-- 检查表是否存在
USE ecoschema;
SHOW TABLES;

-- 检查 sales_data 表结构
DESCRIBE sales_data;

-- 检查是否有 id 字段
SELECT COLUMN_NAME, DATA_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'ecoschema' 
  AND TABLE_NAME = 'sales_data' 
  AND COLUMN_NAME = 'id';

-- 检查 Flyway 迁移历史
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

---

希望这个指南能帮助你解决问题！如果仍有问题，请查看完整的错误日志。

