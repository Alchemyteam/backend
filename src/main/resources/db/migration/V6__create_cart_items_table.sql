-- ============================================
-- V6: Create cart_items table
-- ============================================

CREATE TABLE IF NOT EXISTS ecoschema.cart_items (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT 'Cart item ID (UUID)',
    user_id VARCHAR(36) NOT NULL COMMENT 'User ID (foreign key)',
    product_id BIGINT NOT NULL COMMENT 'Sales data ID (foreign key to sales_data)',
    quantity INT NOT NULL DEFAULT 1 COMMENT 'Item quantity',
    price DECIMAL(10,2) NOT NULL COMMENT 'Item price at time of adding to cart',
    created_at DATETIME NOT NULL COMMENT 'Creation timestamp',
    updated_at DATETIME DEFAULT NULL COMMENT 'Last update timestamp',
    
    FOREIGN KEY (user_id) REFERENCES ecoschema.users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (product_id) REFERENCES ecoschema.sales_data(id) ON DELETE CASCADE ON UPDATE CASCADE,
    
    UNIQUE KEY uk_user_product (user_id, product_id) COMMENT 'One cart item per user-product combination',
    INDEX idx_user_id (user_id) COMMENT 'User ID index',
    INDEX idx_product_id (product_id) COMMENT 'Product ID index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Shopping cart items table';

