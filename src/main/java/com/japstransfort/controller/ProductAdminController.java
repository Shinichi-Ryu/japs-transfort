package com.japstransfort.controller;

import com.japstransfort.dto.ProductFormDto;
import com.japstransfort.entity.Product;
import com.japstransfort.service.CategoryService;
import com.japstransfort.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Admin CRUD operations on Products (/admin/products).
 */
@Controller
@RequestMapping("/admin/products")
public class ProductAdminController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductAdminController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String listProducts(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "status", required = false, defaultValue = "all") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", defaultValue = "name") String sort,
            @RequestParam(name = "dir", defaultValue = "asc") String dir,
            Model model) {

        Page<Product> productsPage = productService.getProducts(search, categoryId, status, page, 10, sort, dir);

        model.addAttribute("productsPage", productsPage);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status != null ? status : "all");
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("reverseDir", "asc".equalsIgnoreCase(dir) ? "desc" : "asc");

        return "products/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("productDto")) {
            model.addAttribute("productDto", new ProductFormDto());
        }
        model.addAttribute("categories", categoryService.findAll());
        return "products/form";
    }

    @PostMapping("/create")
    public String createProduct(@Valid @ModelAttribute("productDto") ProductFormDto dto,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "products/form";
        }

        try {
            Product created = productService.create(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product '" + created.getName() + "' (SKU: " + created.getSku() + ") created successfully.");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("sku", "duplicate", ex.getMessage());
            model.addAttribute("categories", categoryService.findAll());
            return "products/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        Product product = productService.findById(id);

        ProductFormDto dto = new ProductFormDto();
        dto.setProductId(product.getProductId());
        dto.setSku(product.getSku());
        dto.setName(product.getName());
        dto.setCategoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null);
        dto.setUnit(product.getUnit());
        dto.setCostPrice(product.getCostPrice());
        dto.setSrp(product.getSrp());
        dto.setReorderLevel(product.getReorderLevel());
        dto.setIsActive(product.getIsActive());

        model.addAttribute("productDto", dto);
        model.addAttribute("categories", categoryService.findAll());
        return "products/form";
    }

    @PostMapping("/edit/{id}")
    public String updateProduct(@PathVariable("id") Integer id,
                                @Valid @ModelAttribute("productDto") ProductFormDto dto,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (dto.getSku() == null || dto.getSku().trim().isEmpty()) {
            bindingResult.rejectValue("sku", "NotBlank", "SKU is required.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "products/form";
        }

        try {
            Product updated = productService.update(id, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product '" + updated.getName() + "' updated successfully.");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("sku", "duplicate", ex.getMessage());
            model.addAttribute("categories", categoryService.findAll());
            return "products/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String softDeleteProduct(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            productService.softDelete(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product marked as inactive (soft deleted).");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/restore/{id}")
    public String restoreProduct(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            productService.restore(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product restored to active status.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/products";
    }
}
