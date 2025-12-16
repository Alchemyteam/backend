package com.ecosystem.service;

import com.ecosystem.entity.ProductMaster;
import com.ecosystem.entity.SalesData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating embedding text and hash for AI search functionality.
 * Generates English description text by concatenating product fields.
 */
@Slf4j
@Service
public class EmbeddingService {

    /**
     * Generate embedding text from SalesData entity.
     * Concatenates fields in the specified order, skipping empty/null values.
     * 
     * Format: ItemName: {ItemName}; Model: {Model}; Performance: {Performance}; 
     *         Performance2: {Performance.1}; Material: {Material}; Brand: {Brand Code}; 
     *         UOM: {UOM}; Function: {Function}; ItemType: {ItemType}; Hierarchy: {Product Hierarchy 3}
     * 
     * @param salesData The SalesData entity
     * @return The generated embedding text, or null if ItemName is empty
     */
    public String generateEmbeddingText(SalesData salesData) {
        if (salesData == null) {
            return null;
        }

        // ItemName is required (most important field)
        String itemName = salesData.getItemName();
        if (itemName == null || itemName.trim().isEmpty()) {
            log.debug("Skipping embedding generation for SalesData id={} - ItemName is empty", salesData.getId());
            return null;
        }

        List<String> parts = new ArrayList<>();

        // ItemName (required, most important)
        parts.add("ItemName: " + itemName.trim());

        // Model
        addField(parts, "Model", salesData.getModel());

        // Performance
        addField(parts, "Performance", salesData.getPerformance());

        // Performance.1 (Performance2)
        addField(parts, "Performance2", salesData.getPerformance1());

        // Material
        addField(parts, "Material", salesData.getMaterial());

        // Brand Code
        addField(parts, "Brand", salesData.getBrandCode());

        // UOM
        addField(parts, "UOM", salesData.getUom());

        // Function
        addField(parts, "Function", salesData.getFunction());

        // ItemType
        addField(parts, "ItemType", salesData.getItemType());

        // Product Hierarchy 3
        addField(parts, "Hierarchy", salesData.getProductHierarchy3());

        return String.join("; ", parts);
    }

    /**
     * Add a field to the parts list if it's not empty.
     * 
     * @param parts The list of parts to add to
     * @param fieldName The name of the field
     * @param fieldValue The value of the field
     */
    private void addField(List<String> parts, String fieldName, String fieldValue) {
        if (fieldValue != null && !fieldValue.trim().isEmpty()) {
            parts.add(fieldName + ": " + fieldValue.trim());
        }
    }

    /**
     * Generate SHA256 hash of the embedding text.
     * 
     * @param embeddingText The embedding text
     * @return The SHA256 hash as a hexadecimal string, or null if input is null/empty
     */
    public String generateEmbeddingHash(String embeddingText) {
        if (embeddingText == null || embeddingText.trim().isEmpty()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(embeddingText.getBytes(StandardCharsets.UTF_8));
            
            // Convert bytes to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generate both embedding text and hash for a SalesData entity.
     * 
     * @param salesData The SalesData entity
     * @return An array with [embeddingText, embeddingHash], or [null, null] if ItemName is empty
     */
    public String[] generateEmbeddingTextAndHash(SalesData salesData) {
        String embeddingText = generateEmbeddingText(salesData);
        if (embeddingText == null) {
            return new String[]{null, null};
        }
        
        String embeddingHash = generateEmbeddingHash(embeddingText);
        return new String[]{embeddingText, embeddingHash};
    }

    /**
     * Generate embedding text from ProductMaster entity.
     * Uses the same format as SalesData.
     * 
     * @param productMaster The ProductMaster entity
     * @return The generated embedding text, or null if itemName is empty
     */
    public String generateEmbeddingText(ProductMaster productMaster) {
        if (productMaster == null) {
            return null;
        }

        // item_name is required (most important field)
        String itemName = productMaster.getItemName();
        if (itemName == null || itemName.trim().isEmpty()) {
            log.debug("Skipping embedding generation for ProductMaster productUid={} - itemName is empty", productMaster.getProductUid());
            return null;
        }

        List<String> parts = new ArrayList<>();

        // ItemName (required, most important)
        parts.add("ItemName: " + itemName.trim());

        // Model
        addField(parts, "Model", productMaster.getModel());

        // Performance (performance_micron)
        addField(parts, "Performance", productMaster.getPerformanceMicron());

        // Performance2 (performance_efficiency)
        addField(parts, "Performance2", productMaster.getPerformanceEfficiency());

        // Material
        addField(parts, "Material", productMaster.getMaterial());

        // Brand Code
        addField(parts, "Brand", productMaster.getBrandCode());

        // UOM
        addField(parts, "UOM", productMaster.getUom());

        // Function (function_name)
        addField(parts, "Function", productMaster.getFunctionName());

        // ItemType (item_type)
        addField(parts, "ItemType", productMaster.getItemType());

        // Product Hierarchy 3 (product_hierarchy_3)
        addField(parts, "Hierarchy", productMaster.getProductHierarchy3());

        return String.join("; ", parts);
    }

    /**
     * Generate both embedding text and hash for a ProductMaster entity.
     * 
     * @param productMaster The ProductMaster entity
     * @return An array with [embeddingText, embeddingHash], or [null, null] if ItemName is empty
     */
    public String[] generateEmbeddingTextAndHash(ProductMaster productMaster) {
        String embeddingText = generateEmbeddingText(productMaster);
        if (embeddingText == null) {
            return new String[]{null, null};
        }
        
        String embeddingHash = generateEmbeddingHash(embeddingText);
        return new String[]{embeddingText, embeddingHash};
    }
}

