-- ============================================
-- Ecosystem Backend Complete Database Schema
-- Database: ecoschema
-- This script creates the complete database schema and all tables
-- Can be executed multiple times safely (uses IF NOT EXISTS / DROP IF EXISTS)
-- ============================================

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS ecoschema 
    DEFAULT CHARACTER SET utf8mb4 
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- Use the database
USE ecoschema;

-- ============================================
-- 1. Users Table
-- ============================================
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT 'User ID (UUID)',
    email VARCHAR(255) NOT NULL UNIQUE COMMENT 'Email address',
    password VARCHAR(255) NOT NULL COMMENT 'Encrypted password',
    name VARCHAR(255) DEFAULT NULL COMMENT 'User name',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    
    INDEX idx_email (email) COMMENT 'Email index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Users table';

-- ============================================
-- 2. Sellers Table
-- ============================================
DROP TABLE IF EXISTS sellers;

CREATE TABLE sellers (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT 'Seller ID (UUID)',
    name VARCHAR(255) NOT NULL COMMENT 'Seller name',
    verified TINYINT(1) DEFAULT 0 COMMENT 'Verification status: 1-verified, 0-not verified',
    rating DECIMAL(3,2) DEFAULT 0.00 COMMENT 'Seller rating (0-5)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    
    INDEX idx_verified (verified) COMMENT 'Verification status index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sellers table';

-- ============================================
-- 3. Products Table
-- ============================================
DROP TABLE IF EXISTS products;

CREATE TABLE products (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT 'Product ID (UUID)',
    name VARCHAR(255) NOT NULL COMMENT 'Product name',
    description TEXT COMMENT 'Product description',
    price DECIMAL(10,2) NOT NULL COMMENT 'Price',
    currency VARCHAR(10) DEFAULT 'USD' COMMENT 'Currency unit',
    image VARCHAR(500) DEFAULT NULL COMMENT 'Main image URL',
    images JSON DEFAULT NULL COMMENT 'Image list (JSON array)',
    seller_id VARCHAR(255) NOT NULL COMMENT 'Seller ID (foreign key)',
    category VARCHAR(100) DEFAULT NULL COMMENT 'Product category',
    stock INT DEFAULT 0 COMMENT 'Stock quantity',
    rating DECIMAL(3,2) DEFAULT 0.00 COMMENT 'Product rating (0-5)',
    reviews_count INT DEFAULT 0 COMMENT 'Review count',
    pe_certified TINYINT(1) DEFAULT 0 COMMENT 'PE certification: 1-certified, 0-not certified',
    certificate_number VARCHAR(100) DEFAULT NULL COMMENT 'Certificate number',
    certified_by VARCHAR(255) DEFAULT NULL COMMENT 'Certification authority',
    certified_date DATE DEFAULT NULL COMMENT 'Certification date',
    specifications JSON DEFAULT NULL COMMENT 'Product specifications (JSON object)',
    tags JSON DEFAULT NULL COMMENT 'Tags (JSON array)',
    featured TINYINT(1) DEFAULT 0 COMMENT 'Featured product: 1-yes, 0-no',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    historical_low_price DECIMAL(10,2) DEFAULT NULL COMMENT 'Historical lowest price',
    last_transaction_price DECIMAL(10,2) DEFAULT NULL COMMENT 'Last transaction price',
    product_hierarchy1 VARCHAR(100) NULL COMMENT 'Product hierarchy level 1',
    product_hierarchy2 VARCHAR(100) NULL COMMENT 'Product hierarchy level 2',
    
    -- Foreign key constraint
    CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) 
        REFERENCES sellers(id) ON DELETE CASCADE,
    
    -- Indexes
    INDEX idx_seller_id (seller_id) COMMENT 'Seller ID index',
    INDEX idx_category (category) COMMENT 'Category index',
    INDEX idx_price (price) COMMENT 'Price index',
    INDEX idx_rating (rating) COMMENT 'Rating index',
    INDEX idx_featured (featured) COMMENT 'Featured product index',
    INDEX idx_created_at (created_at) COMMENT 'Creation time index',
    FULLTEXT INDEX ft_name_description (name, description) COMMENT 'Full-text search index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Products table';

-- ============================================
-- 4. Cart Items Table
-- ============================================
DROP TABLE IF EXISTS cart_items;

CREATE TABLE cart_items (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT 'Cart item ID (UUID)',
    user_id VARCHAR(255) NOT NULL COMMENT 'User ID (foreign key references users.id)',
    product_id VARCHAR(255) NOT NULL COMMENT 'Product ID (foreign key references products.id)',
    quantity INT NOT NULL DEFAULT 1 COMMENT 'Quantity',
    price DECIMAL(10,2) NOT NULL COMMENT 'Unit price (price at time of adding)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    
    -- Foreign key constraints
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE,
    
    -- Unique constraint: one record per user per product
    UNIQUE KEY uk_user_product (user_id, product_id),
    
    -- Indexes
    INDEX idx_user_id (user_id) COMMENT 'User ID index',
    INDEX idx_product_id (product_id) COMMENT 'Product ID index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Cart items table';

-- ============================================
-- 5. Orders Table
-- ============================================
DROP TABLE IF EXISTS orders;

CREATE TABLE orders (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT 'Order ID (UUID)',
    order_number VARCHAR(50) NOT NULL UNIQUE COMMENT 'Order number',
    user_id VARCHAR(255) NOT NULL COMMENT 'Buyer ID (foreign key references users.id)',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'Order status: pending, processing, shipped, delivered, cancelled',
    subtotal DECIMAL(10,2) NOT NULL COMMENT 'Subtotal',
    shipping DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Shipping cost',
    tax DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Tax',
    total DECIMAL(10,2) NOT NULL COMMENT 'Total amount',
    currency VARCHAR(10) DEFAULT 'USD' COMMENT 'Currency unit',
    shipping_street VARCHAR(255) NOT NULL COMMENT 'Shipping address - street',
    shipping_city VARCHAR(100) NOT NULL COMMENT 'Shipping address - city',
    shipping_postal_code VARCHAR(20) NOT NULL COMMENT 'Shipping address - postal code',
    shipping_country VARCHAR(100) NOT NULL COMMENT 'Shipping address - country',
    payment_method VARCHAR(50) DEFAULT NULL COMMENT 'Payment method',
    tracking_number VARCHAR(100) DEFAULT NULL COMMENT 'Tracking number',
    estimated_delivery DATETIME DEFAULT NULL COMMENT 'Estimated delivery time',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    
    -- Foreign key constraint
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    
    -- Indexes
    INDEX idx_user_id (user_id) COMMENT 'User ID index',
    INDEX idx_order_number (order_number) COMMENT 'Order number index',
    INDEX idx_status (status) COMMENT 'Order status index',
    INDEX idx_created_at (created_at) COMMENT 'Creation time index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Orders table';

-- ============================================
-- 6. Order Items Table
-- ============================================
DROP TABLE IF EXISTS order_items;

CREATE TABLE order_items (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT 'Order item ID (UUID)',
    order_id VARCHAR(255) NOT NULL COMMENT 'Order ID (foreign key references orders.id)',
    product_id VARCHAR(255) NOT NULL COMMENT 'Product ID (foreign key references products.id)',
    product_name VARCHAR(255) NOT NULL COMMENT 'Product name (snapshot)',
    quantity INT NOT NULL COMMENT 'Quantity',
    price DECIMAL(10,2) NOT NULL COMMENT 'Unit price (price at time of order)',
    subtotal DECIMAL(10,2) NOT NULL COMMENT 'Subtotal',
    image VARCHAR(500) DEFAULT NULL COMMENT 'Product image (snapshot)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    
    -- Foreign key constraints
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) 
        REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    
    -- Indexes
    INDEX idx_order_id (order_id) COMMENT 'Order ID index',
    INDEX idx_product_id (product_id) COMMENT 'Product ID index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Order items table';

-- ============================================
-- 7. Wishlist Items Table
-- ============================================
DROP TABLE IF EXISTS wishlist_items;

CREATE TABLE wishlist_items (
    id VARCHAR(255) NOT NULL PRIMARY KEY COMMENT 'Wishlist item ID (UUID)',
    user_id VARCHAR(255) NOT NULL COMMENT 'User ID (foreign key references users.id)',
    product_id VARCHAR(255) NOT NULL COMMENT 'Product ID (foreign key references products.id)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Add time',
    
    -- Foreign key constraints
    CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE,
    
    -- Unique constraint: one record per user per product
    UNIQUE KEY uk_user_product (user_id, product_id),
    
    -- Indexes
    INDEX idx_user_id (user_id) COMMENT 'User ID index',
    INDEX idx_product_id (product_id) COMMENT 'Product ID index',
    INDEX idx_created_at (created_at) COMMENT 'Add time index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Wishlist items table';

-- ============================================
-- 8. Product Hierarchy Mapping Table
-- ============================================
DROP TABLE IF EXISTS product_hierarchy_mapping;

CREATE TABLE product_hierarchy_mapping (
    product_hierarchy1 VARCHAR(100) NOT NULL COMMENT 'Product hierarchy level 1',
    product_hierarchy2 VARCHAR(100) NOT NULL COMMENT 'Product hierarchy level 2',
    PRIMARY KEY (product_hierarchy2) COMMENT 'Primary key on level 2 (assuming each level 2 uniquely corresponds to level 1)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Product hierarchy mapping table';

-- ============================================
-- 9. Sales Data Table
-- ============================================
DROP TABLE IF EXISTS sales_data;

CREATE TABLE sales_data (
    `TXNo` VARCHAR(255) NOT NULL PRIMARY KEY COMMENT 'Transaction number',
    `TXDate` VARCHAR(255) DEFAULT NULL COMMENT 'Transaction date',
    `TXQty` VARCHAR(255) DEFAULT NULL COMMENT 'Transaction quantity',
    `TXP1` VARCHAR(255) DEFAULT NULL COMMENT 'Transaction price 1',
    `BuyerCode` VARCHAR(255) DEFAULT NULL COMMENT 'Buyer code',
    `BuyerName` VARCHAR(255) DEFAULT NULL COMMENT 'Buyer name',
    `ItemCode` VARCHAR(255) DEFAULT NULL COMMENT 'Item code',
    `ItemName` VARCHAR(255) DEFAULT NULL COMMENT 'Item name',
    `Product Hierarchy 3` VARCHAR(255) DEFAULT NULL COMMENT 'Product hierarchy level 3',
    `Function` VARCHAR(255) DEFAULT NULL COMMENT 'Function',
    `ItemType` VARCHAR(255) DEFAULT NULL COMMENT 'Item type',
    `Model` VARCHAR(255) DEFAULT NULL COMMENT 'Model',
    `Performance` VARCHAR(255) DEFAULT NULL COMMENT 'Performance',
    `Performance.1` VARCHAR(255) DEFAULT NULL COMMENT 'Performance 1',
    `Material` VARCHAR(255) DEFAULT NULL COMMENT 'Material',
    `UOM` VARCHAR(255) DEFAULT NULL COMMENT 'Unit of measure',
    `Brand Code` VARCHAR(255) DEFAULT NULL COMMENT 'Brand code',
    `Unit Cost` VARCHAR(255) DEFAULT NULL COMMENT 'Unit cost',
    `Sector` VARCHAR(255) DEFAULT NULL COMMENT 'Sector',
    `SubSector` VARCHAR(255) DEFAULT NULL COMMENT 'Sub sector',
    `Value` VARCHAR(255) DEFAULT NULL COMMENT 'Value',
    `Rationale` VARCHAR(255) DEFAULT NULL COMMENT 'Rationale',
    `www` VARCHAR(255) DEFAULT NULL COMMENT 'Website URL',
    `Source` VARCHAR(255) DEFAULT NULL COMMENT 'Source',
    
    -- Indexes for common filter operations
    INDEX idx_tx_date (`TXDate`) COMMENT 'Transaction date index',
    INDEX idx_tx_no (`TXNo`) COMMENT 'Transaction number index',
    INDEX idx_buyer_code (`BuyerCode`) COMMENT 'Buyer code index',
    INDEX idx_buyer_name (`BuyerName`) COMMENT 'Buyer name index',
    INDEX idx_item_code (`ItemCode`) COMMENT 'Item code index',
    INDEX idx_item_name (`ItemName`) COMMENT 'Item name index',
    INDEX idx_product_hierarchy3 (`Product Hierarchy 3`) COMMENT 'Product hierarchy 3 index',
    INDEX idx_sector (`Sector`) COMMENT 'Sector index',
    INDEX idx_brand_code (`Brand Code`) COMMENT 'Brand code index',
    INDEX idx_function (`Function`) COMMENT 'Function index',
    INDEX idx_item_type (`ItemType`) COMMENT 'Item type index',
    INDEX idx_model (`Model`) COMMENT 'Model index',
    INDEX idx_material (`Material`) COMMENT 'Material index',
    INDEX idx_subsector (`SubSector`) COMMENT 'Sub sector index',
    INDEX idx_source (`Source`) COMMENT 'Source index',
    
    -- Composite indexes for common query patterns
    INDEX idx_category_sector (`Product Hierarchy 3`, `Sector`) COMMENT 'Category and sector composite index',
    INDEX idx_buyer_item (`BuyerCode`, `ItemCode`) COMMENT 'Buyer and item composite index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sales data table';

-- ============================================
-- Script Execution Complete
-- ============================================
-- To verify the schema, you can run:
-- SHOW TABLES;
-- DESCRIBE sales_data;
-- DESCRIBE products;
-- DESCRIBE users;

