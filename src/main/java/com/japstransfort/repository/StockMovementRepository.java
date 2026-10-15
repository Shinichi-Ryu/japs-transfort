package com.japstransfort.repository;

import com.japstransfort.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for StockMovement operations.
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {

    @Query(value = "SELECT m FROM StockMovement m " +
           "LEFT JOIN FETCH m.product p " +
           "LEFT JOIN FETCH m.warehouse w " +
           "LEFT JOIN FETCH m.createdBy u " +
           "ORDER BY m.createdAt DESC, m.movementId DESC",
           countQuery = "SELECT COUNT(m) FROM StockMovement m")
    Page<StockMovement> findAllWithDetailsOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Stock movement summary per product in a date range, pivoted by move type.
     * Returns: product_id, sku, name, qty_in, qty_out, qty_adjust, qty_return
     */
    @Query(value =
        "SELECT p.product_id, p.sku, p.name, " +
        "  COALESCE(SUM(CASE WHEN m.type = 'in' THEN m.quantity ELSE 0 END), 0) AS qty_in, " +
        "  COALESCE(SUM(CASE WHEN m.type = 'out' THEN m.quantity ELSE 0 END), 0) AS qty_out, " +
        "  COALESCE(SUM(CASE WHEN m.type = 'adjust' THEN m.quantity ELSE 0 END), 0) AS qty_adjust, " +
        "  COALESCE(SUM(CASE WHEN m.type = 'return' THEN m.quantity ELSE 0 END), 0) AS qty_return " +
        "FROM products p " +
        "JOIN stock_movements m ON m.product_id = p.product_id " +
        "  AND m.created_at >= :startDateTime AND m.created_at < :endDateTime " +
        "WHERE (:searchNull = true OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
        "    OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
        "GROUP BY p.product_id, p.sku, p.name " +
        "ORDER BY p.name",
        nativeQuery = true)
    List<Object[]> findMovementSummaryByProduct(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("searchNull") boolean searchNull,
            @Param("search") String search);

    /**
     * Stock rows per product × warehouse with cost/SRP.
     * Returns: product_id, sku, name, warehouse_id, warehouse_name, quantity, reorder_level, cost_price, srp
     */
    @Query(value =
        "SELECT p.product_id, p.sku, p.name, w.warehouse_id, w.name AS warehouse_name, " +
        "  COALESCE(i.quantity, 0) AS quantity, COALESCE(p.reorder_level, 0) AS reorder_level, " +
        "  p.cost_price, p.srp " +
        "FROM products p " +
        "CROSS JOIN warehouses w " +
        "LEFT JOIN inventory i ON i.product_id = p.product_id AND i.warehouse_id = w.warehouse_id " +
        "WHERE (:searchNull = true OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
        "    OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
        "  AND (:warehouseNull = true OR w.warehouse_id = :warehouseId) " +
        "  AND (:statusFilter = 'all' OR " +
        "       (:statusFilter = 'out' AND COALESCE(i.quantity, 0) = 0) OR " +
        "       (:statusFilter = 'low' AND COALESCE(i.quantity, 0) > 0 AND COALESCE(i.quantity, 0) <= COALESCE(p.reorder_level, 0)) OR " +
        "       (:statusFilter = 'ok' AND COALESCE(i.quantity, 0) > COALESCE(p.reorder_level, 0))) " +
        "ORDER BY p.name, w.name",
        nativeQuery = true)
    List<Object[]> findStockRows(
            @Param("searchNull") boolean searchNull,
            @Param("search") String search,
            @Param("warehouseNull") boolean warehouseNull,
            @Param("warehouseId") Integer warehouseId,
            @Param("statusFilter") String statusFilter);

    /**
     * Summary aggregates for stock report: total units, cost value, SRP value.
     */
    @Query(value =
        "SELECT COALESCE(SUM(COALESCE(i.quantity, 0)), 0) AS total_units, " +
        "       COALESCE(SUM(COALESCE(i.quantity, 0) * p.cost_price), 0) AS cost_value, " +
        "       COALESCE(SUM(COALESCE(i.quantity, 0) * p.srp), 0) AS srp_value " +
        "FROM products p " +
        "CROSS JOIN warehouses w " +
        "LEFT JOIN inventory i ON i.product_id = p.product_id AND i.warehouse_id = w.warehouse_id",
        nativeQuery = true)
    Object[] findStockSummary();

    @Query(value =
        "SELECT COUNT(DISTINCT p.product_id) FROM products p " +
        "CROSS JOIN warehouses w " +
        "LEFT JOIN inventory i ON i.product_id = p.product_id AND i.warehouse_id = w.warehouse_id " +
        "WHERE COALESCE(i.quantity, 0) = 0",
        nativeQuery = true)
    long countOutOfStockItems();

    @Query(value =
        "SELECT COUNT(DISTINCT p.product_id) FROM products p " +
        "CROSS JOIN warehouses w " +
        "LEFT JOIN inventory i ON i.product_id = p.product_id AND i.warehouse_id = w.warehouse_id " +
        "WHERE COALESCE(i.quantity, 0) > 0 AND COALESCE(i.quantity, 0) <= COALESCE(p.reorder_level, 0)",
        nativeQuery = true)
    long countLowStockItemsCross();
}

