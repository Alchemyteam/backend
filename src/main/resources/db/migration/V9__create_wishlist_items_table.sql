-- ============================================
-- V9: Create wishlist_items table
-- ============================================

CREATE TABLE IF NOT EXISTS ecoschema.wishlist_items (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT 'Wishlist item ID (UUID)',
    user_id VARCHAR(36) NOT NULL COMMENT 'User ID (foreign key)',
    product_id VARCHAR(36) NOT NULL COMMENT 'Product ID (foreign key to products)',
    created_at DATETIME NOT NULL COMMENT 'Creation timestamp',
    
    FOREIGN KEY (user_id) REFERENCES ecoschema.users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (product_id) REFERENCES ecoschema.products(id) ON DELETE CASCADE ON UPDATE CASCADE,
    
    UNIQUE KEY uk_user_product (user_id, product_id) COMMENT 'One wishlist item per user-product combination',
    INDEX idx_user_id (user_id) COMMENT 'User ID index',
    INDEX idx_product_id (product_id) COMMENT 'Product ID index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Wishlist items table';

