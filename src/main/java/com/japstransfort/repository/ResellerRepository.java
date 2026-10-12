package com.japstransfort.repository;

import com.japstransfort.entity.Reseller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Reseller operations.
 */
@Repository
public interface ResellerRepository extends JpaRepository<Reseller, Integer> {

    Optional<Reseller> findByUser_UserId(Integer userId);

    List<Reseller> findByStatus(String status);

    @Query("SELECT r FROM Reseller r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.tier LEFT JOIN FETCH r.parentReseller")
    List<Reseller> findAllWithDetails();
}
