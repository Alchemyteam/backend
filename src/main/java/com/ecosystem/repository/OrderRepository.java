package com.ecosystem.repository;

import com.ecosystem.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByUser_Id(String userId, Pageable pageable);
    Page<Order> findByUser_IdAndStatus(String userId, String status, Pageable pageable);
    Optional<Order> findByIdAndUser_Id(String orderId, String userId);
    Optional<Order> findByOrderNumber(String orderNumber);
}

