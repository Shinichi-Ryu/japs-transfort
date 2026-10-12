package com.japstransfort.repository;

import com.japstransfort.entity.Inventory;
import com.japstransfort.entity.InventoryId;
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
}
