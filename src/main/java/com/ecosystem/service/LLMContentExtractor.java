package com.ecosystem.service;

import com.ecosystem.dto.search.ProductCard;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

/**
 * LLM 内容抽取服务
 * 当 JSON-LD 无法提供足够信息时，使用 LLM 从页面内容中抽取产品信息
 * 设计为可插拔组件，支持配置开关
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LLMContentExtractor {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.api.url:https://generativelanguage.googleapis.com/v1beta}")
    private String llmApiUrl;

    @Value("${llm.api.key:}")
    private String llmApiKey;

    @Value("${llm.model:gemini-2.5-flash}")
    private String llmModel;

    @Value("${tavily.enhancement.llm-extraction-enabled:true}")
    private boolean llmExtractionEnabled;

    @Value("${tavily.enhancement.max-content-length:12000}")
    private int maxContentLength;

    /**
     * 检查 LLM 抽取是否可用
     */
    public boolean isAvailable() {
        return llmExtractionEnabled && llmApiKey != null && !llmApiKey.isEmpty();
    }

    /**
     * 使用 LLM 从页面内容中抽取产品信息
     * @param card 原始 ProductCard
     * @param pageContent 页面可读正文
     * @return 增强后的 ProductCard
     */
    public ProductCard extractProductInfo(ProductCard card, String pageContent) {
        if (!isAvailable()) {
            log.debug("LLM extraction not available, skipping");
            return card;
        }

        if (pageContent == null || pageContent.isEmpty()) {
            return card;
        }

        // 检查是否需要 LLM 抽取（price 或 vendor 为空）
        if (card.getPrice() != null && card.getVendor() != null && !card.getVendor().isEmpty()) {
            log.debug("ProductCard already has price and vendor, skipping LLM extraction");
            return card;
        }

        log.debug("🤖 Using LLM to extract product info from: {}", card.getUrl());

        try {
            String extractedJson = callLLMForExtraction(card, pageContent);
            if (extractedJson != null) {
                applyLLMExtraction(card, extractedJson);
            }
        } catch (Exception e) {
            log.warn("⚠️ LLM extraction failed for {}: {}", card.getUrl(), e.getMessage());
        }

        return card;
    }

    /**
     * 调用 LLM 进行产品信息抽取
     */
    private String callLLMForExtraction(ProductCard card, String pageContent) {
        // 截断内容
        if (pageContent.length() > maxContentLength) {
            pageContent = pageContent.substring(0, maxContentLength);
        }

        String prompt = buildExtractionPrompt(card, pageContent);
        
        try {
            // 构建 API URL (Google Gemini 格式)
            String apiUrl = llmApiUrl + "/models/" + llmModel + ":generateContent?key=" + llmApiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求体
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

            // 生成配置 - 使用低温度确保输出格式一致
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.1);
            generationConfig.put("maxOutputTokens", 500);
            requestBody.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                    apiUrl, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return extractTextFromGeminiResponse(response.getBody());
            }
            
        } catch (Exception e) {
            log.error("❌ LLM API call failed: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 构建抽取 prompt
     */
    private String buildExtractionPrompt(ProductCard card, String pageContent) {
        return """
            You are a product information extractor. Extract product details from the following web page content.
            
            URL: %s
            Title: %s
            Snippet: %s
            
            Page Content:
            %s
            
            Extract the following information and respond ONLY with a valid JSON object (no markdown, no explanation):
            {
              "name": "product name (string or null)",
              "vendor": "seller/vendor name (string or null)",
              "brand": "brand name (string or null)",
              "price": numeric price value only (number or null, NO currency symbol),
              "currency": "currency code like SGD, USD (string or null)",
              "availability": "in_stock, out_of_stock, limited, or unknown (string)",
              "confidence": confidence level 0.0-1.0 (number)
            }
            
            IMPORTANT RULES:
            - DO NOT make up information. If not found, use null.
            - Price MUST be a number only (e.g., 29.99), not a string with currency.
            - If you cannot determine something with confidence, use null.
            - Respond with ONLY the JSON object, nothing else.
            """.formatted(
                card.getUrl() != null ? card.getUrl() : "",
                card.getName() != null ? card.getName() : "",
                card.getDescription() != null ? card.getDescription() : "",
                pageContent
            );
    }

    /**
     * 从 Gemini API 响应中提取文本
     */
    private String extractTextFromGeminiResponse(Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                if (content != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract text from Gemini response: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 将 LLM 抽取结果应用到 ProductCard
     */
    private void applyLLMExtraction(ProductCard card, String jsonResponse) {
        try {
            // 清理可能的 markdown 代码块
            String cleanJson = jsonResponse.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            JsonNode root = objectMapper.readTree(cleanJson);

            // 应用抽取的字段（只有当原值为空时才覆盖）
            if (card.getName() == null || card.getName().isEmpty()) {
                String name = getJsonString(root, "name");
                if (name != null) card.setName(name);
            }

            if (card.getVendor() == null || card.getVendor().isEmpty()) {
                String vendor = getJsonString(root, "vendor");
                if (vendor != null) card.setVendor(vendor);
            }

            if (card.getBrand() == null || card.getBrand().isEmpty()) {
                String brand = getJsonString(root, "brand");
                if (brand != null) card.setBrand(brand);
            }

            if (card.getPrice() == null) {
                BigDecimal price = getJsonDecimal(root, "price");
                if (price != null) card.setPrice(price);
            }

            if (card.getCurrency() == null || card.getCurrency().isEmpty()) {
                String currency = getJsonString(root, "currency");
                if (currency != null) card.setCurrency(currency);
            }

            if (card.getAvailability() == null || card.getAvailability().isEmpty()) {
                String availability = getJsonString(root, "availability");
                if (availability != null) card.setAvailability(availability);
            }

            // 更新置信度（如果 LLM 提供了更高的置信度）
            Double llmConfidence = getJsonDouble(root, "confidence");
            if (llmConfidence != null && (card.getConfidence() == null || llmConfidence > card.getConfidence())) {
                card.setConfidence(llmConfidence);
            }

            card.setEnhanced(true);
            log.debug("✅ LLM extraction applied successfully");

        } catch (Exception e) {
            log.warn("⚠️ Failed to parse LLM extraction result: {}", e.getMessage());
        }
    }

    /**
     * 安全获取 JSON 字符串值
     */
    private String getJsonString(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode != null && !fieldNode.isNull() && fieldNode.isTextual()) {
            String value = fieldNode.asText().trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }

    /**
     * 安全获取 JSON 数值
     */
    private BigDecimal getJsonDecimal(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode != null && !fieldNode.isNull()) {
            if (fieldNode.isNumber()) {
                return BigDecimal.valueOf(fieldNode.asDouble());
            } else if (fieldNode.isTextual()) {
                try {
                    String text = fieldNode.asText().replaceAll("[^\\d.]", "");
                    if (!text.isEmpty()) {
                        return new BigDecimal(text);
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return null;
    }

    /**
     * 安全获取 JSON Double 值
     */
    private Double getJsonDouble(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode != null && !fieldNode.isNull() && fieldNode.isNumber()) {
            return fieldNode.asDouble();
        }
        return null;
    }
}
