package com.ecosystem.service;

import com.ecosystem.dto.buyer.PaginationResponse;
import com.ecosystem.dto.buyer.SalesDataListResponse;
import com.ecosystem.dto.buyer.SalesDataRequest;
import com.ecosystem.dto.buyer.SalesDataResponse;
import com.ecosystem.entity.SalesData;
import com.ecosystem.repository.SalesDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesDataService {

    private final SalesDataRepository salesDataRepository;

    public SalesDataListResponse getSalesData(
            int page, int limit, String sort, String category, String keyword,
            String minDate, String maxDate, String txNo, Integer minQty, Integer maxQty, 
            BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minValue, BigDecimal maxValue,
            String buyerCode, String buyerName,
            String itemCode, String itemName, String productHierarchy3, String itemType, 
            String model, String material, String uom,
            String brandCode, String performance, String performance1,
            BigDecimal minUnitCost, BigDecimal maxUnitCost, String function,
            String sector, String subSector,
            String source) {
        
        // Validate and normalize parameters
        if (page < 1) page = 1;
        if (limit < 1) limit = 20;
        if (limit > 100) limit = 100; // Limit maximum page size
        if (sort == null || sort.isEmpty()) sort = "newest";
        
        // Convert empty strings to null for proper SQL query handling
        minDate = normalizeString(minDate);
        maxDate = normalizeString(maxDate);
        txNo = normalizeString(txNo);
        buyerCode = normalizeString(buyerCode);
        buyerName = normalizeString(buyerName);
        itemCode = normalizeString(itemCode);
        itemName = normalizeString(itemName);
        productHierarchy3 = normalizeString(productHierarchy3);
        itemType = normalizeString(itemType);
        model = normalizeString(model);
        material = normalizeString(material);
        uom = normalizeString(uom);
        brandCode = normalizeString(brandCode);
        performance = normalizeString(performance);
        performance1 = normalizeString(performance1);
        function = normalizeString(function);
        sector = normalizeString(sector);
        subSector = normalizeString(subSector);
        source = normalizeString(source);
        category = normalizeString(category);
        keyword = normalizeString(keyword);
        
        // Create pagination object
        Pageable pageable = PageRequest.of(page - 1, limit);
        
        // Use the new filter query method
        Page<SalesData> salesDataPage = salesDataRepository.findAllWithFilters(
            minDate, maxDate, txNo, minQty, maxQty, minPrice, maxPrice, minValue, maxValue,
            buyerCode, buyerName,
            itemCode, itemName, productHierarchy3, itemType, model, material, uom,
            brandCode, performance, performance1,
            minUnitCost, maxUnitCost, function,
            sector, subSector,
            source,
            category, keyword, sort,
            pageable
        );
        
        List<SalesDataResponse> data = salesDataPage.getContent().stream()
            .map(this::toSalesDataResponse)
            .collect(Collectors.toList());
        
        PaginationResponse pagination = new PaginationResponse(
            page, limit, salesDataPage.getTotalElements(), salesDataPage.getTotalPages()
        );
        
        return new SalesDataListResponse(data, pagination);
    }

    /**
     * Create new sales data record
     * @param request The sales data to create
     * @return The created sales data as response DTO
     */
    public SalesDataResponse createSalesData(SalesDataRequest request) {
        // Convert Request DTO → Entity
        SalesData entity = toSalesDataEntity(request);

        // Save to database
        SalesData savedEntity = salesDataRepository.save(entity);

        // Convert Entity → Response DTO and return
        return toSalesDataResponse(savedEntity);
    }

    /**
     * Update existing sales data record
     * @param txNo The transaction number (ID) of the record to update
     * @param request The updated sales data
     * @return The updated sales data as response DTO
     */
    public SalesDataResponse updateSalesData(String txNo, SalesDataRequest request) {
        // Find record by TXNo (since method parameter is still txNo for backward compatibility)
        // Note: If txNo is actually an id (number), try to parse it first
        SalesData existingEntity = null;
        try {
            Long id = Long.parseLong(txNo);
            existingEntity = salesDataRepository.findById(id).orElse(null);
        } catch (NumberFormatException e) {
            // If not a number, it might be a TXNo - but we don't have findByTxNo method
            // For now, we'll need to add that method or use a different approach
        }
        
        if (existingEntity == null) {
            throw new RuntimeException("Sales data not found with ID/TXNo: " + txNo);
        }

        // Update the entity with new values
        updateEntityFromRequest(existingEntity, request);

        // Save updated entity to database
        SalesData updatedEntity = salesDataRepository.save(existingEntity);

        // Convert Entity → Response DTO and return
        return toSalesDataResponse(updatedEntity);
    }

    /**
     * Delete sales data record
     * @param txNo The transaction number or ID of the record to delete
     */
    public void deleteSalesData(String txNo) {
        // Find record by id (try to parse as Long first)
        SalesData existingEntity = null;
        try {
            Long id = Long.parseLong(txNo);
            existingEntity = salesDataRepository.findById(id).orElse(null);
        } catch (NumberFormatException e) {
            // If not a number, it might be a TXNo
        }
        
        if (existingEntity == null) {
            throw new RuntimeException("Sales data not found with ID/TXNo: " + txNo);
        }

        // Delete from database
        salesDataRepository.delete(existingEntity);
    }

    /**
     * Get distinct category values for dropdown options
     * @return List of unique category names
     */
    public List<String> getCategories() {
        return salesDataRepository.findDistinctCategories();
    }

    /**
     * Update an existing entity with values from request
     * This allows updating without creating a new object
     */
    private void updateEntityFromRequest(SalesData entity, SalesDataRequest request) {
        // Update the ID (in case it changed in the request)
        entity.setTxNo(request.getTxNo());

        // Convert and update LocalDate → String
        if (request.getTxDate() != null) {
            entity.setTxDate(request.getTxDate().toString());
        } else {
            entity.setTxDate(null);
        }

        // Convert and update Integer → String
        if (request.getTxQty() != null) {
            entity.setTxQty(request.getTxQty().toString());
        } else {
            entity.setTxQty(null);
        }

        // Convert and update BigDecimal → String
        if (request.getTxP1() != null) {
            entity.setTxP1(request.getTxP1().toString());
        } else {
            entity.setTxP1(null);
        }

        if (request.getUnitCost() != null) {
            entity.setUnitCost(request.getUnitCost().toString());
        } else {
            entity.setUnitCost(null);
        }

        if (request.getValue() != null) {
            entity.setValue(request.getValue().toString());
        } else {
            entity.setValue(null);
        }

        // Update string fields
        entity.setBuyerCode(request.getBuyerCode());
        entity.setBuyerName(request.getBuyerName());
        entity.setItemCode(request.getItemCode());
        entity.setItemName(request.getItemName());
        entity.setProductHierarchy3(request.getProductHierarchy3());
        entity.setFunction(request.getFunction());
        entity.setItemType(request.getItemType());
        entity.setModel(request.getModel());
        entity.setPerformance(request.getPerformance());
        entity.setPerformance1(request.getPerformance1());
        entity.setMaterial(request.getMaterial());
        entity.setUom(request.getUom());
        entity.setBrandCode(request.getBrandCode());
        entity.setSector(request.getSector());
        entity.setSubSector(request.getSubSector());
        entity.setRationale(request.getRationale());
        entity.setWww(request.getWww());
        entity.setSource(request.getSource());
    }

    /**
     * Convert SalesDataRequest DTO to SalesData Entity
     * This is the REVERSE of toSalesDataResponse()
     */
    private SalesData toSalesDataEntity(SalesDataRequest request) {
        SalesData entity = new SalesData();

        // Set the ID (required field)
        entity.setTxNo(request.getTxNo());

        // Convert LocalDate → String for database
        if (request.getTxDate() != null) {
            entity.setTxDate(request.getTxDate().toString()); // "2024-01-15"
        }

        // Convert Integer → String for database
        if (request.getTxQty() != null) {
            entity.setTxQty(request.getTxQty().toString());
        }

        // Convert BigDecimal → String for database
        if (request.getTxP1() != null) {
            entity.setTxP1(request.getTxP1().toString());
        }

        if (request.getUnitCost() != null) {
            entity.setUnitCost(request.getUnitCost().toString());
        }

        if (request.getValue() != null) {
            entity.setValue(request.getValue().toString());
        }

        // Direct string assignments (no conversion needed)
        entity.setBuyerCode(request.getBuyerCode());
        entity.setBuyerName(request.getBuyerName());
        entity.setItemCode(request.getItemCode());
        entity.setItemName(request.getItemName());
        entity.setProductHierarchy3(request.getProductHierarchy3());
        entity.setFunction(request.getFunction());
        entity.setItemType(request.getItemType());
        entity.setModel(request.getModel());
        entity.setPerformance(request.getPerformance());
        entity.setPerformance1(request.getPerformance1());
        entity.setMaterial(request.getMaterial());
        entity.setUom(request.getUom());
        entity.setBrandCode(request.getBrandCode());
        entity.setSector(request.getSector());
        entity.setSubSector(request.getSubSector());
        entity.setRationale(request.getRationale());
        entity.setWww(request.getWww());
        entity.setSource(request.getSource());

        return entity;
    }

    private SalesDataResponse toSalesDataResponse(SalesData salesData) {
        SalesDataResponse response = new SalesDataResponse();
        
        // Set id field (primary key)
        response.setId(salesData.getId());
        
        // Convert date field
        if (salesData.getTxDate() != null && !salesData.getTxDate().isEmpty()) {
            try {
                // Try multiple date formats
                LocalDate date = parseDate(salesData.getTxDate());
                response.setTxDate(date);
            } catch (Exception e) {
                // Set to null if parsing fails
                response.setTxDate(null);
            }
        }
        
        // Direct assignment for string fields
        response.setTxNo(salesData.getTxNo());
        response.setBuyerCode(salesData.getBuyerCode());
        response.setBuyerName(salesData.getBuyerName());
        response.setItemCode(salesData.getItemCode());
        response.setItemName(salesData.getItemName());
        response.setProductHierarchy3(salesData.getProductHierarchy3());
        response.setFunction(salesData.getFunction());
        response.setItemType(salesData.getItemType());
        response.setModel(salesData.getModel());
        response.setPerformance(salesData.getPerformance());
        response.setPerformance1(salesData.getPerformance1());
        response.setMaterial(salesData.getMaterial());
        response.setUom(salesData.getUom());
        response.setBrandCode(salesData.getBrandCode());
        response.setSector(salesData.getSector());
        response.setSubSector(salesData.getSubSector());
        response.setRationale(salesData.getRationale());
        response.setWww(salesData.getWww());
        response.setSource(salesData.getSource());
        
        // Convert numeric fields
        response.setTxQty(parseInteger(salesData.getTxQty()));
        response.setTxP1(parseBigDecimal(salesData.getTxP1()));
        response.setUnitCost(parseBigDecimal(salesData.getUnitCost()));
        response.setValue(parseBigDecimal(salesData.getValue()));
        
        return response;
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        // Try multiple date formats
        String[] formats = {
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd/MM/yyyy",
            "MM/dd/yyyy",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss"
        };
        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDate.parse(dateStr.split(" ")[0], formatter);
            } catch (DateTimeParseException e) {
                // Continue trying next format
            }
        }
        throw new IllegalArgumentException("Unable to parse date: " + dateStr);
    }
    
    private Integer parseInteger(String value) {
        if (value == null || value.isEmpty() || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty() || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Normalize string parameter: convert empty strings or whitespace-only strings to null
     */
    private String normalizeString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}

