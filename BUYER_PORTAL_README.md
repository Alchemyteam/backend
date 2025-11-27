# Buyer Portal 后端实现

根据 Buyer Portal API 接口规范文档实现的后端系统。

## 已实现的功能

### ✅ 数据库设计
- **sellers** 表 - 卖家信息
- **products** 表 - 产品信息（包含认证、规格等）
- **cart_items** 表 - 购物车项
- **orders** 表 - 订单
- **order_items** 表 - 订单项
- **wishlist_items** 表 - 愿望清单项

### ✅ 实体类 (Entity)
- `Seller` - 卖家实体
- `Product` - 产品实体
- `CartItem` - 购物车项实体
- `Order` - 订单实体
- `OrderItem` - 订单项实体
- `WishlistItem` - 愿望清单项实体

### ✅ Repository 层
- `ProductRepository` - 产品数据访问（支持搜索、排序）
- `SellerRepository` - 卖家数据访问
- `CartItemRepository` - 购物车数据访问
- `OrderRepository` - 订单数据访问
- `OrderItemRepository` - 订单项数据访问
- `WishlistItemRepository` - 愿望清单数据访问

### ✅ Service 层
- `BuyerProductService` - 产品相关业务逻辑
- `BuyerCartService` - 购物车业务逻辑
- `BuyerOrderService` - 订单业务逻辑
- `BuyerWishlistService` - 愿望清单业务逻辑
- `BuyerStatisticsService` - 统计数据业务逻辑

### ✅ Controller 层
- `BuyerController` - 所有 Buyer Portal API 端点

## API 端点列表

### 产品相关
- `GET /api/buyer/products/search` - 搜索产品
- `GET /api/buyer/products/featured` - 获取特色产品
- `GET /api/buyer/products` - 获取所有产品（浏览目录）
- `GET /api/buyer/products/{productId}` - 获取产品详情

### 购物车相关
- `POST /api/buyer/cart/add` - 添加到购物车
- `GET /api/buyer/cart` - 获取购物车
- `DELETE /api/buyer/cart/{cartItemId}` - 从购物车移除

### 订单相关
- `POST /api/buyer/orders` - 创建订单
- `GET /api/buyer/orders` - 获取订单列表
- `GET /api/buyer/orders/{orderId}` - 获取订单详情

### 愿望清单相关
- `POST /api/buyer/wishlist/add` - 添加到愿望清单
- `GET /api/buyer/wishlist` - 获取愿望清单
- `DELETE /api/buyer/wishlist/{wishlistItemId}` - 从愿望清单移除

### 统计相关
- `GET /api/buyer/statistics` - 获取买家统计数据

## 数据库初始化

执行以下 SQL 脚本创建数据库表：

```bash
mysql -u root -p ecoschema < database/buyer_portal_schema.sql
```

或者在 MySQL 客户端中执行 `database/buyer_portal_schema.sql` 文件。

## 配置说明

### 数据库配置
当前配置指向 `ecoschema` 数据库，如需修改请编辑 `application.yml`。

### 安全配置
- 所有 `/buyer` 路径的接口都需要 JWT 认证
- 认证方式：`Authorization: Bearer {token}`

## 注意事项

1. **JSON 字段处理**: Product 实体中的 `images`、`specifications`、`tags` 字段使用 JSON 字符串存储，在 Service 层进行序列化/反序列化。

2. **库存管理**: 创建订单时会自动减少产品库存。

3. **购物车清空**: 创建订单成功后会自动清空购物车。

4. **订单编号**: 使用时间戳生成唯一订单编号。

5. **运费和税费**: 当前使用简单的计算逻辑，可根据需求修改 `BuyerOrderService` 中的计算方法。

## 测试建议

1. 先执行数据库初始化脚本
2. 启动后端服务
3. 使用 Postman 或类似工具测试各个接口
4. 确保所有接口都返回正确的状态码和数据结构

## 后续优化建议

1. 添加产品图片上传功能
2. 实现更复杂的搜索功能（全文搜索）
3. 添加订单状态变更通知
4. 实现产品评价功能
5. 添加库存预警功能

