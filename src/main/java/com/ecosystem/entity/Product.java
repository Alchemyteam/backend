package com.ecosystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", schema = "ecoschema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 10)
    private String currency = "USD";

    @Column(length = 500)
    private String image;

    @Column(name = "images", columnDefinition = "JSON")
    private String images; // JSON 字符串，存储图片数组

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "reviews_count")
    private Integer reviewsCount = 0;

    @Column(name = "pe_certified")
    private Boolean peCertified = false;

    @Column(name = "certificate_number", length = 100)
    private String certificateNumber;

    @Column(name = "certified_by", length = 255)
    private String certifiedBy;

    @Column(name = "certified_date")
    private LocalDate certifiedDate;

    @Column(name = "specifications", columnDefinition = "JSON")
    private String specifications; // JSON 字符串，存储规格对象

    @Column(name = "tags", columnDefinition = "JSON")
    private String tags; // JSON 字符串，存储标签数组

    @Column(nullable = false)
    private Boolean featured = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

