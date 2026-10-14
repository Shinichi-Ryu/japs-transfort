package com.japstransfort.repository;

import com.japstransfort.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Product operations.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    Optional<Product> findBySku(String sku);

    Optional<Product> findBySkuIgnoreCase(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    @Query(value = "SELECT nextval('product_sku_seq')", nativeQuery = true)
    Long getNextSkuSequenceValue();

    List<Product> findByIsActiveTrue();

    List<Product> findByCategory_CategoryIdAndIsActiveTrue(Integer categoryId);

    boolean existsByCategory_CategoryId(Integer categoryId);

    long countByCategory_CategoryId(Integer categoryId);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.isActive = true")
    List<Product> findAllActiveWithCategory();

    /**
     * Filtered search query supporting case-insensitive name/SKU search, category filter,
     * active/inactive status filter, and pagination with dynamic sorting.
     */
    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p " +
           "WHERE (:search IS NULL OR :search = '' OR " +
           "      LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "      LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "  AND (:categoryId IS NULL OR p.category.categoryId = :categoryId) " +
           "  AND (:isActive IS NULL OR p.isActive = :isActive)")
    Page<Product> findWithFilters(@Param("search") String search,
                                 @Param("categoryId") Integer categoryId,
                                 @Param("isActive") Boolean isActive,
                                 Pageable pageable);
}
