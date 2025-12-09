-- ============================================
-- V4: Create product hierarchy mapping table
-- ============================================

CREATE TABLE IF NOT EXISTS ecoschema.product_hierarchy_mapping (
    product_hierarchy1 VARCHAR(100) NOT NULL COMMENT 'Product hierarchy level 1',
    product_hierarchy2 VARCHAR(100) NOT NULL COMMENT 'Product hierarchy level 2',
    PRIMARY KEY (product_hierarchy2) COMMENT 'Primary key on level 2'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Product hierarchy mapping table';

