-- ============================================
-- V3: Create sales_data table
-- ============================================

CREATE TABLE IF NOT EXISTS ecoschema.sales_data (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Sales data ID (auto-increment)',
    `TXNo` VARCHAR(255) DEFAULT NULL COMMENT 'Transaction number',
    `TXDate` VARCHAR(255) DEFAULT NULL COMMENT 'Transaction date',
    `TXQty` VARCHAR(255) DEFAULT NULL COMMENT 'Transaction quantity',
    `TXP1` VARCHAR(255) DEFAULT NULL COMMENT 'Transaction price 1',
    `TXP2` VARCHAR(255) DEFAULT NULL COMMENT 'Transaction price 2',
    `BuyerCode` VARCHAR(255) DEFAULT NULL COMMENT 'Buyer code',
    `BuyerName` VARCHAR(255) DEFAULT NULL COMMENT 'Buyer name',
    `ItemCode` VARCHAR(255) DEFAULT NULL COMMENT 'Item code',
    `ItemName` VARCHAR(255) DEFAULT NULL COMMENT 'Item name',
    `Product Hierarchy 3` VARCHAR(255) DEFAULT NULL COMMENT 'Product hierarchy level 3',
    `Function` VARCHAR(255) DEFAULT NULL COMMENT 'Function',
    `ItemType` VARCHAR(255) DEFAULT NULL COMMENT 'Item type',
    `Model` VARCHAR(255) DEFAULT NULL COMMENT 'Model',
    `Performance` VARCHAR(255) DEFAULT NULL COMMENT 'Performance',
    `Performance.1` VARCHAR(255) DEFAULT NULL COMMENT 'Performance 1',
    `Material` VARCHAR(255) DEFAULT NULL COMMENT 'Material',
    `UOM` VARCHAR(255) DEFAULT NULL COMMENT 'Unit of measure',
    `Bundled` VARCHAR(255) DEFAULT NULL COMMENT 'Bundled',
    `Origin` VARCHAR(255) DEFAULT NULL COMMENT 'Origin',
    `Brand Code` VARCHAR(255) DEFAULT NULL COMMENT 'Brand code',
    `Unit Cost` VARCHAR(255) DEFAULT NULL COMMENT 'Unit cost',
    `Sector` VARCHAR(255) DEFAULT NULL COMMENT 'Sector',
    `SubSector` VARCHAR(255) DEFAULT NULL COMMENT 'Sub sector',
    `Value` VARCHAR(255) DEFAULT NULL COMMENT 'Value',
    `Rationale` VARCHAR(255) DEFAULT NULL COMMENT 'Rationale',
    `www` VARCHAR(255) DEFAULT NULL COMMENT 'Website URL',
    `Source` VARCHAR(255) DEFAULT NULL COMMENT 'Source',
    
    -- Indexes for common filter operations
    INDEX idx_tx_date (`TXDate`) COMMENT 'Transaction date index',
    INDEX idx_tx_no (`TXNo`) COMMENT 'Transaction number index',
    INDEX idx_buyer_code (`BuyerCode`) COMMENT 'Buyer code index',
    INDEX idx_buyer_name (`BuyerName`) COMMENT 'Buyer name index',
    INDEX idx_item_code (`ItemCode`) COMMENT 'Item code index',
    INDEX idx_item_name (`ItemName`) COMMENT 'Item name index',
    INDEX idx_product_hierarchy3 (`Product Hierarchy 3`) COMMENT 'Product hierarchy 3 index',
    INDEX idx_sector (`Sector`) COMMENT 'Sector index',
    INDEX idx_brand_code (`Brand Code`) COMMENT 'Brand code index',
    INDEX idx_function (`Function`) COMMENT 'Function index',
    INDEX idx_item_type (`ItemType`) COMMENT 'Item type index',
    INDEX idx_model (`Model`) COMMENT 'Model index',
    INDEX idx_material (`Material`) COMMENT 'Material index',
    INDEX idx_subsector (`SubSector`) COMMENT 'Sub sector index',
    INDEX idx_source (`Source`) COMMENT 'Source index',
    
    -- Composite indexes for common query patterns
    INDEX idx_category_sector (`Product Hierarchy 3`, `Sector`) COMMENT 'Category and sector composite index',
    INDEX idx_buyer_item (`BuyerCode`, `ItemCode`) COMMENT 'Buyer and item composite index'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sales data table';

