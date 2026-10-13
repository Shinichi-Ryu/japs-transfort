package com.japstransfort.repository;

import com.japstransfort.entity.Order;
import com.japstransfort.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for Order operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByReseller_ResellerIdOrderByCreatedAtDesc(Integer resellerId);

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
}
