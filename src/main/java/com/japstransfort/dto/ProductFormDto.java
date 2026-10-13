package com.japstransfort.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Data Transfer Object for Product creation and editing forms.
 * Separates HTTP form binding from the database entity.
 */
public class ProductFormDto {

    private Integer productId;

    @NotBlank(message = "SKU is required.")
    @Size(max = 40, message = "SKU cannot exceed 40 characters.")
    private String sku;

    @NotBlank(message = "Product name is required.")
    @Size(max = 150, message = "Product name cannot exceed 150 characters.")
    private String name;

    @NotNull(message = "Category selection is required.")
    private Integer categoryId;

    @Size(max = 20, message = "Unit cannot exceed 20 characters.")
    private String unit = "pc";

    @NotNull(message = "Cost price is required.")
    @DecimalMin(value = "0.00", message = "Cost price must be zero or positive.")
    private BigDecimal costPrice;

    @NotNull(message = "Suggested Retail Price (SRP) is required.")
    @DecimalMin(value = "0.00", message = "SRP must be zero or positive.")
    private BigDecimal srp;

    @NotNull(message = "Reorder level is required.")
    @Min(value = 0, message = "Reorder level cannot be negative.")
    private Integer reorderLevel = 10;

    private Boolean isActive = true;

    public ProductFormDto() {
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getSrp() {
        return srp;
    }

    public void setSrp(BigDecimal srp) {
        this.srp = srp;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
