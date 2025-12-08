package com.ecosystem.repository;

import com.ecosystem.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, String> {
    List<CartItem> findByUser_Id(String userId);
    // 修改为使用 Long 类型的 productId（对应 sales_data.id）
    Optional<CartItem> findByUser_IdAndSalesData_Id(String userId, Long productId);
    void deleteByUser_Id(String userId);
    long countByUser_Id(String userId);
}

