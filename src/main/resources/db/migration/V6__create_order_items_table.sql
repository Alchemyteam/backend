-- ============================================
-- V6: Create order items table
-- ============================================

CREATE TABLE IF NOT EXISTS order_items (
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

