package com.ecosystem.dto.punchout;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Punchout购物车项
 */
@Data
public class PunchoutCartItem {
    
    /**
     * 产品ID（供应商系统中的产品ID）
     */
    @NotBlank(message = "Product ID is required")
    private String productId;
    
    /**
     * 产品SKU（可选，用于匹配产品）
     */
    private String sku;
    
    /**
     * 产品名称
     */
    private String productName;
    
    /**
     * 数量
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    
    /**
     * 供应商物料代码（可选）
     */
    private String supplierPartId;
    
    /**
     * 单位（UOM）
     */
    private String unitOfMeasure;
}

