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

