package com.japstransfort.dto;

import com.japstransfort.entity.Product;
import java.math.BigDecimal;

/**
 * View-only DTO for rendering a cart row.
 * Prices are always computed server-side from the database.
 */
public class CartRow {
    private final Product product;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal lineTotal;

    public CartRow(Product product, int quantity, BigDecimal unitPrice) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
