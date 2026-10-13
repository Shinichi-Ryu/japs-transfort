package com.japstransfort.repository;

import com.japstransfort.dto.InventoryRowDto;
import com.japstransfort.entity.Inventory;
import com.japstransfort.entity.InventoryId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Inventory operations.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, InventoryId> {

    List<Inventory> findById_WarehouseId(Integer warehouseId);

    List<Inventory> findById_ProductId(Integer productId);

    Optional<Inventory> findById_WarehouseIdAndId_ProductId(Integer warehouseId, Integer productId);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.product p JOIN FETCH i.warehouse w WHERE i.id.warehouseId = :warehouseId")
    List<Inventory> findAllByWarehouseWithDetails(@Param("warehouseId") Integer warehouseId);

    /**
     * Retrieves paginated inventory rows for each product and warehouse combination,
     * including products with no inventory row (quantity 0).
     */
    @Query(value = "SELECT new com.japstransfort.dto.InventoryRowDto(p, w, COALESCE(i.quantity, 0)) " +
           "FROM Product p " +
           "CROSS JOIN Warehouse w " +
           "LEFT JOIN Inventory i ON i.id.productId = p.productId AND i.id.warehouseId = w.warehouseId " +
           "WHERE (:search IS NULL OR :search = '' OR " +
           "      LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "      LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "  AND (:warehouseId IS NULL OR w.warehouseId = :warehouseId)",
           countQuery = "SELECT COUNT(p.productId) " +
           "FROM Product p " +
           "CROSS JOIN Warehouse w " +
           "WHERE (:search IS NULL OR :search = '' OR " +
           "      LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "      LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "  AND (:warehouseId IS NULL OR w.warehouseId = :warehouseId)")
    Page<InventoryRowDto> findInventoryRows(
            @Param("search") String search,
            @Param("warehouseId") Integer warehouseId,
            Pageable pageable);
}
