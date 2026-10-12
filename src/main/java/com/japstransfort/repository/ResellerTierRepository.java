package com.japstransfort.repository;

import com.japstransfort.entity.ResellerTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ResellerTier operations.
 */
@Repository
public interface ResellerTierRepository extends JpaRepository<ResellerTier, Integer> {

    Optional<ResellerTier> findByTierName(String tierName);

    boolean existsByTierName(String tierName);

    List<ResellerTier> findAllByOrderByDiscountPctAsc();
}
