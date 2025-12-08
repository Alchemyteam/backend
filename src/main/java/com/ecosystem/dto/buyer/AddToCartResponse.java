package com.ecosystem.dto.buyer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 添加到购物车的响应
 * 符合文档要求的格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartResponse {
    private String message;
    private CartItemInfo cartItem;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemInfo {
        private String id;           // 购物车项ID
        private String productId;    // 商品ID
        private Integer quantity;    // 商品数量
        private BigDecimal price;    // 商品单价
        private BigDecimal subtotal; // 小计
    }
}

