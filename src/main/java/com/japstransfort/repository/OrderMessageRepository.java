package com.japstransfort.repository;

import com.japstransfort.entity.OrderMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderMessageRepository extends JpaRepository<OrderMessage, Integer> {

    @Query("SELECT m FROM OrderMessage m LEFT JOIN FETCH m.senderUser WHERE m.order.orderId = :orderId ORDER BY m.createdAt ASC")
    List<OrderMessage> findByOrderIdOrderByCreatedAtAsc(@Param("orderId") Integer orderId);

    @Query("SELECT COUNT(m) FROM OrderMessage m WHERE m.order.orderId = :orderId AND m.readByAdmin = false")
    long countUnreadForAdminByOrderId(@Param("orderId") Integer orderId);

    @Query("SELECT COUNT(m) FROM OrderMessage m WHERE m.readByAdmin = false")
    long countUnreadForAdminTotal();

    @Query("SELECT COUNT(m) FROM OrderMessage m WHERE m.order.orderId = :orderId AND m.order.reseller.resellerId = :resellerId AND m.readByReseller = false")
    long countUnreadForResellerByOrderId(@Param("orderId") Integer orderId, @Param("resellerId") Integer resellerId);

    @Query("SELECT COUNT(m) FROM OrderMessage m WHERE m.order.reseller.resellerId = :resellerId AND m.readByReseller = false")
    long countUnreadForResellerTotal(@Param("resellerId") Integer resellerId);

    @Modifying
    @Query("UPDATE OrderMessage m SET m.readByAdmin = true WHERE m.order.orderId = :orderId AND m.readByAdmin = false")
    int markAllAsReadForAdmin(@Param("orderId") Integer orderId);

    @Modifying
    @Query("UPDATE OrderMessage m SET m.readByReseller = true WHERE m.order.orderId = :orderId AND m.readByReseller = false")
    int markAllAsReadForReseller(@Param("orderId") Integer orderId);

    @Query("SELECT m.order.orderId, COUNT(m) FROM OrderMessage m WHERE m.senderRole = 'RESELLER' AND m.readByAdmin = false GROUP BY m.order.orderId")
    List<Object[]> countUnreadGroupedByOrderForAdmin();

    @Query("SELECT m.order.orderId, COUNT(m) FROM OrderMessage m WHERE m.order.reseller.resellerId = :resellerId AND m.senderRole = 'ADMIN' AND m.readByReseller = false GROUP BY m.order.orderId")
    List<Object[]> countUnreadGroupedByOrderForReseller(@Param("resellerId") Integer resellerId);

    @Query("SELECT COUNT(m) FROM OrderMessage m WHERE m.senderRole = 'RESELLER' AND m.readByAdmin = false")
    long countUnreadForAdmin();

    @Query("SELECT COUNT(m) FROM OrderMessage m WHERE m.order.reseller.resellerId = :resellerId AND m.senderRole = 'ADMIN' AND m.readByReseller = false")
    long countUnreadForReseller(@Param("resellerId") Integer resellerId);

    @Query("SELECT COUNT(m) FROM OrderMessage m WHERE (:role = 'ADMIN' AND m.senderRole = 'RESELLER' AND m.readByAdmin = false) OR (:role = 'RESELLER' AND m.order.reseller.resellerId = :resellerId AND m.senderRole = 'ADMIN' AND m.readByReseller = false)")
    long countUnreadMessages(@Param("role") String role, @Param("resellerId") Integer resellerId);
}
