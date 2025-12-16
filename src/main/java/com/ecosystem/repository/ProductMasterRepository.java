package com.ecosystem.repository;

import com.ecosystem.entity.ProductMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductMasterRepository extends JpaRepository<ProductMaster, String> {
    
    // Find all products with pagination
    Page<ProductMaster> findAll(Pageable pageable);
    
    // Find products without embedding_text (using @Query for string empty check)
    @org.springframework.data.jpa.repository.Query("SELECT p FROM ProductMaster p WHERE p.embeddingText IS NULL OR p.embeddingText = '' OR TRIM(p.embeddingText) = ''")
    List<ProductMaster> findWithoutEmbeddingText();
    
    // Find products with embedding_text
    @org.springframework.data.jpa.repository.Query("SELECT p FROM ProductMaster p WHERE p.embeddingText IS NOT NULL AND p.embeddingText != '' AND TRIM(p.embeddingText) != ''")
    List<ProductMaster> findWithEmbeddingText();
}

