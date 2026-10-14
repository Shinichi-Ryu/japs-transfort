package com.japstransfort.repository;

import com.japstransfort.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.receivedBy WHERE p.order.orderId = :orderId ORDER BY p.paidAt DESC, p.paymentId DESC")
    List<Payment> findByOrderIdWithReceivedBy(@Param("orderId") Integer orderId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.orderId = :orderId")
    BigDecimal sumAmountByOrderId(@Param("orderId") Integer orderId);

    boolean existsByOrder_OrderId(Integer orderId);
}
