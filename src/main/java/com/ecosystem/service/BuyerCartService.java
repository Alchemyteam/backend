package com.ecosystem.service;

import com.ecosystem.dto.buyer.*;
import com.ecosystem.entity.CartItem;
import com.ecosystem.entity.Product;
import com.ecosystem.entity.User;
import com.ecosystem.repository.CartItemRepository;
import com.ecosystem.repository.ProductRepository;
import com.ecosystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuyerCartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BuyerProductService buyerProductService;

    @Transactional
    public CartItemResponse addToCart(String userId, AddToCartRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (product.getStock() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }
        
        CartItem cartItem = cartItemRepository.findByUser_IdAndProduct_Id(userId, request.getProductId())
            .orElse(null);
        
        if (cartItem != null) {
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            cartItem.setPrice(product.getPrice());
        } else {
            cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setPrice(product.getPrice());
        }
        
        cartItem = cartItemRepository.save(cartItem);
        
        return new CartItemResponse(
            cartItem.getId(),
            null, // product will be set in getCart
            cartItem.getQuantity(),
            cartItem.getSubtotal()
        );
    }

    public CartResponse getCart(String userId) {
        List<CartItem> cartItems = cartItemRepository.findByUser_Id(userId);
        
        BigDecimal total = BigDecimal.ZERO;
        List<CartItemResponse> items = cartItems.stream()
            .map(item -> {
                ProductResponse product = buyerProductService.getProductDetail(
                    item.getProduct().getId(), userId);
                BigDecimal subtotal = item.getSubtotal();
                total.add(subtotal);
                return new CartItemResponse(
                    item.getId(),
                    product,
                    item.getQuantity(),
                    subtotal
                );
            })
            .collect(Collectors.toList());
        
        // 重新计算总价
        BigDecimal finalTotal = cartItems.stream()
            .map(CartItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new CartResponse(items, finalTotal, items.size());
    }

    @Transactional
    public void removeFromCart(String userId, String cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new RuntimeException("Cart item not found"));
        
        if (!cartItem.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        cartItemRepository.delete(cartItem);
    }
}

