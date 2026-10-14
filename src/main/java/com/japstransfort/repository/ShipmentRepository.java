package com.japstransfort.repository;

import com.japstransfort.entity.Shipment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Integer> {

    Optional<Shipment> findByOrder_OrderId(Integer orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Shipment s WHERE s.order.orderId = :orderId")
    Optional<Shipment> findByOrderIdWithLock(@Param("orderId") Integer orderId);
}
