package com.ecosystem.repository;

import com.ecosystem.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, String> {
    Page<WishlistItem> findByUser_Id(String userId, Pageable pageable);
    Optional<WishlistItem> findByUser_IdAndProduct_Id(String userId, String productId);
    boolean existsByUser_IdAndProduct_Id(String userId, String productId);
    long countByUser_Id(String userId);
}

