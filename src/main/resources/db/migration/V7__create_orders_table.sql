-- ============================================
-- V7: Create orders table
-- ============================================

CREATE TABLE IF NOT EXISTS ecoschema.orders (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT 'Order ID (UUID)',
    order_number VARCHAR(50) NOT NULL UNIQUE COMMENT 'Order number',
    user_id VARCHAR(36) NOT NULL COMMENT 'User ID (foreign key)',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'Order status (pending, processing, shipped, delivered, cancelled)',
    subtotal DECIMAL(10,2) NOT NULL COMMENT 'Order subtotal',
    shipping DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Shipping cost',
    tax DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Tax amount',
    total DECIMAL(10,2) NOT NULL COMMENT 'Order total',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT 'Currency code',
    shipping_street VARCHAR(255) NOT NULL COMMENT 'Shipping street address',
    shipping_city VARCHAR(100) NOT NULL COMMENT 'Shipping city',
    shipping_postal_code VARCHAR(20) NOT NULL COMMENT 'Shipping postal code',
    shipping_country VARCHAR(100) NOT NULL COMMENT 'Shipping country',
    payment_method VARCHAR(50) DEFAULT NULL COMMENT 'Payment method',
    tracking_number VARCHAR(100) DEFAULT NULL COMMENT 'Tracking number',
    estimated_delivery DATETIME DEFAULT NULL COMMENT 'Estimated delivery date',
    created_at DATETIME NOT NULL COMMENT 'Creation timestamp',
    updated_at DATETIME DEFAULT NULL COMMENT 'Last update timestamp',
    
    FOREIGN KEY (user_id) REFERENCES ecoschema.users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    
    INDEX idx_user_id (user_id) COMMENT 'User ID index',
    INDEX idx_order_number (order_number) COMMENT 'Order number index',
    INDEX idx_status (status) COMMENT 'Order status index',
    INDEX idx_created_at (created_at) COMMENT 'Creation date index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Orders table';

