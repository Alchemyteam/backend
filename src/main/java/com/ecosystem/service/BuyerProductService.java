package com.ecosystem.service;

import com.ecosystem.dto.buyer.*;
import com.ecosystem.entity.Product;
import com.ecosystem.repository.ProductRepository;
import com.ecosystem.repository.WishlistItemRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuyerProductService {

    private final ProductRepository productRepository;
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
        Pageable pageable = createPageable(page, limit, sort);
        
        Page<Product> productPage;
        if (category != null && !category.isEmpty() && !"all".equals(category)) {
            productPage = productRepository.findByCategory(category, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }
        
        Set<String> wishlistProductIds = getWishlistProductIds(userId);
        
        List<ProductResponse> products = productPage.getContent().stream()
            .map(p -> toProductResponse(p, wishlistProductIds.contains(p.getId())))
            .collect(Collectors.toList());
        
        PaginationResponse pagination = new PaginationResponse(
            page, limit, productPage.getTotalElements(), productPage.getTotalPages()
        );
        
        return new ProductListResponse(products, pagination);
    }

    public ProductResponse getProductDetail(String productId, String userId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        boolean inWishlist = wishlistItemRepository.existsByUser_IdAndProduct_Id(userId, productId);
        
        return toProductDetailResponse(product, inWishlist);
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

