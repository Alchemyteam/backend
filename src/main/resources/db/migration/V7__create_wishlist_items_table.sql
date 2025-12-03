-- ============================================
-- V7: Create wishlist items table
-- ============================================

CREATE TABLE IF NOT EXISTS wishlist_items (
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

