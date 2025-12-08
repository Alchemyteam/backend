package com.ecosystem.service;

import com.ecosystem.dto.chat.MaterialSearchCriteria;
import com.ecosystem.entity.SalesData;
import com.ecosystem.repository.SalesDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 物料搜索服务
 * 支持多种搜索方式：物料编码、物料名称、品类、功能、品牌、组合条件
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialSearchService {

    private final SalesDataRepository salesDataRepository;

    /**
     * 执行物料搜索
     */
    public List<SalesData> searchMaterials(MaterialSearchCriteria criteria) {
        log.info("Searching materials with criteria: {}", criteria);
        
        List<SalesData> results = new ArrayList<>();
        
        // 1. 精确物料编码搜索
        if (criteria.hasItemCode()) {
            log.info("Searching by exact ItemCode: {}", criteria.getItemCode());
            results = salesDataRepository.findByItemCode(criteria.getItemCode());
            log.info("Found {} results by ItemCode", results.size());
            return results;
        }
        
        // 2. 多关键词搜索（优先处理）
        if (criteria.hasKeywords() && criteria.getKeywords().size() > 1) {
            log.info("Performing multi-keyword search with {} keywords: {}", 
                criteria.getKeywords().size(), criteria.getKeywords());
            results = performMultiKeywordSearch(criteria.getKeywords());
            log.info("Multi-keyword search found {} results", results.size());
        }
        // 3. 组合搜索（支持任意字段的组合）
        else if (criteria.isCombinedSearch()) {
            log.info("Performing combined search with multiple conditions");
            results = performCombinedSearch(criteria);
            log.info("Combined search found {} results", results.size());
        }
        // 3. 单一条件搜索（优先级：品类 > 功能 > 品牌 > 物料名称关键字）
        else if (criteria.hasCategory()) {
            log.info("Searching by Product Hierarchy 3: '{}'", criteria.getProductHierarchy3());
            log.info("Search criteria details: hasPriceRange={}, hasDateRange={}", 
                criteria.hasPriceRange(), criteria.hasDateRange());
            
            results = salesDataRepository.findByProductHierarchy3(criteria.getProductHierarchy3(), 100);
            log.info("Found {} results by Product Hierarchy 3 before filtering", results.size());
            
            // 如果品类搜索无结果，尝试功能搜索（某些品类可能存储在 Function 字段中）
            if (results.isEmpty()) {
                log.warn("No results found by Product Hierarchy 3, trying Function field");
                results = salesDataRepository.findByFunction(criteria.getProductHierarchy3(), 100);
                log.info("Found {} results by Function", results.size());
            }
        } else if (criteria.hasFunction()) {
            log.info("Searching by Function: {}", criteria.getFunction());
            results = salesDataRepository.findByFunction(criteria.getFunction(), 100);
        } else if (criteria.hasBrand()) {
            log.info("Searching by Brand Code: {}", criteria.getBrandCode());
            results = salesDataRepository.findByBrandCode(criteria.getBrandCode(), 100);
        } else if (criteria.hasItemNameKeyword()) {
            log.info("Searching by ItemName keyword: {}", criteria.getItemNameKeyword());
            String keyword = criteria.getItemNameKeyword();
            
            // 尝试多种搜索方式
            // 1. 完整关键词搜索
            results = salesDataRepository.searchByItemNameKeyword(keyword, 100);
            log.info("Full keyword search found {} results", results.size());
            
            // 2. 如果结果为空，尝试拆分关键词（支持 "Safety Shoes" -> "Safety" 和 "Shoes"）
            if (results.isEmpty() && keyword.contains(" ")) {
                String[] words = keyword.split("\\s+");
                for (String word : words) {
                    if (word.length() > 2) { // 只搜索长度大于2的词
                        List<SalesData> wordResults = salesDataRepository.searchByItemNameKeyword(word, 50);
                        results.addAll(wordResults);
                        log.info("Searching for word '{}' found {} results", word, wordResults.size());
                    }
                }
                // 去重（基于 ItemCode 或 TXNo）
                results = results.stream()
                    .collect(Collectors.toMap(
                        SalesData::getTxNo,
                        item -> item,
                        (existing, replacement) -> existing
                    ))
                    .values()
                    .stream()
                    .limit(100)
                    .collect(Collectors.toList());
                log.info("After word-by-word search, total unique results: {}", results.size());
            }
        }
        
        // 4. 如果所有特定搜索都无结果，尝试全文搜索
        if (results.isEmpty() && !criteria.hasItemCode()) {
            // 优先使用原始查询（包含完整的搜索文本，如 "AIR LIQUIDE SINGAPORE PRIVATE LIMITED"）
            String searchKeyword = null;
            
            // 优先使用原始查询，因为它包含完整的搜索文本
            if (criteria.getRawQuery() != null && !criteria.getRawQuery().trim().isEmpty()) {
                searchKeyword = criteria.getRawQuery().trim();
            }
            // 如果没有原始查询，尝试从其他字段提取
            else if (criteria.hasItemNameKeyword()) {
                searchKeyword = criteria.getItemNameKeyword();
            } else if (criteria.hasCategory()) {
                searchKeyword = criteria.getProductHierarchy3();
            } else if (criteria.hasFunction()) {
                searchKeyword = criteria.getFunction();
            } else if (criteria.hasBrand()) {
                searchKeyword = criteria.getBrandCode();
            }
            
            if (searchKeyword != null && !searchKeyword.trim().isEmpty() && searchKeyword.trim().length() > 1) {
                log.info("No results from specific search, trying full-text search with keyword: '{}'", searchKeyword);
                results = salesDataRepository.fullTextSearch(searchKeyword.trim(), 100);
                log.info("Full-text search found {} results", results.size());
            }
        }
        
        // 5. 应用价格和日期过滤
        int beforeFilterCount = results.size();
        if (!results.isEmpty()) {
            results = applyFilters(results, criteria);
            log.info("After filtering: {} results (filtered out: {})", 
                results.size(), beforeFilterCount - results.size());
        }
        
        log.info("Total results found: {}", results.size());
        if (results.isEmpty() && beforeFilterCount > 0) {
            log.warn("All results were filtered out! Criteria: hasPriceRange={}, hasDateRange={}, minPrice={}, maxPrice={}, startDate={}, endDate={}",
                criteria.hasPriceRange(), criteria.hasDateRange(), 
                criteria.getMinPrice(), criteria.getMaxPrice(), 
                criteria.getStartDate(), criteria.getEndDate());
        }
        return results;
    }
    
    /**
     * 多关键词搜索（支持多个关键词在多个字段中搜索）
     * 每个关键词必须在至少一个字段中匹配，所有关键词都必须匹配（AND 逻辑）
     */
    private List<SalesData> performMultiKeywordSearch(List<String> keywords) {
        log.info("Performing multi-keyword search with keywords: {}", keywords);
        
        // 限制最多5个关键词（与 Repository 方法参数匹配）
        List<String> limitedKeywords = keywords.stream()
            .limit(5)
            .collect(Collectors.toList());
        
        // 如果关键词少于5个，用 null 填充
        while (limitedKeywords.size() < 5) {
            limitedKeywords.add(null);
        }
        
        return salesDataRepository.searchByMultipleKeywords(
            limitedKeywords.get(0),
            limitedKeywords.get(1),
            limitedKeywords.get(2),
            limitedKeywords.get(3),
            limitedKeywords.get(4),
            100
        );
    }
    
    /**
     * 组合搜索
     */
    private List<SalesData> performCombinedSearch(MaterialSearchCriteria criteria) {
        log.info("Performing combined search with criteria: buyerName={}, buyerCode={}, productHierarchy3={}, function={}, brandCode={}, itemNameKeyword={}",
            criteria.getBuyerName(), criteria.getBuyerCode(), criteria.getProductHierarchy3(), 
            criteria.getFunction(), criteria.getBrandCode(), criteria.getItemNameKeyword());
        
        return salesDataRepository.searchByCombinedCriteria(
            criteria.getItemNameKeyword(),
            criteria.getProductHierarchy3(),
            criteria.getFunction(),
            criteria.getBrandCode(),
            criteria.getBuyerName(),
            criteria.getBuyerCode(),
            criteria.getMinPrice(),
            criteria.getMaxPrice(),
            criteria.getStartDate() != null ? criteria.getStartDate().toString() : null,
            criteria.getEndDate() != null ? criteria.getEndDate().toString() : null,
            100
        );
    }
    
    /**
     * 应用价格和日期过滤
     */
    private List<SalesData> applyFilters(List<SalesData> results, MaterialSearchCriteria criteria) {
        return results.stream()
            .filter(item -> {
                // 价格过滤（优先使用 Unit Cost，如果没有则使用 TXP1）
                if (criteria.hasPriceRange()) {
                    boolean priceMatch = false;
                    
                    // 尝试使用 Unit Cost
                    if (item.getUnitCost() != null && !item.getUnitCost().trim().isEmpty()) {
                        try {
                            BigDecimal unitCost = new BigDecimal(item.getUnitCost());
                            boolean minOk = criteria.getMinPrice() == null || unitCost.compareTo(criteria.getMinPrice()) >= 0;
                            boolean maxOk = criteria.getMaxPrice() == null || unitCost.compareTo(criteria.getMaxPrice()) <= 0;
                            if (minOk && maxOk) {
                                priceMatch = true;
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse Unit Cost: {}", item.getUnitCost());
                        }
                    }
                    
                    // 如果 Unit Cost 不匹配或为空，尝试使用 TXP1
                    if (!priceMatch && item.getTxP1() != null && !item.getTxP1().trim().isEmpty()) {
                        try {
                            BigDecimal price = new BigDecimal(item.getTxP1());
                            boolean minOk = criteria.getMinPrice() == null || price.compareTo(criteria.getMinPrice()) >= 0;
                            boolean maxOk = criteria.getMaxPrice() == null || price.compareTo(criteria.getMaxPrice()) <= 0;
                            if (minOk && maxOk) {
                                priceMatch = true;
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse TXP1: {}", item.getTxP1());
                        }
                    }
                    
                    if (!priceMatch) {
                        return false;
                    }
                }
                
                // 日期过滤
                if (criteria.hasDateRange()) {
                    try {
                        LocalDate txDate = parseDate(item.getTxDate());
                        if (txDate != null) {
                            if (criteria.getStartDate() != null && txDate.isBefore(criteria.getStartDate())) {
                                return false;
                            }
                            if (criteria.getEndDate() != null && txDate.isAfter(criteria.getEndDate())) {
                                return false;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse date: {}", item.getTxDate());
                    }
                }
                
                return true;
            })
            .collect(Collectors.toList());
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        String[] formats = {"yyyy-MM-dd", "yyyy/MM/dd", "dd/MM/yyyy", "MM/dd/yyyy"};
        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDate.parse(dateStr.split(" ")[0], formatter);
            } catch (DateTimeParseException e) {
                // 继续尝试下一个格式
            }
        }
        return null;
    }
    
    /**
     * 获取物料的历史交易统计
     */
    public MaterialHistoryStats getMaterialHistory(String itemCode) {
        List<SalesData> history = salesDataRepository.findByItemCode(itemCode);
        
        if (history.isEmpty()) {
            return null;
        }
        
        MaterialHistoryStats stats = new MaterialHistoryStats();
        stats.setItemCode(itemCode);
        stats.setItemName(history.get(0).getItemName());
        stats.setTotalTransactions(history.size());
        
        // 计算价格区间
        List<BigDecimal> prices = history.stream()
            .map(item -> {
                try {
                    return new BigDecimal(item.getTxP1());
                } catch (Exception e) {
                    return BigDecimal.ZERO;
                }
            })
            .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.toList());
        
        if (!prices.isEmpty()) {
            stats.setMinPrice(prices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            stats.setMaxPrice(prices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            stats.setAvgPrice(prices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(prices.size()), 2, java.math.RoundingMode.HALF_UP));
        }
        
        // 获取最早和最晚交易日期
        List<LocalDate> dates = history.stream()
            .map(item -> parseDate(item.getTxDate()))
            .filter(date -> date != null)
            .collect(Collectors.toList());
        
        if (!dates.isEmpty()) {
            stats.setFirstTransactionDate(dates.stream().min(LocalDate::compareTo).orElse(null));
            stats.setLastTransactionDate(dates.stream().max(LocalDate::compareTo).orElse(null));
        }
        
        stats.setHistory(history);
        
        return stats;
    }
    
    /**
     * 物料历史统计
     */
    public static class MaterialHistoryStats {
        private String itemCode;
        private String itemName;
        private int totalTransactions;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private BigDecimal avgPrice;
        private LocalDate firstTransactionDate;
        private LocalDate lastTransactionDate;
        private List<SalesData> history;
        
        // Getters and Setters
        public String getItemCode() { return itemCode; }
        public void setItemCode(String itemCode) { this.itemCode = itemCode; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public int getTotalTransactions() { return totalTransactions; }
        public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }
        public BigDecimal getMinPrice() { return minPrice; }
        public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
        public BigDecimal getMaxPrice() { return maxPrice; }
        public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
        public BigDecimal getAvgPrice() { return avgPrice; }
        public void setAvgPrice(BigDecimal avgPrice) { this.avgPrice = avgPrice; }
        public LocalDate getFirstTransactionDate() { return firstTransactionDate; }
        public void setFirstTransactionDate(LocalDate firstTransactionDate) { this.firstTransactionDate = firstTransactionDate; }
        public LocalDate getLastTransactionDate() { return lastTransactionDate; }
        public void setLastTransactionDate(LocalDate lastTransactionDate) { this.lastTransactionDate = lastTransactionDate; }
        public List<SalesData> getHistory() { return history; }
        public void setHistory(List<SalesData> history) { this.history = history; }
    }
}

