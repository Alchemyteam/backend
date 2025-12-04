package com.ecosystem.dto.buyer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating or updating sales data
 * This class receives JSON data from the frontend and validates it
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesDataRequest {

    @JsonProperty("TXDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate txDate;

    @JsonProperty("TXNo")
    @NotBlank(message = "Transaction number is required")  // ← Must not be empty!
    private String txNo;

    @JsonProperty("TXQty")
    private Integer txQty;

    @JsonProperty("TXP1")
    private BigDecimal txP1;

    @JsonProperty("BuyerCode")
    private String buyerCode;

    @JsonProperty("BuyerName")
    private String buyerName;

    @JsonProperty("ItemCode")
    private String itemCode;

    @JsonProperty("ItemName")
    private String itemName;

    @JsonProperty("Product Hierarchy 3")
    private String productHierarchy3;

    @JsonProperty("Function")
    private String function;

    @JsonProperty("ItemType")
    private String itemType;

    @JsonProperty("Model")
    private String model;

    @JsonProperty("Performance")
    private String performance;

    @JsonProperty("Performance.1")
    private String performance1;

    @JsonProperty("Material")
    private String material;

    @JsonProperty("UOM")
    private String uom;

    @JsonProperty("Brand Code")
    private String brandCode;

    @JsonProperty("Unit Cost")
    private BigDecimal unitCost;

    @JsonProperty("Sector")
    private String sector;

    @JsonProperty("SubSector")
    private String subSector;

    @JsonProperty("Value")
    private BigDecimal value;

    @JsonProperty("Rationale")
    private String rationale;

    @JsonProperty("www")
    private String www;

    @JsonProperty("Source")
    private String source;
}
