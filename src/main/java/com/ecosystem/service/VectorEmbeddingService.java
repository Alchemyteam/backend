package com.ecosystem.service;

import com.ecosystem.entity.ProductMaster;
import com.ecosystem.repository.ProductMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating embeddings using Cohere and storing them in Qdrant.
 * This service integrates embedding generation with vector database storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cohere.enabled", havingValue = "true", matchIfMissing = false)
public class VectorEmbeddingService {

    private final ProductMasterRepository productMasterRepository;
    private final EmbeddingService embeddingService;
    private final CohereEmbeddingService cohereEmbeddingService;
    private final QdrantService qdrantService;

    /**
     * Generate embeddings and store vectors in Qdrant for all records that have embedding_text.
     * 
     * @param batchSize The number of records to process per batch
     * @return Batch processing result with statistics
     */
    @Transactional
    public VectorBatchResult generateAndStoreVectors(int batchSize) {
        log.info("Starting vector generation and storage for all records (batch size: {})", batchSize);
        
        // Ensure Qdrant collection exists
        if (!qdrantService.collectionExists()) {
            log.info("Creating Qdrant collection...");
            qdrantService.createCollection();
        }

        int totalProcessed = 0;
        int totalStored = 0;
        int totalSkipped = 0;
        int page = 0;
        
        Pageable pageable = PageRequest.of(page, batchSize);
        Page<ProductMaster> productMasterPage = productMasterRepository.findAll(pageable);
        
        while (productMasterPage.hasContent()) {
            List<ProductMaster> productMasterList = productMasterPage.getContent();
            
            // Filter records that have embedding_text but may not have vectors in Qdrant
            List<ProductMaster> recordsToProcess = productMasterList.stream()
                    .filter(pm -> pm.getEmbeddingText() != null && !pm.getEmbeddingText().trim().isEmpty())
                    .collect(Collectors.toList());
            
            if (recordsToProcess.isEmpty()) {
                // Move to next page
                if (productMasterPage.hasNext()) {
                    page++;
                    pageable = PageRequest.of(page, batchSize);
                    productMasterPage = productMasterRepository.findAll(pageable);
                } else {
                    break;
                }
                continue;
            }
            
            // Generate embeddings in batch
            List<String> texts = recordsToProcess.stream()
                    .map(ProductMaster::getEmbeddingText)
                    .collect(Collectors.toList());
            
            log.info("Generating embeddings for {} texts using Cohere API...", texts.size());
            
            // Add delay to avoid rate limiting (Trial key: 100 calls/minute)
            // Process in smaller batches to stay under rate limit
            int cohereBatchSize = 10; // Process 10 texts at a time to avoid rate limits
            List<List<Float>> allVectors = new ArrayList<>();
            
            for (int i = 0; i < texts.size(); i += cohereBatchSize) {
                int end = Math.min(i + cohereBatchSize, texts.size());
                List<String> batchTexts = texts.subList(i, end);
                
                log.debug("Processing Cohere batch {}/{} ({} texts)", 
                        (i / cohereBatchSize) + 1, 
                        (texts.size() + cohereBatchSize - 1) / cohereBatchSize,
                        batchTexts.size());
                
                List<List<Float>> batchVectors = cohereEmbeddingService.generateEmbeddings(batchTexts);
                allVectors.addAll(batchVectors);
                
                // Add delay between batches to avoid rate limiting (100 calls/minute = ~1.67 calls/second)
                // Wait 1 second between batches to stay under limit
                if (i + cohereBatchSize < texts.size()) {
                    try {
                        Thread.sleep(1000); // 1 second delay
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted during rate limit delay");
                        break;
                    }
                }
            }
            
            List<List<Float>> vectors = allVectors;
            
            if (vectors.isEmpty()) {
                log.error("Cohere API returned empty vectors. Check Cohere API key, rate limits, and network connection.");
                totalSkipped += recordsToProcess.size();
                continue;
            }
            
            if (vectors.size() != recordsToProcess.size()) {
                log.warn("Mismatch between records and vectors: {} records, {} vectors", 
                        recordsToProcess.size(), vectors.size());
            }
            
            // Check vector dimension
            if (!vectors.isEmpty() && !vectors.get(0).isEmpty()) {
                int vectorDim = vectors.get(0).size();
                log.info("Generated vectors with dimension: {}", vectorDim);
                if (vectorDim != 1024) {
                    log.warn("Vector dimension is {}, expected 1024. This may cause Qdrant insertion to fail.", vectorDim);
                }
            }
            
            // Prepare data for Qdrant insertion
            // Note: Qdrant uses String IDs, so we'll use product_uid
            List<String> ids = new ArrayList<>();
            List<List<Float>> validVectors = new ArrayList<>();
            
            for (int i = 0; i < recordsToProcess.size() && i < vectors.size(); i++) {
                ProductMaster productMaster = recordsToProcess.get(i);
                List<Float> vector = vectors.get(i);
                
                if (vector != null && !vector.isEmpty()) {
                    // Convert product_uid to a numeric ID for Qdrant (or use string if Qdrant supports it)
                    // For now, we'll use a hash of product_uid as Long
                    ids.add(productMaster.getProductUid());
                    validVectors.add(vector);
                    totalProcessed++;
                } else {
                    totalSkipped++;
                    log.debug("Skipped ProductMaster productUid={} - failed to generate vector", productMaster.getProductUid());
                }
            }
            
            // Store vectors in Qdrant
            if (!ids.isEmpty() && !validVectors.isEmpty()) {
                // Convert String IDs to Long for Qdrant (using hash)
                // Note: Using hash might cause collisions, but for now it's acceptable
                List<Long> longIds = ids.stream()
                        .map(id -> {
                            // Use a more stable hash to avoid negative numbers
                            long hash = id.hashCode();
                            return hash < 0 ? Math.abs(hash) : hash;
                        })
                        .collect(Collectors.toList());
                
                log.info("Attempting to store {} vectors in Qdrant (first ID: {}, last ID: {})", 
                        ids.size(), longIds.get(0), longIds.get(longIds.size() - 1));
                
                boolean success = qdrantService.insertVectors(longIds, validVectors);
                if (success) {
                    totalStored += ids.size();
                    log.info("Stored {} vectors in Qdrant (total stored: {})", ids.size(), totalStored);
                } else {
                    log.error("Failed to store {} vectors in Qdrant. Check logs above for details.", ids.size());
                    totalSkipped += ids.size();
                }
            }
            
            // Move to next page
            if (productMasterPage.hasNext()) {
                page++;
                pageable = PageRequest.of(page, batchSize);
                productMasterPage = productMasterRepository.findAll(pageable);
            } else {
                break;
            }
        }
        
        log.info("Vector generation and storage completed. Total processed: {}, Stored: {}, Skipped: {}", 
                totalProcessed, totalStored, totalSkipped);
        
        return new VectorBatchResult(totalProcessed, totalStored, totalSkipped);
    }

    /**
     * Generate embedding and store vector for a single ProductMaster record.
     * 
     * @param productMaster The ProductMaster record
     * @return true if successful
     */
    public boolean generateAndStoreVector(ProductMaster productMaster) {
        if (productMaster == null || productMaster.getEmbeddingText() == null || 
            productMaster.getEmbeddingText().trim().isEmpty()) {
            log.warn("Cannot generate vector for ProductMaster without embedding_text");
            return false;
        }

        // Ensure Qdrant collection exists
        if (!qdrantService.collectionExists()) {
            qdrantService.createCollection();
        }

        // Generate embedding
        List<Float> vector = cohereEmbeddingService.generateEmbedding(productMaster.getEmbeddingText());
        if (vector == null || vector.isEmpty()) {
            log.error("Failed to generate vector for ProductMaster productUid={}", productMaster.getProductUid());
            return false;
        }

        // Store in Qdrant (convert product_uid to Long hash)
        List<Long> ids = List.of((long) productMaster.getProductUid().hashCode());
        List<List<Float>> vectors = List.of(vector);
        return qdrantService.insertVectors(ids, vectors);
    }

    /**
     * Search for similar products using vector similarity search.
     * 
     * @param queryText The query text
     * @param topK Number of results to return
     * @return List of SalesData IDs that are similar to the query
     */
    public List<Long> searchSimilar(String queryText, int topK) {
        if (queryText == null || queryText.trim().isEmpty()) {
            log.warn("Cannot search with empty query text");
            return new ArrayList<>();
        }

        // Generate embedding for query
        List<Float> queryVector = cohereEmbeddingService.generateEmbedding(queryText);
        if (queryVector == null || queryVector.isEmpty()) {
            log.error("Failed to generate query vector");
            return new ArrayList<>();
        }

        // Search in Qdrant
        return qdrantService.searchSimilar(queryVector, topK);
    }

    /**
     * Result class for vector batch processing operations.
     */
    public static class VectorBatchResult {
        private final int totalProcessed;
        private final int totalStored;
        private final int totalSkipped;

        public VectorBatchResult(int totalProcessed, int totalStored, int totalSkipped) {
            this.totalProcessed = totalProcessed;
            this.totalStored = totalStored;
            this.totalSkipped = totalSkipped;
        }

        public int getTotalProcessed() {
            return totalProcessed;
        }

        public int getTotalStored() {
            return totalStored;
        }

        public int getTotalSkipped() {
            return totalSkipped;
        }
    }
}

