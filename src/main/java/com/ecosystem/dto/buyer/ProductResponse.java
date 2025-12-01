package com.ecosystem.dto.buyer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private String image;
    private List<String> images;
    private SellerResponse seller;
    private CertificationResponse certification;
    private Object specifications; // Map<String, Object>
    private Integer stock;
    private BigDecimal rating;
    private Integer reviewsCount;
    private String category;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal historicalLowPrice;
    private BigDecimal lastTransactionPrice;
    private Boolean inWishlist; // 是否在愿望清单中（前端不需要，但后端使用）
}

