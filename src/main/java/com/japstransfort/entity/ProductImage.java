package com.japstransfort.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.time.LocalDateTime;

/**
 * Stores one photo per product in the product_images table.
 * product_id is the primary key (no association to Product).
 * data is plain byte[] (BYTEA) — do NOT use @Lob.
 */
@Entity
@Table(name = "product_images")
public class ProductImage implements Persistable<Integer> {

    @Id
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "data", nullable = false, columnDefinition = "bytea")
    private byte[] data;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private boolean isNew = false;

    public ProductImage() {}

    public ProductImage(Integer productId, String contentType, byte[] data, LocalDateTime updatedAt) {
        this.productId = productId;
        this.contentType = contentType;
        this.data = data;
        this.updatedAt = updatedAt;
        this.isNew = true;
    }

    @Override
    public Integer getId() {
        return productId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
