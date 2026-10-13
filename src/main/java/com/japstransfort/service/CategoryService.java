package com.japstransfort.service;

import com.japstransfort.dto.CategoryDto;
import com.japstransfort.entity.Category;
import com.japstransfort.repository.CategoryRepository;
import com.japstransfort.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service encapsulating business logic for Category operations.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Category findById(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
    }

    @Transactional
    public Category create(CategoryDto dto) {
        String trimmedName = dto.getName().trim();

        if (categoryRepository.existsByName(trimmedName)) {
            throw new IllegalArgumentException("A category with the name '" + trimmedName + "' already exists.");
        }

        Category category = new Category();
        category.setName(trimmedName);
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Integer categoryId, CategoryDto dto) {
        Category category = findById(categoryId);
        String trimmedName = dto.getName().trim();

        categoryRepository.findByName(trimmedName).ifPresent(existing -> {
            if (!existing.getCategoryId().equals(categoryId)) {
                throw new IllegalArgumentException("Another category with the name '" + trimmedName + "' already exists.");
            }
        });

        category.setName(trimmedName);
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Integer categoryId) {
        Category category = findById(categoryId);

        long productCount = productRepository.countByCategory_CategoryId(categoryId);
        if (productCount > 0) {
            throw new IllegalStateException("Cannot delete category '" + category.getName() +
                    "' because " + productCount + " product(s) are currently assigned to it.");
        }

        categoryRepository.delete(category);
    }
}
