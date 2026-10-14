package com.japstransfort.repository;

import com.japstransfort.entity.Order;
import com.japstransfort.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Order operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH o.shipment WHERE o.reseller.resellerId = :resellerId ORDER BY o.createdAt DESC, o.orderId DESC")
    List<Order> findByReseller_ResellerIdOrderByCreatedAtDesc(@Param("resellerId") Integer resellerId);

    List<Order> findByReseller_ResellerIdAndStatus(Integer resellerId, OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.reseller.resellerId = :resellerId AND o.status = :status")
    BigDecimal sumTotalByResellerAndStatus(@Param("resellerId") Integer resellerId, @Param("status") OrderStatus status);

    default BigDecimal sumPendingTotalByReseller(Integer resellerId) {
        return sumTotalByResellerAndStatus(resellerId, OrderStatus.pending);
    }

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderNo = :orderNo")
    long countByOrderNo(@Param("orderNo") String orderNo);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderNo LIKE :prefix%")
    long countByOrderNoPrefix(@Param("prefix") String prefix);

    @Query(value = "SELECT o FROM Order o LEFT JOIN FETCH o.reseller r "
            + "WHERE (:search IS NULL OR :search = '' OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(r.businessName) LIKE LOWER(CONCAT('%', :search, '%'))) "
            + "AND (:statusNull = true OR o.status = :status)",
           countQuery = "SELECT COUNT(o) FROM Order o LEFT JOIN o.reseller r "
            + "WHERE (:search IS NULL OR :search = '' OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(r.businessName) LIKE LOWER(CONCAT('%', :search, '%'))) "
            + "AND (:statusNull = true OR o.status = :status)")
    Page<Order> findWithFilters(@Param("search") String search, @Param("statusNull") boolean statusNull, @Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH o.reseller LEFT JOIN FETCH o.warehouse WHERE o.orderId = :id")
    Optional<Order> findWithItemsById(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :id")
    Optional<Order> findByIdWithLock(@Param("id") Integer id);

    @Modifying
    @Query(value = "UPDATE orders SET updated_at = NOW() WHERE order_id = :id", nativeQuery = true)
    void touchUpdatedAt(@Param("id") Integer id);
}
