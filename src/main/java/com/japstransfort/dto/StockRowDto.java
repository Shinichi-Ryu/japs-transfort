package com.japstransfort.dto;

import java.math.BigDecimal;

/**
 * DTO for the Stock report — one row per product × warehouse.
 */
public class StockRowDto {

    private final Integer productId;
    private final String sku;
    private final String name;
    private final Integer warehouseId;
    private final String warehouseName;
    private final int quantity;
    private final int reorderLevel;
    private final BigDecimal costPrice;
    private final BigDecimal srp;

    public StockRowDto(Integer productId, String sku, String name,
                       Integer warehouseId, String warehouseName,
                       int quantity, int reorderLevel,
                       BigDecimal costPrice, BigDecimal srp) {
        this.productId = productId;
        this.sku = sku;
        this.name = name;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
        this.costPrice = costPrice != null ? costPrice : BigDecimal.ZERO;
        this.srp = srp != null ? srp : BigDecimal.ZERO;
    }

    public String getStockStatus() {
        if (quantity == 0) return "Out of stock";
        if (quantity <= reorderLevel) return "Low stock";
        return "OK";
    }

    public String getStockStatusBadgeClass() {
        return switch (getStockStatus()) {
            case "Out of stock" -> "bg-danger";
            case "Low stock" -> "bg-warning text-dark";
            default -> "bg-success";
        };
    }

    public BigDecimal getStockValueAtCost() {
        return costPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getStockValueAtSrp() {
        return srp.multiply(BigDecimal.valueOf(quantity));
    }

    public Integer getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public Integer getWarehouseId() { return warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public int getQuantity() { return quantity; }
    public int getReorderLevel() { return reorderLevel; }
    public BigDecimal getCostPrice() { return costPrice; }
    public BigDecimal getSrp() { return srp; }
}
