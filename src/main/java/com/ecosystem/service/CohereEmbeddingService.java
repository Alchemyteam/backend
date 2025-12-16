package com.ecosystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating embeddings using Cohere API via HTTP REST.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "cohere.enabled", havingValue = "true", matchIfMissing = false)
public class CohereEmbeddingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cohere.api.key}")
    private String apiKey;

    @Value("${cohere.api.model:embed-english-v3.0}")
    private String model;

    @Value("${cohere.api.input-type:search_document}")
    private String inputType;

    private static final String COHERE_API_URL = "https://api.cohere.ai/v1/embed";

    public CohereEmbeddingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate embedding vector for a single text.
     * 
     * @param text The text to embed
     * @return List of floats representing the embedding vector, or null if failed
     */
    public List<Float> generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Cannot generate embedding for empty text");
            return null;
        }

        try {
            List<String> texts = List.of(text);
            List<List<Float>> embeddings = generateEmbeddings(texts);
            
            if (embeddings != null && !embeddings.isEmpty()) {
                return embeddings.get(0);
            }
            return null;
        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate embeddings for multiple texts in batch.
     * 
     * @param texts List of texts to embed
     * @return List of embedding vectors (one per text), or empty list if failed
     */
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            log.warn("Cannot generate embeddings for empty text list");
            return new ArrayList<>();
        }

        try {
            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("texts", texts);
            requestBody.put("model", model);
            requestBody.put("input_type", inputType);
            requestBody.put("truncate", "END"); // Truncate if text is too long

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Accept", "application/json");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Make API call with retry logic for rate limiting
            ResponseEntity<String> response = null;
            int maxRetries = 3;
            long retryDelayMs = 60000; // 60 seconds for rate limit
            
            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    response = restTemplate.exchange(
                            COHERE_API_URL,
                            HttpMethod.POST,
                            request,
                            String.class
                    );
                    
                    // If successful, break out of retry loop
                    if (response.getStatusCode() == HttpStatus.OK) {
                        break;
                    }
                    
                    // If rate limited (429), wait and retry
                    if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                        if (attempt < maxRetries - 1) {
                            log.warn("Rate limited (429). Waiting {} seconds before retry {}/{}", 
                                    retryDelayMs / 1000, attempt + 1, maxRetries);
                            Thread.sleep(retryDelayMs);
                            continue;
                        } else {
                            log.error("Rate limited after {} retries. Please wait or upgrade to Production API key.", maxRetries);
                            return new ArrayList<>();
                        }
                    }
                } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                    if (attempt < maxRetries - 1) {
                        log.warn("Rate limited (429). Waiting {} seconds before retry {}/{}", 
                                retryDelayMs / 1000, attempt + 1, maxRetries);
                        try {
                            Thread.sleep(retryDelayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.error("Retry interrupted");
                            return new ArrayList<>();
                        }
                        continue;
                    } else {
                        log.error("Rate limited after {} retries: {}", maxRetries, e.getMessage());
                        return new ArrayList<>();
                    }
                }
            }
            
            if (response == null) {
                log.error("Failed to get response from Cohere API after {} retries", maxRetries);
                return new ArrayList<>();
            }

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Parse response
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                // Check for errors in response
                JsonNode errorNode = jsonResponse.get("message");
                if (errorNode != null) {
                    log.error("Cohere API returned error: {}", errorNode.asText());
                    return new ArrayList<>();
                }
                
                JsonNode embeddingsNode = jsonResponse.get("embeddings");

                if (embeddingsNode != null && embeddingsNode.isArray()) {
                    List<List<Float>> floatEmbeddings = new ArrayList<>();
                    for (JsonNode embeddingNode : embeddingsNode) {
                        if (embeddingNode.isArray()) {
                            List<Float> floatEmbedding = new ArrayList<>();
                            for (JsonNode valueNode : embeddingNode) {
                                if (valueNode.isNumber()) {
                                    floatEmbedding.add((float) valueNode.asDouble());
                                }
                            }
                            floatEmbeddings.add(floatEmbedding);
                        }
                    }
                    log.info("Generated {} embedding vectors (first vector size: {})", 
                            floatEmbeddings.size(), 
                            floatEmbeddings.isEmpty() ? 0 : floatEmbeddings.get(0).size());
                    return floatEmbeddings;
                } else {
                    log.error("Invalid response format: embeddings field not found or not an array. Response: {}", 
                            response.getBody());
                    return new ArrayList<>();
                }
            } else if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.error("Cohere API rate limit exceeded (429). Response: {}", response.getBody());
                log.error("Trial API keys are limited to 100 calls/minute. Please wait or upgrade to Production key.");
                return new ArrayList<>();
            } else {
                log.error("Cohere API call failed with status: {}, Response: {}", 
                        response.getStatusCode(), response.getBody());
                return new ArrayList<>();
            }
        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            log.error("Cohere API rate limit exceeded (429): {}", e.getMessage());
            log.error("Trial API keys are limited to 100 calls/minute. Please wait 60 seconds or upgrade to Production key.");
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error generating embeddings: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
