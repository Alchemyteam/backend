package com.ecosystem.service;

import com.ecosystem.dto.buyer.BuyerStatisticsResponse;
import com.ecosystem.entity.Order;
import com.ecosystem.repository.OrderRepository;
import com.ecosystem.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BuyerStatisticsService {

    private final OrderRepository orderRepository;
    private final WishlistItemRepository wishlistItemRepository;

    public BuyerStatisticsResponse getStatistics(String userId, String period) {
        LocalDateTime startDate = getStartDate(period);
        
        List<Order> orders = orderRepository.findByUser_Id(userId, org.springframework.data.domain.Pageable.unpaged())
            .getContent().stream()
            .filter(order -> order.getCreatedAt().isAfter(startDate))
            .toList();
        
        int totalOrders = orders.size();
        int activeOrders = (int) orders.stream()
            .filter(o -> !"delivered".equals(o.getStatus()) && !"cancelled".equals(o.getStatus()))
            .count();
        
        int wishlistItems = (int) wishlistItemRepository.countByUser_Id(userId);
        
        BigDecimal totalSpent = orders.stream()
            .filter(o -> "delivered".equals(o.getStatus()))
            .map(Order::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Integer> orderStatusBreakdown = new HashMap<>();
        orderStatusBreakdown.put("pending", 0);
        orderStatusBreakdown.put("processing", 0);
        orderStatusBreakdown.put("shipped", 0);
        orderStatusBreakdown.put("delivered", 0);
        orderStatusBreakdown.put("cancelled", 0);
        
        orders.forEach(order -> {
            String status = order.getStatus();
            orderStatusBreakdown.put(status, orderStatusBreakdown.getOrDefault(status, 0) + 1);
        });
        
        return new BuyerStatisticsResponse(
            totalOrders,
            activeOrders,
            wishlistItems,
            totalSpent,
            "USD",
            period != null ? period : "month",
            orderStatusBreakdown
        );
    }

    private LocalDateTime getStartDate(String period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period != null ? period : "month") {
            case "week" -> now.minusWeeks(1);
            case "year" -> now.minusYears(1);
            default -> now.minusMonths(1);
        };
    }
}

