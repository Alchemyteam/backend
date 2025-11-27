package com.ecosystem.dto.buyer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuyerStatisticsResponse {
    private Integer totalOrders;
    private Integer activeOrders;
    private Integer wishlistItems;
    private BigDecimal totalSpent;
    private String currency;
    private String period;
    private Map<String, Integer> orderStatusBreakdown;
}

