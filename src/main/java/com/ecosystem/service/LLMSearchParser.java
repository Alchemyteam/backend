package com.ecosystem.service;

import com.ecosystem.dto.chat.MaterialSearchCriteria;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 使用 LLM 解析自然语言查询，提取结构化搜索条件
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LLMSearchParser {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String llmApiUrl;

    @Value("${llm.api.key:}")
    private String llmApiKey;

    @Value("${llm.model:gemini-pro}")
    private String llmModel;

    /**
     * 从自然语言查询中提取搜索条件
     */
    public MaterialSearchCriteria parseSearchQuery(String userQuery) {
        log.info("Parsing search query: {}", userQuery);
        
        MaterialSearchCriteria criteria = new MaterialSearchCriteria();
        criteria.setRawQuery(userQuery); // 保存原始查询，用于全文搜索回退
        
        // 1. 先尝试规则匹配（快速且准确）
        criteria = applyRuleBasedParsing(userQuery, criteria);
        
        // 2. 如果规则匹配不够，使用 LLM 解析
        if (needsLLMParsing(criteria)) {
            criteria = applyLLMParsing(userQuery, criteria);
        }
        
        log.info("Parsed criteria: {}", criteria);
        return criteria;
    }
    
    /**
     * 基于规则的解析（快速、准确）
     */
    private MaterialSearchCriteria applyRuleBasedParsing(String query, MaterialSearchCriteria criteria) {
        String lowerQuery = query.toLowerCase();
        
        // 0. 如果查询包含 "+" 或 "and"，先拆分并分别解析每个部分
        if (query.contains("+") || lowerQuery.contains(" and ") || lowerQuery.contains(" 和 ")) {
            log.info("Detected combined query with separator, splitting and parsing each part");
            String[] parts = query.split("\\s*[+]\\s*|\\s+and\\s+|\\s+和\\s+");
            log.info("Split query into {} parts: {}", parts.length, java.util.Arrays.toString(parts));
            
            // 对每个部分单独解析（创建新的 criteria 避免互相干扰）
            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty()) continue;
                
                // 优先检查是否是买家名称格式（在解析之前）
                boolean isBuyerName = part.matches(".*\\b(LIMITED|PRIVATE|COMPANY|CORP|INC|LLC|PTE|LTD|SINGAPORE|SINGAPORE PRIVATE)\\b.*") || 
                                     part.length() > 20;
                
                if (isBuyerName && !criteria.hasBuyerName()) {
                    criteria.setBuyerName(part);
                    log.info("Detected BuyerName from part (before parsing): {}", criteria.getBuyerName());
                    continue; // 跳过后续解析，直接作为买家名称
                }
                
                MaterialSearchCriteria partCriteria = new MaterialSearchCriteria();
                partCriteria.setRawQuery(part);
                // 递归解析这个部分（但不检查 "+"，避免无限递归）
                partCriteria = parseSingleQuery(part, partCriteria);
                
                // 合并结果到主 criteria（支持多个相同类型的条件，取第一个）
                if (partCriteria.hasItemCode() && !criteria.hasItemCode()) {
                    criteria.setItemCode(partCriteria.getItemCode());
                }
                if (partCriteria.hasCategory() && !criteria.hasCategory()) {
                    criteria.setProductHierarchy3(partCriteria.getProductHierarchy3());
                }
                if (partCriteria.hasFunction() && !criteria.hasFunction()) {
                    criteria.setFunction(partCriteria.getFunction());
                }
                if (partCriteria.hasBrand() && !criteria.hasBrand()) {
                    criteria.setBrandCode(partCriteria.getBrandCode());
                }
                if (partCriteria.hasItemNameKeyword() && !criteria.hasItemNameKeyword()) {
                    criteria.setItemNameKeyword(partCriteria.getItemNameKeyword());
                }
                
                // 如果部分没有被识别为任何特定类型，尝试作为买家名称或物料名称关键字
                if (!partCriteria.hasItemCode() && !partCriteria.hasCategory() && 
                    !partCriteria.hasFunction() && !partCriteria.hasBrand() && 
                    !partCriteria.hasItemNameKeyword()) {
                    // 检查是否是买家名称格式
                    boolean isBuyerNameFormat = part.matches(".*\\b(LIMITED|PRIVATE|COMPANY|CORP|INC|LLC|PTE|LTD|SINGAPORE|SINGAPORE PRIVATE)\\b.*") || 
                                               part.length() > 15;
                    
                    if (isBuyerNameFormat && !criteria.hasBuyerName()) {
                        criteria.setBuyerName(part);
                        log.info("Detected BuyerName from part (after parsing): {}", criteria.getBuyerName());
                    } else if (!criteria.hasItemNameKeyword()) {
                        // 否则作为物料名称关键字（用于在 ItemName 字段中搜索）
                        criteria.setItemNameKeyword(part);
                        log.info("Extracted ItemName keyword from part: {}", criteria.getItemNameKeyword());
                    }
                }
            }
            
            // 组合查询解析完成，直接返回
            return criteria;
        }
        
        // 单一查询解析（没有 "+" 分隔符）
        return parseSingleQuery(query, criteria);
    }
    
    /**
     * 解析单一查询（不包含 "+" 分隔符）
     */
    private MaterialSearchCriteria parseSingleQuery(String query, MaterialSearchCriteria criteria) {
        String lowerQuery = query.toLowerCase();
        
        // 1. 检测物料编码（通常以字母开头，包含数字，如 TI00040）
        String itemCodePattern = "\\b([A-Z]{2,}\\d{3,})\\b";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(itemCodePattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(query);
        if (matcher.find()) {
            criteria.setItemCode(matcher.group(1).toUpperCase());
            criteria.setSearchType(MaterialSearchCriteria.SearchType.EXACT_ITEM_CODE);
            log.info("Detected ItemCode: {}", criteria.getItemCode());
            return criteria;
        }
        
        // 2. 检测品牌代码
        // 2.1 检测 "from X"、"by X"、"products from X"、"items from X" 等模式
        String[] brandPatterns = {
            "from\\s+([A-Z]{2,}(?:\\s+[A-Z]{2,})?)",  // "from AET", "from AIR LIQUIDE"
            "by\\s+([A-Z]{2,}(?:\\s+[A-Z]{2,})?)",    // "by AET", "by AIR LIQUIDE"
            "products\\s+from\\s+([A-Z]{2,}(?:\\s+[A-Z]{2,})?)",  // "products from AET"
            "items\\s+from\\s+([A-Z]{2,}(?:\\s+[A-Z]{2,})?)",     // "items from AET"
            "product\\s+from\\s+([A-Z]{2,}(?:\\s+[A-Z]{2,})?)",   // "product from AET"
            "item\\s+from\\s+([A-Z]{2,}(?:\\s+[A-Z]{2,})?)"      // "item from AET"
        };
        
        for (String patternStr : brandPatterns) {
            java.util.regex.Pattern brandPattern = java.util.regex.Pattern.compile(patternStr, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher brandMatcher = brandPattern.matcher(query);
            if (brandMatcher.find()) {
                String brand = brandMatcher.group(1).trim().toUpperCase();
                // 排除常见的非品牌词
                if (!brand.equals("ALL") && !brand.equals("THE") && !brand.equals("AND") && 
                    !brand.equals("OR") && !brand.equals("FOR") && brand.length() >= 2) {
                    criteria.setBrandCode(brand);
                    log.info("Detected Brand Code from pattern '{}': {}", patternStr, criteria.getBrandCode());
                    break;
                }
            }
        }
        
        // 2.2 检测 "品牌"、"brand" 等词之后的品牌代码
        if (!criteria.hasBrand()) {
            String[] brandKeywords = {"brand", "品牌", "牌子"};
            for (String keyword : brandKeywords) {
                int index = lowerQuery.indexOf(keyword);
                if (index >= 0) {
                    String afterBrand = query.substring(index + keyword.length()).trim();
                    // 提取品牌代码（通常是3-5个大写字母）
                    java.util.regex.Pattern brandPattern = java.util.regex.Pattern.compile("\\b([A-Z]{2,10})\\b");
                    java.util.regex.Matcher brandMatcher = brandPattern.matcher(afterBrand);
                    if (brandMatcher.find()) {
                        criteria.setBrandCode(brandMatcher.group(1));
                        log.info("Detected Brand Code after keyword '{}': {}", keyword, criteria.getBrandCode());
                        break;
                    }
                }
            }
        }
        
        // 2.2 检测常见的品牌名称（如 "AIR LIQUIDE"）
        String[] knownBrands = {"AIR LIQUIDE", "AET", "FLUKE", "3M", "HONEYWELL"};
        for (String brand : knownBrands) {
            if (query.toUpperCase().contains(brand)) {
                criteria.setBrandCode(brand);
                log.info("Detected known brand: {}", brand);
                break;
            }
        }
        
        // 2.3 如果没有找到，尝试提取所有大写字母组合（可能是品牌名称，如 "AIR LIQUIDE"）
        if (!criteria.hasBrand()) {
            // 先尝试匹配多个单词的大写组合（如 "AIR LIQUIDE"）
            java.util.regex.Pattern multiWordBrandPattern = java.util.regex.Pattern.compile("\\b([A-Z]{2,}\\s+[A-Z]{2,}(?:\\s+[A-Z]{2,})?)\\b");
            java.util.regex.Matcher multiWordMatcher = multiWordBrandPattern.matcher(query);
            while (multiWordMatcher.find()) {
                String potentialBrand = multiWordMatcher.group(1);
                // 排除常见的非品牌词
                if (!potentialBrand.contains("AND") && !potentialBrand.contains("OR") && 
                    !potentialBrand.contains("THE") && !potentialBrand.contains("FOR") &&
                    !potentialBrand.contains("TOOL") && !potentialBrand.contains("COST") &&
                    potentialBrand.length() >= 4) {
                    criteria.setBrandCode(potentialBrand);
                    log.info("Detected potential multi-word brand from caps: {}", potentialBrand);
                    break;
                }
            }
            
            // 如果没有找到多词品牌，尝试单词大写组合
            if (!criteria.hasBrand()) {
                java.util.regex.Pattern allCapsPattern = java.util.regex.Pattern.compile("\\b([A-Z]{2,})\\b");
                java.util.regex.Matcher allCapsMatcher = allCapsPattern.matcher(query);
                while (allCapsMatcher.find()) {
                    String potentialBrand = allCapsMatcher.group(1);
                    // 排除常见的非品牌词
                    if (!potentialBrand.equals("AND") && !potentialBrand.equals("OR") && 
                        !potentialBrand.equals("THE") && !potentialBrand.equals("FOR") &&
                        !potentialBrand.equals("TOOL") && !potentialBrand.equals("COST") &&
                        potentialBrand.length() >= 2) {
                        criteria.setBrandCode(potentialBrand);
                        log.info("Detected potential brand from caps: {}", potentialBrand);
                        break;
                    }
                }
            }
        }
        
        // 3. 检测品类关键词
        Map<String, String> categoryKeywords = new HashMap<>();
        categoryKeywords.put("site safety equipment", "Site Safety Equipment");
        categoryKeywords.put("site safety", "Site Safety Equipment");
        categoryKeywords.put("safety equipment", "Site Safety Equipment");
        categoryKeywords.put("安全设备", "Site Safety Equipment");
        categoryKeywords.put("filters", "Filters");
        categoryKeywords.put("过滤器", "Filters");
        categoryKeywords.put("maintenance chemicals", "Maintenance Chemicals");
        categoryKeywords.put("维护化学品", "Maintenance Chemicals");
        categoryKeywords.put("cutting tool", "Cutting Tool");
        categoryKeywords.put("cutting tools", "Cutting Tool");
        categoryKeywords.put("切削工具", "Cutting Tool");
        categoryKeywords.put("electrical accessories", "Electrical Accessories");
        categoryKeywords.put("electrical", "Electrical Accessories");
        categoryKeywords.put("电气配件", "Electrical Accessories");
        
        // 优先匹配完整的关键词（更长的关键词优先）
        List<Map.Entry<String, String>> sortedKeywords = categoryKeywords.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
            .collect(Collectors.toList());
        
        for (Map.Entry<String, String> entry : sortedKeywords) {
            if (lowerQuery.contains(entry.getKey())) {
                criteria.setProductHierarchy3(entry.getValue());
                log.info("Detected Category: '{}' (matched keyword: '{}' from query: '{}')", 
                    criteria.getProductHierarchy3(), entry.getKey(), query);
                break;
            }
        }
        
        // 如果关键词映射中没有找到，但查询看起来像是一个品类名称（首字母大写，多个单词）
        if (!criteria.hasCategory() && !criteria.hasBuyerName()) {
            // 检查是否是完整的品类名称（如 "Site Safety Equipment" 或 "Electrical Accessories"）
            String trimmedQuery = query.trim();
            
            // 先排除买家名称格式（避免误识别）
            boolean isBuyerNameFormat = trimmedQuery.matches(".*\\b(LIMITED|PRIVATE|COMPANY|CORP|INC|LLC|PTE|LTD|SINGAPORE|SINGAPORE PRIVATE)\\b.*") || 
                                       trimmedQuery.length() > 20;
            
            if (!isBuyerNameFormat && trimmedQuery.matches("^[A-Z][a-zA-Z\\s]+$") && trimmedQuery.split("\\s+").length >= 2) {
                // 可能是品类名称，直接使用
                criteria.setProductHierarchy3(trimmedQuery);
                log.info("Using query as category name: '{}'", criteria.getProductHierarchy3());
            } else {
                log.debug("Query does not match category pattern or is buyer name format. Pattern match: {}, Word count: {}, Is buyer name: {}", 
                    trimmedQuery.matches("^[A-Z][a-zA-Z\\s]+$"), trimmedQuery.split("\\s+").length, isBuyerNameFormat);
            }
        }
        
        // 4. 检测功能关键词（只有在没有识别到品类，或者品类名称不包含该功能关键词时才识别）
        Map<String, String> functionKeywords = new HashMap<>();
        functionKeywords.put("maintenance chemicals", "Maintenance Chemicals");
        functionKeywords.put("safety", "Safety");
        functionKeywords.put("protection", "Protection");
        functionKeywords.put("cutting", "Cutting");
        functionKeywords.put("cutting tool", "Cutting Tool");
        functionKeywords.put("cutting tools", "Cutting Tool");
        
        // 如果已经识别到品类，检查功能关键词是否是品类名称的一部分
        boolean shouldSkipFunction = false;
        if (criteria.hasCategory()) {
            String categoryLower = criteria.getProductHierarchy3().toLowerCase();
            // 如果功能关键词是品类名称的一部分，跳过功能识别
            for (String funcKey : functionKeywords.keySet()) {
                if (categoryLower.contains(funcKey) && funcKey.length() > 2) {
                    shouldSkipFunction = true;
                    log.debug("Skipping function detection for '{}' as it's part of category '{}'", 
                        funcKey, criteria.getProductHierarchy3());
                    break;
                }
            }
        }
        
        if (!shouldSkipFunction) {
            for (Map.Entry<String, String> entry : functionKeywords.entrySet()) {
                if (lowerQuery.contains(entry.getKey())) {
                    // 再次检查：如果品类名称包含这个功能关键词，跳过
                    if (criteria.hasCategory()) {
                        String categoryLower = criteria.getProductHierarchy3().toLowerCase();
                        if (categoryLower.contains(entry.getKey()) && entry.getKey().length() > 2) {
                            log.debug("Skipping function '{}' as it's part of category '{}'", 
                                entry.getValue(), criteria.getProductHierarchy3());
                            continue;
                        }
                    }
                    criteria.setFunction(entry.getValue());
                    log.info("Detected Function: {}", criteria.getFunction());
                    break;
                }
            }
        }
        
        // 5. 检测时间关键词
        if (lowerQuery.contains("去年") || lowerQuery.contains("last year")) {
            LocalDate now = LocalDate.now();
            criteria.setStartDate(now.minusYears(1).withDayOfYear(1));
            criteria.setEndDate(now.minusYears(1).withMonth(12).withDayOfMonth(31));
            log.info("Detected date range: last year");
        } else if (lowerQuery.contains("今年") || lowerQuery.contains("this year")) {
            LocalDate now = LocalDate.now();
            criteria.setStartDate(now.withDayOfYear(1));
            criteria.setEndDate(now);
            log.info("Detected date range: this year");
        }
        
        // 6. 检测价格区间关键词
        // 6.1 检测 "unit cost"、"单价"、"价格" 等关键词后的数字
        String[] priceKeywords = {"unit cost", "单价", "价格", "price", "cost"};
        boolean foundPriceKeyword = false;
        for (String priceKeyword : priceKeywords) {
            if (lowerQuery.contains(priceKeyword)) {
                foundPriceKeyword = true;
                break;
            }
        }
        
        // 6.2 提取价格区间（支持多种格式：0-100, 0 到 100, 0~100, 0至100, unit cost 0-100）
        // 先尝试在价格关键词附近查找
        if (foundPriceKeyword) {
            // 在价格关键词后查找数字区间
            java.util.regex.Pattern priceAfterKeywordPattern = java.util.regex.Pattern.compile(
                "(?i)(?:unit cost|单价|价格|price|cost)\\s+(\\d+(?:\\.\\d+)?)\\s*(?:到|-|~|至|to)\\s*(\\d+(?:\\.\\d+)?)"
            );
            java.util.regex.Matcher priceAfterKeywordMatcher = priceAfterKeywordPattern.matcher(query);
            if (priceAfterKeywordMatcher.find()) {
                try {
                    criteria.setMinPrice(new BigDecimal(priceAfterKeywordMatcher.group(1)));
                    criteria.setMaxPrice(new BigDecimal(priceAfterKeywordMatcher.group(2)));
                    log.info("Detected price range after keyword: {} - {}", criteria.getMinPrice(), criteria.getMaxPrice());
                } catch (Exception e) {
                    log.warn("Failed to parse price range after keyword", e);
                }
            }
        }
        
        // 如果没有找到，尝试通用格式
        if (!criteria.hasPriceRange()) {
            java.util.regex.Pattern pricePattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:到|-|~|至|to)\\s*(\\d+(?:\\.\\d+)?)");
            java.util.regex.Matcher priceMatcher = pricePattern.matcher(query);
            if (priceMatcher.find()) {
                try {
                    criteria.setMinPrice(new BigDecimal(priceMatcher.group(1)));
                    criteria.setMaxPrice(new BigDecimal(priceMatcher.group(2)));
                    log.info("Detected price range: {} - {}", criteria.getMinPrice(), criteria.getMaxPrice());
                } catch (Exception e) {
                    log.warn("Failed to parse price range", e);
                }
            }
        }
        
        // 6.3 如果没有找到价格区间，但找到了价格关键词，尝试提取单个数字（作为最大值）
        if (foundPriceKeyword && !criteria.hasPriceRange()) {
            java.util.regex.Pattern singlePricePattern = java.util.regex.Pattern.compile("(?:unit cost|单价|价格|price|cost)\\s*(?:<|小于|less than|under|below)?\\s*(\\d+(?:\\.\\d+)?)");
            java.util.regex.Matcher singlePriceMatcher = singlePricePattern.matcher(lowerQuery);
            if (singlePriceMatcher.find()) {
                try {
                    criteria.setMaxPrice(new BigDecimal(singlePriceMatcher.group(1)));
                    criteria.setMinPrice(BigDecimal.ZERO);
                    log.info("Detected max price: {}", criteria.getMaxPrice());
                } catch (Exception e) {
                    log.warn("Failed to parse single price", e);
                }
            }
        }
        
        // 7. 如果没有明确的搜索类型，提取物料名称关键字
        // 注意：只有在没有识别到品类、功能、品牌的情况下，才提取物料名称关键字
        // 这样可以避免将品类名称误识别为物料名称关键字
        if (!criteria.hasItemCode() && !criteria.hasCategory() && !criteria.hasFunction() && !criteria.hasBrand()) {
            // 移除常见词汇，提取产品名称
            String[] commonWords = {"find", "search", "show", "list", "get", "for", "the", "a", "an", 
                                    "找", "搜索", "显示", "列出", "获取", "的", "一个", "can", "you", "please",
                                    "what", "where", "how", "when", "why", "is", "are", "was", "were"};
            String cleanedQuery = query;
            for (String word : commonWords) {
                cleanedQuery = cleanedQuery.replaceAll("\\b" + word + "\\b", " ").trim();
            }
            // 清理多余的空格
            cleanedQuery = cleanedQuery.replaceAll("\\s+", " ").trim();
            if (!cleanedQuery.isEmpty() && cleanedQuery.length() > 1) {
                criteria.setItemNameKeyword(cleanedQuery);
                criteria.setSearchType(MaterialSearchCriteria.SearchType.ITEM_NAME_FUZZY);
                log.info("Extracted ItemName keyword: {}", criteria.getItemNameKeyword());
            }
        }
        
        // 8. 如果仍然没有识别到任何条件，检查是否是买家名称格式
        if (!criteria.hasItemCode() && !criteria.hasCategory() && !criteria.hasFunction() && 
            !criteria.hasBrand() && !criteria.hasItemNameKeyword() && !criteria.hasBuyerName()) {
            // 检查是否是买家名称格式（通常包含公司相关词汇或长度较长）
            if (query.matches(".*\\b(LIMITED|PRIVATE|COMPANY|CORP|INC|LLC|PTE|LTD|SINGAPORE)\\b.*") || 
                query.length() > 15) {
                criteria.setBuyerName(query.trim());
                log.info("Detected BuyerName from query: {}", criteria.getBuyerName());
            }
        }
        
        // 9. 处理非 "+" 分隔的组合查询（剩余部分提取）
        if ((criteria.hasBrand() || criteria.hasCategory() || criteria.hasFunction() || criteria.hasPriceRange()) 
            && !criteria.hasItemNameKeyword() && !criteria.hasBuyerName()) {
            // 处理非 "+" 分隔的组合查询
            String remainingQuery = query;
            
            // 移除品牌名称（支持空格分隔的品牌名称，如 "AIR LIQUIDE"）
            if (criteria.hasBrand()) {
                String brandPattern = criteria.getBrandCode().replace(" ", "\\s+");
                remainingQuery = remainingQuery.replaceAll("(?i)\\b" + brandPattern + "\\b", "").trim();
            }
            
            // 移除品类关键词
            if (criteria.hasCategory()) {
                for (Map.Entry<String, String> entry : categoryKeywords.entrySet()) {
                    if (lowerQuery.contains(entry.getKey())) {
                        remainingQuery = remainingQuery.replaceAll("(?i)\\b" + entry.getKey().replace(" ", "\\s+") + "\\b", "").trim();
                    }
                }
            }
            
            // 移除功能关键词
            if (criteria.hasFunction()) {
                for (Map.Entry<String, String> entry : functionKeywords.entrySet()) {
                    if (lowerQuery.contains(entry.getKey())) {
                        remainingQuery = remainingQuery.replaceAll("(?i)\\b" + entry.getKey().replace(" ", "\\s+") + "\\b", "").trim();
                    }
                }
            }
            
            // 移除价格相关文本（包括 "unit cost 0-100" 这样的格式）
            remainingQuery = remainingQuery.replaceAll("(?i)(unit cost|单价|价格|price|cost)\\s*\\d+\\s*(?:到|-|~|至|to)\\s*\\d+", "").trim();
            remainingQuery = remainingQuery.replaceAll("(?i)\\d+\\s*(?:到|-|~|至|to)\\s*\\d+", "").trim();
            
            // 移除连接词
            remainingQuery = remainingQuery.replaceAll("(?i)\\b(and|or|\\+|和|或)\\b", "").trim();
            
            // 清理多余空格
            remainingQuery = remainingQuery.replaceAll("\\s+", " ").trim();
            
            if (!remainingQuery.isEmpty() && remainingQuery.length() > 1) {
                // 排除常见的查询动词和介词（如 "Show all products from"）
                String trimmed = remainingQuery.trim();
                boolean isQueryVerb = trimmed.toLowerCase().matches(".*\\b(show|all|products|from|items|find|search|list|get|display|by|with|the|a|an)\\b.*");
                
                // 检查是否是买家名称格式（必须包含公司标识词，且不是查询动词）
                if (!isQueryVerb && trimmed.matches(".*\\b(LIMITED|PRIVATE|COMPANY|CORP|INC|LLC|PTE|LTD|SINGAPORE|SINGAPORE PRIVATE)\\b.*")) {
                    criteria.setBuyerName(trimmed);
                    log.info("Detected BuyerName from remaining query: {}", criteria.getBuyerName());
                } else if (!isQueryVerb && trimmed.length() > 2) {
                    // 排除查询动词后，作为物料名称关键字
                    criteria.setItemNameKeyword(trimmed);
                    log.info("Extracted ItemName keyword from combined query: {}", criteria.getItemNameKeyword());
                } else {
                    log.debug("Skipping remaining query '{}' as it appears to be query verbs, not a product name or buyer name", trimmed);
                }
            }
        }
        
        // 9. 如果仍然没有设置搜索类型，将整个查询作为物料名称关键字（用于全文搜索回退）
        if (!criteria.hasItemCode() && !criteria.hasItemNameKeyword() && 
            !criteria.hasCategory() && !criteria.hasFunction() && !criteria.hasBrand()) {
            // 如果查询看起来像是一个产品名称（不是问句，长度合理）
            String trimmedQuery = query.trim();
            if (trimmedQuery.length() > 2 && trimmedQuery.length() < 200 && 
                !trimmedQuery.endsWith("?") && !trimmedQuery.toLowerCase().startsWith("how") &&
                !trimmedQuery.toLowerCase().startsWith("what") && !trimmedQuery.toLowerCase().startsWith("why")) {
                criteria.setItemNameKeyword(trimmedQuery);
                criteria.setSearchType(MaterialSearchCriteria.SearchType.ITEM_NAME_FUZZY);
                log.info("Using entire query as ItemName keyword for full-text search: {}", criteria.getItemNameKeyword());
            }
        }
        
        // 10. 确定搜索类型
        if (criteria.isCombinedSearch()) {
            criteria.setSearchType(MaterialSearchCriteria.SearchType.COMBINED);
            log.info("Search type set to COMBINED. Conditions: itemCode={}, category={}, function={}, brand={}, buyerName={}, buyerCode={}, itemNameKeyword={}",
                criteria.hasItemCode(), criteria.hasCategory(), criteria.hasFunction(), 
                criteria.hasBrand(), criteria.hasBuyerName(), criteria.hasBuyerCode(), criteria.hasItemNameKeyword());
        } else if (criteria.hasItemCode()) {
            criteria.setSearchType(MaterialSearchCriteria.SearchType.EXACT_ITEM_CODE);
        } else if (criteria.hasItemNameKeyword()) {
            if (criteria.getSearchType() == null) {
                criteria.setSearchType(MaterialSearchCriteria.SearchType.ITEM_NAME_FUZZY);
            }
        } else if (criteria.hasCategory()) {
            criteria.setSearchType(MaterialSearchCriteria.SearchType.CATEGORY);
        } else if (criteria.hasFunction()) {
            criteria.setSearchType(MaterialSearchCriteria.SearchType.FUNCTION);
        } else if (criteria.hasBrand()) {
            criteria.setSearchType(MaterialSearchCriteria.SearchType.BRAND);
        } else if (criteria.hasBuyerName() || criteria.hasBuyerCode()) {
            // 如果只有买家名称或买家代码，也算组合搜索（会使用全文搜索）
            criteria.setSearchType(MaterialSearchCriteria.SearchType.COMBINED);
        }
        
        return criteria;
    }
    
    /**
     * 判断是否需要使用 LLM 解析
     */
    private boolean needsLLMParsing(MaterialSearchCriteria criteria) {
        // 如果已经识别出明确的搜索条件，就不需要 LLM
        if (criteria.hasItemCode()) {
            return false;
        }
        // 如果查询很复杂（包含多个条件），可能需要 LLM 帮助
        // 或者如果没有任何识别到的条件，使用 LLM 帮助解析
        return criteria.isCombinedSearch() || 
               (!criteria.hasItemNameKeyword() && !criteria.hasCategory() && !criteria.hasFunction() && 
                !criteria.hasBrand() && !criteria.hasBuyerName() && !criteria.hasBuyerCode());
    }
    
    /**
     * 强制使用 LLM 解析查询（用于搜索无结果时的回退）
     */
    public MaterialSearchCriteria parseSearchQueryWithLLM(String userQuery) {
        log.info("Asking LLM expert (construction materials specialist) to understand query and suggest search strategy: {}", userQuery);
        
        MaterialSearchCriteria criteria = new MaterialSearchCriteria();
        criteria.setRawQuery(userQuery);
        
        // 先尝试使用 LLM 作为专家解析
        try {
            criteria = applyLLMParsing(userQuery, criteria);
            log.info("LLM expert suggested search criteria: {}", criteria);
            
            // 如果 LLM 返回的所有字段都是 null（LLM 调用失败或没有提取到关键词），回退到规则匹配
            if (!criteria.hasItemCode() && !criteria.hasItemNameKeyword() && !criteria.hasCategory() && 
                !criteria.hasFunction() && !criteria.hasBrand() && !criteria.hasBuyerName() && 
                !criteria.hasBuyerCode() && !criteria.hasPriceRange() && !criteria.hasDateRange()) {
                log.warn("LLM expert did not extract any keywords (likely API call failed), falling back to rule-based parsing");
                criteria = applyRuleBasedParsing(userQuery, criteria);
                log.info("Rule-based parsing result: {}", criteria);
            }
        } catch (Exception e) {
            log.error("LLM expert parsing failed, falling back to rule-based parsing", e);
            // 如果 LLM 解析失败，回退到规则匹配
            criteria = applyRuleBasedParsing(userQuery, criteria);
        }
        
        return criteria;
    }
    
    /**
     * 使用 LLM 解析复杂查询
     */
    private MaterialSearchCriteria applyLLMParsing(String query, MaterialSearchCriteria criteria) {
        if (llmApiKey == null || llmApiKey.isEmpty() || llmApiKey.trim().isEmpty()) {
            log.error("LLM API key not configured! Please set llm.api.key in application.yml. Skipping LLM parsing.");
            return criteria;
        }
        
        try {
            log.info("Asking LLM expert (construction materials specialist) to understand query: {}", query);
            String prompt = buildParsingPrompt(query);
            log.debug("LLM expert prompt: {}", prompt);
            
            String llmResponse = callLLM(prompt);
            log.info("LLM expert response received: {}", llmResponse != null ? 
                (llmResponse.length() > 200 ? llmResponse.substring(0, 200) + "..." : llmResponse) : "null");
            
            // 解析 LLM 返回的 JSON
            if (llmResponse != null && !llmResponse.trim().isEmpty()) {
                // 尝试提取 JSON 部分
                String jsonPart = extractJSON(llmResponse);
                log.info("Extracted JSON from LLM response: {}", jsonPart);
                
                if (jsonPart != null) {
                    Map<String, Object> parsed = objectMapper.readValue(jsonPart, Map.class);
                    log.info("Parsed LLM JSON: {}", parsed);
                    
                    // 检查是否所有字段都是 null（LLM 没有提取到关键词）
                    boolean allNull = parsed.values().stream().allMatch(v -> v == null || 
                        (v instanceof String && ((String) v).trim().isEmpty()) ||
                        (v instanceof String && "null".equalsIgnoreCase(((String) v).trim())));
                    
                    if (allNull) {
                        log.warn("LLM returned all null fields! This means LLM did not extract keywords from query: {}", query);
                        log.warn("LLM response was: {}", llmResponse);
                    } else {
                        log.info("LLM successfully extracted keywords from query: {}", query);
                    }
                    
                    updateCriteriaFromLLMResponse(criteria, parsed);
                    log.info("Criteria updated from LLM expert: {}", criteria);
                } else {
                    log.warn("Failed to extract JSON from LLM response. Full response: {}", llmResponse);
                }
            } else {
                log.warn("LLM returned empty response");
            }
        } catch (Exception e) {
            log.error("LLM parsing failed, using rule-based results", e);
        }
        
        return criteria;
    }
    
    /**
     * 构建 LLM 解析提示词 - 让 LLM 作为建筑领域专家，从自然语言中提取关键词并生成搜索策略
     */
    private String buildParsingPrompt(String userQuery) {
        return "你是一位专业的建筑材料和工程设备领域的专家。你的任务是从用户的自然语言查询中提取关键信息，然后从数据库中搜索相关的所有数据。\n\n" +
               "用户查询: \"" + userQuery + "\"\n\n" +
               "**重要：你必须从用户的查询中提取关键词，不要直接返回空结果！**\n\n" +
               "数据库包含以下字段，你可以使用这些字段来搜索：\n" +
               "- ItemCode: 物料编码（精确匹配，如 TI00040）\n" +
               "- ItemName: 物料名称（模糊搜索，如 \"Safety Shoes\"、\"安全鞋\"）\n" +
               "- BuyerName: 买家公司名称（如 \"AIR LIQUIDE SINGAPORE PRIVATE LIMITED\"）\n" +
               "- BuyerCode: 买家代码\n" +
               "- Product Hierarchy 3: 产品分类（如 \"Site Safety Equipment\"、\"Electrical Accessories\"、\"Filters\"、\"Maintenance Chemicals\"、\"Cutting Tool\"）\n" +
               "- Function: 功能分类（如 \"Maintenance Chemicals\"、\"Cutting Tool\"）\n" +
               "- Brand Code: 品牌代码（如 \"AIR LIQUIDE\"、\"AET\"、\"FLUKE\"）\n" +
               "- Unit Cost / TXP1: 价格（单位成本或交易价格）\n" +
               "- TXDate: 交易日期\n\n" +
               "**关键词提取示例：**\n" +
               "1. \"Show all products from AET\" → 提取 \"AET\" → brandCode: \"AET\"\n" +
               "2. \"Find Safety Shoes\" → 提取 \"Safety Shoes\" → itemNameKeyword: \"Safety Shoes\"\n" +
               "3. \"Show me Site Safety Equipment\" → 提取 \"Site Safety Equipment\" → productHierarchy3: \"Site Safety Equipment\"\n" +
               "4. \"Products by AIR LIQUIDE\" → 提取 \"AIR LIQUIDE\" → brandCode: \"AIR LIQUIDE\"\n" +
               "5. \"Show items from AIR LIQUIDE SINGAPORE PRIVATE LIMITED\" → 提取公司名称 → buyerName: \"AIR LIQUIDE SINGAPORE PRIVATE LIMITED\"\n" +
               "6. \"Find TI00040\" → 提取物料编码 → itemCode: \"TI00040\"\n" +
               "7. \"Show all Maintenance Chemicals\" → 提取 \"Maintenance Chemicals\" → function: \"Maintenance Chemicals\"\n\n" +
               "请仔细分析用户的查询，提取所有可能的关键词，然后返回一个 JSON 对象，包含你认为最合适的搜索条件。\n\n" +
               "返回格式（必须是有效的 JSON，不要包含其他文字或 Markdown 格式）：\n" +
               "{\n" +
               "  \"itemCode\": \"精确的物料编码（如果查询中提到了，如 TI00040），否则为 null\",\n" +
               "  \"itemNameKeyword\": \"物料名称关键词（用于模糊搜索，如 \"Safety Shoes\"、\"安全鞋\"），如果查询包含产品名称，提取关键词，否则为 null\",\n" +
               "  \"productHierarchy3\": \"产品分类名称（如 \"Site Safety Equipment\"、\"Electrical Accessories\"），如果查询提到了分类，否则为 null\",\n" +
               "  \"function\": \"功能分类（如 \"Maintenance Chemicals\"、\"Cutting Tool\"），如果查询提到了功能，否则为 null\",\n" +
               "  \"brandCode\": \"品牌代码（如 \"AIR LIQUIDE\"、\"AET\"、\"FLUKE\"），如果查询提到了品牌（如 \"from AET\"、\"by AIR LIQUIDE\"、\"products from AET\"），提取品牌名称，否则为 null\",\n" +
               "  \"buyerName\": \"买家公司名称（如 \"AIR LIQUIDE SINGAPORE PRIVATE LIMITED\"），如果查询提到了公司名称，否则为 null\",\n" +
               "  \"buyerCode\": \"买家代码，如果查询提到了，否则为 null\",\n" +
               "  \"minPrice\": 最低价格（数字，如果查询提到了价格范围，否则为 null）,\n" +
               "  \"maxPrice\": 最高价格（数字，如果查询提到了价格范围，否则为 null）,\n" +
               "  \"startDate\": \"开始日期（YYYY-MM-DD 格式，如果查询提到了日期范围，否则为 null）\",\n" +
               "  \"endDate\": \"结束日期（YYYY-MM-DD 格式，如果查询提到了日期范围，否则为 null）\"\n" +
               "}\n\n" +
               "**关键提取规则：**\n" +
               "1. **品牌识别**：如果查询包含 \"from X\"、\"by X\"、\"products from X\"、\"items from X\"，其中 X 是品牌名称（如 AET、AIR LIQUIDE），提取 X 并设置 brandCode\n" +
               "2. **产品名称**：如果查询包含产品名称（如 \"Safety Shoes\"、\"安全鞋\"），提取并设置 itemNameKeyword\n" +
               "3. **分类识别**：如果查询包含产品分类（如 \"Site Safety Equipment\"、\"Electrical Accessories\"），设置 productHierarchy3\n" +
               "4. **功能识别**：如果查询包含功能分类（如 \"Maintenance Chemicals\"、\"Cutting Tool\"），设置 function\n" +
               "5. **公司名称**：如果查询包含公司名称（包含 LIMITED、PRIVATE、COMPANY 等词），设置 buyerName\n" +
               "6. **物料编码**：如果查询包含物料编码格式（如 TI00040），设置 itemCode\n" +
               "7. **必须提取关键词**：即使查询是自然语言（如 \"Show all products from AET\"），也要提取 \"AET\" 并设置 brandCode，不要返回所有字段都是 null！\n" +
               "8. 只返回有效的 JSON，不要包含任何其他文字、说明或 Markdown 格式\n" +
               "9. 如果某个字段没有值，设置为 null（不要省略字段）";
    }
    
    /**
     * 调用 LLM API
     */
    private String callLLM(String prompt) {
        // 检查 API key 是否配置
        if (llmApiKey == null || llmApiKey.isEmpty() || llmApiKey.trim().isEmpty()) {
            log.error("LLM API key is not configured! Please set llm.api.key in application.yml");
            return null;
        }
        
        log.info("🔵 Calling LLM API: {} with model: {}", llmApiUrl, llmModel);
        log.info("🔵 API Key configured: {} (length: {})", 
            llmApiKey != null && !llmApiKey.isEmpty() ? "Yes" : "No",
            llmApiKey != null ? llmApiKey.length() : 0);
        
        try {
            // 构建 API URL: https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}
            String apiUrl = llmApiUrl + "/models/" + llmModel + ":generateContent?key=" + llmApiKey;
            log.info("🔵 LLM API URL: {}", apiUrl.replace(llmApiKey, "***"));
            log.info("🔵 Full API URL (for debugging): {}", apiUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            content.put("role", "user");
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            parts.add(part);
            content.put("parts", parts);
            contents.add(content);
            requestBody.put("contents", contents);
            
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.3); // 降低温度以获得更准确的解析
            generationConfig.put("maxOutputTokens", 1000); // 增加 token 限制，确保 JSON 完整返回
            requestBody.put("generationConfig", generationConfig);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                apiUrl, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);
            
            log.info("LLM API response status: {}", response != null ? response.getStatusCode() : "null");
            
            if (response != null) {
                // 记录完整的响应信息用于诊断
                log.info("LLM API response status code: {}", response.getStatusCode());
                log.info("LLM API response headers: {}", response.getHeaders());
                
                if (response.getBody() != null) {
                    Map<String, Object> body = response.getBody();
                    log.info("LLM API response body keys: {}", body.keySet());
                    log.info("LLM API response body (first 500 chars): {}", 
                        body.toString().length() > 500 ? body.toString().substring(0, 500) + "..." : body.toString());
                } else {
                    log.warn("LLM API response body is null");
                }
            }
            
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                
                // 检查是否有错误
                if (body.containsKey("error")) {
                    Map<String, Object> error = (Map<String, Object>) body.get("error");
                    log.error("LLM API returned error: {}", error);
                    if (error.containsKey("message")) {
                        log.error("LLM API error message: {}", error.get("message"));
                    }
                    if (error.containsKey("code")) {
                        log.error("LLM API error code: {}", error.get("code"));
                        // 检查是否是额度问题
                        if (error.get("code") != null && error.get("code").toString().contains("429")) {
                            log.error("⚠️ LLM API rate limit exceeded! You may have reached your free quota.");
                        }
                    }
                    return null;
                }
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> candidate = candidates.get(0);
                    
                    // 检查 candidate 中是否有错误
                    if (candidate.containsKey("finishReason") && !"STOP".equals(candidate.get("finishReason"))) {
                        log.warn("LLM API candidate finish reason: {}", candidate.get("finishReason"));
                    }
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contentResponse = (Map<String, Object>) candidate.get("content");
                    if (contentResponse != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> partsResponse = (List<Map<String, Object>>) contentResponse.get("parts");
                        if (partsResponse != null && !partsResponse.isEmpty()) {
                            String responseText = (String) partsResponse.get(0).get("text");
                            log.info("LLM API call successful, response length: {}", responseText != null ? responseText.length() : 0);
                            return responseText;
                        } else {
                            log.warn("LLM API response has no parts in content");
                        }
                    } else {
                        log.warn("LLM API response has no content in candidate");
                    }
                } else {
                    log.warn("LLM API response has no candidates. Response body: {}", body);
                }
            } else {
                if (response != null) {
                    log.error("LLM API call failed. Status: {}, Body: {}", 
                        response.getStatusCode(), response.getBody());
                } else {
                    log.error("LLM API call returned null response");
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("❌ LLM API HTTP client error (4xx): Status={}, Response={}", 
                e.getStatusCode(), e.getResponseBodyAsString(), e);
            
            // 检查是否是认证问题
            if (e.getStatusCode().value() == 401) {
                log.error("⚠️ LLM API authentication failed! Please check your API key in application.yml");
            } else if (e.getStatusCode().value() == 403) {
                log.error("⚠️ LLM API access forbidden! Your API key may not have permission or quota may be exceeded.");
            } else if (e.getStatusCode().value() == 429) {
                log.error("⚠️ LLM API rate limit exceeded! You may have reached your free quota. Please check Google AI Studio.");
            } else if (e.getStatusCode().value() == 400) {
                log.error("⚠️ LLM API bad request! Check your API URL and request format.");
            }
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("❌ LLM API HTTP server error (5xx): Status={}, Response={}", 
                e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("❌ LLM API resource access error (network/timeout): {}", e.getMessage(), e);
            log.error("This could be a network issue or the API endpoint is unreachable.");
        } catch (Exception e) {
            log.error("❌ Error calling LLM API: {}", e.getMessage(), e);
            log.error("Exception class: {}", e.getClass().getName());
            log.error("Exception stack trace:", e);
        }
        
        log.warn("LLM API call returned null");
        return null;
    }
    
    /**
     * 从 LLM 响应中提取 JSON
     */
    private String extractJSON(String response) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
        
        // 尝试找到 JSON 对象
        int startIndex = response.indexOf("{");
        if (startIndex < 0) {
            log.warn("No opening brace found in LLM response");
            return null;
        }
        
        // 从第一个 { 开始，尝试找到匹配的 }
        int braceCount = 0;
        int endIndex = -1;
        for (int i = startIndex; i < response.length(); i++) {
            char c = response.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    endIndex = i;
                    break;
                }
            }
        }
        
        if (endIndex > startIndex) {
            String json = response.substring(startIndex, endIndex + 1);
            log.debug("Extracted JSON (length: {}): {}", json.length(), json);
            return json;
        } else {
            // 如果 JSON 不完整（可能被截断），尝试修复
            log.warn("JSON appears to be incomplete (unmatched braces). Attempting to fix...");
            // 尝试找到最后一个完整的字段
            int lastComma = response.lastIndexOf(",");
            int lastQuote = response.lastIndexOf("\"");
            if (lastComma > lastQuote && lastComma > startIndex) {
                // 移除最后一个不完整的字段
                String partialJson = response.substring(startIndex, lastComma);
                // 尝试补全 JSON
                String fixedJson = partialJson + "}";
                log.info("Attempting to use fixed JSON: {}", fixedJson);
                return fixedJson;
            }
            log.warn("Could not extract valid JSON from response");
            return null;
        }
    }
    
    /**
     * 从 LLM 响应更新搜索条件
     */
    private void updateCriteriaFromLLMResponse(MaterialSearchCriteria criteria, Map<String, Object> parsed) {
        log.info("Updating criteria from LLM response: {}", parsed);
        
        if (parsed.containsKey("itemCode") && parsed.get("itemCode") != null) {
            criteria.setItemCode(parsed.get("itemCode").toString());
            log.info("LLM set itemCode: {}", criteria.getItemCode());
        }
        if (parsed.containsKey("itemNameKeyword") && parsed.get("itemNameKeyword") != null) {
            criteria.setItemNameKeyword(parsed.get("itemNameKeyword").toString());
            log.info("LLM set itemNameKeyword: {}", criteria.getItemNameKeyword());
        }
        if (parsed.containsKey("productHierarchy3") && parsed.get("productHierarchy3") != null) {
            criteria.setProductHierarchy3(parsed.get("productHierarchy3").toString());
            log.info("LLM set productHierarchy3: {}", criteria.getProductHierarchy3());
        }
        if (parsed.containsKey("function") && parsed.get("function") != null) {
            criteria.setFunction(parsed.get("function").toString());
            log.info("LLM set function: {}", criteria.getFunction());
        }
        if (parsed.containsKey("brandCode") && parsed.get("brandCode") != null) {
            criteria.setBrandCode(parsed.get("brandCode").toString());
            log.info("LLM set brandCode: {}", criteria.getBrandCode());
        }
        if (parsed.containsKey("buyerName") && parsed.get("buyerName") != null) {
            criteria.setBuyerName(parsed.get("buyerName").toString());
            log.info("LLM set buyerName: {}", criteria.getBuyerName());
        }
        if (parsed.containsKey("buyerCode") && parsed.get("buyerCode") != null) {
            criteria.setBuyerCode(parsed.get("buyerCode").toString());
            log.info("LLM set buyerCode: {}", criteria.getBuyerCode());
        }
        if (parsed.containsKey("minPrice") && parsed.get("minPrice") != null) {
            try {
                criteria.setMinPrice(new BigDecimal(parsed.get("minPrice").toString()));
            } catch (Exception e) {
                log.warn("Failed to parse minPrice", e);
            }
        }
        if (parsed.containsKey("maxPrice") && parsed.get("maxPrice") != null) {
            try {
                criteria.setMaxPrice(new BigDecimal(parsed.get("maxPrice").toString()));
            } catch (Exception e) {
                log.warn("Failed to parse maxPrice", e);
            }
        }
        if (parsed.containsKey("startDate") && parsed.get("startDate") != null) {
            try {
                criteria.setStartDate(LocalDate.parse(parsed.get("startDate").toString()));
            } catch (Exception e) {
                log.warn("Failed to parse startDate", e);
            }
        }
        if (parsed.containsKey("endDate") && parsed.get("endDate") != null) {
            try {
                criteria.setEndDate(LocalDate.parse(parsed.get("endDate").toString()));
            } catch (Exception e) {
                log.warn("Failed to parse endDate", e);
            }
        }
        
        // 确定搜索类型
        if (criteria.hasItemCode()) {
            criteria.setSearchType(MaterialSearchCriteria.SearchType.EXACT_ITEM_CODE);
        } else if (criteria.isCombinedSearch()) {
            criteria.setSearchType(MaterialSearchCriteria.SearchType.COMBINED);
        } else if (criteria.hasItemNameKeyword()) {
            criteria.setSearchType(MaterialSearchCriteria.SearchType.ITEM_NAME_FUZZY);
        } else if (criteria.hasCategory()) {
            criteria.setSearchType(MaterialSearchCriteria.SearchType.CATEGORY);
        } else if (criteria.hasFunction()) {
            criteria.setSearchType(MaterialSearchCriteria.SearchType.FUNCTION);
        } else if (criteria.hasBrand()) {
            criteria.setSearchType(MaterialSearchCriteria.SearchType.BRAND);
        }
    }
}

