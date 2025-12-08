package com.ecosystem.dto.buyer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private String id;                    // 购物车项ID
    private CartProductResponse product;  // 商品信息（简化版）
    private Integer quantity;             // 商品数量
    private BigDecimal subtotal;          // 小计
}

