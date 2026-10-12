package com.japstransfort.entity;

import jakarta.persistence.*;

/**
 * Entity representing storage facilities/warehouses.
 */
@Entity
@Table(name = "warehouses")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id")
    private Integer warehouseId;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "location", columnDefinition = "TEXT")
    private String location;

    public Warehouse() {
    }

    public Warehouse(String name, String location) {
        this.name = name;
        this.location = location;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
