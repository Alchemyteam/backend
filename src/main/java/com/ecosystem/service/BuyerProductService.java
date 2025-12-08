package com.ecosystem.service;

import com.ecosystem.dto.buyer.*;
import com.ecosystem.entity.Product;
import com.ecosystem.entity.SalesData;
import com.ecosystem.exception.ProductNotFoundException;
import com.ecosystem.repository.ProductRepository;
import com.ecosystem.repository.SalesDataRepository;
import com.ecosystem.repository.WishlistItemRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuyerProductService {

    private final ProductRepository productRepository;
    private final SalesDataRepository salesDataRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ObjectMapper objectMapper;

    public ProductListResponse searchProducts(String keyword, String category, 
                                             BigDecimal minPrice, BigDecimal maxPrice,
                                             int page, int limit, String userId) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Product> productPage = productRepository.searchProducts(
            keyword, category, minPrice, maxPrice, pageable
        );
        
        Set<String> wishlistProductIds = getWishlistProductIds(userId);
        
        List<ProductResponse> products = productPage.getContent().stream()
            .map(p -> toProductResponse(p, wishlistProductIds.contains(p.getId())))
            .collect(Collectors.toList());
        
        PaginationResponse pagination = new PaginationResponse(
            page, limit, productPage.getTotalElements(), productPage.getTotalPages()
        );
        
        return new ProductListResponse(products, pagination);
    }

    public ProductListResponse getFeaturedProducts(int limit, String userId) {
        Pageable pageable = PageRequest.of(0, limit);
        Page<Product> productPage = productRepository.findByFeaturedTrue(pageable);
        
        Set<String> wishlistProductIds = getWishlistProductIds(userId);
        
        List<ProductResponse> products = productPage.getContent().stream()
            .map(p -> toProductResponse(p, wishlistProductIds.contains(p.getId())))
            .collect(Collectors.toList());
        
        return new ProductListResponse(products, null);
    }

    public ProductListResponse getAllProducts(int page, int limit, String sort, 
                                            String category, String userId) {
        // 从 sales_data 表查询，而不是 products 表
        Page<SalesData> salesDataPage;
        Pageable pageable = createSalesDataPageable(page, limit, sort);
        
        if (category != null && !category.isEmpty() && !"all".equals(category)) {
            // 按分类查询
            salesDataPage = salesDataRepository.findByCategoryOrderByTxDateDesc(category, pageable);
        } else {
            // 查询所有，根据排序方式选择不同的查询方法
            switch (sort) {
                case "price_asc":
                    salesDataPage = salesDataRepository.findAllByOrderByTxP1Asc(pageable);
                    break;
                case "price_desc":
                    salesDataPage = salesDataRepository.findAllByOrderByTxP1Desc(pageable);
                    break;
                case "newest":
                default:
                    salesDataPage = salesDataRepository.findAllByOrderByTxDateDesc(pageable);
                    break;
            }
        }
        
        // 将 SalesData 转换为 ProductResponse
        List<ProductResponse> products = salesDataPage.getContent().stream()
            .map(this::toProductResponseFromSalesData)
            .collect(Collectors.toList());
        
        PaginationResponse pagination = new PaginationResponse(
            page, limit, salesDataPage.getTotalElements(), salesDataPage.getTotalPages()
        );
        
        return new ProductListResponse(products, pagination);
    }

    public ProductResponse getProductDetail(String productId, String userId) {
        // 验证 productId 不能为空
        if (productId == null || productId.trim().isEmpty()) {
            throw new ProductNotFoundException("Product ID cannot be empty");
        }
        
        String trimmedProductId = productId.trim();
        SalesData salesData = null;
        String itemCode = null;
        
        // 优先通过 id（数字）查询（新的主键）
        try {
            Long id = Long.parseLong(trimmedProductId);
            salesData = salesDataRepository.findById(id).orElse(null);
            if (salesData != null) {
                itemCode = salesData.getItemCode(); // 使用查询到的 ItemCode 进行价格统计
            }
        } catch (NumberFormatException e) {
            // 如果不是数字，尝试通过 ItemCode 查询（向后兼容）
            salesData = salesDataRepository.findFirstByItemCode(trimmedProductId);
            if (salesData != null) {
                itemCode = trimmedProductId; // 使用传入的 ItemCode 进行价格统计
            }
        }
        
        // 如果找不到产品，抛出异常
        if (salesData == null) {
            throw new ProductNotFoundException(
                String.format("Product not found with ID: %s. Please use a valid product ID (number) or item code.", trimmedProductId)
            );
        }
        
        // 获取价格统计信息（基于 ItemCode）
        Object[] priceStats = null;
        if (itemCode != null && !itemCode.trim().isEmpty()) {
            priceStats = salesDataRepository.getPriceStatisticsByItemCode(itemCode);
        }
        
        // 构建 ProductResponse
        ProductResponse response = new ProductResponse();
        // 使用 id（数字）作为唯一的产品 ID
        // 确保 id 字段有值
        Long productIdLong = salesData.getId();
        
        // 添加日志以诊断问题
        log.debug("Product detail query - productId: {}, salesData.getId(): {}, itemCode: {}", 
                  trimmedProductId, productIdLong, salesData.getItemCode());
        
        // 如果 id 为 null 或无效，尝试重新查询获取 id
        if (productIdLong == null || productIdLong <= 0) {
            log.warn("salesData.getId() is null or invalid ({}), attempting to retrieve id by ItemCode", productIdLong);
            // 如果通过 ItemCode 查询且 id 为 null，尝试通过 ItemCode 重新查询获取 id
            if (itemCode != null && !itemCode.trim().isEmpty()) {
                SalesData retrySalesData = salesDataRepository.findFirstByItemCode(itemCode);
                if (retrySalesData != null && retrySalesData.getId() != null && retrySalesData.getId() > 0) {
                    productIdLong = retrySalesData.getId();
                    log.info("Retrieved id {} from retry query", productIdLong);
                }
            }
        }
        
        // 设置 id 字段，确保始终有值
        if (productIdLong != null && productIdLong > 0) {
            response.setId(productIdLong.toString());
            log.debug("Setting response id to: {}", productIdLong.toString());
        } else {
            // 如果仍然无法获取 id，使用 productId（如果是数字）或 ItemCode
            log.warn("Unable to retrieve valid id, using productId: {}", trimmedProductId);
            response.setId(trimmedProductId);
        }
        response.setName(salesData.getItemName() != null ? salesData.getItemName() : "Unknown Product");
        
        // 构建描述
        List<String> descParts = new ArrayList<>();
        if (salesData.getItemType() != null && !salesData.getItemType().trim().isEmpty()) {
            descParts.add(salesData.getItemType());
        }
        if (salesData.getModel() != null && !salesData.getModel().trim().isEmpty()) {
            descParts.add(salesData.getModel());
        }
        if (salesData.getMaterial() != null && !salesData.getMaterial().trim().isEmpty()) {
            descParts.add(salesData.getMaterial());
        }
        response.setDescription(descParts.isEmpty() ? null : String.join(" - ", descParts));
        
        // 价格信息 - 优先从当前记录获取
        BigDecimal latestPrice = BigDecimal.ZERO;
        if (salesData.getTxP1() != null && !salesData.getTxP1().trim().isEmpty()) {
            try {
                String priceStr = salesData.getTxP1().trim();
                if (!priceStr.isEmpty()) {
                    BigDecimal parsedPrice = new BigDecimal(priceStr);
                    if (parsedPrice.compareTo(BigDecimal.ZERO) >= 0) {
                        latestPrice = parsedPrice;
                    }
                }
            } catch (NumberFormatException e) {
                latestPrice = BigDecimal.ZERO;
            }
        }
        
        // 如果价格统计有最新价格，使用统计中的最新价格
        if (priceStats != null && priceStats.length >= 3 && priceStats[2] != null) {
            try {
                Object latestPriceObj = priceStats[2];
                if (latestPriceObj != null) {
                    BigDecimal statsLatestPrice = new BigDecimal(latestPriceObj.toString());
                    if (statsLatestPrice.compareTo(BigDecimal.ZERO) > 0) {
                        latestPrice = statsLatestPrice;
                    }
                }
            } catch (Exception e) {
                // 如果转换失败，使用之前获取的价格
            }
        }
        
        // 确保 latestPrice 不为 null 且 >= 0
        if (latestPrice == null) {
            latestPrice = BigDecimal.ZERO;
        }
        if (latestPrice.compareTo(BigDecimal.ZERO) < 0) {
            latestPrice = BigDecimal.ZERO;
        }
        
        // 设置价格字段，确保都不为 null
        response.setPrice(latestPrice);
        response.setCurrency("USD");
        
        // 历史最低价 - 默认使用当前价格
        BigDecimal historicalLowPrice = latestPrice;
        if (priceStats != null && priceStats.length >= 1 && priceStats[0] != null) {
            try {
                Object minPriceObj = priceStats[0];
                if (minPriceObj != null) {
                    String minPriceStr = minPriceObj.toString();
                    if (minPriceStr != null && !minPriceStr.trim().isEmpty()) {
                        BigDecimal minPrice = new BigDecimal(minPriceStr);
                        if (minPrice.compareTo(BigDecimal.ZERO) > 0) {
                            historicalLowPrice = minPrice;
                        }
                    }
                }
            } catch (Exception e) {
                // 如果转换失败，使用默认值 latestPrice
                historicalLowPrice = latestPrice;
            }
        }
        response.setHistoricalLowPrice(historicalLowPrice);
        
        // 最近交易价（使用最新价格，确保不为 null）
        response.setLastTransactionPrice(latestPrice != null ? latestPrice : BigDecimal.ZERO);
        
        // 分类
        response.setCategory(salesData.getProductHierarchy3());
        
        // 构建标签
        List<String> tags = new ArrayList<>();
        if (salesData.getItemType() != null && !salesData.getItemType().trim().isEmpty()) {
            tags.add(salesData.getItemType());
        }
        if (salesData.getBrandCode() != null && !salesData.getBrandCode().trim().isEmpty()) {
            tags.add(salesData.getBrandCode());
        }
        if (salesData.getSector() != null && !salesData.getSector().trim().isEmpty()) {
            tags.add(salesData.getSector());
        }
        if (salesData.getFunction() != null && !salesData.getFunction().trim().isEmpty()) {
            tags.add(salesData.getFunction());
        }
        response.setTags(tags.isEmpty() ? null : tags);
        
        // 卖家信息
        if (salesData.getBuyerName() != null && !salesData.getBuyerName().trim().isEmpty()) {
            response.setSeller(new SellerResponse(
                salesData.getBuyerCode() != null ? salesData.getBuyerCode() : "unknown",
                salesData.getBuyerName(),
                false, // verified
                null   // rating
            ));
        }
        
        // 设置时间戳
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        
        // 其他可选字段保持为 null
        response.setImage(null);
        response.setImages(null);
        response.setStock(null);
        response.setRating(null);
        response.setReviewsCount(null);
        response.setCertification(null);
        response.setSpecifications(null);
        response.setInWishlist(false);
        
        return response;
    }
    
    /**
     * 将 SalesData 转换为 ProductResponse（用于产品列表）
     * 这是简化版本，不包含价格统计信息
     */
    private ProductResponse toProductResponseFromSalesData(SalesData salesData) {
        ProductResponse response = new ProductResponse();
        
        // 设置 id（确保始终有值）
        Long productIdLong = salesData.getId();
        if (productIdLong != null && productIdLong > 0) {
            response.setId(productIdLong.toString());
        } else {
            // 如果 id 为 null，使用 ItemCode 或 TXNo 作为临时标识
            response.setId(salesData.getItemCode() != null ? salesData.getItemCode() : 
                          (salesData.getTxNo() != null ? salesData.getTxNo() : "unknown"));
        }
        
        response.setName(salesData.getItemName() != null ? salesData.getItemName() : "Unknown Product");
        
        // 构建描述
        List<String> descParts = new ArrayList<>();
        if (salesData.getItemType() != null && !salesData.getItemType().trim().isEmpty()) {
            descParts.add(salesData.getItemType());
        }
        if (salesData.getModel() != null && !salesData.getModel().trim().isEmpty()) {
            descParts.add(salesData.getModel());
        }
        if (salesData.getMaterial() != null && !salesData.getMaterial().trim().isEmpty()) {
            descParts.add(salesData.getMaterial());
        }
        response.setDescription(descParts.isEmpty() ? null : String.join(" - ", descParts));
        
        // 价格信息
        BigDecimal price = BigDecimal.ZERO;
        if (salesData.getTxP1() != null && !salesData.getTxP1().trim().isEmpty()) {
            try {
                String priceStr = salesData.getTxP1().trim();
                if (!priceStr.isEmpty()) {
                    BigDecimal parsedPrice = new BigDecimal(priceStr);
                    if (parsedPrice.compareTo(BigDecimal.ZERO) >= 0) {
                        price = parsedPrice;
                    }
                }
            } catch (NumberFormatException e) {
                price = BigDecimal.ZERO;
            }
        }
        response.setPrice(price);
        response.setCurrency("USD");
        
        // 历史最低价和最近交易价（列表页面使用当前价格）
        response.setHistoricalLowPrice(price);
        response.setLastTransactionPrice(price);
        
        // 分类
        response.setCategory(salesData.getProductHierarchy3());
        
        // 构建标签
        List<String> tags = new ArrayList<>();
        if (salesData.getItemType() != null && !salesData.getItemType().trim().isEmpty()) {
            tags.add(salesData.getItemType());
        }
        if (salesData.getBrandCode() != null && !salesData.getBrandCode().trim().isEmpty()) {
            tags.add(salesData.getBrandCode());
        }
        if (salesData.getSector() != null && !salesData.getSector().trim().isEmpty()) {
            tags.add(salesData.getSector());
        }
        if (salesData.getFunction() != null && !salesData.getFunction().trim().isEmpty()) {
            tags.add(salesData.getFunction());
        }
        response.setTags(tags.isEmpty() ? null : tags);
        
        // 卖家信息
        if (salesData.getBuyerName() != null && !salesData.getBuyerName().trim().isEmpty()) {
            response.setSeller(new SellerResponse(
                salesData.getBuyerCode() != null ? salesData.getBuyerCode() : "unknown",
                salesData.getBuyerName(),
                false, // verified
                null   // rating
            ));
        }
        
        // 设置时间戳
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        
        // 其他可选字段保持为 null
        response.setImage(null);
        response.setImages(null);
        response.setStock(null);
        response.setRating(null);
        response.setReviewsCount(null);
        response.setCertification(null);
        response.setSpecifications(null);
        response.setInWishlist(false);
        
        return response;
    }

    private Pageable createPageable(int page, int limit, String sort) {
        Sort sortObj;
        switch (sort) {
            case "price_asc":
                sortObj = Sort.by(Sort.Direction.ASC, "price");
                break;
            case "price_desc":
                sortObj = Sort.by(Sort.Direction.DESC, "price");
                break;
            case "rating_desc":
                sortObj = Sort.by(Sort.Direction.DESC, "rating");
                break;
            case "newest":
                sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
            default:
                sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return PageRequest.of(page - 1, limit, sortObj);
    }
    
    private Pageable createSalesDataPageable(int page, int limit, String sort) {
        // 对于 sales_data 表，排序已经在 native query 中处理，这里只需要分页
        return PageRequest.of(page - 1, limit);
    }

    private Set<String> getWishlistProductIds(String userId) {
        if (userId == null) {
            return Set.of();
        }
        return wishlistItemRepository.findByUser_Id(userId, Pageable.unpaged())
            .getContent().stream()
            .map(item -> item.getProduct().getId())
            .collect(Collectors.toSet());
    }

    private ProductResponse toProductResponse(Product product, boolean inWishlist) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setCurrency(product.getCurrency());
        response.setImage(product.getImage());
        response.setStock(product.getStock());
        response.setRating(product.getRating());
        response.setReviewsCount(product.getReviewsCount());
        response.setCategory(product.getCategory());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());
        response.setHistoricalLowPrice(product.getHistoricalLowPrice());
        response.setLastTransactionPrice(product.getLastTransactionPrice());
        response.setInWishlist(inWishlist);
        
        // Seller
        if (product.getSeller() != null) {
            response.setSeller(new SellerResponse(
                product.getSeller().getId(),
                product.getSeller().getName(),
                product.getSeller().getVerified(),
                product.getSeller().getRating()
            ));
        }
        
        // Certification
        if (product.getPeCertified() != null && product.getPeCertified()) {
            response.setCertification(new CertificationResponse(
                true,
                product.getCertificateNumber(),
                product.getCertifiedBy(),
                product.getCertifiedDate() != null ? product.getCertifiedDate().toString() : null
            ));
        }
        
        // Images
        if (product.getImages() != null) {
            try {
                List<String> images = objectMapper.readValue(product.getImages(), 
                    new TypeReference<List<String>>() {});
                response.setImages(images);
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Tags
        if (product.getTags() != null) {
            try {
                List<String> tags = objectMapper.readValue(product.getTags(), 
                    new TypeReference<List<String>>() {});
                response.setTags(tags);
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Specifications
        if (product.getSpecifications() != null) {
            try {
                Object specs = objectMapper.readValue(product.getSpecifications(), 
                    new TypeReference<Object>() {});
                response.setSpecifications(specs);
            } catch (Exception e) {
                // Ignore
            }
        }
        
        return response;
    }

    private ProductResponse toProductDetailResponse(Product product, boolean inWishlist) {
        ProductResponse response = toProductResponse(product, inWishlist);
        // 详情页可能需要更多信息，这里可以扩展
        return response;
    }
}

