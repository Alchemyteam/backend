package com.ecosystem.service;

import com.ecosystem.dto.buyer.*;
import com.ecosystem.entity.CartItem;
import com.ecosystem.entity.SalesData;
import com.ecosystem.entity.User;
import com.ecosystem.exception.ProductNotFoundException;
import com.ecosystem.repository.CartItemRepository;
import com.ecosystem.repository.SalesDataRepository;
import com.ecosystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuyerCartService {

    private final CartItemRepository cartItemRepository;
    private final SalesDataRepository salesDataRepository;
    private final UserRepository userRepository;

    /**
     * 从 SalesData 获取商品价格
     * 优先级：TXP1 > Unit Cost > 0
     */
    private BigDecimal getProductPrice(SalesData salesData) {
        // 优先使用 TXP1
        if (salesData.getTxP1() != null && !salesData.getTxP1().trim().isEmpty()) {
            try {
                BigDecimal price = new BigDecimal(salesData.getTxP1().trim());
                if (price.compareTo(BigDecimal.ZERO) >= 0) {
                    return price;
                }
            } catch (NumberFormatException e) {
                // 如果解析失败，继续尝试其他字段
            }
        }
        
        // 其次使用 Unit Cost
        if (salesData.getUnitCost() != null && !salesData.getUnitCost().trim().isEmpty()) {
            try {
                BigDecimal price = new BigDecimal(salesData.getUnitCost().trim());
                if (price.compareTo(BigDecimal.ZERO) >= 0) {
                    return price;
                }
            } catch (NumberFormatException e) {
                // 如果解析失败，返回 0
            }
        }
        
        // 默认返回 0
        return BigDecimal.ZERO;
    }

    /**
     * 将 SalesData 转换为 CartProductResponse
     */
    private CartProductResponse toCartProductResponse(SalesData salesData) {
        CartProductResponse product = new CartProductResponse();
        product.setId(salesData.getId() != null ? salesData.getId().toString() : "unknown");
        product.setName(salesData.getItemName() != null ? salesData.getItemName() : "Unknown Product");
        product.setPrice(getProductPrice(salesData));
        product.setImage(null); // sales_data 表中没有图片字段
        return product;
    }

    /**
     * 将 CartItem 转换为 CartItemResponse
     */
    private CartItemResponse toCartItemResponse(CartItem cartItem) {
        CartProductResponse product = toCartProductResponse(cartItem.getSalesData());
        return new CartItemResponse(
            cartItem.getId(),
            product,
            cartItem.getQuantity(),
            cartItem.getSubtotal()
        );
    }

    /**
     * 添加到购物车
     */
    @Transactional
    public AddToCartResponse addToCart(String userId, AddToCartRequest request) {
        // 验证用户是否存在
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        
        // 解析 productId 为 Long
        Long productId;
        try {
            productId = Long.parseLong(request.getProductId());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Invalid productId format. Must be a number.");
        }
        
        // 验证商品是否存在
        SalesData salesData = salesDataRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(
                String.format("Product with id %s does not exist", request.getProductId())));
        
        // 验证数量
        Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "quantity must be greater than 0");
        }
        
        // 获取商品价格
        BigDecimal price = getProductPrice(salesData);
        
        // 检查购物车中是否已有该商品
        CartItem cartItem = cartItemRepository.findByUser_IdAndSalesData_Id(userId, productId)
            .orElse(null);
        
        if (cartItem != null) {
            // 更新数量
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setPrice(price); // 更新价格为最新价格
        } else {
            // 创建新的购物车项
            cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setSalesData(salesData);
            cartItem.setQuantity(quantity);
            cartItem.setPrice(price);
        }
        
        cartItem = cartItemRepository.save(cartItem);
        
        // 构建响应（符合文档格式）
        AddToCartResponse.CartItemInfo cartItemInfo = new AddToCartResponse.CartItemInfo(
            cartItem.getId(),
            salesData.getId().toString(),
            cartItem.getQuantity(),
            price,
            cartItem.getSubtotal()
        );
        
        return new AddToCartResponse("Product added to cart", cartItemInfo);
    }

    /**
     * 获取购物车
     */
    public CartResponse getCart(String userId) {
        List<CartItem> cartItems = cartItemRepository.findByUser_Id(userId);
        
        // 转换为响应格式
        List<CartItemResponse> items = cartItems.stream()
            .map(this::toCartItemResponse)
            .collect(Collectors.toList());
        
        // 计算总金额
        BigDecimal total = cartItems.stream()
            .map(CartItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 计算商品总数量
        Integer itemCount = cartItems.stream()
            .mapToInt(CartItem::getQuantity)
            .sum();
        
        return new CartResponse(items, total, itemCount);
    }

    /**
     * 更新购物车商品数量
     */
    @Transactional
    public CartItemResponse updateCartItem(String userId, String cartItemId, UpdateCartItemRequest request) {
        // 验证数量
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "quantity must be greater than 0");
        }
        
        // 验证购物车项是否存在且属于当前用户
        CartItem cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                String.format("Cart item with id %s does not exist", cartItemId)));
        
        if (!cartItem.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "You do not have permission to modify this cart item");
        }
        
        // 更新数量
        cartItem.setQuantity(request.getQuantity());
        // 更新价格为最新价格
        BigDecimal latestPrice = getProductPrice(cartItem.getSalesData());
        cartItem.setPrice(latestPrice);
        
        cartItem = cartItemRepository.save(cartItem);
        
        return toCartItemResponse(cartItem);
    }

    /**
     * 从购物车中删除商品
     */
    @Transactional
    public void removeFromCart(String userId, String cartItemId) {
        // 验证购物车项是否存在且属于当前用户
        CartItem cartItem = cartItemRepository.findById(cartItemId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                String.format("Cart item with id %s does not exist", cartItemId)));
        
        if (!cartItem.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                "You do not have permission to delete this cart item");
        }
        
        cartItemRepository.delete(cartItem);
    }
}

