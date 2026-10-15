package com.japstransfort.repository;

import com.japstransfort.dto.ReceivablesRowProjection;
import com.japstransfort.entity.Order;
import com.japstransfort.entity.OrderStatus;
import com.japstransfort.entity.PaymentMethod;
import com.japstransfort.entity.PaymentStatus;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Order operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    long countByStatus(OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN (:salesStatuses) AND o.createdAt >= :startDateTime AND o.createdAt < :endDateTime")
    long countSalesOrders(@Param("salesStatuses") List<OrderStatus> salesStatuses,
                          @Param("startDateTime") LocalDateTime startDateTime,
                          @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status IN (:salesStatuses) AND o.createdAt >= :startDateTime AND o.createdAt < :endDateTime")
    BigDecimal sumGrossSales(@Param("salesStatuses") List<OrderStatus> salesStatuses,
                             @Param("startDateTime") LocalDateTime startDateTime,
                             @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT new com.japstransfort.dto.ResellerSalesDto(r.businessName, COUNT(o), COALESCE(SUM(o.total), 0)) " +
           "FROM Order o JOIN o.reseller r " +
           "WHERE o.status IN (:salesStatuses) AND o.createdAt >= :startDateTime AND o.createdAt < :endDateTime " +
           "GROUP BY r.resellerId, r.businessName " +
           "ORDER BY SUM(o.total) DESC")
    List<com.japstransfort.dto.ResellerSalesDto> findSalesByReseller(
            @Param("salesStatuses") List<OrderStatus> salesStatuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT new com.japstransfort.dto.ProductSalesDto(p.sku, p.name, SUM(i.quantity), COALESCE(SUM(i.lineTotal), 0)) " +
           "FROM OrderItem i JOIN i.order o JOIN i.product p " +
           "WHERE o.status IN (:salesStatuses) AND o.createdAt >= :startDateTime AND o.createdAt < :endDateTime " +
           "GROUP BY p.productId, p.sku, p.name " +
           "ORDER BY SUM(i.lineTotal) DESC")
    List<com.japstransfort.dto.ProductSalesDto> findSalesByProduct(
            @Param("salesStatuses") List<OrderStatus> salesStatuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT new com.japstransfort.dto.PaymentMethodSalesDto(o.paymentMethod, COUNT(o), COALESCE(SUM(o.total), 0)) " +
           "FROM Order o " +
           "WHERE o.status IN (:salesStatuses) AND o.createdAt >= :startDateTime AND o.createdAt < :endDateTime " +
           "GROUP BY o.paymentMethod " +
           "ORDER BY SUM(o.total) DESC")
    List<com.japstransfort.dto.PaymentMethodSalesDto> findSalesByPaymentMethod(
            @Param("salesStatuses") List<OrderStatus> salesStatuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    @Query(value = "SELECT TO_CHAR(o.created_at, 'YYYY-MM-DD') AS saleDate, COUNT(o.order_id) AS orderCount, COALESCE(SUM(o.total), 0) AS totalAmount " +
                   "FROM orders o " +
                   "WHERE o.status IN ('approved', 'packed', 'shipped', 'delivered') " +
                   "  AND o.created_at >= :startDateTime " +
                   "  AND o.created_at < :endDateTime " +
                   "GROUP BY TO_CHAR(o.created_at, 'YYYY-MM-DD') " +
                   "ORDER BY saleDate DESC", nativeQuery = true)
    List<Object[]> findDailySales(@Param("startDateTime") LocalDateTime startDateTime,
                                   @Param("endDateTime") LocalDateTime endDateTime);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.reseller LEFT JOIN FETCH o.payments " +
           "WHERE o.status IN (:salesStatuses) AND o.createdAt >= :startDateTime AND o.createdAt < :endDateTime " +
           "ORDER BY o.createdAt DESC, o.orderId DESC")
    List<Order> findSalesOrdersForExport(
            @Param("salesStatuses") List<OrderStatus> salesStatuses,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    /**
     * Receivables report: per reseller, sums remaining (total - paid) bucketed by age in days from order created_at.
     * Only open orders (approved/packed/shipped/delivered) with remaining > 0 are included.
     * Returns: reseller_id, business_name, balance, credit_limit, open_order_count, oldest_order_date,
     *          bucket_030, bucket_3160, bucket_6190, bucket_over_90, total_remaining
     */
    @Query(value =
        "SELECT r.reseller_id AS reseller_id, r.business_name AS business_name, " +
        "       COALESCE(r.balance, 0) AS balance, COALESCE(r.credit_limit, 0) AS credit_limit, " +
        "       COUNT(DISTINCT o.order_id) AS open_order_count, " +
        "       MIN(o.created_at::date) AS oldest_order_date, " +
        "       COALESCE(SUM(CASE WHEN CURRENT_DATE - o.created_at::date BETWEEN 0 AND 30 THEN GREATEST(o.total - COALESCE(paid.paid_sum,0), 0) ELSE 0 END), 0) AS bucket_030, " +
        "       COALESCE(SUM(CASE WHEN CURRENT_DATE - o.created_at::date BETWEEN 31 AND 60 THEN GREATEST(o.total - COALESCE(paid.paid_sum,0), 0) ELSE 0 END), 0) AS bucket_3160, " +
        "       COALESCE(SUM(CASE WHEN CURRENT_DATE - o.created_at::date BETWEEN 61 AND 90 THEN GREATEST(o.total - COALESCE(paid.paid_sum,0), 0) ELSE 0 END), 0) AS bucket_6190, " +
        "       COALESCE(SUM(CASE WHEN CURRENT_DATE - o.created_at::date > 90 THEN GREATEST(o.total - COALESCE(paid.paid_sum,0), 0) ELSE 0 END), 0) AS bucket_over_90, " +
        "       COALESCE(SUM(GREATEST(o.total - COALESCE(paid.paid_sum,0), 0)), 0) AS total_remaining " +
        "FROM resellers r " +
        "JOIN orders o ON o.reseller_id = r.reseller_id " +
        "     AND o.status IN ('approved','packed','shipped','delivered') " +
        "LEFT JOIN (SELECT order_id, SUM(amount) AS paid_sum FROM payments GROUP BY order_id) paid ON paid.order_id = o.order_id " +
        "WHERE (:searchNull = true OR LOWER(r.business_name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
        "GROUP BY r.reseller_id, r.business_name, r.balance, r.credit_limit " +
        "HAVING (:balanceOnly = false OR COALESCE(SUM(GREATEST(o.total - COALESCE(paid.paid_sum,0), 0)), 0) > 0) " +
        "ORDER BY total_remaining DESC",
        nativeQuery = true)
    List<ReceivablesRowProjection> findReceivablesRows(
            @Param("searchNull") boolean searchNull,
            @Param("search") String search,
            @Param("balanceOnly") boolean balanceOnly);

    /**
     * Sum of all remaining amounts across all open orders (for reconciliation).
     */
    @Query(value =
        "SELECT COALESCE(SUM(GREATEST(o.total - COALESCE(paid.paid_sum,0), 0)), 0) " +
        "FROM orders o " +
        "LEFT JOIN (SELECT order_id, SUM(amount) AS paid_sum FROM payments GROUP BY order_id) paid ON paid.order_id = o.order_id " +
        "WHERE o.status IN ('approved','packed','shipped','delivered')",
        nativeQuery = true)
    BigDecimal sumTotalRemainingOpenOrders();

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
            + "AND (:statusNull = true OR o.status = :status) "
            + "AND (:paymentStatusNull = true OR o.paymentStatus = :paymentStatus) "
            + "AND (:paymentMethodNull = true OR o.paymentMethod = :paymentMethod)",
           countQuery = "SELECT COUNT(o) FROM Order o LEFT JOIN o.reseller r "
            + "WHERE (:search IS NULL OR :search = '' OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(r.businessName) LIKE LOWER(CONCAT('%', :search, '%'))) "
            + "AND (:statusNull = true OR o.status = :status) "
            + "AND (:paymentStatusNull = true OR o.paymentStatus = :paymentStatus) "
            + "AND (:paymentMethodNull = true OR o.paymentMethod = :paymentMethod)")
    Page<Order> findWithFilters(@Param("search") String search,
                                @Param("statusNull") boolean statusNull,
                                @Param("status") OrderStatus status,
                                @Param("paymentStatusNull") boolean paymentStatusNull,
                                @Param("paymentStatus") PaymentStatus paymentStatus,
                                @Param("paymentMethodNull") boolean paymentMethodNull,
                                @Param("paymentMethod") PaymentMethod paymentMethod,
                                Pageable pageable);

    default Page<Order> findWithFilters(String search,
                                        boolean statusNull,
                                        OrderStatus status,
                                        boolean paymentStatusNull,
                                        PaymentStatus paymentStatus,
                                        Pageable pageable) {
        return findWithFilters(search, statusNull, status, paymentStatusNull, paymentStatus, true, PaymentMethod.CREDIT, pageable);
    }

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product LEFT JOIN FETCH o.reseller LEFT JOIN FETCH o.warehouse WHERE o.orderId = :id")
    Optional<Order> findWithItemsById(@Param("id") Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :id")
    Optional<Order> findByIdWithLock(@Param("id") Integer id);

    @Modifying
    @Query(value = "UPDATE orders SET updated_at = NOW() WHERE order_id = :id", nativeQuery = true)
    void touchUpdatedAt(@Param("id") Integer id);

    /**
     * Sum of order totals (charges) for a reseller with approved/packed/shipped/delivered status
     * strictly before the given cutoff (used for opening balance).
     */
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
           "WHERE o.reseller.resellerId = :resellerId " +
           "AND o.status IN :statuses " +
           "AND o.createdAt < :before")
    BigDecimal sumChargesBeforeDate(@Param("resellerId") Integer resellerId,
                                    @Param("statuses") List<OrderStatus> statuses,
                                    @Param("before") LocalDateTime before);

    /**
     * Fetch invoiceable orders for a reseller within [from, toExclusive).
     * Returns orderId, orderNo, createdAt, total – no lazy associations.
     */
    @Query("SELECT o FROM Order o " +
           "WHERE o.reseller.resellerId = :resellerId " +
           "AND o.status IN :statuses " +
           "AND o.createdAt >= :from " +
           "AND o.createdAt < :toExclusive " +
           "ORDER BY o.createdAt ASC, o.orderId ASC")
    List<Order> findStatementOrders(@Param("resellerId") Integer resellerId,
                                    @Param("statuses") List<OrderStatus> statuses,
                                    @Param("from") LocalDateTime from,
                                    @Param("toExclusive") LocalDateTime toExclusive);
}
