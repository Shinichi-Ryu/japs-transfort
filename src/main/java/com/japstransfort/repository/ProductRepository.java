package com.japstransfort.repository;

import com.japstransfort.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Product operations.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findByIsActiveTrue();

    List<Product> findByCategory_CategoryIdAndIsActiveTrue(Integer categoryId);

    boolean existsByCategory_CategoryId(Integer categoryId);

    long countByCategory_CategoryId(Integer categoryId);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.isActive = true")
    List<Product> findAllActiveWithCategory();
}
