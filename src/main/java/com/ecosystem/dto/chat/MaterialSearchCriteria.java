package com.ecosystem.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 物料搜索条件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialSearchCriteria {
    
    // 精确搜索
    private String itemCode;              // 物料编码（精确匹配）
    
    // 模糊搜索
    private String itemNameKeyword;       // 物料名称关键字
    private String productHierarchy3;     // 产品分类层级3
    private String function;               // 功能
    private String brandCode;             // 品牌代码
    private String buyerName;            // 买家名称（用于组合搜索）
    private String buyerCode;             // 买家代码（用于组合搜索）
    
    // 组合条件
    private BigDecimal minPrice;          // 最低价格
    private BigDecimal maxPrice;           // 最高价格
    private LocalDate startDate;          // 开始日期
    private LocalDate endDate;            // 结束日期
    private String sector;                // 行业
    private String subSector;             // 子行业
    
    // 原始查询（用于全文搜索回退）
    private String rawQuery;              // 原始查询文本
    
    // 搜索类型
    private SearchType searchType;
    
    public enum SearchType {
        EXACT_ITEM_CODE,      // 精确物料编码
        ITEM_NAME_FUZZY,      // 物料名称模糊
        CATEGORY,             // 品类搜索
        FUNCTION,              // 功能搜索
        BRAND,                // 品牌搜索
        COMBINED              // 组合搜索
    }
    
    public boolean hasItemCode() {
        return itemCode != null && !itemCode.trim().isEmpty();
    }
    
    public boolean hasItemNameKeyword() {
        return itemNameKeyword != null && !itemNameKeyword.trim().isEmpty();
    }
    
    public boolean hasCategory() {
        return productHierarchy3 != null && !productHierarchy3.trim().isEmpty();
    }
    
    public boolean hasFunction() {
        return function != null && !function.trim().isEmpty();
    }
    
    public boolean hasBrand() {
        return brandCode != null && !brandCode.trim().isEmpty();
    }
    
    public boolean hasBuyerName() {
        return buyerName != null && !buyerName.trim().isEmpty();
    }
    
    public boolean hasBuyerCode() {
        return buyerCode != null && !buyerCode.trim().isEmpty();
    }
    
    public boolean hasPriceRange() {
        return minPrice != null || maxPrice != null;
    }
    
    public boolean hasDateRange() {
        return startDate != null || endDate != null;
    }
    
    public boolean isCombinedSearch() {
        int conditionCount = 0;
        if (hasItemCode()) conditionCount++;
        if (hasItemNameKeyword()) conditionCount++;
        if (hasCategory()) conditionCount++;
        if (hasFunction()) conditionCount++;
        if (hasBrand()) conditionCount++;
        if (hasBuyerName()) conditionCount++;
        if (hasBuyerCode()) conditionCount++;
        if (hasPriceRange()) conditionCount++;
        if (hasDateRange()) conditionCount++;
        
        // 如果只有品类和功能，且功能是品类名称的一部分，不算组合搜索
        if (conditionCount == 2 && hasCategory() && hasFunction()) {
            String categoryLower = productHierarchy3 != null ? productHierarchy3.toLowerCase() : "";
            String functionLower = function != null ? function.toLowerCase() : "";
            // 如果功能关键词是品类名称的一部分，不算组合搜索
            if (categoryLower.contains(functionLower) && functionLower.length() > 2) {
                return false;
            }
        }
        
        // 如果有两个或更多条件，就是组合搜索
        return conditionCount > 1;
    }
}

