package com.japstransfort.repository;

import com.japstransfort.entity.Reseller;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Reseller operations.
 */
@Repository
public interface ResellerRepository extends JpaRepository<Reseller, Integer> {

    @Query("SELECT COALESCE(SUM(r.balance), 0) FROM Reseller r")
    BigDecimal sumTotalBalance();

    Optional<Reseller> findByUser_UserId(Integer userId);

    List<Reseller> findByStatus(String status);

    long countByStatus(String status);

    @Query("SELECT r FROM Reseller r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.tier LEFT JOIN FETCH r.parentReseller")
    List<Reseller> findAllWithDetails();

    long countByTier_TierId(Integer tierId);

    @EntityGraph(attributePaths = {"user", "tier", "parentReseller"})
    @Query("SELECT r FROM Reseller r " +
           "WHERE (:search IS NULL OR :search = '' OR " +
           "      LOWER(r.businessName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "  AND (:tierId IS NULL OR r.tier.tierId = :tierId) " +
           "  AND (:status IS NULL OR :status = 'all' OR LOWER(r.status) = LOWER(:status))")
    Page<Reseller> findWithFilters(@Param("search") String search,
                                  @Param("tierId") Integer tierId,
                                  @Param("status") String status,
                                  Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reseller r WHERE r.resellerId = :id")
    Optional<Reseller> findByIdWithLock(@Param("id") Integer id);

    @Query("SELECT COUNT(r) FROM Reseller r WHERE r.balance > 0")
    long countWithBalance();

    @Query("SELECT COUNT(r) FROM Reseller r WHERE r.creditLimit > 0 AND r.balance > r.creditLimit")
    long countOverCreditLimit();
}
