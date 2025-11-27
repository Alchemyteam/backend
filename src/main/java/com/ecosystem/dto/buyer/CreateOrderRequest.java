package com.ecosystem.dto.buyer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotEmpty(message = "Items are required")
    @Valid
    private List<OrderItemRequest> items;
    
    @NotNull(message = "Shipping address is required")
    @Valid
    private ShippingAddressRequest shippingAddress;
    
    private String paymentMethod;
}

