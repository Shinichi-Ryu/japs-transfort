package com.japstransfort.controller;

import com.japstransfort.dto.CategoryDto;
import com.japstransfort.entity.Category;
import com.japstransfort.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Admin CRUD operations on Categories (/admin/categories).
 */
@Controller
@RequestMapping("/admin/categories")
public class CategoryAdminController {

    private final CategoryService categoryService;

    public CategoryAdminController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        if (!model.containsAttribute("categoryDto")) {
            model.addAttribute("categoryDto", new CategoryDto());
        }
        return "categories/list";
    }

    @PostMapping("/create")
    public String createCategory(@Valid @ModelAttribute("categoryDto") CategoryDto categoryDto,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "categories/list";
        }

        try {
            Category created = categoryService.create(categoryDto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Category '" + created.getName() + "' created successfully.");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            model.addAttribute("categories", categoryService.findAll());
            return "categories/list";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        Category category = categoryService.findById(id);
        CategoryDto dto = new CategoryDto(category.getCategoryId(), category.getName());
        model.addAttribute("categoryDto", dto);
        return "categories/form";
    }

    @PostMapping("/edit/{id}")
    public String updateCategory(@PathVariable("id") Integer id,
                                 @Valid @ModelAttribute("categoryDto") CategoryDto categoryDto,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "categories/form";
        }

        try {
            categoryService.update(id, categoryDto);
            redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully.");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            return "categories/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
