package com.ecosystem.repository;

import com.ecosystem.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    
    // 搜索产品（关键词、分类、价格范围）
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR p.name LIKE %:keyword% OR p.description LIKE %:keyword%) AND " +
           "(:category IS NULL OR :category = '' OR p.category = :category) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchProducts(
        @Param("keyword") String keyword,
        @Param("category") String category,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );
    
    // 获取特色产品
    Page<Product> findByFeaturedTrue(Pageable pageable);
    
    // 按分类查找
    Page<Product> findByCategory(String category, Pageable pageable);
    
    // 按卖家查找
    List<Product> findBySellerId(String sellerId);
    
    // 按价格排序
    Page<Product> findAllByOrderByPriceAsc(Pageable pageable);
    Page<Product> findAllByOrderByPriceDesc(Pageable pageable);
    
    // 按评分排序
    Page<Product> findAllByOrderByRatingDesc(Pageable pageable);
    
    // 按创建时间排序
    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

