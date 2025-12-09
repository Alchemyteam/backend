-- ============================================
-- V8: Create order_items table
-- ============================================

CREATE TABLE IF NOT EXISTS ecoschema.order_items (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT 'Order item ID (UUID)',
    order_id VARCHAR(36) NOT NULL COMMENT 'Order ID (foreign key)',
    product_id VARCHAR(36) NOT NULL COMMENT 'Product ID (foreign key to products)',
    product_name VARCHAR(255) NOT NULL COMMENT 'Product name at time of order',
    quantity INT NOT NULL COMMENT 'Item quantity',
    price DECIMAL(10,2) NOT NULL COMMENT 'Item price at time of order',
    subtotal DECIMAL(10,2) NOT NULL COMMENT 'Item subtotal (quantity * price)',
    image VARCHAR(500) DEFAULT NULL COMMENT 'Product image URL at time of order',
    created_at DATETIME NOT NULL COMMENT 'Creation timestamp',
    
    FOREIGN KEY (order_id) REFERENCES ecoschema.orders(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (product_id) REFERENCES ecoschema.products(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    
    INDEX idx_order_id (order_id) COMMENT 'Order ID index',
    INDEX idx_product_id (product_id) COMMENT 'Product ID index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Order items table';

