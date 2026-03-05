package com.ecosystem.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 统一的产品卡片 DTO，用于内部搜索和 Web 搜索结果
 * 支持从数据库结果和 SERP 结果映射
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductCard {
    
    // 基本信息
    private String id;              // 内部产品ID（内部结果有，Web结果为null）
    private String name;            // 产品名称
    private String description;     // 产品描述/snippet
    private String url;             // 产品链接（Web结果必有，内部结果可选）
    private String imageUrl;        // 产品图片URL
    
    // 价格信息
    private BigDecimal price;       // 价格（可能为null）
    private String currency;        // 货币代码（SGD, USD等）
    
    // 来源信息
    private String vendor;          // 供应商/卖家名称
    private String brand;           // 品牌
    private String platform;        // 平台类型："internal" 或 "web"
    private String sourceDomain;    // 来源域名（Web结果）
    
    // 分类信息
    private String category;        // 产品分类
    private String itemCode;        // 物料编码（内部结果）
    
    // 可用性
    private String availability;    // 库存状态：in_stock, out_of_stock, limited, unknown
    private Integer stock;          // 库存数量（内部结果）
    
    // 评分与可信度
    private Double confidence;      // 结果可信度（0.0-1.0）
    private Double relevanceScore;  // 相关性得分
    
    // 元数据
    private String canonicalUrl;    // 去重用的规范化URL
    private Boolean enhanced;       // 是否经过增强处理（抓取页面、LLM解析等）
    
    /**
     * 计算信息完整度得分（用于排序）
     * 价格、供应商、名称各占权重
     */
    public double getCompletenessScore() {
        double score = 0.0;
        if (name != null && !name.isEmpty()) score += 0.3;
        if (price != null) score += 0.4;
        if (vendor != null && !vendor.isEmpty()) score += 0.2;
        if (imageUrl != null && !imageUrl.isEmpty()) score += 0.1;
        return score;
    }
    
    /**
     * 计算综合排序得分
     */
    public double getSortScore() {
        double completeness = getCompletenessScore();
        double conf = confidence != null ? confidence : 0.5;
        // 完整度权重 60%，可信度权重 40%
        return completeness * 0.6 + conf * 0.4;
    }
}
