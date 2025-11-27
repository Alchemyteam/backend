package com.ecosystem.repository;

import com.ecosystem.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, String> {
    List<CartItem> findByUser_Id(String userId);
    Optional<CartItem> findByUser_IdAndProduct_Id(String userId, String productId);
    void deleteByUser_Id(String userId);
    long countByUser_Id(String userId);
}

