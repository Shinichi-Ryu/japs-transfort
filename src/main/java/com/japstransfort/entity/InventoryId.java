package com.japstransfort.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for the inventory table (warehouse_id + product_id).
 */
@Embeddable
public class InventoryId implements Serializable {

    @Column(name = "warehouse_id")
    private Integer warehouseId;

    @Column(name = "product_id")
    private Integer productId;

    public InventoryId() {
    }

    public InventoryId(Integer warehouseId, Integer productId) {
        this.warehouseId = warehouseId;
        this.productId = productId;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryId that = (InventoryId) o;
        return Objects.equals(warehouseId, that.warehouseId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warehouseId, productId);
    }
}
