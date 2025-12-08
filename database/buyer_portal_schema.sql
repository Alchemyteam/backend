-- ============================================
-- Buyer Portal 数据库设计
-- 数据库: ecoschema
-- 根据 Buyer Portal API 接口规范设计
-- ============================================

USE ecoschema;

-- ============================================
-- 1. sellers 表（卖家表）
-- ============================================
DROP TABLE IF EXISTS sellers;

CREATE TABLE sellers (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT '卖家ID (UUID)',
    name VARCHAR(255) NOT NULL COMMENT '卖家名称',
    verified TINYINT(1) DEFAULT 0 COMMENT '是否认证: 1-已认证, 0-未认证',
    rating DECIMAL(3,2) DEFAULT 0.00 COMMENT '卖家评分 (0-5)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_verified (verified) COMMENT '认证状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卖家表';

-- ============================================
-- 2. products 表（产品表）
-- ============================================
DROP TABLE IF EXISTS products;

CREATE TABLE products (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT '产品ID (UUID)',
    name VARCHAR(255) NOT NULL COMMENT '产品名称',
    description TEXT COMMENT '产品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    currency VARCHAR(10) DEFAULT 'USD' COMMENT '货币单位',
    image VARCHAR(500) DEFAULT NULL COMMENT '主图片URL',
    images JSON DEFAULT NULL COMMENT '图片列表 (JSON数组)',
    seller_id VARCHAR(255) NOT NULL COMMENT '卖家ID (外键)',
    category VARCHAR(100) DEFAULT NULL COMMENT '产品分类',
    stock INT DEFAULT 0 COMMENT '库存数量',
    rating DECIMAL(3,2) DEFAULT 0.00 COMMENT '产品评分 (0-5)',
    reviews_count INT DEFAULT 0 COMMENT '评价数量',
    pe_certified TINYINT(1) DEFAULT 0 COMMENT 'PE认证: 1-已认证, 0-未认证',
    certificate_number VARCHAR(100) DEFAULT NULL COMMENT '证书编号',
    certified_by VARCHAR(255) DEFAULT NULL COMMENT '认证机构',
    certified_date DATE DEFAULT NULL COMMENT '认证日期',
    specifications JSON DEFAULT NULL COMMENT '产品规格 (JSON对象)',
    tags JSON DEFAULT NULL COMMENT '标签 (JSON数组)',
    featured TINYINT(1) DEFAULT 0 COMMENT '是否特色产品: 1-是, 0-否',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    historical_low_price DECIMAL(10,2) DEFAULT NULL COMMENT '历史最低价',
    last_transaction_price DECIMAL(10,2) DEFAULT NULL COMMENT '最后交易价格',
    
    -- 外键约束
    CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) 
        REFERENCES sellers(id) ON DELETE CASCADE,
    
    -- 索引
    INDEX idx_seller_id (seller_id) COMMENT '卖家ID索引',
    INDEX idx_category (category) COMMENT '分类索引',
    INDEX idx_price (price) COMMENT '价格索引',
    INDEX idx_rating (rating) COMMENT '评分索引',
    INDEX idx_featured (featured) COMMENT '特色产品索引',
    INDEX idx_created_at (created_at) COMMENT '创建时间索引',
    FULLTEXT INDEX ft_name_description (name, description) COMMENT '全文搜索索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品表';

-- ============================================
-- 3. cart_items 表（购物车项表）
-- ============================================
DROP TABLE IF EXISTS cart_items;

CREATE TABLE cart_items (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT '购物车项ID (UUID)',
    user_id VARCHAR(255) NOT NULL COMMENT '用户ID (外键关联 users.id)',
    product_id VARCHAR(255) NOT NULL COMMENT '产品ID (外键关联 products.id)',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    price DECIMAL(10,2) NOT NULL COMMENT '单价（添加时的价格）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 外键约束
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE,
    
    -- 唯一约束：同一用户同一产品只能有一条记录
    UNIQUE KEY uk_user_product (user_id, product_id),
    
    -- 索引
    INDEX idx_user_id (user_id) COMMENT '用户ID索引',
    INDEX idx_product_id (product_id) COMMENT '产品ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车项表';

-- ============================================
-- 4. orders 表（订单表）
-- ============================================
DROP TABLE IF EXISTS orders;

CREATE TABLE orders (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT '订单ID (UUID)',
    order_number VARCHAR(50) NOT NULL UNIQUE COMMENT '订单编号',
    user_id VARCHAR(255) NOT NULL COMMENT '买家ID (外键关联 users.id)',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '订单状态: pending, processing, shipped, delivered, cancelled',
    subtotal DECIMAL(10,2) NOT NULL COMMENT '小计',
    shipping DECIMAL(10,2) DEFAULT 0.00 COMMENT '运费',
    tax DECIMAL(10,2) DEFAULT 0.00 COMMENT '税费',
    total DECIMAL(10,2) NOT NULL COMMENT '总金额',
    currency VARCHAR(10) DEFAULT 'USD' COMMENT '货币单位',
    shipping_street VARCHAR(255) NOT NULL COMMENT '收货地址-街道',
    shipping_city VARCHAR(100) NOT NULL COMMENT '收货地址-城市',
    shipping_postal_code VARCHAR(20) NOT NULL COMMENT '收货地址-邮编',
    shipping_country VARCHAR(100) NOT NULL COMMENT '收货地址-国家',
    payment_method VARCHAR(50) DEFAULT NULL COMMENT '支付方式',
    tracking_number VARCHAR(100) DEFAULT NULL COMMENT '物流单号',
    estimated_delivery DATETIME DEFAULT NULL COMMENT '预计送达时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 外键约束
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- 索引
    INDEX idx_user_id (user_id) COMMENT '用户ID索引',
    INDEX idx_order_number (order_number) COMMENT '订单编号索引',
    INDEX idx_status (status) COMMENT '订单状态索引',
    INDEX idx_created_at (created_at) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ============================================
-- 5. order_items 表（订单项表）
-- ============================================
DROP TABLE IF EXISTS order_items;

CREATE TABLE order_items (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT '订单项ID (UUID)',
    order_id VARCHAR(255) NOT NULL COMMENT '订单ID (外键关联 orders.id)',
    product_id VARCHAR(255) NOT NULL COMMENT '产品ID (外键关联 products.id)',
    product_name VARCHAR(255) NOT NULL COMMENT '产品名称（快照）',
    quantity INT NOT NULL COMMENT '数量',
    price DECIMAL(10,2) NOT NULL COMMENT '单价（下单时的价格）',
    subtotal DECIMAL(10,2) NOT NULL COMMENT '小计',
    image VARCHAR(500) DEFAULT NULL COMMENT '产品图片（快照）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 外键约束
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) 
        REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    
    -- 索引
    INDEX idx_order_id (order_id) COMMENT '订单ID索引',
    INDEX idx_product_id (product_id) COMMENT '产品ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单项表';

-- ============================================
-- 6. wishlist_items 表（愿望清单项表）
-- ============================================
DROP TABLE IF EXISTS wishlist_items;

CREATE TABLE wishlist_items (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT '愿望清单项ID (UUID)',
    user_id VARCHAR(255) NOT NULL COMMENT '用户ID (外键关联 users.id)',
    product_id VARCHAR(255) NOT NULL COMMENT '产品ID (外键关联 products.id)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    
    -- 外键约束
    CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE,
    
    -- 唯一约束：同一用户同一产品只能有一条记录
    UNIQUE KEY uk_user_product (user_id, product_id),
    
    -- 索引
    INDEX idx_user_id (user_id) COMMENT '用户ID索引',
    INDEX idx_product_id (product_id) COMMENT '产品ID索引',
    INDEX idx_created_at (created_at) COMMENT '添加时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='愿望清单项表';

-- ============================================
-- 7. 插入测试数据（可选）
-- ============================================
-- 插入测试卖家
INSERT INTO sellers (id, name, verified, rating) VALUES
('seller_001', 'ABC Construction Supplies', 1, 4.8),
('seller_002', 'XYZ Building Materials', 1, 4.5),
('seller_003', 'Premium Steel Works', 1, 4.9);

-- 插入测试产品
INSERT INTO products (id, name, description, price, currency, image, seller_id, category, stock, rating, reviews_count, pe_certified, certificate_number, certified_by, featured) VALUES
('prod_001', 'Steel Formwork System', 'High-quality steel formwork system for construction', 299.99, 'USD', 'https://example.com/image1.jpg', 'seller_001', 'steel', 50, 4.5, 23, 1, 'CERT-001', 'PE-123', 1),
('prod_002', 'Concrete Mixer', 'Professional grade concrete mixer', 599.99, 'USD', 'https://example.com/image2.jpg', 'seller_002', 'equipment', 30, 4.7, 15, 1, 'CERT-002', 'PE-123', 1),
('prod_003', 'Rebar Steel Bars', 'High-strength rebar steel bars', 89.99, 'USD', 'https://example.com/image3.jpg', 'seller_003', 'steel', 100, 4.6, 31, 1, 'CERT-003', 'PE-456', 0);

-- ============================================
-- 8. 验证表结构
-- ============================================
-- SHOW TABLES;
-- DESCRIBE products;
-- DESCRIBE orders;

INSERT INTO sellers (id, name, verified, rating, created_at)
VALUES
    ('seller_004', 'Global Build Materials Co.', 1, 4.6, NOW()),
    ('seller_005', 'ProEngineering Supplies', 1, 4.7, NOW()),
    ('seller_006', 'United Steel Group', 1, 4.4, NOW()),
    ('seller_007', 'HeavyWorks Machinery', 1, 4.8, NOW()),
    ('seller_008', 'Prime Cement & Tools', 0, 4.2, NOW());

INSERT INTO products (
    id, name, description, price, currency, image, seller_id, category, stock,
    rating, reviews_count, pe_certified, certificate_number, certified_by,
    featured, specifications, tags, created_at
)
VALUES
    ('prod_004', 'Heavy Duty Crane', 'Industrial crane suitable for large construction sites', 12999.00, 'USD',
     'https://example.com/crane.jpg', 'seller_007', 'equipment', 12, 4.8, 48, 1, 'CERT-004', 'PE-789', 1,
     JSON_OBJECT('power', '450kW', 'capacity', '20T', 'height', '40m'),
     JSON_ARRAY('heavy', 'crane', 'industrial'),
     NOW()),

    ('prod_005', 'Premium Cement (50kg)', 'High-strength cement for structural engineering', 19.99, 'USD',
     'https://example.com/cement.jpg', 'seller_008', 'cement', 500, 4.3, 112, 1, 'CERT-005', 'PE-321', 0,
     JSON_OBJECT('weight', '50kg', 'grade', '42.5R'),
     JSON_ARRAY('cement', 'construction'),
     NOW()),

    ('prod_006', 'Scaffold System Set', 'Complete aluminum scaffold system', 899.00, 'USD',
     'https://example.com/scaffold.jpg', 'seller_004', 'scaffold', 80, 4.6, 36, 1, 'CERT-006', 'PE-654', 1,
     JSON_OBJECT('material', 'aluminum', 'height', '6m'),
     JSON_ARRAY('scaffold', 'materials'),
     NOW()),

    ('prod_007', 'Safety Helmet (Premium)', 'Protective helmet with PE certification', 29.99, 'USD',
     'https://example.com/helmet.jpg', 'seller_005', 'safety', 300, 4.9, 278, 1, 'CERT-007', 'PE-888', 0,
     JSON_OBJECT('material', 'polycarbonate', 'weight', '420g'),
     JSON_ARRAY('helmet', 'safety', 'ppe'),
     NOW()),

    ('prod_008', 'Welding Machine XT-500', 'High-performance welding machine for steelworks', 499.00, 'USD',
     'https://example.com/welding.jpg', 'seller_006', 'equipment', 60, 4.7, 89, 1, 'CERT-008', 'PE-555', 1,
     JSON_OBJECT('voltage', '220V', 'power', '5000W'),
     JSON_ARRAY('welding', 'steel'),
     NOW());

INSERT INTO cart_items (id, user_id, product_id, quantity, price, created_at)
VALUES
    ('cart_001', 'user_001', 'prod_004', 1, 12999.00, NOW()),
    ('cart_002', 'user_001', 'prod_007', 2, 29.99, NOW()),
    ('cart_003', 'user_002', 'prod_006', 1, 899.00, NOW());
INSERT INTO orders (
    id, order_number, user_id, status, subtotal, shipping, tax, total, currency,
    shipping_street, shipping_city, shipping_postal_code, shipping_country,
    payment_method, tracking_number, estimated_delivery, created_at
)
VALUES
    ('order_001', 'ORD-2025001', 'user_001', 'processing', 12999.00, 120.00, 0.00, 13119.00, 'USD',
     '123 Main St', 'New York', '10001', 'USA', 'Credit Card',
     'TRK-998877', DATE_ADD(NOW(), INTERVAL 7 DAY), NOW()),

    ('order_002', 'ORD-2025002', 'user_002', 'shipped', 928.99, 50.00, 0.00, 978.99, 'USD',
     '45 Industrial Road', 'Los Angeles', '90001', 'USA', 'PayPal',
     'TRK-123456', DATE_ADD(NOW(), INTERVAL 3 DAY), NOW()),

    ('order_003', 'ORD-2025003', 'user_001', 'delivered', 59.98, 10.00, 0.00, 69.98, 'USD',
     '123 Main St', 'New York', '10001', 'USA', 'Credit Card',
     'TRK-445566', DATE_ADD(NOW(), INTERVAL -2 DAY), NOW());
INSERT INTO order_items (
    id, order_id, product_id, product_name, quantity, price, subtotal, image, created_at
)
VALUES
    ('item_001', 'order_001', 'prod_004', 'Heavy Duty Crane', 1, 12999.00, 12999.00, 'https://example.com/crane.jpg', NOW()),

    ('item_002', 'order_002', 'prod_006', 'Scaffold System Set', 1, 899.00, 899.00, 'https://example.com/scaffold.jpg', NOW()),
    ('item_003', 'order_002', 'prod_007', 'Safety Helmet (Premium)', 1, 29.99, 29.99, 'https://example.com/helmet.jpg', NOW()),

    ('item_004', 'order_003', 'prod_007', 'Safety Helmet (Premium)', 2, 29.99, 59.98, 'https://example.com/helmet.jpg', NOW());
INSERT INTO wishlist_items (id, user_id, product_id, created_at)
VALUES
    ('wish_001', 'user_001', 'prod_005', NOW()),
    ('wish_002', 'user_001', 'prod_008', NOW()),
    ('wish_003', 'user_002', 'prod_004', NOW()),
    ('wish_004', 'user_003', 'prod_006', NOW());


ALTER TABLE products
    ADD COLUMN product_hierarchy1 VARCHAR(100) NULL,
    ADD COLUMN product_hierarchy2 VARCHAR(100) NULL;
CREATE TABLE product_hierarchy_mapping (
                                           product_hierarchy1 VARCHAR(100) NOT NULL,
                                           product_hierarchy2 VARCHAR(100) NOT NULL,
                                           PRIMARY KEY (product_hierarchy2)  -- 假设每个二级类目唯一对应一级类目
);

SELECT p.id, p.category, m.product_hierarchy1, m.product_hierarchy2
FROM products p
         JOIN product_hierarchy_mapping m
              ON p.category = m.product_hierarchy2
WHERE (p.product_hierarchy1 IS NULL OR p.product_hierarchy2 IS NULL)
  AND p.category IS NOT NULL;

CREATE TABLE sales_data (
                            TXDate DATE,
                            TXNo VARCHAR(50),
                            TXQty INT,
                            TXP1 DECIMAL(10,2),
                            BuyerCode VARCHAR(20),
                            BuyerName VARCHAR(255),
                            ItemCode VARCHAR(50),
                            ItemName TEXT,
                            `Product Hierarchy 3` VARCHAR(100),
                            `Function` VARCHAR(100),          -- ✅ 加了反引号
                            ItemType VARCHAR(100),
                            Model VARCHAR(100),
                            Performance VARCHAR(100),
                            `Performance.1` VARCHAR(100),
                            Material VARCHAR(100),
                            UOM VARCHAR(50),
                            `Brand Code` VARCHAR(50),
                            `Unit Cost` DECIMAL(10,4),
                            Sector VARCHAR(100),           -- 原列名有空格，保留
                            SubSector VARCHAR(100),
                            Value DECIMAL(10,4),
                            Rationale VARCHAR(255),
                            www VARCHAR(255),
                            Source VARCHAR(50)
);

SELECT ItemName
FROM sales_data
WHERE ItemName LIKE '%19P SPARE ELEMENT%';

DROP TABLE IF EXISTS `sales_data`;

CREATE TABLE `sales_data` (
                              `TXDate` VARCHAR(255),
                              `TXNo` VARCHAR(255),
                              `TXQty` VARCHAR(255),
                              `TXP1` VARCHAR(255),
                              `BuyerCode` VARCHAR(255),
                              `BuyerName` VARCHAR(255),
                              `ItemCode` VARCHAR(255),
                              `ItemName` VARCHAR(255),
                              `Product Hierarchy 3` VARCHAR(255),
                              `Function` VARCHAR(255),
                              `ItemType` VARCHAR(255),
                              `Model` VARCHAR(255),
                              `Performance` VARCHAR(255),
                              `Performance.1` VARCHAR(255),
                              `Material` VARCHAR(255),
                              `UOM` VARCHAR(255),
                              `Brand Code` VARCHAR(255),
                              `Unit Cost` VARCHAR(255),
                              `Sector` VARCHAR(255),
                              `SubSector` VARCHAR(255),
                              `Value` VARCHAR(255),
                              `Rationale` VARCHAR(255),
                              `www` VARCHAR(255),
                              `Source` VARCHAR(255)
);

SELECT COUNT(*)
FROM sales_data
WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER('Site Safety Equipment');
SELECT DISTINCT `Product Hierarchy 3`
FROM ecoschema.sales_data
WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER('Site Safety Equipment');
ALTER TABLE `sales_data`
    ADD COLUMN id INT NOT NULL AUTO_INCREMENT PRIMARY KEY;

USE ecoschema;
DESCRIBE sales_data;
SHOW COLUMNS FROM sales_data IN ecoschema;