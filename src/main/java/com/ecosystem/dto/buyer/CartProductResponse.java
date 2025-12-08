package com.ecosystem.dto.buyer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 购物车中的商品信息（简化版）
 * 用于购物车接口返回
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartProductResponse {
    private String id;        // 商品ID（sales_data.id）
    private String name;      // 商品名称（ItemName）
    private BigDecimal price;  // 商品单价
    private String image;     // 商品图片URL（可选）
}

