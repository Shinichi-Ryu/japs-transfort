package com.japstransfort.service;

import com.japstransfort.dto.ProductFormDto;
import com.japstransfort.entity.Category;
import com.japstransfort.entity.Product;
import com.japstransfort.repository.CategoryRepository;
import com.japstransfort.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Service encapsulating business logic for Product management.
 */
@Service
public class ProductService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "productId", "name", "sku", "costPrice", "srp", "reorderLevel", "isActive"
    );

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<Product> getProducts(String search, Integer categoryId, String status,
                                     int page, int size, String sortField, String sortDir) {
        // Map status filter
        Boolean isActive = null;
        if ("active".equalsIgnoreCase(status)) {
            isActive = true;
        } else if ("inactive".equalsIgnoreCase(status)) {
            isActive = false;
        }

        if ("price".equalsIgnoreCase(sortField)) {
            sortField = "srp";
        }

        // Validate sort field to prevent invalid property injection
        String validSortField = ALLOWED_SORT_FIELDS.contains(sortField) ? sortField : "name";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(Math.max(page, 0), size > 0 ? size : 10, Sort.by(direction, validSortField));

        return productRepository.findWithFilters(
                (search != null && !search.trim().isEmpty()) ? search.trim() : null,
                categoryId,
                isActive,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Product findById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));
    }

    @Transactional
    public Product create(ProductFormDto dto) {
        String trimmedSku = dto.getSku() != null ? dto.getSku().trim() : "";
        if (trimmedSku.isEmpty()) {
            do {
                Long nextVal = productRepository.getNextSkuSequenceValue();
                trimmedSku = String.format("PRD-%06d", nextVal);
            } while (productRepository.existsBySkuIgnoreCase(trimmedSku));
        } else {
            if (productRepository.existsBySkuIgnoreCase(trimmedSku)) {
                throw new IllegalArgumentException("A product with SKU '" + trimmedSku + "' already exists.");
            }
        }

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Selected category does not exist."));

        Product product = new Product();
        product.setSku(trimmedSku);
        product.setName(dto.getName().trim());
        product.setCategory(category);
        product.setUnit(dto.getUnit() != null ? dto.getUnit().trim() : "pc");
        product.setCostPrice(dto.getCostPrice());
        product.setSrp(dto.getSrp());
        product.setReorderLevel(dto.getReorderLevel() != null ? dto.getReorderLevel() : 10);
        product.setIsActive(true);

        return productRepository.save(product);
    }

    @Transactional
    public Product update(Integer id, ProductFormDto dto) {
        Product product = findById(id);

        String trimmedSku = dto.getSku() != null ? dto.getSku().trim() : "";
        if (trimmedSku.isEmpty()) {
            throw new IllegalArgumentException("SKU is required.");
        }

        productRepository.findBySkuIgnoreCase(trimmedSku).ifPresent(existing -> {
            if (!existing.getProductId().equals(id)) {
                throw new IllegalArgumentException("Another product with SKU '" + trimmedSku + "' already exists.");
            }
        });

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Selected category does not exist."));

        product.setSku(trimmedSku);
        product.setName(dto.getName().trim());
        product.setCategory(category);
        product.setUnit(dto.getUnit() != null ? dto.getUnit().trim() : "pc");
        product.setCostPrice(dto.getCostPrice());
        product.setSrp(dto.getSrp());
        product.setReorderLevel(dto.getReorderLevel() != null ? dto.getReorderLevel() : 10);

        return productRepository.save(product);
    }

    /**
     * Soft delete: preserves audit history and stock movement records while deactivating the product.
     */
    @Transactional
    public void softDelete(Integer id) {
        Product product = findById(id);
        product.setIsActive(false);
        productRepository.save(product);
    }

    /**
     * Reactivates a previously soft-deleted product.
     */
    @Transactional
    public void restore(Integer id) {
        Product product = findById(id);
        product.setIsActive(true);
        productRepository.save(product);
    }
}
