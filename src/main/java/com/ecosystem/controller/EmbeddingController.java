package com.ecosystem.controller;

import com.ecosystem.dto.ErrorResponse;
import com.ecosystem.service.EmbeddingBatchService;
import com.ecosystem.service.VectorEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for embedding generation and batch processing operations.
 */
@Slf4j
@RestController
@RequestMapping("/admin/embedding")
@RequiredArgsConstructor
public class EmbeddingController {

    private final EmbeddingBatchService embeddingBatchService;
    private final VectorEmbeddingService vectorEmbeddingService;

    /**
     * Generate embedding_text and embedding_hash for all SalesData records.
     * 
     * @param batchSize Optional batch size (default: 100)
     * @return Batch processing result
     */
    @PostMapping("/generate-all")
    public ResponseEntity<?> generateAllEmbeddings(
            @RequestParam(defaultValue = "100") int batchSize) {
        try {
            log.info("Starting batch embedding generation for all records (batch size: {})", batchSize);
            
            EmbeddingBatchService.BatchProcessingResult result = 
                    embeddingBatchService.processAllRecords(batchSize);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Batch processing completed");
            response.put("totalProcessed", result.getTotalProcessed());
            response.put("totalUpdated", result.getTotalUpdated());
            response.put("totalSkipped", result.getTotalSkipped());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during batch embedding generation", e);
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setMessage("Failed to generate embeddings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * Generate embedding_text and embedding_hash only for records that don't have them yet.
     * 
     * @param batchSize Optional batch size (default: 100)
     * @return Batch processing result
     */
    @PostMapping("/generate-missing")
    public ResponseEntity<?> generateMissingEmbeddings(
            @RequestParam(defaultValue = "100") int batchSize) {
        try {
            log.info("Starting batch embedding generation for missing records (batch size: {})", batchSize);
            
            EmbeddingBatchService.BatchProcessingResult result = 
                    embeddingBatchService.processMissingEmbeddings(batchSize);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Batch processing completed");
            response.put("totalProcessed", result.getTotalProcessed());
            response.put("totalUpdated", result.getTotalUpdated());
            response.put("totalSkipped", result.getTotalSkipped());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during batch embedding generation", e);
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setMessage("Failed to generate embeddings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * Generate vectors using Cohere and store them in Qdrant for all records that have embedding_text.
     * 
     * @param batchSize Optional batch size (default: 100)
     * @return Batch processing result
     */
    @PostMapping("/generate-vectors")
    public ResponseEntity<?> generateAndStoreVectors(
            @RequestParam(defaultValue = "100") int batchSize) {
        try {
            log.info("Starting vector generation and storage (batch size: {})", batchSize);
            
            VectorEmbeddingService.VectorBatchResult result = 
                    vectorEmbeddingService.generateAndStoreVectors(batchSize);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Vector generation and storage completed");
            response.put("totalProcessed", result.getTotalProcessed());
            response.put("totalStored", result.getTotalStored());
            response.put("totalSkipped", result.getTotalSkipped());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during vector generation and storage", e);
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setMessage("Failed to generate and store vectors: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * Search for similar products using vector similarity search.
     * 
     * @param query The search query text
     * @param topK Number of results to return (default: 10)
     * @return List of similar product IDs
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchSimilar(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int topK) {
        try {
            log.info("Searching for similar products: query='{}', topK={}", query, topK);
            
            java.util.List<Long> similarIds = vectorEmbeddingService.searchSimilar(query, topK);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("query", query);
            response.put("topK", topK);
            response.put("results", similarIds);
            response.put("count", similarIds.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during vector search", e);
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setMessage("Failed to search: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
}

