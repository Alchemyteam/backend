package com.ecosystem.service;

import com.ecosystem.dto.buyer.BulkImportResponse;
import com.ecosystem.dto.buyer.PaginationResponse;
import com.ecosystem.dto.buyer.SalesDataListResponse;
import com.ecosystem.dto.buyer.SalesDataRequest;
import com.ecosystem.dto.buyer.SalesDataResponse;
import com.ecosystem.entity.SalesData;
import com.ecosystem.repository.SalesDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
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
        if (page < 1)
            page = 1;
        if (limit < 1)
            limit = 20;
        if (limit > 100)
            limit = 100; // Limit maximum page size
        if (sort == null || sort.isEmpty())
            sort = "newest";

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
                pageable);

        List<SalesDataResponse> data = salesDataPage.getContent().stream()
                .map(this::toSalesDataResponse)
                .collect(Collectors.toList());

        PaginationResponse pagination = new PaginationResponse(
                page, limit, salesDataPage.getTotalElements(), salesDataPage.getTotalPages());

        return new SalesDataListResponse(data, pagination);
    }

    /**
     * Create new sales data record
     * 
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
     * 
     * @param txNo    The transaction number (ID) of the record to update
     * @param request The updated sales data
     * @return The updated sales data as response DTO
     */
    public SalesDataResponse updateSalesData(String txNo, SalesDataRequest request) {
        // Find record by TXNo (since method parameter is still txNo for backward
        // compatibility)
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
     * 
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
     * 
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
     * Normalize string parameter: convert empty strings or whitespace-only strings
     * to null
     */
    private String normalizeString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Bulk import sales data from Excel file
     * 
     * @param file The Excel file to import
     * @return BulkImportResponse with success/failed counts and error messages
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public BulkImportResponse bulkImportFromExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                errors.add("File is empty or not provided");
                return new BulkImportResponse(0, 1, errors);
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
                errors.add("Invalid file format. Only .xlsx and .xls files are supported");
                return new BulkImportResponse(0, 1, errors);
            }

            // Parse Excel file
            List<SalesDataRequest> dataList = parseExcelFile(file);
            log.info("Parsed {} rows from Excel file", dataList.size());

            // Process each record
            for (int i = 0; i < dataList.size(); i++) {
                SalesDataRequest request = dataList.get(i);
                int rowNum = i + 2; // +2 because Excel rows start at 1 and we skip header

                try {
                    // Debug: Log parsed data
                    log.debug("Processing row {}: TXNo={}, TXDate={}, ItemCode={}",
                            rowNum, request.getTxNo(), request.getTxDate(), request.getItemCode());

                    // Validate required fields
                    if (request.getTxNo() == null || request.getTxNo().trim().isEmpty()) {
                        errors.add(String.format("Row %d: Transaction Number (TXNo) is required", rowNum));
                        failedCount++;
                        log.warn("Row {} skipped: TXNo is missing", rowNum);
                        continue;
                    }

                    // Validate date format if provided
                    if (request.getTxDate() != null) {
                        // Date validation is already done in parseExcelFile, but double-check
                        try {
                            request.getTxDate().toString(); // This will throw if invalid
                        } catch (Exception e) {
                            errors.add(String.format("Row %d: Invalid date format for TXDate. Expected YYYY-MM-DD",
                                    rowNum));
                            failedCount++;
                            continue;
                        }
                    }

                    // Validate numeric fields
                    if (request.getTxQty() != null && request.getTxQty() < 0) {
                        errors.add(String.format("Row %d: TXQty cannot be negative", rowNum));
                        failedCount++;
                        continue;
                    }

                    if (request.getTxP1() != null && request.getTxP1().compareTo(BigDecimal.ZERO) < 0) {
                        errors.add(String.format("Row %d: TXP1 cannot be negative", rowNum));
                        failedCount++;
                        continue;
                    }

                    if (request.getUnitCost() != null && request.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                        errors.add(String.format("Row %d: Unit Cost cannot be negative", rowNum));
                        failedCount++;
                        continue;
                    }

                    if (request.getValue() != null && request.getValue().compareTo(BigDecimal.ZERO) < 0) {
                        errors.add(String.format("Row %d: Value cannot be negative", rowNum));
                        failedCount++;
                        continue;
                    }

                    // Save record in a separate transaction to avoid rollback issues
                    saveSalesDataRecord(request, rowNum);
                    
                    successCount++;
                    log.info("Successfully processed row {} with TXNo: {} (success count: {})", rowNum, 
                            request.getTxNo().trim(), successCount);

                } catch (Exception e) {
                    log.error("Error importing row {}: {}", rowNum, e.getMessage(), e);
                    log.error("Exception details for row {}: ", rowNum, e);
                    String errorMessage = e.getMessage();
                    if (e.getCause() != null) {
                        errorMessage += " (Cause: " + e.getCause().getMessage() + ")";
                    }
                    errors.add(String.format("Row %d: %s", rowNum, errorMessage));
                    failedCount++;
                }
            }

        } catch (Exception e) {
            log.error("Error processing Excel file: {}", e.getMessage(), e);
            errors.add("Error processing file: " + e.getMessage());
            failedCount++;
        }

        log.info("Bulk import completed: success={}, failed={}, total errors={}",
                successCount, failedCount, errors.size());

        // Final verification: Check if any records were actually saved
        if (successCount > 0) {
            log.info("Verifying saved records in database...");
            // Force flush all pending changes
            try {
                salesDataRepository.flush();
                log.info("All changes flushed to database successfully");
            } catch (Exception e) {
                log.error("Error flushing changes to database: {}", e.getMessage(), e);
                errors.add("Warning: Some changes may not have been saved to database");
            }
        }

        return new BulkImportResponse(successCount, failedCount, errors);
    }

    /**
     * Save a single sales data record in its own transaction
     * This prevents one failed record from rolling back all records
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSalesDataRecord(SalesDataRequest request, int rowNum) {
        String txNo = request.getTxNo().trim();
        log.debug("Checking for existing record with TXNo: {}", txNo);

        SalesData existing = salesDataRepository.findByTxNo(txNo);
        if (existing != null) {
            // Update existing record
            log.info("Updating existing record with TXNo: {} (ID: {})", txNo, existing.getId());
            updateEntityFromRequest(existing, request);
            SalesData saved = salesDataRepository.save(existing);
            salesDataRepository.flush(); // Force immediate database write
            log.info("Updated record saved with ID: {}, TXNo: {}", saved.getId(), saved.getTxNo());
        } else {
            // Create new record
            log.info("Creating new record with TXNo: {}", txNo);
            SalesData entity = toSalesDataEntity(request);
            log.debug("Entity created: TXNo={}, TXDate={}, ItemCode={}",
                    entity.getTxNo(), entity.getTxDate(), entity.getItemCode());

            SalesData saved = salesDataRepository.save(entity);
            salesDataRepository.flush(); // Force immediate database write
            log.info("New record saved with ID: {}, TXNo: {}", saved.getId(), saved.getTxNo());

            // Verify save
            if (saved.getId() == null) {
                log.error("Failed to save record - ID is null for TXNo: {}", txNo);
                throw new RuntimeException("Failed to save record - ID is null");
            }
        }
    }

    /**
     * Parse Excel file and convert to list of SalesDataRequest
     */
    private List<SalesDataRequest> parseExcelFile(MultipartFile file) throws Exception {
        List<SalesDataRequest> dataList = new ArrayList<>();
        InputStream inputStream = file.getInputStream();
        Workbook workbook;

        // Determine file type and create appropriate workbook
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.endsWith(".xlsx")) {
            workbook = new XSSFWorkbook(inputStream);
        } else {
            workbook = new HSSFWorkbook(inputStream);
        }

        try {
            Sheet sheet = workbook.getSheetAt(0); // Get first sheet
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                throw new IllegalArgumentException("Excel file must have at least a header row and one data row");
            }

            // Read header row to map column names
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columnMap = new HashMap<>();
            for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String columnName = getCellValueAsString(cell).trim();
                    if (!columnName.isEmpty()) {
                        columnMap.put(columnName, i);
                        // Also add normalized version (lowercase, no spaces) for flexible matching
                        String normalized = columnName.toLowerCase().replaceAll("\\s+", "");
                        if (!normalized.isEmpty() && !columnMap.containsKey(normalized)) {
                            columnMap.put(normalized, i);
                        }
                    }
                }
            }

            // Create field mapping: Excel header names -> database field names
            Map<String, String> fieldMapping = createFieldMapping();

            // Read data rows
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue; // Skip empty rows
                }

                // Check if row is empty
                boolean isEmpty = true;
                for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null && !getCellValueAsString(cell).trim().isEmpty()) {
                        isEmpty = false;
                        break;
                    }
                }
                if (isEmpty) {
                    continue; // Skip empty rows
                }

                SalesDataRequest request = new SalesDataRequest();

                // Map Excel columns to request fields using flexible matching
                mapExcelRowToRequest(row, columnMap, fieldMapping, request);

                // Debug: Log what was parsed
                log.debug("Parsed row {}: TXNo={}, TXDate={}, ItemCode={}, ItemName={}",
                        rowIndex, request.getTxNo(), request.getTxDate(), request.getItemCode(), request.getItemName());

                dataList.add(request);
            }

        } finally {
            workbook.close();
            inputStream.close();
        }

        return dataList;
    }

    /**
     * Get cell value as string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Format numeric value without scientific notation
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * Parse date from cell
     */
    private LocalDate parseDateFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
            } else {
                String dateStr = getCellValueAsString(cell).trim();
                if (dateStr.isEmpty()) {
                    return null;
                }
                return parseDate(dateStr);
            }
        } catch (Exception e) {
            log.warn("Failed to parse date from cell: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse integer from cell
     */
    private Integer parseIntegerFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                double numericValue = cell.getNumericCellValue();
                return (int) numericValue;
            } else {
                String value = getCellValueAsString(cell).trim();
                if (value.isEmpty()) {
                    return null;
                }
                return Integer.parseInt(value);
            }
        } catch (Exception e) {
            log.warn("Failed to parse integer from cell: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse BigDecimal from cell
     */
    private BigDecimal parseBigDecimalFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } else {
                String value = getCellValueAsString(cell).trim();
                if (value.isEmpty()) {
                    return null;
                }
                return new BigDecimal(value);
            }
        } catch (Exception e) {
            log.warn("Failed to parse BigDecimal from cell: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate Excel template file for sales data import
     * 
     * @return Excel file as byte array resource
     */
    public Resource generateExcelTemplate() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sales Data Template");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Transaction Date",
                "Transaction Number",
                "Transaction Quantity",
                "Transaction Price",
                "Buyer Code",
                "Buyer Name",
                "Item Code",
                "Item Name",
                "Product Hierarchy 3",
                "Item Type",
                "Model",
                "Material",
                "Unit of Measure",
                "Brand Code",
                "Unit Cost",
                "Sector",
                "Sub Sector",
                "Value",
                "Function",
                "Performance",
                "Performance.1",
                "Rationale",
                "Website",
                "Source"
        };

        // Set header style (bold)
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Write headers
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create example row
        Row exampleRow = sheet.createRow(1);
        Object[] exampleData = {
                "2024-01-20", // Transaction Date
                "TXN-2024-001", // Transaction Number (required field example)
                "", // Transaction Quantity
                "", // Transaction Price
                "", // Buyer Code
                "", // Buyer Name
                "", // Item Code
                "", // Item Name
                "", // Product Hierarchy 3
                "", // Item Type
                "", // Model
                "", // Material
                "", // Unit of Measure
                "", // Brand Code
                "", // Unit Cost
                "", // Sector
                "", // Sub Sector
                "", // Value
                "", // Function
                "", // Performance
                "", // Performance.1
                "", // Rationale
                "", // Website
                "" // Source
        };

        // Write example data
        for (int i = 0; i < exampleData.length; i++) {
            Cell cell = exampleRow.createCell(i);
            if (exampleData[i] != null && !exampleData[i].toString().isEmpty()) {
                cell.setCellValue(exampleData[i].toString());
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // Set minimum width
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }

        // Convert to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        byte[] bytes = outputStream.toByteArray();
        outputStream.close();

        return new ByteArrayResource(bytes);
    }

    /**
     * Get Excel template file name
     */
    public String getTemplateFileName() {
        return "Sales_Data_Template.xlsx";
    }

    /**
     * Create field mapping from Excel header names to database field names
     */
    private Map<String, String> createFieldMapping() {
        Map<String, String> mapping = new HashMap<>();

        // Map all possible Excel header names to database field names
        // Format: Excel header name (case-insensitive, space-insensitive) -> setter
        // method name

        // TXDate
        mapping.put("transactiondate", "TXDate");
        mapping.put("txdate", "TXDate");
        mapping.put("transaction date", "TXDate");

        // TXNo (most important - required field)
        mapping.put("transactionnumber", "TXNo");
        mapping.put("txno", "TXNo");
        mapping.put("transaction number", "TXNo");
        mapping.put("tx no", "TXNo");

        // TXQty
        mapping.put("transactionquantity", "TXQty");
        mapping.put("txqty", "TXQty");
        mapping.put("transaction quantity", "TXQty");
        mapping.put("tx qty", "TXQty");

        // TXP1
        mapping.put("transactionprice", "TXP1");
        mapping.put("txp1", "TXP1");
        mapping.put("transaction price", "TXP1");
        mapping.put("tx p1", "TXP1");

        // BuyerCode
        mapping.put("buyercode", "BuyerCode");
        mapping.put("buyer code", "BuyerCode");

        // BuyerName
        mapping.put("buyername", "BuyerName");
        mapping.put("buyer name", "BuyerName");

        // ItemCode
        mapping.put("itemcode", "ItemCode");
        mapping.put("item code", "ItemCode");

        // ItemName
        mapping.put("itemname", "ItemName");
        mapping.put("item name", "ItemName");

        // Product Hierarchy 3
        mapping.put("producthierarchy3", "Product Hierarchy 3");
        mapping.put("product hierarchy 3", "Product Hierarchy 3");
        mapping.put("producthierarchy 3", "Product Hierarchy 3");

        // ItemType
        mapping.put("itemtype", "ItemType");
        mapping.put("item type", "ItemType");

        // Model
        mapping.put("model", "Model");

        // Material
        mapping.put("material", "Material");

        // UOM
        mapping.put("uom", "UOM");
        mapping.put("unitofmeasure", "UOM");
        mapping.put("unit of measure", "UOM");

        // Brand Code
        mapping.put("brandcode", "Brand Code");
        mapping.put("brand code", "Brand Code");

        // Unit Cost
        mapping.put("unitcost", "Unit Cost");
        mapping.put("unit cost", "Unit Cost");

        // Sector
        mapping.put("sector", "Sector");

        // SubSector
        mapping.put("subsector", "SubSector");
        mapping.put("sub sector", "SubSector");
        mapping.put("subsector", "SubSector");

        // Value
        mapping.put("value", "Value");

        // Function
        mapping.put("function", "Function");

        // Performance
        mapping.put("performance", "Performance");

        // Performance.1
        mapping.put("performance.1", "Performance.1");
        mapping.put("performance1", "Performance.1");
        mapping.put("performance 1", "Performance.1");

        // Rationale
        mapping.put("rationale", "Rationale");

        // www
        mapping.put("www", "www");
        mapping.put("website", "www");

        // Source
        mapping.put("source", "Source");

        return mapping;
    }

    /**
     * Map Excel row data to SalesDataRequest using flexible field matching
     */
    private void mapExcelRowToRequest(Row row, Map<String, Integer> columnMap,
            Map<String, String> fieldMapping, SalesDataRequest request) {

        // Create a map to track which columns we've processed (to avoid duplicates)
        Set<Integer> processedColumns = new HashSet<>();

        // Iterate through field mapping to find matching Excel columns
        for (Map.Entry<String, String> fieldEntry : fieldMapping.entrySet()) {
            String excelHeaderPattern = fieldEntry.getKey(); // e.g., "transactionnumber"
            String dbField = fieldEntry.getValue(); // e.g., "TXNo"

            // Find matching column in Excel
            Integer columnIndex = null;
            for (Map.Entry<String, Integer> colEntry : columnMap.entrySet()) {
                String excelHeader = colEntry.getKey();
                String normalizedExcelHeader = excelHeader.toLowerCase().replaceAll("\\s+", "");

                // Match: exact match or normalized match
                if (excelHeaderPattern.equalsIgnoreCase(excelHeader) ||
                        excelHeaderPattern.equals(normalizedExcelHeader) ||
                        normalizedExcelHeader.equals(excelHeaderPattern)) {
                    columnIndex = colEntry.getValue();
                    break;
                }
            }

            if (columnIndex == null || processedColumns.contains(columnIndex)) {
                continue; // Column not found or already processed
            }

            processedColumns.add(columnIndex);

            // Get cell value
            Cell cell = row.getCell(columnIndex);
            String cellValue = getCellValueAsString(cell);
            if (cellValue != null) {
                cellValue = cellValue.trim();
                if (cellValue.isEmpty()) {
                    cellValue = null;
                }
            }

            // Map to request fields based on database field name
            switch (dbField) {
                case "TXDate":
                    request.setTxDate(parseDateFromCell(cell));
                    break;
                case "TXNo":
                    request.setTxNo(cellValue);
                    break;
                case "TXQty":
                    request.setTxQty(parseIntegerFromCell(cell));
                    break;
                case "TXP1":
                    request.setTxP1(parseBigDecimalFromCell(cell));
                    break;
                case "BuyerCode":
                    request.setBuyerCode(cellValue);
                    break;
                case "BuyerName":
                    request.setBuyerName(cellValue);
                    break;
                case "ItemCode":
                    request.setItemCode(cellValue);
                    break;
                case "ItemName":
                    request.setItemName(cellValue);
                    break;
                case "Product Hierarchy 3":
                    request.setProductHierarchy3(cellValue);
                    break;
                case "Function":
                    request.setFunction(cellValue);
                    break;
                case "ItemType":
                    request.setItemType(cellValue);
                    break;
                case "Model":
                    request.setModel(cellValue);
                    break;
                case "Performance":
                    request.setPerformance(cellValue);
                    break;
                case "Performance.1":
                    request.setPerformance1(cellValue);
                    break;
                case "Material":
                    request.setMaterial(cellValue);
                    break;
                case "UOM":
                    request.setUom(cellValue);
                    break;
                case "Brand Code":
                    request.setBrandCode(cellValue);
                    break;
                case "Unit Cost":
                    request.setUnitCost(parseBigDecimalFromCell(cell));
                    break;
                case "Sector":
                    request.setSector(cellValue);
                    break;
                case "SubSector":
                    request.setSubSector(cellValue);
                    break;
                case "Value":
                    request.setValue(parseBigDecimalFromCell(cell));
                    break;
                case "Rationale":
                    request.setRationale(cellValue);
                    break;
                case "www":
                    request.setWww(cellValue);
                    break;
                case "Source":
                    request.setSource(cellValue);
                    break;
            }
        }
    }
}
