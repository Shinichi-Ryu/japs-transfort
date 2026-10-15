package com.japstransfort.dto;

import java.math.BigDecimal;

public interface StockRowProjection {
    String getSku();
    String getName();
    String getWarehouse();
    Integer getQuantity();
    Integer getReorderLevel();
    String getStatus();
    BigDecimal getCostValue();
    BigDecimal getSrpValue();

    default String getWarehouseName() {
        return getWarehouse();
    }

    default String getStockStatus() {
        return getStatus();
    }

    default String getStockStatusBadgeClass() {
        String s = getStatus();
        if ("Out of stock".equalsIgnoreCase(s)) return "bg-danger";
        if ("Low stock".equalsIgnoreCase(s)) return "bg-warning text-dark";
        return "bg-success";
    }

    default BigDecimal getStockValueAtCost() {
        return getCostValue() != null ? getCostValue() : BigDecimal.ZERO;
    }

    default BigDecimal getStockValueAtSrp() {
        return getSrpValue() != null ? getSrpValue() : BigDecimal.ZERO;
    }
}
