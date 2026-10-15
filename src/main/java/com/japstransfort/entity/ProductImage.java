package com.japstransfort.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores one photo per product in the product_images table.
 * product_id is both PK and FK to products.
 * data is plain byte[] (BYTEA) — do NOT use @Lob (maps to oid in PostgreSQL).
 */
@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @Column(name = "product_id")
    private Integer productId;

    /** "image/jpeg" always (we re-encode everything to JPEG on upload) */
    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    /** Raw JPEG bytes stored in BYTEA column — no @Lob */
    @Column(name = "data", nullable = false, columnDefinition = "bytea")
    private byte[] data;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    public ProductImage() {}

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
