-- ============================================
-- V3: Create products table
-- ============================================

CREATE TABLE IF NOT EXISTS products (
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

