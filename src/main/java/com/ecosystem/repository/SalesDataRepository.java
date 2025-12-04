package com.ecosystem.repository;

import com.ecosystem.entity.SalesData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SalesDataRepository extends JpaRepository<SalesData, String> {
    
    // Filter by category and sort by transaction date (using native SQL, explicitly specifying column names)
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data " +
           "WHERE `Product Hierarchy 3` = :category OR `Sector` = :category " +
           "ORDER BY STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC", 
           nativeQuery = true)
    Page<SalesData> findByCategoryOrderByTxDateDesc(@Param("category") String category, Pageable pageable);
    
    // Filter by category and sort by price ascending (using native SQL, explicitly specifying column names)
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data " +
           "WHERE `Product Hierarchy 3` = :category OR `Sector` = :category " +
           "ORDER BY CAST(`TXP1` AS DECIMAL(10,2)) ASC", 
           nativeQuery = true)
    Page<SalesData> findByCategoryOrderByTxP1Asc(@Param("category") String category, Pageable pageable);
    
    // Filter by category and sort by price descending (using native SQL, explicitly specifying column names)
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data " +
           "WHERE `Product Hierarchy 3` = :category OR `Sector` = :category " +
           "ORDER BY CAST(`TXP1` AS DECIMAL(10,2)) DESC", 
           nativeQuery = true)
    Page<SalesData> findByCategoryOrderByTxP1Desc(@Param("category") String category, Pageable pageable);
    
    // Sort by transaction date (using native SQL, explicitly specifying column names)
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data ORDER BY STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC", 
           nativeQuery = true)
    Page<SalesData> findAllByOrderByTxDateDesc(Pageable pageable);
    
    // Sort by price ascending (using native SQL, explicitly specifying column names)
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data ORDER BY CAST(`TXP1` AS DECIMAL(10,2)) ASC", 
           nativeQuery = true)
    Page<SalesData> findAllByOrderByTxP1Asc(Pageable pageable);
    
    // Sort by price descending (using native SQL, explicitly specifying column names)
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data ORDER BY CAST(`TXP1` AS DECIMAL(10,2)) DESC", 
           nativeQuery = true)
    Page<SalesData> findAllByOrderByTxP1Desc(Pageable pageable);
    
    // Search by product name (using native SQL)
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data " +
           "WHERE LOWER(`ItemName`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(`ItemCode`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(`Model`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC " +
           "LIMIT :limit", 
           nativeQuery = true)
    List<SalesData> searchByItemName(@Param("keyword") String keyword, @Param("limit") int limit);
    
    // ==================== Material Search Methods ====================
    
    // 1. Exact search by material code
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data " +
           "WHERE `ItemCode` = :itemCode " +
           "ORDER BY STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC", 
           nativeQuery = true)
    List<SalesData> findByItemCode(@Param("itemCode") String itemCode);
    
    // 2. Fuzzy search by material name keyword (supports multiple keywords separated by spaces)
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data " +
           "WHERE LOWER(`ItemName`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(`ItemName`) LIKE LOWER(CONCAT('%', REPLACE(:keyword, ' ', '%'), '%')) " +
           "ORDER BY STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC " +
           "LIMIT :limit", 
           nativeQuery = true)
    List<SalesData> searchByItemNameKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
    
    // 3. Search by category (Product Hierarchy 3, case-insensitive matching with space handling)
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data " +
           "WHERE LOWER(TRIM(`Product Hierarchy 3`)) = LOWER(TRIM(:productHierarchy3)) " +
           "ORDER BY STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC " +
           "LIMIT :limit", 
           nativeQuery = true)
    List<SalesData> findByProductHierarchy3(@Param("productHierarchy3") String productHierarchy3, @Param("limit") int limit);
    
    // 4. Search by function (Function, case-insensitive matching)
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data " +
           "WHERE LOWER(`Function`) = LOWER(:function) " +
           "ORDER BY STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC " +
           "LIMIT :limit", 
           nativeQuery = true)
    List<SalesData> findByFunction(@Param("function") String function, @Param("limit") int limit);
    
    // 5. Search by brand (Brand Code)
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data " +
           "WHERE `Brand Code` = :brandCode " +
           "ORDER BY STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC " +
           "LIMIT :limit", 
           nativeQuery = true)
    List<SalesData> findByBrandCode(@Param("brandCode") String brandCode, @Param("limit") int limit);
    
    // 6. Combined criteria search (supports Unit Cost and TXP1 price fields, fuzzy matching for buyer name and category)
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data " +
           "WHERE (:itemNameKeyword IS NULL OR LOWER(`ItemName`) LIKE LOWER(CONCAT('%', :itemNameKeyword, '%'))) " +
           "AND (:productHierarchy3 IS NULL OR LOWER(TRIM(`Product Hierarchy 3`)) LIKE LOWER(CONCAT('%', TRIM(:productHierarchy3), '%'))) " +
           "AND (:function IS NULL OR LOWER(`Function`) LIKE LOWER(CONCAT('%', :function, '%'))) " +
           "AND (:brandCode IS NULL OR LOWER(`Brand Code`) LIKE LOWER(CONCAT('%', :brandCode, '%'))) " +
           "AND (:buyerName IS NULL OR LOWER(`BuyerName`) LIKE LOWER(CONCAT('%', :buyerName, '%'))) " +
           "AND (:buyerCode IS NULL OR LOWER(`BuyerCode`) LIKE LOWER(CONCAT('%', :buyerCode, '%'))) " +
           "AND (:minPrice IS NULL OR CAST(`Unit Cost` AS DECIMAL(10,4)) >= :minPrice OR CAST(`TXP1` AS DECIMAL(10,2)) >= :minPrice) " +
           "AND (:maxPrice IS NULL OR CAST(`Unit Cost` AS DECIMAL(10,4)) <= :maxPrice OR CAST(`TXP1` AS DECIMAL(10,2)) <= :maxPrice) " +
           "AND (:startDate IS NULL OR STR_TO_DATE(`TXDate`, '%Y-%m-%d') >= STR_TO_DATE(:startDate, '%Y-%m-%d')) " +
           "AND (:endDate IS NULL OR STR_TO_DATE(`TXDate`, '%Y-%m-%d') <= STR_TO_DATE(:endDate, '%Y-%m-%d')) " +
           "ORDER BY STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC " +
           "LIMIT :limit", 
           nativeQuery = true)
    List<SalesData> searchByCombinedCriteria(
        @Param("itemNameKeyword") String itemNameKeyword,
        @Param("productHierarchy3") String productHierarchy3,
        @Param("function") String function,
        @Param("brandCode") String brandCode,
        @Param("buyerName") String buyerName,
        @Param("buyerCode") String buyerCode,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("limit") int limit
    );
    
    // 7. Full-text search (search in all relevant fields)
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data " +
           "WHERE LOWER(`ItemName`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(`ItemCode`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(`BuyerName`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(`BuyerCode`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(`Product Hierarchy 3`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(`Function`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(`Brand Code`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(`Model`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(`ItemType`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(`Material`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(`Sector`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(`SubSector`) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC " +
           "LIMIT :limit", 
           nativeQuery = true)
    List<SalesData> fullTextSearch(@Param("keyword") String keyword, @Param("limit") int limit);
    
    // 8. Complete filter query (supports all filter conditions)
    @Query(value = "SELECT `TXNo`, `TXDate`, `TXQty`, `TXP1`, `BuyerCode`, `BuyerName`, `ItemCode`, `ItemName`, " +
           "`Product Hierarchy 3`, `Function`, `ItemType`, `Model`, `Performance`, `Performance.1`, `Material`, " +
           "`UOM`, `Brand Code`, `Unit Cost`, `Sector`, `SubSector`, `Value`, `Rationale`, `www`, `Source` " +
           "FROM ecoschema.sales_data " +
           "WHERE 1=1 " +
           "AND (:minDate IS NULL OR STR_TO_DATE(`TXDate`, '%Y-%m-%d') >= STR_TO_DATE(:minDate, '%Y-%m-%d')) " +
           "AND (:maxDate IS NULL OR STR_TO_DATE(`TXDate`, '%Y-%m-%d') <= STR_TO_DATE(:maxDate, '%Y-%m-%d')) " +
           "AND (:txNo IS NULL OR LOWER(`TXNo`) LIKE LOWER(CONCAT('%', :txNo, '%'))) " +
           "AND (:minQty IS NULL OR CAST(`TXQty` AS UNSIGNED) >= :minQty) " +
           "AND (:maxQty IS NULL OR CAST(`TXQty` AS UNSIGNED) <= :maxQty) " +
           "AND (:minPrice IS NULL OR CAST(`TXP1` AS DECIMAL(10,2)) >= :minPrice) " +
           "AND (:maxPrice IS NULL OR CAST(`TXP1` AS DECIMAL(10,2)) <= :maxPrice) " +
           "AND (:minValue IS NULL OR CAST(`Value` AS DECIMAL(10,2)) >= :minValue) " +
           "AND (:maxValue IS NULL OR CAST(`Value` AS DECIMAL(10,2)) <= :maxValue) " +
           "AND (:buyerCode IS NULL OR LOWER(`BuyerCode`) LIKE LOWER(CONCAT('%', :buyerCode, '%'))) " +
           "AND (:buyerName IS NULL OR LOWER(`BuyerName`) LIKE LOWER(CONCAT('%', :buyerName, '%'))) " +
           "AND (:itemCode IS NULL OR LOWER(`ItemCode`) LIKE LOWER(CONCAT('%', :itemCode, '%'))) " +
           "AND (:itemName IS NULL OR LOWER(`ItemName`) LIKE LOWER(CONCAT('%', :itemName, '%'))) " +
           "AND (:productHierarchy3 IS NULL OR LOWER(`Product Hierarchy 3`) LIKE LOWER(CONCAT('%', :productHierarchy3, '%'))) " +
           "AND (:itemType IS NULL OR LOWER(`ItemType`) LIKE LOWER(CONCAT('%', :itemType, '%'))) " +
           "AND (:model IS NULL OR LOWER(`Model`) LIKE LOWER(CONCAT('%', :model, '%'))) " +
           "AND (:material IS NULL OR LOWER(`Material`) LIKE LOWER(CONCAT('%', :material, '%'))) " +
           "AND (:uom IS NULL OR LOWER(`UOM`) LIKE LOWER(CONCAT('%', :uom, '%'))) " +
           "AND (:brandCode IS NULL OR LOWER(`Brand Code`) LIKE LOWER(CONCAT('%', :brandCode, '%'))) " +
           "AND (:performance IS NULL OR LOWER(`Performance`) LIKE LOWER(CONCAT('%', :performance, '%'))) " +
           "AND (:performance1 IS NULL OR LOWER(`Performance.1`) LIKE LOWER(CONCAT('%', :performance1, '%'))) " +
           "AND (:minUnitCost IS NULL OR CAST(`Unit Cost` AS DECIMAL(10,4)) >= :minUnitCost) " +
           "AND (:maxUnitCost IS NULL OR CAST(`Unit Cost` AS DECIMAL(10,4)) <= :maxUnitCost) " +
           "AND (:function IS NULL OR LOWER(`Function`) LIKE LOWER(CONCAT('%', :function, '%'))) " +
           "AND (:sector IS NULL OR LOWER(`Sector`) LIKE LOWER(CONCAT('%', :sector, '%'))) " +
           "AND (:subSector IS NULL OR LOWER(`SubSector`) LIKE LOWER(CONCAT('%', :subSector, '%'))) " +
           "AND (:source IS NULL OR LOWER(`Source`) LIKE LOWER(CONCAT('%', :source, '%'))) " +
           "AND (:category IS NULL OR :category = 'all' OR LOWER(`Product Hierarchy 3`) LIKE LOWER(CONCAT('%', :category, '%')) OR LOWER(`Sector`) LIKE LOWER(CONCAT('%', :category, '%'))) " +
           "AND (:keyword IS NULL OR " +
           "     LOWER(`ItemName`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`ItemCode`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`BuyerName`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`BuyerCode`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`Product Hierarchy 3`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`Function`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`Brand Code`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`Model`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`ItemType`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`Material`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`Sector`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`SubSector`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`TXNo`) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY " +
           "  CASE WHEN :sort = 'newest' THEN 1 ELSE 0 END DESC, " +
           "  CASE WHEN :sort = 'newest' THEN STR_TO_DATE(`TXDate`, '%Y-%m-%d') END DESC, " +
           "  CASE WHEN :sort = 'price_asc' THEN 1 ELSE 0 END DESC, " +
           "  CASE WHEN :sort = 'price_asc' THEN CAST(`TXP1` AS DECIMAL(10,2)) END ASC, " +
           "  CASE WHEN :sort = 'price_desc' THEN 1 ELSE 0 END DESC, " +
           "  CASE WHEN :sort = 'price_desc' THEN CAST(`TXP1` AS DECIMAL(10,2)) END DESC, " +
           "  STR_TO_DATE(`TXDate`, '%Y-%m-%d') DESC",
           countQuery = "SELECT COUNT(*) FROM ecoschema.sales_data " +
           "WHERE 1=1 " +
           "AND (:minDate IS NULL OR STR_TO_DATE(`TXDate`, '%Y-%m-%d') >= STR_TO_DATE(:minDate, '%Y-%m-%d')) " +
           "AND (:maxDate IS NULL OR STR_TO_DATE(`TXDate`, '%Y-%m-%d') <= STR_TO_DATE(:maxDate, '%Y-%m-%d')) " +
           "AND (:txNo IS NULL OR LOWER(`TXNo`) LIKE LOWER(CONCAT('%', :txNo, '%'))) " +
           "AND (:minQty IS NULL OR CAST(`TXQty` AS UNSIGNED) >= :minQty) " +
           "AND (:maxQty IS NULL OR CAST(`TXQty` AS UNSIGNED) <= :maxQty) " +
           "AND (:minPrice IS NULL OR CAST(`TXP1` AS DECIMAL(10,2)) >= :minPrice) " +
           "AND (:maxPrice IS NULL OR CAST(`TXP1` AS DECIMAL(10,2)) <= :maxPrice) " +
           "AND (:minValue IS NULL OR CAST(`Value` AS DECIMAL(10,2)) >= :minValue) " +
           "AND (:maxValue IS NULL OR CAST(`Value` AS DECIMAL(10,2)) <= :maxValue) " +
           "AND (:buyerCode IS NULL OR LOWER(`BuyerCode`) LIKE LOWER(CONCAT('%', :buyerCode, '%'))) " +
           "AND (:buyerName IS NULL OR LOWER(`BuyerName`) LIKE LOWER(CONCAT('%', :buyerName, '%'))) " +
           "AND (:itemCode IS NULL OR LOWER(`ItemCode`) LIKE LOWER(CONCAT('%', :itemCode, '%'))) " +
           "AND (:itemName IS NULL OR LOWER(`ItemName`) LIKE LOWER(CONCAT('%', :itemName, '%'))) " +
           "AND (:productHierarchy3 IS NULL OR LOWER(`Product Hierarchy 3`) LIKE LOWER(CONCAT('%', :productHierarchy3, '%'))) " +
           "AND (:itemType IS NULL OR LOWER(`ItemType`) LIKE LOWER(CONCAT('%', :itemType, '%'))) " +
           "AND (:model IS NULL OR LOWER(`Model`) LIKE LOWER(CONCAT('%', :model, '%'))) " +
           "AND (:material IS NULL OR LOWER(`Material`) LIKE LOWER(CONCAT('%', :material, '%'))) " +
           "AND (:uom IS NULL OR LOWER(`UOM`) LIKE LOWER(CONCAT('%', :uom, '%'))) " +
           "AND (:brandCode IS NULL OR LOWER(`Brand Code`) LIKE LOWER(CONCAT('%', :brandCode, '%'))) " +
           "AND (:performance IS NULL OR LOWER(`Performance`) LIKE LOWER(CONCAT('%', :performance, '%'))) " +
           "AND (:performance1 IS NULL OR LOWER(`Performance.1`) LIKE LOWER(CONCAT('%', :performance1, '%'))) " +
           "AND (:minUnitCost IS NULL OR CAST(`Unit Cost` AS DECIMAL(10,4)) >= :minUnitCost) " +
           "AND (:maxUnitCost IS NULL OR CAST(`Unit Cost` AS DECIMAL(10,4)) <= :maxUnitCost) " +
           "AND (:function IS NULL OR LOWER(`Function`) LIKE LOWER(CONCAT('%', :function, '%'))) " +
           "AND (:sector IS NULL OR LOWER(`Sector`) LIKE LOWER(CONCAT('%', :sector, '%'))) " +
           "AND (:subSector IS NULL OR LOWER(`SubSector`) LIKE LOWER(CONCAT('%', :subSector, '%'))) " +
           "AND (:source IS NULL OR LOWER(`Source`) LIKE LOWER(CONCAT('%', :source, '%'))) " +
           "AND (:category IS NULL OR :category = 'all' OR LOWER(`Product Hierarchy 3`) LIKE LOWER(CONCAT('%', :category, '%')) OR LOWER(`Sector`) LIKE LOWER(CONCAT('%', :category, '%'))) " +
           "AND (:keyword IS NULL OR " +
           "     LOWER(`ItemName`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`ItemCode`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`BuyerName`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`BuyerCode`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`Product Hierarchy 3`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`Function`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`Brand Code`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`Model`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`ItemType`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`Material`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`Sector`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`SubSector`) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(`TXNo`) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           nativeQuery = true)
    Page<SalesData> findAllWithFilters(
        @Param("minDate") String minDate,
        @Param("maxDate") String maxDate,
        @Param("txNo") String txNo,
        @Param("minQty") Integer minQty,
        @Param("maxQty") Integer maxQty,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("minValue") BigDecimal minValue,
        @Param("maxValue") BigDecimal maxValue,
        @Param("buyerCode") String buyerCode,
        @Param("buyerName") String buyerName,
        @Param("itemCode") String itemCode,
        @Param("itemName") String itemName,
        @Param("productHierarchy3") String productHierarchy3,
        @Param("itemType") String itemType,
        @Param("model") String model,
        @Param("material") String material,
        @Param("uom") String uom,
        @Param("brandCode") String brandCode,
        @Param("performance") String performance,
        @Param("performance1") String performance1,
        @Param("minUnitCost") BigDecimal minUnitCost,
        @Param("maxUnitCost") BigDecimal maxUnitCost,
        @Param("function") String function,
        @Param("sector") String sector,
        @Param("subSector") String subSector,
        @Param("source") String source,
        @Param("category") String category,
        @Param("keyword") String keyword,
        @Param("sort") String sort,
        Pageable pageable
    );

    // Get distinct category values for dropdown options
    @Query(value = "SELECT DISTINCT `Product Hierarchy 3` " +
           "FROM ecoschema.sales_data " +
           "WHERE `Product Hierarchy 3` IS NOT NULL AND `Product Hierarchy 3` != '' " +
           "ORDER BY `Product Hierarchy 3` ASC",
           nativeQuery = true)
    List<String> findDistinctCategories();
}

