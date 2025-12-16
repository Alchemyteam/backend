package com.ecosystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProductMaster entity - aggregated product data from sales_data.
 * This table contains unique products with their master information.
 */
@Entity
@Table(name = "product_master", schema = "ecoschema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductMaster {

    @Id
    @Column(name = "product_uid", length = 16)
    private String productUid;

    @Column(name = "product_fingerprint", length = 255, unique = true)
    private String productFingerprint;

    // Product information fields
    @Column(name = "item_code", length = 64)
    private String itemCode;

    @Column(name = "item_name", length = 255)
    private String itemName;

    @Column(name = "product_hierarchy_3", length = 255)
    private String productHierarchy3;

    @Column(name = "function_name", length = 255)
    private String functionName;

    @Column(name = "item_type", length = 255)
    private String itemType;

    @Column(name = "model", length = 255)
    private String model;

    @Column(name = "performance_micron", length = 255)
    private String performanceMicron;

    @Column(name = "performance_efficiency", length = 255)
    private String performanceEfficiency;

    @Column(name = "material", length = 255)
    private String material;

    @Column(name = "uom", length = 64)
    private String uom;

    @Column(name = "brand_code", length = 255)
    private String brandCode;

    // Embedding fields
    @Column(name = "embedding_text", columnDefinition = "TEXT")
    private String embeddingText;

    @Column(name = "embedding_hash", length = 64)
    private String embeddingHash;

    @Column(name = "qdrant_point_id", length = 36)
    private String qdrantPointId;
}

