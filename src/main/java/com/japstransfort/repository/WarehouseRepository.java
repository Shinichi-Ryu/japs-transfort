package com.japstransfort.repository;

import com.japstransfort.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Warehouse operations.
 */
@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {

    Optional<Warehouse> findByName(String name);
}
