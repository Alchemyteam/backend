package com.ecosystem.service;

import com.ecosystem.entity.ProductMaster;
import com.ecosystem.repository.ProductMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Batch service for generating and updating embedding text and hash for all ProductMaster records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingBatchService {

    private final ProductMasterRepository productMasterRepository;
    private final EmbeddingService embeddingService;

    /**
     * Batch process all SalesData records to generate embedding_text and embedding_hash.
     * Processes records in batches to avoid memory issues.
     * 
     * @param batchSize The number of records to process per batch (default: 100)
     * @return Batch processing result with statistics
     */
    @Transactional
    public BatchProcessingResult processAllRecords(int batchSize) {
        log.info("Starting batch processing of embedding generation for all ProductMaster records (batch size: {})", batchSize);
        
        int totalProcessed = 0;
        int totalUpdated = 0;
        int totalSkipped = 0;
        int page = 0;
        
        Pageable pageable = PageRequest.of(page, batchSize);
        Page<ProductMaster> productMasterPage = productMasterRepository.findAll(pageable);
        
        while (productMasterPage.hasContent()) {
            List<ProductMaster> productMasterList = productMasterPage.getContent();
            
            for (ProductMaster productMaster : productMasterList) {
                totalProcessed++;
                
                try {
                    String[] result = embeddingService.generateEmbeddingTextAndHash(productMaster);
                    String embeddingText = result[0];
                    String embeddingHash = result[1];
                    
                    if (embeddingText != null && embeddingHash != null) {
                        productMaster.setEmbeddingText(embeddingText);
                        productMaster.setEmbeddingHash(embeddingHash);
                        productMasterRepository.save(productMaster);
                        totalUpdated++;
                        
                        if (totalUpdated % 100 == 0) {
                            log.debug("Processed {} records, updated {} records", totalProcessed, totalUpdated);
                        }
                    } else {
                        totalSkipped++;
                        log.debug("Skipped ProductMaster productUid={} - ItemName is empty", productMaster.getProductUid());
                    }
                } catch (Exception e) {
                    log.error("Error processing ProductMaster productUid={}", productMaster.getProductUid(), e);
                    totalSkipped++;
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
        
        log.info("Batch processing completed. Total processed: {}, Updated: {}, Skipped: {}", 
                totalProcessed, totalUpdated, totalSkipped);
        
        return new BatchProcessingResult(totalProcessed, totalUpdated, totalSkipped);
    }

    /**
     * Process only records that don't have embedding_text or embedding_hash yet.
     * 
     * @param batchSize The number of records to process per batch (default: 100)
     * @return Batch processing result with statistics
     */
    @Transactional
    public BatchProcessingResult processMissingEmbeddings(int batchSize) {
        log.info("Starting batch processing of missing embeddings (batch size: {})", batchSize);
        
        // Get all records where embedding_text is null or empty
        List<ProductMaster> productMasterList = productMasterRepository.findWithoutEmbeddingText();
        
        int totalProcessed = 0;
        int totalUpdated = 0;
        int totalSkipped = 0;
        
        for (ProductMaster productMaster : productMasterList) {
            totalProcessed++;
            
            try {
                String[] result = embeddingService.generateEmbeddingTextAndHash(productMaster);
                String embeddingText = result[0];
                String embeddingHash = result[1];
                
                if (embeddingText != null && embeddingHash != null) {
                    productMaster.setEmbeddingText(embeddingText);
                    productMaster.setEmbeddingHash(embeddingHash);
                    productMasterRepository.save(productMaster);
                    totalUpdated++;
                    
                    if (totalUpdated % 100 == 0) {
                        log.debug("Processed {} records, updated {} records", totalProcessed, totalUpdated);
                    }
                } else {
                    totalSkipped++;
                    log.debug("Skipped ProductMaster productUid={} - ItemName is empty", productMaster.getProductUid());
                }
            } catch (Exception e) {
                log.error("Error processing ProductMaster productUid={}", productMaster.getProductUid(), e);
                totalSkipped++;
            }
        }
        
        log.info("Batch processing completed. Total processed: {}, Updated: {}, Skipped: {}", 
                totalProcessed, totalUpdated, totalSkipped);
        
        return new BatchProcessingResult(totalProcessed, totalUpdated, totalSkipped);
    }

    /**
     * Result class for batch processing operations.
     */
    public static class BatchProcessingResult {
        private final int totalProcessed;
        private final int totalUpdated;
        private final int totalSkipped;

        public BatchProcessingResult(int totalProcessed, int totalUpdated, int totalSkipped) {
            this.totalProcessed = totalProcessed;
            this.totalUpdated = totalUpdated;
            this.totalSkipped = totalSkipped;
        }

        public int getTotalProcessed() {
            return totalProcessed;
        }

        public int getTotalUpdated() {
            return totalUpdated;
        }

        public int getTotalSkipped() {
            return totalSkipped;
        }
    }
}

