package com.ecosystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sales_data", schema = "ecoschema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "TXNo", length = 255)
    private String txNo;

    @Column(name = "TXDate", length = 255)
    private String txDate;

    @Column(name = "TXQty", length = 255)
    private String txQty;

    @Column(name = "TXP1", length = 255)
    private String txP1;

    // 注意：TXP2 字段暂时不存在于数据库中，使用 @Transient 避免 Hibernate 尝试读取
    @Transient
    private String txP2;

    @Column(name = "BuyerCode", length = 255)
    private String buyerCode;

    @Column(name = "BuyerName", length = 255)
    private String buyerName;

    @Column(name = "ItemCode", length = 255)
    private String itemCode;

    @Column(name = "ItemName", length = 255)
    private String itemName;

    @Column(name = "`Product Hierarchy 3`", length = 255)
    private String productHierarchy3;

    @Column(name = "`Function`", length = 255)
    private String function;

    @Column(name = "ItemType", length = 255)
    private String itemType;

    @Column(name = "Model", length = 255)
    private String model;

    @Column(name = "Performance", length = 255)
    private String performance;

    @Column(name = "`Performance.1`", length = 255)
    private String performance1;

    @Column(name = "Material", length = 255)
    private String material;

    @Column(name = "UOM", length = 255)
    private String uom;

    // 注意：Bundled 和 Origin 字段暂时不存在于数据库中，使用 @Transient 避免 Hibernate 尝试读取
    @Transient
    private String bundled;

    @Transient
    private String origin;

    @Column(name = "`Brand Code`", length = 255)
    private String brandCode;

    @Column(name = "`Unit Cost`", length = 255)
    private String unitCost;

    @Column(name = "Sector", length = 255)
    private String sector;

    @Column(name = "SubSector", length = 255)
    private String subSector;

    @Column(name = "Value", length = 255)
    private String value;

    @Column(name = "Rationale", length = 255)
    private String rationale;

    @Column(name = "www", length = 255)
    private String www;

    @Column(name = "Source", length = 255)
    private String source;

    // Note: embedding_text and embedding_hash are only in product_master table, not in sales_data
    // Mark as @Transient to avoid Hibernate trying to read them from sales_data table
    @Transient
    private String embeddingText;

    @Transient
    private String embeddingHash;
}
