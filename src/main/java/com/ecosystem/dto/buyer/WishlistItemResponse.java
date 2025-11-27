package com.ecosystem.dto.buyer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemResponse {
    private String id;
    private ProductResponse product;
    private LocalDateTime addedAt;
}

