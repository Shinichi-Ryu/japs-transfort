package com.japstransfort.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Category form binding and validation.
 */
public class CategoryDto {

    private Integer categoryId;

    @NotBlank(message = "Category name is required.")
    @Size(max = 80, message = "Category name must not exceed 80 characters.")
    private String name;

    public CategoryDto() {
    }

    public CategoryDto(Integer categoryId, String name) {
        this.categoryId = categoryId;
        this.name = name;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
