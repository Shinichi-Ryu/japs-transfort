package com.japstransfort.repository;

import com.japstransfort.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for product_images. Never exposes the data bytes in list queries.
 */
@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {

    /** True if an image exists for the given product_id (no byte loading). */
    @Query("SELECT COUNT(pi) > 0 FROM ProductImage pi WHERE pi.productId = :productId")
    boolean existsByProductId(@Param("productId") Integer productId);

    /** Lightweight projection: just the updatedAt timestamp (no bytes). */
    @Query("SELECT pi.updatedAt FROM ProductImage pi WHERE pi.productId = :productId")
    Optional<java.time.LocalDateTime> findUpdatedAtByProductId(@Param("productId") Integer productId);

    /** Full image row including bytes – used only by the image-serving endpoint. */
    @Query("SELECT pi FROM ProductImage pi WHERE pi.productId = :productId")
    Optional<ProductImage> findByProductId(@Param("productId") Integer productId);

    @Modifying
    @Query("DELETE FROM ProductImage pi WHERE pi.productId = :productId")
    void deleteByProductId(@Param("productId") Integer productId);
}
