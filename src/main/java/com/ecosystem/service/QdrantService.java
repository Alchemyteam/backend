package com.ecosystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
 * Service for Qdrant vector database operations using HTTP REST API.
 * Handles collection creation, vector insertion, and similarity search.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "qdrant.enabled", havingValue = "true", matchIfMissing = false)
public class QdrantService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${qdrant.host:localhost}")
    private String host;

    @Value("${qdrant.port:6333}")
    private int port;

    @Value("${qdrant.collection:sales_data_vectors}")
    private String collectionName;

    @Value("${qdrant.vector-size:1024}")
    private int vectorSize;

    private String getBaseUrl() {
        return "http://" + host + ":" + port;
    }

    /**
     * Create collection for storing sales data vectors.
     * 
     * @return true if collection was created successfully
     */
    public boolean createCollection() {
        log.info("Creating Qdrant collection: {} with vector size: {}", collectionName, vectorSize);

        try {
            // Check if collection exists
            if (collectionExists()) {
                log.info("Collection {} already exists", collectionName);
                return true;
            }

            // Create collection - Qdrant API format
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> vectorsConfig = new HashMap<>();
            Map<String, Object> params = new HashMap<>();
            params.put("size", vectorSize);
            params.put("distance", "Cosine");
            vectorsConfig.put("params", params);
            
            requestBody.put("vectors", vectorsConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String url = getBaseUrl() + "/collections/" + collectionName;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Collection {} created successfully", collectionName);
                // Wait a bit for collection to be ready
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            } else {
                log.error("Failed to create collection. Status: {}, Response: {}", 
                        response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to create collection: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Insert vectors into the collection.
     * 
     * @param ids List of SalesData IDs
     * @param vectors List of embedding vectors
     * @return true if insertion was successful
     */
    public boolean insertVectors(List<Long> ids, List<List<Float>> vectors) {
        if (ids == null || vectors == null || ids.size() != vectors.size()) {
            log.error("Invalid input: ids and vectors must have the same size");
            return false;
        }

        // Check if collection exists
        if (!collectionExists()) {
            log.error("Collection {} does not exist. Please create it first.", collectionName);
            return false;
        }

        // Validate vector dimensions
        if (!vectors.isEmpty()) {
            int firstVectorSize = vectors.get(0).size();
            if (firstVectorSize != vectorSize) {
                log.error("Vector dimension mismatch: expected {}, got {}", vectorSize, firstVectorSize);
                return false;
            }
        }

        log.info("Inserting {} vectors into collection: {}", ids.size(), collectionName);

        try {
            List<Map<String, Object>> points = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                Map<String, Object> point = new HashMap<>();
                point.put("id", ids.get(i));
                point.put("vector", vectors.get(i));
                points.add(point);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("points", points);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String url = getBaseUrl() + "/collections/" + collectionName + "/points";
            
            log.debug("Inserting vectors to: {}", url);
            log.debug("First point sample: id={}, vector_size={}", 
                    ids.get(0), vectors.get(0).size());
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // Parse response to verify insertion
                if (response.getBody() != null) {
                    try {
                        JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                        JsonNode statusNode = jsonResponse.get("status");
                        if (statusNode != null && "ok".equals(statusNode.asText())) {
                            log.info("Successfully inserted {} vectors", ids.size());
                            return true;
                        } else {
                            log.error("Qdrant returned non-ok status: {}", response.getBody());
                            return false;
                        }
                    } catch (Exception e) {
                        log.warn("Could not parse Qdrant response, but status code was 2xx: {}", response.getBody());
                        log.info("Assuming success for {} vectors", ids.size());
                        return true;
                    }
                } else {
                    log.info("Successfully inserted {} vectors (no response body)", ids.size());
                    return true;
                }
            } else {
                log.error("Failed to insert vectors. Status: {}, Response: {}", 
                        response.getStatusCode(), response.getBody());
                // Log request body for debugging (first point only)
                if (!points.isEmpty()) {
                    log.debug("Sample point structure: id={}, vector_size={}", 
                            points.get(0).get("id"), 
                            ((List<?>) points.get(0).get("vector")).size());
                }
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to insert vectors: {}", e.getMessage(), e);
            // Log more details about the request
            if (ids != null && !ids.isEmpty()) {
                log.debug("First ID: {}, Last ID: {}, Total IDs: {}", 
                        ids.get(0), ids.get(ids.size() - 1), ids.size());
            }
            if (vectors != null && !vectors.isEmpty()) {
                log.debug("First vector size: {}, Last vector size: {}", 
                        vectors.get(0).size(), vectors.get(vectors.size() - 1).size());
            }
            return false;
        }
    }

    /**
     * Search for similar vectors.
     * 
     * @param queryVector The query vector
     * @param topK Number of results to return
     * @return List of similar IDs
     */
    public List<Long> searchSimilar(List<Float> queryVector, int topK) {
        log.info("Searching for {} similar vectors in collection: {}", topK, collectionName);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("vector", queryVector);
            requestBody.put("top", topK);
            requestBody.put("with_payload", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String url = getBaseUrl() + "/collections/" + collectionName + "/points/search";
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode resultNode = jsonResponse.get("result");

                List<Long> resultIds = new ArrayList<>();
                if (resultNode != null && resultNode.isArray()) {
                    for (JsonNode pointNode : resultNode) {
                        JsonNode idNode = pointNode.get("id");
                        if (idNode != null && idNode.isNumber()) {
                            resultIds.add(idNode.asLong());
                        }
                    }
                }

                log.info("Found {} similar vectors", resultIds.size());
                return resultIds;
            } else {
                log.error("Search failed: {}", response.getBody());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Check if collection exists.
     * 
     * @return true if collection exists
     */
    public boolean collectionExists() {
        try {
            String url = getBaseUrl() + "/collections/" + collectionName;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );
            boolean exists = response.getStatusCode().is2xxSuccessful();
            if (!exists) {
                log.warn("Collection {} does not exist. Status: {}, Response: {}", 
                        collectionName, response.getStatusCode(), response.getBody());
            } else {
                log.debug("Collection {} exists", collectionName);
            }
            return exists;
        } catch (Exception e) {
            log.warn("Error checking collection existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Delete collection.
     * 
     * @return true if deletion was successful
     */
    public boolean deleteCollection() {
        log.info("Deleting collection: {}", collectionName);
        try {
            String url = getBaseUrl() + "/collections/" + collectionName;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    null,
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Collection {} deleted successfully", collectionName);
                return true;
            } else {
                log.error("Failed to delete collection: {}", response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to delete collection: {}", e.getMessage(), e);
            return false;
        }
    }
}
