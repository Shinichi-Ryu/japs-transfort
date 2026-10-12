package com.japstransfort.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entity representing a distributed product.
 * Generic product structure suitable for any product type.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "sku", nullable = false, unique = true, length = 40)
    private String sku;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "unit", length = 20)
    private String unit = "pc";

    @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "srp", nullable = false, precision = 12, scale = 2)
    private BigDecimal srp;

    @Column(name = "reorder_level")
    private Integer reorderLevel = 10;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public Product() {
    }

    public Product(String sku, String name, Category category, String unit, BigDecimal costPrice, BigDecimal srp, Integer reorderLevel) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.costPrice = costPrice;
        this.srp = srp;
        this.reorderLevel = reorderLevel;
        this.isActive = true;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getSrp() {
        return srp;
    }

    public void setSrp(BigDecimal srp) {
        this.srp = srp;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
