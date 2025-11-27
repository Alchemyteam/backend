package com.ecosystem.dto.buyer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddToWishlistRequest {
    @NotBlank(message = "Product ID is required")
    private String productId;
}

