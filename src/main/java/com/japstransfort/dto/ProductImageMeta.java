package com.japstransfort.dto;

import java.time.LocalDateTime;

/**
 * Lightweight image metadata for list pages — never carries byte[] data.
 */
public class ProductImageMeta {

    private final Integer productId;
    private final boolean hasImage;
    private final LocalDateTime updatedAt;

    public ProductImageMeta(Integer productId, boolean hasImage, LocalDateTime updatedAt) {
        this.productId = productId;
        this.hasImage = hasImage;
        this.updatedAt = updatedAt;
    }

    public Integer getProductId() { return productId; }
    public boolean isHasImage() { return hasImage; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /** Epoch second of updatedAt, used as version parameter in image URLs. */
    public long getVersion() {
        return updatedAt != null ? updatedAt.toEpochSecond(java.time.ZoneOffset.UTC) : 0L;
    }
}
