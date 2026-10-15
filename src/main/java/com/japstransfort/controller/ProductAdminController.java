package com.japstransfort.controller;

import com.japstransfort.dto.ProductFormDto;
import com.japstransfort.dto.ProductImageMeta;
import com.japstransfort.entity.Product;
import com.japstransfort.entity.ProductImage;
import com.japstransfort.service.CategoryService;
import com.japstransfort.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

        // Lightweight image metadata map (productId -> meta) – no byte[] loaded
        Map<Integer, ProductImageMeta> imageMetas = new HashMap<>();
        for (Product p : productsPage.getContent()) {
            imageMetas.put(p.getProductId(), productService.getImageMeta(p.getProductId()));
        }

        model.addAttribute("productsPage", productsPage);
        model.addAttribute("imageMetas", imageMetas);
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
        model.addAttribute("hasImage", false);
        return "products/form";
    }

    @PostMapping(value = "/create", consumes = "multipart/form-data")
    public String createProduct(@Valid @ModelAttribute("productDto") ProductFormDto dto,
                                BindingResult bindingResult,
                                @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("hasImage", false);
            return "products/form";
        }

        try {
            Product created = productService.create(dto);
            if (photoFile != null && !photoFile.isEmpty()) {
                productService.saveImage(created.getProductId(), photoFile);
            }
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product '" + created.getName() + "' (SKU: " + created.getSku() + ") created successfully.");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("sku")) {
                bindingResult.rejectValue("sku", "duplicate", ex.getMessage());
            } else {
                model.addAttribute("imageError", ex.getMessage());
            }
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("hasImage", false);
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

        ProductImageMeta meta = productService.getImageMeta(id);

        model.addAttribute("productDto", dto);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("hasImage", meta.isHasImage());
        model.addAttribute("imageVersion", meta.getVersion());
        return "products/form";
    }

    @PostMapping(value = "/edit/{id}", consumes = "multipart/form-data")
    public String updateProduct(@PathVariable("id") Integer id,
                                @Valid @ModelAttribute("productDto") ProductFormDto dto,
                                BindingResult bindingResult,
                                @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
                                @RequestParam(value = "removePhoto", required = false) Boolean removePhoto,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (dto.getSku() == null || dto.getSku().trim().isEmpty()) {
            bindingResult.rejectValue("sku", "NotBlank", "SKU is required.");
        }

        if (bindingResult.hasErrors()) {
            ProductImageMeta meta = productService.getImageMeta(id);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("hasImage", meta.isHasImage());
            model.addAttribute("imageVersion", meta.getVersion());
            return "products/form";
        }

        try {
            Product updated = productService.update(id, dto);

            if (Boolean.TRUE.equals(removePhoto)) {
                productService.removeImage(id);
            } else if (photoFile != null && !photoFile.isEmpty()) {
                productService.saveImage(id, photoFile);
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Product '" + updated.getName() + "' updated successfully.");
            return "redirect:/admin/products";
        } catch (IllegalArgumentException ex) {
            ProductImageMeta meta = productService.getImageMeta(id);
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("sku")) {
                bindingResult.rejectValue("sku", "duplicate", ex.getMessage());
            } else {
                model.addAttribute("imageError", ex.getMessage());
            }
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("hasImage", meta.isHasImage());
            model.addAttribute("imageVersion", meta.getVersion());
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

    /**
     * Serves a product image. Accessible to any authenticated user (admin or reseller).
     * Cache-Control: private, max-age=86400.
     */
    @GetMapping("/{id}/image")
    @ResponseBody
    public ResponseEntity<byte[]> serveImage(@PathVariable("id") Integer id) {
        Optional<ProductImage> opt = productService.getImage(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ProductImage img = opt.get();
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(img.getContentType()))
                .cacheControl(CacheControl.maxAge(86400, TimeUnit.SECONDS).cachePrivate())
                .body(img.getData());
    }

    /**
     * Handles multipart size exceeded — shows a friendly error on the form.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleFileTooLarge(MaxUploadSizeExceededException ex, Model model,
                                     @RequestParam(value = "productId", required = false) Integer productId) {
        model.addAttribute("imageError", "Image file is too large. Maximum allowed size is 2 MB.");
        model.addAttribute("categories", categoryService.findAll());
        if (productId != null) {
            ProductImageMeta meta = productService.getImageMeta(productId);
            model.addAttribute("hasImage", meta.isHasImage());
            model.addAttribute("imageVersion", meta.getVersion());
            model.addAttribute("productDto", new ProductFormDto());
        } else {
            model.addAttribute("hasImage", false);
            model.addAttribute("productDto", new ProductFormDto());
        }
        return "products/form";
    }
}
