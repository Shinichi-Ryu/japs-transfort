package com.japstransfort.dto;

import java.io.Serializable;

/**
 * Simple session-stored cart item: only product ID and quantity.
 * Prices are always recomputed server-side, never trusted from the browser.
 */
public class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer productId;
    private int quantity;

    public CartItem() {
    }

    public CartItem(Integer productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
