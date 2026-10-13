package com.japstransfort.dto;

import com.japstransfort.entity.Product;
import com.japstransfort.entity.Warehouse;

/**
 * DTO representing an inventory row per product and warehouse.
 */
public class InventoryRowDto {

    private Product product;
    private Warehouse warehouse;
    private Integer quantity;

    public InventoryRowDto() {
    }

    public InventoryRowDto(Product product, Warehouse warehouse, Integer quantity) {
        this.product = product;
        this.warehouse = warehouse;
        this.quantity = quantity != null ? quantity : 0;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Integer getQuantity() {
        return quantity != null ? quantity : 0;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity != null ? quantity : 0;
    }

    /**
     * Checks if current quantity is less than or equal to product reorder level.
     */
    public boolean isLowStock() {
        if (product == null) return false;
        int level = product.getReorderLevel() != null ? product.getReorderLevel() : 0;
        return getQuantity() <= level;
    }
}
