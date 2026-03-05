package com.ecosystem.service;

import com.ecosystem.dto.search.ProductCard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.*;

/**
 * Web 内容抽取服务
 * 负责抓取页面 HTML 并从 JSON-LD (schema.org) 提取产品信息
 */
@Service
@Slf4j
public class WebContentExtractor {

    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private final Semaphore semaphore;

    @Value("${tavily.enhancement.enabled:true}")
    private boolean enhancementEnabled;

    @Value("${tavily.enhancement.max-concurrent:3}")
    private int maxConcurrent;

    @Value("${tavily.enhancement.page-timeout-ms:8000}")
    private int pageTimeoutMs;

    @Value("${tavily.enhancement.max-content-length:12000}")
    private int maxContentLength;

    public WebContentExtractor() {
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newFixedThreadPool(5);
        this.semaphore = new Semaphore(3); // 默认并发限制
    }

    /**
     * 检查增强功能是否启用
     */
    public boolean isEnhancementEnabled() {
        return enhancementEnabled;
    }

    /**
     * 增强 ProductCard - 抓取页面并提取结构化数据
     * @param card 原始 ProductCard
     * @return 增强后的 ProductCard
     */
    public ProductCard enhance(ProductCard card) {
        if (!enhancementEnabled || card.getUrl() == null) {
            return card;
        }

        try {
            // 使用信号量控制并发
            if (!semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                log.debug("Skipping enhancement due to concurrency limit: {}", card.getUrl());
                return card;
            }

            try {
                return doEnhance(card);
            } finally {
                semaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return card;
        }
    }

    /**
     * 异步增强 ProductCard
     */
    public CompletableFuture<ProductCard> enhanceAsync(ProductCard card) {
        if (!enhancementEnabled || card.getUrl() == null) {
            return CompletableFuture.completedFuture(card);
        }

        return CompletableFuture.supplyAsync(() -> enhance(card), executorService)
                .orTimeout(pageTimeoutMs, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    log.warn("Async enhancement failed for {}: {}", card.getUrl(), ex.getMessage());
                    return card;
                });
    }

    /**
     * 执行实际的增强处理
     */
    private ProductCard doEnhance(ProductCard card) {
        log.debug("🔧 Enhancing ProductCard: {}", card.getUrl());

        try {
            // 抓取页面
            Document doc = Jsoup.connect(card.getUrl())
                    .timeout(pageTimeoutMs)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .get();

            // 优先从 JSON-LD 提取
            ExtractedData jsonLdData = extractFromJsonLd(doc);
            
            if (jsonLdData != null) {
                applyExtractedData(card, jsonLdData);
                card.setEnhanced(true);
                log.debug("✅ Enhanced from JSON-LD: {}", card.getName());
            } else {
                // 尝试从 meta 标签和常见选择器提取
                ExtractedData metaData = extractFromMetaTags(doc);
                if (metaData != null) {
                    applyExtractedData(card, metaData);
                    card.setEnhanced(true);
                    log.debug("✅ Enhanced from meta tags: {}", card.getName());
                }
            }

            // 预提取正文，便于排查页面可读内容质量
            String readableContent = extractReadableContent(doc);
            log.trace("Readable content length for {}: {}", card.getUrl(), readableContent.length());

        } catch (Exception e) {
            log.warn("⚠️ Failed to enhance {}: {}", card.getUrl(), e.getMessage());
        }

        return card;
    }

    /**
     * 从 JSON-LD (schema.org) 提取产品信息
     */
    private ExtractedData extractFromJsonLd(Document doc) {
        Elements scripts = doc.select("script[type=application/ld+json]");
        
        for (Element script : scripts) {
            try {
                String json = script.html();
                JsonNode root = objectMapper.readTree(json);
                
                // 处理数组格式的 JSON-LD
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        ExtractedData data = parseJsonLdNode(node);
                        if (data != null) {
                            return data;
                        }
                    }
                } else {
                    ExtractedData data = parseJsonLdNode(root);
                    if (data != null) {
                        return data;
                    }
                }
            } catch (Exception e) {
                log.trace("Failed to parse JSON-LD: {}", e.getMessage());
            }
        }
        
        return null;
    }

    /**
     * 解析单个 JSON-LD 节点
     */
    private ExtractedData parseJsonLdNode(JsonNode node) {
        String type = getNodeText(node, "@type");
        
        // 检查是否是 Product 类型
        if ("Product".equals(type)) {
            ExtractedData data = new ExtractedData();
            
            // 提取产品名称
            data.name = getNodeText(node, "name");
            
            // 提取品牌
            JsonNode brand = node.get("brand");
            if (brand != null) {
                if (brand.isTextual()) {
                    data.brand = brand.asText();
                } else {
                    data.brand = getNodeText(brand, "name");
                }
            }
            
            // 提取图片
            JsonNode image = node.get("image");
            if (image != null) {
                if (image.isTextual()) {
                    data.imageUrl = image.asText();
                } else if (image.isArray() && image.size() > 0) {
                    JsonNode firstImage = image.get(0);
                    if (firstImage.isTextual()) {
                        data.imageUrl = firstImage.asText();
                    } else {
                        data.imageUrl = getNodeText(firstImage, "url");
                    }
                } else {
                    data.imageUrl = getNodeText(image, "url");
                }
            }
            
            // 提取 Offer 信息（价格）
            JsonNode offers = node.get("offers");
            if (offers != null) {
                JsonNode offer = offers.isArray() ? (offers.size() > 0 ? offers.get(0) : null) : offers;
                if (offer != null) {
                    // 价格
                    String priceStr = getNodeText(offer, "price");
                    if (priceStr != null) {
                        try {
                            data.price = new BigDecimal(priceStr.replaceAll("[^\\d.]", ""));
                        } catch (NumberFormatException e) {
                            log.trace("Failed to parse price: {}", priceStr);
                        }
                    }
                    
                    // 货币
                    data.currency = getNodeText(offer, "priceCurrency");
                    
                    // 可用性
                    String availability = getNodeText(offer, "availability");
                    if (availability != null) {
                        if (availability.contains("InStock")) {
                            data.availability = "in_stock";
                        } else if (availability.contains("OutOfStock")) {
                            data.availability = "out_of_stock";
                        } else if (availability.contains("LimitedAvailability")) {
                            data.availability = "limited";
                        }
                    }
                    
                    // 卖家
                    JsonNode seller = offer.get("seller");
                    if (seller != null) {
                        data.vendor = getNodeText(seller, "name");
                    }
                }
            }
            
            // 只有提取到有用信息才返回
            if (data.name != null || data.price != null || data.brand != null) {
                return data;
            }
        }
        
        // 递归检查嵌套的 @graph
        JsonNode graph = node.get("@graph");
        if (graph != null && graph.isArray()) {
            for (JsonNode graphNode : graph) {
                ExtractedData data = parseJsonLdNode(graphNode);
                if (data != null) {
                    return data;
                }
            }
        }
        
        return null;
    }

    /**
     * 从 meta 标签提取产品信息
     */
    private ExtractedData extractFromMetaTags(Document doc) {
        ExtractedData data = new ExtractedData();
        
        // Open Graph 标签
        data.name = getMetaContent(doc, "og:title");
        data.imageUrl = getMetaContent(doc, "og:image");
        
        // Twitter 卡片
        if (data.name == null) {
            data.name = getMetaContent(doc, "twitter:title");
        }
        if (data.imageUrl == null) {
            data.imageUrl = getMetaContent(doc, "twitter:image");
        }
        
        // 产品特定 meta
        String priceStr = getMetaContent(doc, "product:price:amount");
        if (priceStr != null) {
            try {
                data.price = new BigDecimal(priceStr.replaceAll("[^\\d.]", ""));
            } catch (NumberFormatException e) {
                log.trace("Failed to parse meta price: {}", priceStr);
            }
        }
        data.currency = getMetaContent(doc, "product:price:currency");
        
        // 只有提取到有用信息才返回
        if (data.name != null || data.price != null || data.imageUrl != null) {
            return data;
        }
        
        return null;
    }

    /**
     * 提取页面可读正文（用于 LLM 处理）
     */
    public String extractReadableContent(Document doc) {
        // 移除脚本和样式
        doc.select("script, style, nav, footer, header, aside, .ad, .advertisement").remove();
        
        // 获取主要内容区域
        Element main = doc.selectFirst("main, article, .content, .product-description, #content");
        String text;
        if (main != null) {
            text = main.text();
        } else {
            text = doc.body() != null ? doc.body().text() : "";
        }
        
        // 截断到最大长度
        if (text.length() > maxContentLength) {
            text = text.substring(0, maxContentLength) + "...";
        }
        
        return text;
    }

    /**
     * 获取 JSON 节点的文本值
     */
    private String getNodeText(JsonNode node, String fieldName) {
        if (node == null) return null;
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() ? field.asText() : null;
    }

    /**
     * 获取 meta 标签内容
     */
    private String getMetaContent(Document doc, String property) {
        Element meta = doc.selectFirst("meta[property=" + property + "]");
        if (meta == null) {
            meta = doc.selectFirst("meta[name=" + property + "]");
        }
        return meta != null ? meta.attr("content") : null;
    }

    /**
     * 将提取的数据应用到 ProductCard
     */
    private void applyExtractedData(ProductCard card, ExtractedData data) {
        if (data.name != null && !data.name.isEmpty()) {
            // 只有当提取的名称更详细时才覆盖
            if (card.getName() == null || data.name.length() > card.getName().length()) {
                card.setName(data.name);
            }
        }
        if (data.price != null) {
            card.setPrice(data.price);
        }
        if (data.currency != null) {
            card.setCurrency(data.currency);
        }
        if (data.brand != null) {
            card.setBrand(data.brand);
        }
        if (data.vendor != null) {
            card.setVendor(data.vendor);
        }
        if (data.imageUrl != null && card.getImageUrl() == null) {
            card.setImageUrl(data.imageUrl);
        }
        if (data.availability != null) {
            card.setAvailability(data.availability);
        }
    }

    /**
     * 内部类：提取的数据
     */
    private static class ExtractedData {
        String name;
        BigDecimal price;
        String currency;
        String brand;
        String vendor;
        String imageUrl;
        String availability;
    }

    /**
     * 关闭服务时释放资源
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
