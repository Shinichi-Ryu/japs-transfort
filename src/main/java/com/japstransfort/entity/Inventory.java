package com.japstransfort.entity;

import jakarta.persistence.*;

/**
 * Entity representing stock levels for a specific product at a specific warehouse.
 * Uses composite primary key InventoryId (warehouse_id, product_id).
 */
@Entity
@Table(name = "inventory")
public class Inventory {

    @EmbeddedId
    private InventoryId id = new InventoryId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("warehouseId")
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    public Inventory() {
    }

    public Inventory(Warehouse warehouse, Product product, Integer quantity) {
        this.warehouse = warehouse;
        this.product = product;
        this.id = new InventoryId(warehouse.getWarehouseId(), product.getProductId());
        this.quantity = quantity;
    }

    public InventoryId getId() {
        return id;
    }

    public void setId(InventoryId id) {
        this.id = id;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
        if (warehouse != null && this.id != null) {
            this.id.setWarehouseId(warehouse.getWarehouseId());
        }
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
        if (product != null && this.id != null) {
            this.id.setProductId(product.getProductId());
        }
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
