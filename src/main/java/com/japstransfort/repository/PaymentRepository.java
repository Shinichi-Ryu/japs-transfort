package com.japstransfort.repository;

import com.japstransfort.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.receivedBy WHERE p.order.orderId = :orderId ORDER BY p.paidAt DESC, p.paymentId DESC")
    List<Payment> findByOrderIdWithReceivedBy(@Param("orderId") Integer orderId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.orderId = :orderId")
    BigDecimal sumAmountByOrderId(@Param("orderId") Integer orderId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paidAt >= :startDateTime AND p.paidAt < :endDateTime")
    BigDecimal sumAmountByPaidAtBetween(@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime);

    boolean existsByOrder_OrderId(Integer orderId);

    /**
     * Sum of payments on a reseller's orders strictly before the cutoff (for opening balance).
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.order.reseller.resellerId = :resellerId " +
           "AND p.paidAt < :before")
    BigDecimal sumPaymentsForResellerBeforeDate(@Param("resellerId") Integer resellerId,
                                                @Param("before") LocalDateTime before);

    /**
     * Fetch payment rows for a reseller within [from, toExclusive).
     * Ordered by paidAt ASC, paymentId ASC so they interleave with order rows by date.
     */
    @Query("SELECT p FROM Payment p " +
           "WHERE p.order.reseller.resellerId = :resellerId " +
           "AND p.paidAt >= :from " +
           "AND p.paidAt < :toExclusive " +
           "ORDER BY p.paidAt ASC, p.paymentId ASC")
    List<Payment> findStatementPayments(@Param("resellerId") Integer resellerId,
                                        @Param("from") LocalDateTime from,
                                        @Param("toExclusive") LocalDateTime toExclusive);
}

