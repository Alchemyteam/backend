package com.ecosystem.service;

import com.ecosystem.dto.search.ProductCard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tavily Search API 服务
 * 保留类名以兼容现有调用方，内部已切换为 Tavily。
 */
@Service
@Slf4j
public class BraveSearchService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${tavily.api.key:}")
    private String braveApiKey;

    @Value("${tavily.api.base-url:https://api.tavily.com}")
    private String braveBaseUrl;

    @Value("${tavily.search.default-count:10}")
    private int defaultCount;

    @Value("${tavily.search.country:ALL}")
    private String defaultCountry;

    @Value("${tavily.search.locale:en}")
    private String defaultLocale;

    @Value("${tavily.search.timeout-ms:5000}")
    private int timeoutMs;

    @Value("${tavily.search.max-retries:1}")
    private int maxRetries;

    @Value("${tavily.enabled:true}")
    private boolean enabled;

    // 用于从 URL 提取域名的正则
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^(?:https?://)?(?:www\\.)?([^/]+)");
    
    // 常见跟踪参数，用于 URL 规范化
    private static final Pattern TRACKING_PARAMS_PATTERN = Pattern.compile(
            "[?&](utm_[^&]*|ref[^&]*|fbclid[^&]*|gclid[^&]*|mc_[^&]*|_ga[^&]*|affiliate[^&]*)");

    public BraveSearchService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 检查 Tavily Search 是否可用
     */
    public boolean isAvailable() {
        return enabled && braveApiKey != null && !braveApiKey.isEmpty();
    }

    /**
     * 执行 Web 搜索
     * @param query 搜索查询
     * @return ProductCard 列表
     */
    public List<ProductCard> search(String query) {
        return search(query, defaultCount);
    }

    /**
     * 执行 Web 搜索（指定结果数量）
     * @param query 搜索查询
     * @param count 结果数量
     * @return ProductCard 列表
     */
    public List<ProductCard> search(String query, int count) {
        if (!isAvailable()) {
            log.warn("Tavily Search is not available. Check API key configuration.");
            return new ArrayList<>();
        }

        log.info("🔍 Executing Tavily Search for query: '{}', count: {}", query, count);
        
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts <= maxRetries) {
            try {
                return executeSearch(query, count);
            } catch (ResourceAccessException e) {
                lastException = e;
                attempts++;
                log.warn("⚠️ Tavily Search timeout/network error (attempt {}/{}): {}",
                        attempts, maxRetries + 1, e.getMessage());
                if (attempts <= maxRetries) {
                    try {
                        Thread.sleep(500 * attempts); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (HttpClientErrorException e) {
                log.error("❌ Tavily Search API error: {} - {}", e.getStatusCode(), e.getMessage());
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    attempts++;
                    if (attempts <= maxRetries) {
                        try {
                            Thread.sleep(1000 * attempts);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        continue;
                    }
                }
                return new ArrayList<>();
            } catch (Exception e) {
                log.error("❌ Tavily Search unexpected error: {}", e.getMessage(), e);
                return new ArrayList<>();
            }
        }
        
        log.error("❌ Tavily Search failed after {} attempts", attempts);
        if (lastException != null) {
            log.error("Last error: {}", lastException.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * 执行 Tavily 搜索请求
     */
    private List<ProductCard> executeSearch(String query, int count) {
        String apiUrl = braveBaseUrl + "/search";
        log.debug("🌐 Tavily Search URL: {}", apiUrl);

        // 查询增强：尽量把召回范围拉向新加坡
        String localizedQuery = buildLocalizedQuery(query);

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // Tavily 请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("api_key", braveApiKey);
        requestBody.put("query", localizedQuery);
        requestBody.put("max_results", count);
        requestBody.put("search_depth", "basic");
        requestBody.put("include_answer", false);
        requestBody.put("include_raw_content", false);
        requestBody.put("include_images", false);
        requestBody.put("topic", "general");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 执行请求
        long startTime = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
        long elapsed = System.currentTimeMillis() - startTime;

        log.info("✅ Tavily Search completed in {}ms, status: {}", elapsed, response.getStatusCode());

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return parseSearchResults(response.getBody(), query);
        }

        log.warn("⚠️ Tavily Search returned empty or non-2xx response");
        return new ArrayList<>();
    }

    /**
     * 增强查询，优先召回新加坡站点
     */
    private String buildLocalizedQuery(String query) {
        if (query == null) {
            return "Singapore site:.sg";
        }
        String lower = query.toLowerCase();
        if (lower.contains("singapore") || lower.contains("site:.sg")) {
            return query;
        }
        return query + " Singapore site:.sg";
    }

    /**
     * 解析 Tavily 响应并转换为 ProductCard
     */
    private List<ProductCard> parseSearchResults(String responseBody, String originalQuery) {
        List<ProductCard> results = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode resultNodes = root.path("results");

            if (!resultNodes.isArray()) {
                log.warn("No results array in Tavily response");
                return results;
            }

            log.info("📋 Parsed {} web results from Tavily", resultNodes.size());
            for (JsonNode node : resultNodes) {
                ProductCard card = mapToProductCard(node, originalQuery);
                if (card != null) {
                    results.add(card);
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to parse Tavily response: {}", e.getMessage(), e);
        }
        
        return results;
    }

    /**
     * 将 Tavily 搜索结果映射为 ProductCard（MVP 阶段）
     */
    private ProductCard mapToProductCard(JsonNode webResult, String originalQuery) {
        String url = webResult.path("url").asText(null);
        String title = webResult.path("title").asText(null);
        String content = webResult.path("content").asText(null);
        double score = webResult.has("score") ? webResult.path("score").asDouble(0.0) : 0.0;

        if (url == null || title == null) {
            return null;
        }

        String vendor = extractDomainFromUrl(url);
        String canonicalUrl = normalizeUrl(url);
        double confidence = score > 0.0
                ? Math.min(1.0, Math.max(0.4, score))
                : calculateConfidence(title, content, originalQuery);

        return ProductCard.builder()
                .name(title)
                .description(content)
                .url(url)
                .imageUrl(null)
                .vendor(vendor)
                .platform("web")
                .sourceDomain(vendor)
                .confidence(confidence)
                .canonicalUrl(canonicalUrl)
                .enhanced(false)  // MVP 阶段未增强
                .price(null)      // MVP 阶段无价格
                .currency(null)
                .build();
    }

    /**
     * 从 URL 提取域名（去除 www 前缀）
     */
    public String extractDomainFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "unknown";
        }
        
        Matcher matcher = DOMAIN_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }

    /**
     * 规范化 URL（去除跟踪参数）
     */
    public String normalizeUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        
        // 移除跟踪参数
        String normalized = TRACKING_PARAMS_PATTERN.matcher(url).replaceAll("");
        
        // 清理多余的 ? 或 &
        normalized = normalized.replaceAll("\\?&", "?")
                              .replaceAll("&&+", "&")
                              .replaceAll("[?&]$", "");
        
        return normalized;
    }

    /**
     * 计算搜索结果的置信度
     * 基于标题和描述与查询的匹配程度
     */
    private double calculateConfidence(String title, String description, String query) {
        if (title == null || query == null) {
            return 0.5;
        }

        String lowerTitle = title.toLowerCase();
        String lowerQuery = query.toLowerCase();
        String lowerDesc = description != null ? description.toLowerCase() : "";
        
        double score = 0.65; // 基础分数
        
        // 查询词完全出现在标题中
        if (lowerTitle.contains(lowerQuery)) {
            score += 0.2;
        } else {
            // 检查查询词的各个部分
            String[] queryWords = lowerQuery.split("\\s+");
            int matchCount = 0;
            for (String word : queryWords) {
                if (word.length() > 2 && lowerTitle.contains(word)) {
                    matchCount++;
                }
            }
            score += 0.15 * ((double) matchCount / queryWords.length);
        }
        
        // 查询词出现在描述中
        if (lowerDesc.contains(lowerQuery)) {
            score += 0.1;
        }
        
        // 标题包含价格相关词汇（表示可能是商品页面）
        if (lowerTitle.matches(".*\\$\\d+.*") || 
            lowerTitle.contains("price") || 
            lowerTitle.contains("buy") ||
            lowerTitle.contains("shop")) {
            score += 0.05;
        }
        
        return Math.min(score, 1.0);
    }
}
