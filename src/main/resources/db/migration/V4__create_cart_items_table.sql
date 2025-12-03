-- ============================================
-- V4: Create cart items table
-- ============================================

CREATE TABLE IF NOT EXISTS cart_items (
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

