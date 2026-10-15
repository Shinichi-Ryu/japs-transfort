package com.japstransfort.dto;

/**
 * DTO for stock movement summary per product in a date range.
 */
public class StockMovementSummaryDto {

    private final Integer productId;
    private final String sku;
    private final String name;
    private final long qtyIn;
    private final long qtyOut;
    private final long qtyAdjust;
    private final long qtyReturn;

    public StockMovementSummaryDto(Integer productId, String sku, String name,
                                   long qtyIn, long qtyOut, long qtyAdjust, long qtyReturn) {
        this.productId = productId;
        this.sku = sku;
        this.name = name;
        this.qtyIn = qtyIn;
        this.qtyOut = qtyOut;
        this.qtyAdjust = qtyAdjust;
        this.qtyReturn = qtyReturn;
    }

    public Integer getProductId() { return productId; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public long getQtyIn() { return qtyIn; }
    public long getQtyOut() { return qtyOut; }
    public long getQtyAdjust() { return qtyAdjust; }
    public long getQtyReturn() { return qtyReturn; }
}
