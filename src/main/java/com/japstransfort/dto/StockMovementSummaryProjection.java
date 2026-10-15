package com.japstransfort.dto;

public interface StockMovementSummaryProjection {
    Integer getProductId();
    String getSku();
    String getName();
    Long getQtyIn();
    Long getQtyOut();
    Long getQtyAdjust();
    Long getQtyReturn();
}
