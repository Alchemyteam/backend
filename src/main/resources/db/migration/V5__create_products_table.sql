-- ============================================
-- V5: Create products table
-- ============================================

CREATE TABLE IF NOT EXISTS ecoschema.products (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT 'Product ID (UUID)',
    name VARCHAR(255) NOT NULL COMMENT 'Product name',
    description TEXT DEFAULT NULL COMMENT 'Product description',
    price DECIMAL(10,2) NOT NULL COMMENT 'Product price',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT 'Currency code',
    image VARCHAR(500) DEFAULT NULL COMMENT 'Product image URL',
    images JSON DEFAULT NULL COMMENT 'Product images array (JSON)',
    seller_id VARCHAR(36) NOT NULL COMMENT 'Seller ID (foreign key)',
    category VARCHAR(100) DEFAULT NULL COMMENT 'Product category',
    stock INT NOT NULL DEFAULT 0 COMMENT 'Product stock quantity',
    rating DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT 'Product rating',
    reviews_count INT NOT NULL DEFAULT 0 COMMENT 'Number of reviews',
    pe_certified BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'PE certification status',
    certificate_number VARCHAR(100) DEFAULT NULL COMMENT 'Certificate number',
    certified_by VARCHAR(255) DEFAULT NULL COMMENT 'Certified by organization',
    certified_date DATE DEFAULT NULL COMMENT 'Certification date',
    specifications JSON DEFAULT NULL COMMENT 'Product specifications (JSON)',
    tags JSON DEFAULT NULL COMMENT 'Product tags array (JSON)',
    featured BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Featured product flag',
    historical_low_price DECIMAL(10,2) DEFAULT NULL COMMENT 'Historical lowest price',
    last_transaction_price DECIMAL(10,2) DEFAULT NULL COMMENT 'Last transaction price',
    created_at DATETIME NOT NULL COMMENT 'Creation timestamp',
    updated_at DATETIME DEFAULT NULL COMMENT 'Last update timestamp',
    
    FOREIGN KEY (seller_id) REFERENCES ecoschema.sellers(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    
    INDEX idx_seller_id (seller_id) COMMENT 'Seller ID index',
    INDEX idx_category (category) COMMENT 'Category index',
    INDEX idx_stock (stock) COMMENT 'Stock index',
    INDEX idx_rating (rating) COMMENT 'Rating index',
    INDEX idx_featured (featured) COMMENT 'Featured products index',
    INDEX idx_name (name) COMMENT 'Product name index',
    INDEX idx_price (price) COMMENT 'Price index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Products table';

