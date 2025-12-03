package com.ecosystem.service;

import com.ecosystem.dto.buyer.PaginationResponse;
import com.ecosystem.dto.buyer.SalesDataListResponse;
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


    private SalesDataResponse toSalesDataResponse(SalesData salesData) {
        SalesDataResponse response = new SalesDataResponse();
        
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

