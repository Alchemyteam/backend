package com.ecosystem.service;

import com.ecosystem.dto.buyer.*;
import com.ecosystem.entity.*;
import com.ecosystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuyerOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setUser(user);
        order.setStatus("pending");
        order.setCurrency("USD");
        order.setShippingStreet(request.getShippingAddress().getStreet());
        order.setShippingCity(request.getShippingAddress().getCity());
        order.setShippingPostalCode(request.getShippingAddress().getPostalCode());
        order.setShippingCountry(request.getShippingAddress().getCountry());
        order.setPaymentMethod(request.getPaymentMethod());
        
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + itemRequest.getProductId()));
            
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            
            BigDecimal itemSubtotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setSubtotal(itemSubtotal);
            orderItem.setImage(product.getImage());
            
            // 更新库存
            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);
            
            orderItems.add(orderItem);
        }
        
        BigDecimal shipping = calculateShipping(subtotal);
        BigDecimal tax = calculateTax(subtotal);
        BigDecimal total = subtotal.add(shipping).add(tax);
        
        order.setSubtotal(subtotal);
        order.setShipping(shipping);
        order.setTax(tax);
        order.setTotal(total);
        order.setItems(orderItems);
        
        Order savedOrder = orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);
        
        // 清空购物车
        cartItemRepository.deleteByUser_Id(userId);
        
        return toOrderResponse(savedOrder);
    }

    public OrderListResponse getOrders(String userId, String status, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Order> orderPage;
        
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            orderPage = orderRepository.findByUser_IdAndStatus(userId, status, pageable);
        } else {
            orderPage = orderRepository.findByUser_Id(userId, pageable);
        }
        
        List<OrderResponse> orders = orderPage.getContent().stream()
            .map(this::toOrderResponse)
            .collect(Collectors.toList());
        
        PaginationResponse pagination = new PaginationResponse(
            page, limit, orderPage.getTotalElements(), orderPage.getTotalPages()
        );
        
        return new OrderListResponse(orders, pagination);
    }

    public OrderResponse getOrderDetail(String userId, String orderId) {
        Order order = orderRepository.findByIdAndUser_Id(orderId, userId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
        
        return toOrderResponse(order);
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemDetailResponse> items = order.getItems().stream()
            .map(item -> new OrderItemDetailResponse(
                item.getProduct().getId(),
                item.getProductName(),
                item.getQuantity(),
                item.getPrice(),
                item.getSubtotal(),
                item.getImage()
            ))
            .collect(Collectors.toList());
        
        ShippingAddressResponse shippingAddress = new ShippingAddressResponse(
            order.getShippingStreet(),
            order.getShippingCity(),
            order.getShippingPostalCode(),
            order.getShippingCountry()
        );
        
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus());
        response.setItems(items);
        response.setShippingAddress(shippingAddress);
        response.setSubtotal(order.getSubtotal());
        response.setShipping(order.getShipping());
        response.setTax(order.getTax());
        response.setTotal(order.getTotal());
        response.setCurrency(order.getCurrency());
        response.setTrackingNumber(order.getTrackingNumber());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setEstimatedDelivery(order.getEstimatedDelivery());
        response.setItemCount(items.size());
        
        return response;
    }

    private BigDecimal calculateShipping(BigDecimal subtotal) {
        // 简单的运费计算逻辑，可以根据需求修改
        if (subtotal.compareTo(new BigDecimal("100")) >= 0) {
            return BigDecimal.ZERO; // 满100免运费
        }
        return new BigDecimal("50.00"); // 默认运费
    }

    private BigDecimal calculateTax(BigDecimal subtotal) {
        // 简单的税费计算逻辑，可以根据需求修改
        return subtotal.multiply(new BigDecimal("0.08")); // 8% 税费
    }
}

