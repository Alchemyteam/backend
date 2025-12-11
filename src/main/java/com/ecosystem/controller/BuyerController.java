package com.ecosystem.controller;

import com.ecosystem.dto.ErrorResponse;
import com.ecosystem.dto.buyer.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.ecosystem.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/buyer")
@RequiredArgsConstructor
public class BuyerController {

    private final BuyerProductService buyerProductService;
    private final BuyerCartService buyerCartService;
    private final BuyerOrderService buyerOrderService;
    private final BuyerWishlistService buyerWishlistService;
    private final BuyerStatisticsService buyerStatisticsService;
    private final SalesDataService salesDataService;

    // ==================== Product Related Endpoints ====================

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

    // ==================== Cart Related Endpoints ====================

    @PostMapping("/cart/add")
    public ResponseEntity<AddToCartResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        AddToCartResponse response = buyerCartService.addToCart(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cart")
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        String userId = authentication.getName();
        CartResponse response = buyerCartService.getCart(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/cart/{cartItemId}")
    public ResponseEntity<CartItemResponse> updateCartItem(
            @PathVariable String cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {
        String userId = authentication.getName();
        CartItemResponse response = buyerCartService.updateCartItem(userId, cartItemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cart/{cartItemId}")
    public ResponseEntity<?> removeFromCart(
            @PathVariable String cartItemId,
            Authentication authentication) {
        String userId = authentication.getName();
        buyerCartService.removeFromCart(userId, cartItemId);
        return ResponseEntity.ok().body(new ErrorResponse("Cart item removed successfully", null, null));
    }

    // ==================== Order Related Endpoints ====================

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

    // ==================== Wishlist Related Endpoints ====================

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

    // ==================== Statistics Related Endpoints ====================

    @GetMapping("/statistics")
    public ResponseEntity<BuyerStatisticsResponse> getStatistics(
            @RequestParam(required = false, defaultValue = "month") String period,
            Authentication authentication) {
        String userId = authentication.getName();
        BuyerStatisticsResponse response = buyerStatisticsService.getStatistics(userId, period);
        return ResponseEntity.ok(response);
    }

    // ==================== Sales Data Related Endpoints ====================

    @GetMapping("/sales-data")
    public ResponseEntity<SalesDataListResponse> getSalesData(
            // Basic parameters
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            // Transaction related filter parameters
            @RequestParam(required = false) String minDate,
            @RequestParam(required = false) String maxDate,
            @RequestParam(required = false) String txNo,
            @RequestParam(required = false) Integer minQty,
            @RequestParam(required = false) Integer maxQty,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minValue,
            @RequestParam(required = false) BigDecimal maxValue,
            // Buyer related filter parameters
            @RequestParam(required = false) String buyerCode,
            @RequestParam(required = false) String buyerName,
            // Product related filter parameters
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String productHierarchy3,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) String uom,
            // Brand and performance filter parameters
            @RequestParam(required = false) String brandCode,
            @RequestParam(required = false) String performance,
            @RequestParam(required = false) String performance1,
            // Cost and function filter parameters
            @RequestParam(required = false) BigDecimal minUnitCost,
            @RequestParam(required = false) BigDecimal maxUnitCost,
            @RequestParam(required = false) String function,
            // Industry related filter parameters
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String subSector,
            // Other filter parameters
            @RequestParam(required = false) String source,
            Authentication authentication) {
        SalesDataListResponse response = salesDataService.getSalesData(
            page, limit, sort, category, keyword,
            minDate, maxDate, txNo, minQty, maxQty, minPrice, maxPrice, minValue, maxValue,
            buyerCode, buyerName,
            itemCode, itemName, productHierarchy3, itemType, model, material, uom,
            brandCode, performance, performance1,
            minUnitCost, maxUnitCost, function,
            sector, subSector,
            source
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sales-data")
    public ResponseEntity<SalesDataResponse> createSalesData(
            @Valid @RequestBody SalesDataRequest request,
            Authentication authentication) {
        SalesDataResponse response = salesDataService.createSalesData(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/sales-data/{txNo}")
    public ResponseEntity<SalesDataResponse> updateSalesData(
            @PathVariable String txNo,
            @Valid @RequestBody SalesDataRequest request,
            Authentication authentication) {
        SalesDataResponse response = salesDataService.updateSalesData(txNo, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/sales-data/{txNo}")
    public ResponseEntity<?> deleteSalesData(
            @PathVariable String txNo,
            Authentication authentication) {
        salesDataService.deleteSalesData(txNo);
        return ResponseEntity.ok().body(new ErrorResponse("Sales data deleted successfully", null, null));
    }

    @GetMapping("/sales-data/categories")
    public ResponseEntity<List<String>> getSalesDataCategories(Authentication authentication) {
        List<String> categories = salesDataService.getCategories();
        return ResponseEntity.ok(categories);
    }

    @PostMapping("/sales-data/bulk-import")
    public ResponseEntity<BulkImportResponse> bulkImportSalesData(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Authentication authentication) {
        BulkImportResponse response = salesDataService.bulkImportFromExcel(file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sales-data/template")
    public ResponseEntity<Resource> downloadTemplate(Authentication authentication) {
        try {
            Resource resource = salesDataService.generateExcelTemplate();
            String fileName = salesDataService.getTemplateFileName();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate template: " + e.getMessage(), e);
        }
    }
}

