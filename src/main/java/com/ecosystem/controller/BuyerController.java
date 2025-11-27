package com.ecosystem.controller;

import com.ecosystem.dto.ErrorResponse;
import com.ecosystem.dto.buyer.*;
import com.ecosystem.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/buyer")
@RequiredArgsConstructor
public class BuyerController {

    private final BuyerProductService buyerProductService;
    private final BuyerCartService buyerCartService;
    private final BuyerOrderService buyerOrderService;
    private final BuyerWishlistService buyerWishlistService;
    private final BuyerStatisticsService buyerStatisticsService;

    // ==================== 产品相关接口 ====================

    @GetMapping("/products/search")
    public ResponseEntity<ProductListResponse> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        String userId = authentication.getName();
        ProductListResponse response = buyerProductService.searchProducts(
            keyword, category, minPrice, maxPrice, page, limit, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/featured")
    public ResponseEntity<ProductListResponse> getFeaturedProducts(
            @RequestParam(defaultValue = "3") int limit,
            Authentication authentication) {
        String userId = authentication.getName();
        ProductListResponse response = buyerProductService.getFeaturedProducts(limit, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products")
    public ResponseEntity<ProductListResponse> getAllProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(required = false) String category,
            Authentication authentication) {
        String userId = authentication.getName();
        ProductListResponse response = buyerProductService.getAllProducts(
            page, limit, sort, category, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductResponse> getProductDetail(
            @PathVariable String productId,
            Authentication authentication) {
        String userId = authentication.getName();
        ProductResponse response = buyerProductService.getProductDetail(productId, userId);
        return ResponseEntity.ok(response);
    }

    // ==================== 购物车相关接口 ====================

    @PostMapping("/cart/add")
    public ResponseEntity<CartItemResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        CartItemResponse response = buyerCartService.addToCart(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cart")
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        String userId = authentication.getName();
        CartResponse response = buyerCartService.getCart(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cart/{cartItemId}")
    public ResponseEntity<?> removeFromCart(
            @PathVariable String cartItemId,
            Authentication authentication) {
        String userId = authentication.getName();
        buyerCartService.removeFromCart(userId, cartItemId);
        return ResponseEntity.ok().body(new ErrorResponse("Product removed from cart", null, null));
    }

    // ==================== 订单相关接口 ====================

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        OrderResponse response = buyerOrderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<OrderListResponse> getOrders(
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        String userId = authentication.getName();
        OrderListResponse response = buyerOrderService.getOrders(userId, status, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrderDetail(
            @PathVariable String orderId,
            Authentication authentication) {
        String userId = authentication.getName();
        OrderResponse response = buyerOrderService.getOrderDetail(userId, orderId);
        return ResponseEntity.ok(response);
    }

    // ==================== 愿望清单相关接口 ====================

    @PostMapping("/wishlist/add")
    public ResponseEntity<WishlistItemResponse> addToWishlist(
            @Valid @RequestBody AddToWishlistRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        WishlistItemResponse response = buyerWishlistService.addToWishlist(userId, request.getProductId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/wishlist")
    public ResponseEntity<WishlistResponse> getWishlist(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        String userId = authentication.getName();
        WishlistResponse response = buyerWishlistService.getWishlist(userId, page, limit);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/wishlist/{wishlistItemId}")
    public ResponseEntity<?> removeFromWishlist(
            @PathVariable String wishlistItemId,
            Authentication authentication) {
        String userId = authentication.getName();
        buyerWishlistService.removeFromWishlist(userId, wishlistItemId);
        return ResponseEntity.ok().body(new ErrorResponse("Product removed from wishlist", null, null));
    }

    // ==================== 统计相关接口 ====================

    @GetMapping("/statistics")
    public ResponseEntity<BuyerStatisticsResponse> getStatistics(
            @RequestParam(required = false, defaultValue = "month") String period,
            Authentication authentication) {
        String userId = authentication.getName();
        BuyerStatisticsResponse response = buyerStatisticsService.getStatistics(userId, period);
        return ResponseEntity.ok(response);
    }
}

