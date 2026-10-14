package com.japstransfort.repository;

import com.japstransfort.entity.Commission;
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
 * Spring Data JPA repository for Commission operations.
 */
@Repository
public interface CommissionRepository extends JpaRepository<Commission, Integer> {

    boolean existsByOrder_OrderId(Integer orderId);

    Optional<Commission> findByOrder_OrderId(Integer orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Commission c WHERE c.commissionId = :id")
    Optional<Commission> findByIdWithLock(@Param("id") Integer id);

    @Query(value = "SELECT c FROM Commission c LEFT JOIN FETCH c.reseller r LEFT JOIN FETCH c.order o "
            + "WHERE (:search IS NULL OR :search = '' "
            + "  OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "  OR LOWER(r.businessName) LIKE LOWER(CONCAT('%', :search, '%'))) "
            + "AND (:statusNull = true OR LOWER(c.status) = LOWER(:status))",
           countQuery = "SELECT COUNT(c) FROM Commission c LEFT JOIN c.reseller r LEFT JOIN c.order o "
            + "WHERE (:search IS NULL OR :search = '' "
            + "  OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "  OR LOWER(r.businessName) LIKE LOWER(CONCAT('%', :search, '%'))) "
            + "AND (:statusNull = true OR LOWER(c.status) = LOWER(:status))")
    Page<Commission> findWithFilters(@Param("search") String search,
                                     @Param("statusNull") boolean statusNull,
                                     @Param("status") String status,
                                     Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Commission c WHERE LOWER(c.status) = LOWER(:status)")
    BigDecimal sumAmountByStatus(@Param("status") String status);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Commission c WHERE c.reseller.resellerId = :resellerId AND LOWER(c.status) = LOWER(:status)")
    BigDecimal sumAmountByResellerIdAndStatus(@Param("resellerId") Integer resellerId, @Param("status") String status);

    @Query("SELECT c FROM Commission c LEFT JOIN FETCH c.order o WHERE c.reseller.resellerId = :resellerId ORDER BY c.createdAt DESC, c.commissionId DESC")
    List<Commission> findByReseller_ResellerIdOrderByCreatedAtDesc(@Param("resellerId") Integer resellerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT INTO commissions (reseller_id, order_id, amount, status, created_at) " +
                   "VALUES (:resellerId, :orderId, :amount, 'pending', NOW()) " +
                   "ON CONFLICT (order_id) DO NOTHING", nativeQuery = true)
    int insertIfNotExists(@Param("resellerId") Integer resellerId,
                          @Param("orderId") Integer orderId,
                          @Param("amount") BigDecimal amount);
}
