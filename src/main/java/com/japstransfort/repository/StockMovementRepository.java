package com.japstransfort.repository;

import com.japstransfort.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}
