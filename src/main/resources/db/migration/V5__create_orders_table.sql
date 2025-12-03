-- ============================================
-- V5: Create orders table
-- ============================================

CREATE TABLE IF NOT EXISTS orders (
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

