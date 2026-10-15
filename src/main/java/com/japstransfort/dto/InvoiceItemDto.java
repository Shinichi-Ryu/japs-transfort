package com.japstransfort.dto;

import java.math.BigDecimal;

public class InvoiceItemDto {

    private String sku;
    private String description;
    private String unit;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public InvoiceItemDto() {
    }

    public InvoiceItemDto(String sku, String description, String unit, Integer quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.sku = sku;
        this.description = description;
        this.unit = unit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
