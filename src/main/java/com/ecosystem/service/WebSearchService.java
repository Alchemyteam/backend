package com.ecosystem.service;

import com.ecosystem.dto.search.ProductCard;
import com.ecosystem.dto.search.WebSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Web 搜索服务
 * 整合 Tavily 搜索、内容增强、去重和排序逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSearchService {

    private final BraveSearchService braveSearchService;
    private final WebContentExtractor webContentExtractor;
    private final LLMContentExtractor llmContentExtractor;

    @Value("${tavily.enabled:true}")
    private boolean braveEnabled;

    @Value("${tavily.enhancement.enabled:true}")
    private boolean enhancementEnabled;

    @Value("${tavily.enhancement.max-concurrent:3}")
    private int maxConcurrent;

    @Value("${tavily.enhancement.page-timeout-ms:8000}")
    private int pageTimeoutMs;

    @Value("${tavily.search.strict-singapore:true}")
    private boolean strictSingapore;

    @Value("${tavily.search.allow-sea-fallback:false}")
    private boolean allowSeaFallback;

    /**
     * 执行 Web 搜索（用于数据库无结果时的回退）
     * @param query 搜索查询
     * @return WebSearchResponse
     */
    public WebSearchResponse searchWeb(String query) {
        return searchWeb(query, 10);
    }

    /**
     * 执行 Web 搜索（指定结果数量）
     * @param query 搜索查询
     * @param count 结果数量
     * @return WebSearchResponse
     */
    public WebSearchResponse searchWeb(String query, int count) {
        if (!braveEnabled || !braveSearchService.isAvailable()) {
            log.warn("⚠️ Tavily Search is not available");
            return WebSearchResponse.errorResult(query, "Web search is not available");
        }

        long startTime = System.currentTimeMillis();
        log.info("🌐 Starting Web search for: '{}'", query);

        try {
            // 1. 执行 Tavily 搜索
            List<ProductCard> results = braveSearchService.search(query, count);
            
            if (results.isEmpty()) {
                log.info("📭 No web results found for: '{}'", query);
                return WebSearchResponse.webFallbackResult(query, results, 
                        System.currentTimeMillis() - startTime, false);
            }

            log.info("📋 Got {} raw results from Tavily Search", results.size());

            // 2. 去重
            results = deduplicateResults(results);
            log.info("📋 After deduplication: {} results", results.size());

            // 2.5 地域约束：优先新加坡，可选东南亚回退
            results = applyGeoPreference(results);
            log.info("📋 After geo preference filter: {} results", results.size());

            if (results.isEmpty()) {
                log.info("📭 No results left after Singapore/SEA geo filtering for '{}'", query);
                return WebSearchResponse.webFallbackResult(query, results,
                        System.currentTimeMillis() - startTime, false);
            }

            // 3. 增强处理（如果启用）
            boolean enhanced = false;
            if (enhancementEnabled && webContentExtractor.isEnhancementEnabled()) {
                results = enhanceResults(results);
                enhanced = true;
            }

            // 4. 排序
            results = sortResults(results);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ Web search completed in {}ms, {} results", elapsed, results.size());

            return WebSearchResponse.webFallbackResult(query, results, elapsed, enhanced);

        } catch (Exception e) {
            log.error("❌ Web search failed: {}", e.getMessage(), e);
            return WebSearchResponse.errorResult(query, "Web search failed: " + e.getMessage());
        }
    }

    /**
     * 合并内部搜索结果和 Web 搜索结果
     * @param query 搜索查询
     * @param internalResults 内部搜索结果
     * @param webResults Web 搜索结果
     * @return 合并后的 WebSearchResponse
     */
    public WebSearchResponse mergeResults(String query, List<ProductCard> internalResults, 
                                          List<ProductCard> webResults, long searchTimeMs) {
        List<ProductCard> merged = new ArrayList<>();
        
        // 内部结果优先
        if (internalResults != null && !internalResults.isEmpty()) {
            merged.addAll(internalResults);
        }
        
        // 添加 Web 结果
        if (webResults != null && !webResults.isEmpty()) {
            merged.addAll(webResults);
        }
        
        // 去重（基于 canonical URL）
        merged = deduplicateResults(merged);
        
        // 排序
        merged = sortResults(merged);
        
        int internalCount = internalResults != null ? internalResults.size() : 0;
        int webCount = webResults != null ? webResults.size() : 0;
        
        // 确定来源类型
        if (internalCount == 0 && webCount > 0) {
            return WebSearchResponse.webFallbackResult(query, merged, searchTimeMs, 
                    merged.stream().anyMatch(c -> Boolean.TRUE.equals(c.getEnhanced())));
        } else if (internalCount > 0 && webCount > 0) {
            return WebSearchResponse.mixedResult(query, merged, internalCount, webCount, searchTimeMs,
                    merged.stream().anyMatch(c -> Boolean.TRUE.equals(c.getEnhanced())));
        } else {
            return WebSearchResponse.internalResult(query, merged, searchTimeMs);
        }
    }

    /**
     * 去重：基于 canonical URL
     */
    private List<ProductCard> deduplicateResults(List<ProductCard> results) {
        Map<String, ProductCard> uniqueResults = new LinkedHashMap<>();
        
        for (ProductCard card : results) {
            String key = getDeduplicationKey(card);
            
            // 如果已存在，保留信息更完整的那个
            if (uniqueResults.containsKey(key)) {
                ProductCard existing = uniqueResults.get(key);
                if (card.getCompletenessScore() > existing.getCompletenessScore()) {
                    uniqueResults.put(key, card);
                }
            } else {
                uniqueResults.put(key, card);
            }
        }
        
        return new ArrayList<>(uniqueResults.values());
    }

    /**
     * 获取去重用的 key
     */
    private String getDeduplicationKey(ProductCard card) {
        // 优先使用 canonical URL
        if (card.getCanonicalUrl() != null && !card.getCanonicalUrl().isEmpty()) {
            return card.getCanonicalUrl().toLowerCase();
        }
        // 其次使用普通 URL
        if (card.getUrl() != null && !card.getUrl().isEmpty()) {
            return braveSearchService.normalizeUrl(card.getUrl()).toLowerCase();
        }
        // 最后使用 ID 或名称
        if (card.getId() != null) {
            return "id:" + card.getId();
        }
        if (card.getName() != null) {
            return "name:" + card.getName().toLowerCase();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * 增强搜索结果
     */
    private List<ProductCard> enhanceResults(List<ProductCard> results) {
        log.info("🔧 Enhancing {} results...", results.size());
        
        // 限制并发增强的数量
        int toEnhance = Math.min(results.size(), maxConcurrent);
        
        // 使用 CompletableFuture 并行处理
        List<CompletableFuture<ProductCard>> futures = new ArrayList<>();
        
        for (int i = 0; i < toEnhance; i++) {
            final ProductCard card = results.get(i);
            
            CompletableFuture<ProductCard> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // 1. 先用 WebContentExtractor 抓取和解析 JSON-LD
                    ProductCard enhanced = webContentExtractor.enhance(card);
                    
                    // 2. 如果还缺少 price 或 vendor，且 LLM 可用，则使用 LLM 抽取
                    if ((enhanced.getPrice() == null || enhanced.getVendor() == null) 
                            && llmContentExtractor.isAvailable()) {
                        // 抓取页面内容供 LLM 使用
                        try {
                            Document doc = Jsoup.connect(card.getUrl())
                                    .timeout(pageTimeoutMs)
                                    .userAgent("Mozilla/5.0")
                                    .get();
                            String content = webContentExtractor.extractReadableContent(doc);
                            enhanced = llmContentExtractor.extractProductInfo(enhanced, content);
                        } catch (Exception e) {
                            log.debug("Failed to fetch content for LLM: {}", e.getMessage());
                        }
                    }
                    
                    return enhanced;
                } catch (Exception e) {
                    log.warn("Enhancement failed for {}: {}", card.getUrl(), e.getMessage());
                    return card;
                }
            });
            
            futures.add(future);
        }
        
        // 等待所有增强完成（带超时）
        List<ProductCard> enhanced = new ArrayList<>();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(pageTimeoutMs * 2L, TimeUnit.MILLISECONDS);
            
            for (int i = 0; i < toEnhance; i++) {
                try {
                    enhanced.add(futures.get(i).get());
                } catch (Exception e) {
                    enhanced.add(results.get(i));
                }
            }
        } catch (Exception e) {
            log.warn("Some enhancements timed out, using original results");
            for (int i = 0; i < toEnhance; i++) {
                if (futures.get(i).isDone() && !futures.get(i).isCompletedExceptionally()) {
                    try {
                        enhanced.add(futures.get(i).get());
                    } catch (Exception ex) {
                        enhanced.add(results.get(i));
                    }
                } else {
                    enhanced.add(results.get(i));
                }
            }
        }
        
        // 添加未增强的结果
        for (int i = toEnhance; i < results.size(); i++) {
            enhanced.add(results.get(i));
        }
        
        log.info("✅ Enhancement completed for {} results", enhanced.size());
        return enhanced;
    }

    /**
     * 排序搜索结果
     * 优先级：内部结果 > Web 结果
     * Web 结果内部按信息完整度和置信度排序
     */
    private List<ProductCard> sortResults(List<ProductCard> results) {
        return results.stream()
                .sorted((a, b) -> {
                    // 1. 内部结果优先
                    boolean aIsInternal = "internal".equals(a.getPlatform());
                    boolean bIsInternal = "internal".equals(b.getPlatform());
                    if (aIsInternal && !bIsInternal) return -1;
                    if (!aIsInternal && bIsInternal) return 1;

                    // 2. Web 结果按地区优先级排序：新加坡 > 东南亚 > 其他
                    if (!aIsInternal && !bIsInternal) {
                        int aRegionPriority = getRegionPriority(a);
                        int bRegionPriority = getRegionPriority(b);
                        if (aRegionPriority != bRegionPriority) {
                            return Integer.compare(aRegionPriority, bRegionPriority);
                        }
                    }

                    // 3. 按综合得分排序（完整度 + 置信度）
                    double aScore = a.getSortScore();
                    double bScore = b.getSortScore();
                    return Double.compare(bScore, aScore); // 降序
                })
                .collect(Collectors.toList());
    }

    /**
     * 地区优先级（数值越小优先级越高）
     * 0: 新加坡
     * 1: 东南亚
     * 2: 其他
     */
    private int getRegionPriority(ProductCard card) {
        StringBuilder combined = new StringBuilder();
        if (card.getUrl() != null) combined.append(card.getUrl()).append(" ");
        if (card.getSourceDomain() != null) combined.append(card.getSourceDomain()).append(" ");
        if (card.getVendor() != null) combined.append(card.getVendor()).append(" ");
        if (card.getName() != null) combined.append(card.getName()).append(" ");
        if (card.getDescription() != null) combined.append(card.getDescription());

        String text = combined.toString().toLowerCase();

        // 新加坡优先
        if (text.contains(".sg")
                || text.contains("singapore")
                || text.contains("singapura")
                || text.contains("sg ")) {
            return 0;
        }

        // 东南亚其次（不包含新加坡，已在上面判定）
        if (text.contains(".my") || text.contains(".id") || text.contains(".th")
                || text.contains(".vn") || text.contains(".ph") || text.contains(".kh")
                || text.contains(".la") || text.contains(".mm") || text.contains(".bn")
                || text.contains("malaysia") || text.contains("indonesia") || text.contains("thailand")
                || text.contains("vietnam") || text.contains("philippines") || text.contains("cambodia")
                || text.contains("laos") || text.contains("myanmar") || text.contains("brunei")
                || text.contains("asean") || text.contains("southeast asia")) {
            return 1;
        }

        return 2;
    }

    /**
     * 应用地域偏好过滤：
     * - strictSingapore=true: 只保留新加坡结果
     * - 若无新加坡且 allowSeaFallback=true: 回退保留东南亚结果
     * - strictSingapore=false: 不做强过滤，仅依赖排序
     */
    private List<ProductCard> applyGeoPreference(List<ProductCard> results) {
        if (!strictSingapore || results == null || results.isEmpty()) {
            return results;
        }

        List<ProductCard> singaporeResults = results.stream()
                .filter(card -> getRegionPriority(card) == 0)
                .collect(Collectors.toList());
        if (!singaporeResults.isEmpty()) {
            return singaporeResults;
        }

        if (allowSeaFallback) {
            List<ProductCard> seaResults = results.stream()
                    .filter(card -> getRegionPriority(card) == 1)
                    .collect(Collectors.toList());
            if (!seaResults.isEmpty()) {
                log.info("No Singapore results found, fallback to Southeast Asia results");
                return seaResults;
            }
        }

        return Collections.emptyList();
    }

    /**
     * 检查 Web 搜索是否可用
     */
    public boolean isWebSearchAvailable() {
        return braveEnabled && braveSearchService.isAvailable();
    }
}
