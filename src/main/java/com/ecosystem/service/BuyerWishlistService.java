package com.ecosystem.service;

import com.ecosystem.dto.buyer.*;
import com.ecosystem.entity.Product;
import com.ecosystem.entity.User;
import com.ecosystem.entity.WishlistItem;
import com.ecosystem.repository.ProductRepository;
import com.ecosystem.repository.UserRepository;
import com.ecosystem.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuyerWishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BuyerProductService buyerProductService;

    @Transactional
    public WishlistItemResponse addToWishlist(String userId, String productId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (wishlistItemRepository.existsByUser_IdAndProduct_Id(userId, productId)) {
            throw new RuntimeException("Product already in wishlist");
        }
        
        WishlistItem wishlistItem = new WishlistItem();
        wishlistItem.setUser(user);
        wishlistItem.setProduct(product);
        wishlistItem = wishlistItemRepository.save(wishlistItem);
        
        ProductResponse productResponse = buyerProductService.getProductDetail(productId, userId);
        
        return new WishlistItemResponse(
            wishlistItem.getId(),
            productResponse,
            wishlistItem.getCreatedAt()
        );
    }

    public WishlistResponse getWishlist(String userId, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<WishlistItem> wishlistPage = wishlistItemRepository.findByUser_Id(userId, pageable);
        
        List<WishlistItemResponse> items = wishlistPage.getContent().stream()
            .map(item -> {
                ProductResponse product = buyerProductService.getProductDetail(
                    item.getProduct().getId(), userId);
                return new WishlistItemResponse(
                    item.getId(),
                    product,
                    item.getCreatedAt()
                );
            })
            .collect(Collectors.toList());
        
        PaginationResponse pagination = new PaginationResponse(
            page, limit, wishlistPage.getTotalElements(), wishlistPage.getTotalPages()
        );
        
        return new WishlistResponse(items, pagination);
    }

    @Transactional
    public void removeFromWishlist(String userId, String wishlistItemId) {
        WishlistItem wishlistItem = wishlistItemRepository.findById(wishlistItemId)
            .orElseThrow(() -> new RuntimeException("Wishlist item not found"));
        
        if (!wishlistItem.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        wishlistItemRepository.delete(wishlistItem);
    }
}

