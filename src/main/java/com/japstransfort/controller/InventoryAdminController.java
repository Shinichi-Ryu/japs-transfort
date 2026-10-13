package com.japstransfort.controller;

import com.japstransfort.config.CustomUserDetails;
import com.japstransfort.dto.InventoryRowDto;
import com.japstransfort.entity.StockMovement;
import com.japstransfort.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for admin inventory management (/admin/inventory).
 */
@Controller
@RequestMapping("/admin/inventory")
@PreAuthorize("hasRole('ADMIN')")
public class InventoryAdminController {

    private final InventoryService inventoryService;

    public InventoryAdminController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Lists current inventory by product and warehouse.
     */
    @GetMapping
    public String listInventory(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "warehouseId", required = false) Integer warehouseId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", defaultValue = "name") String sort,
            @RequestParam(name = "dir", defaultValue = "asc") String dir,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Page<InventoryRowDto> inventoryPage = inventoryService.getInventoryRows(search, warehouseId, page, 10, sort, dir);

        if (userDetails != null) {
            model.addAttribute("currentUser", userDetails.getUser());
            model.addAttribute("userRole", userDetails.getRole().name());
        }
        model.addAttribute("inventoryPage", inventoryPage);
        model.addAttribute("warehouses", inventoryService.findAllWarehouses());
        model.addAttribute("products", inventoryService.findAllProducts());
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("warehouseId", warehouseId);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("reverseDir", "asc".equalsIgnoreCase(dir) ? "desc" : "asc");

        return "inventory/list";
    }

    /**
     * Search active products by name or SKU for modal autocomplete (case-insensitive, limit 20).
     */
    @GetMapping("/products/search")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> searchProducts(
            @RequestParam(name = "q", required = false) String query) {
        return inventoryService.searchActiveProducts(query, 20);
    }

    /**
     * Handles receiving stock into a warehouse for a product.
     */
    @PostMapping("/receive")
    public String receiveStock(
            @RequestParam("productId") Integer productId,
            @RequestParam("warehouseId") Integer warehouseId,
            @RequestParam("quantity") int quantity,
            @RequestParam("reference") String reference,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            inventoryService.receiveStock(productId, warehouseId, quantity, reference,
                    userDetails != null ? userDetails.getUser() : null);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Stock received successfully (" + quantity + " units). Reference: " + reference);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/inventory";
    }

    /**
     * Handles stock adjustment (relative +/- change or set new count).
     */
    @PostMapping("/adjust")
    public String adjustStock(
            @RequestParam("productId") Integer productId,
            @RequestParam("warehouseId") Integer warehouseId,
            @RequestParam(value = "mode", defaultValue = "relative") String mode,
            @RequestParam("quantity") int quantity,
            @RequestParam("reference") String reference,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            inventoryService.adjustStock(productId, warehouseId, mode, quantity, reference,
                    userDetails != null ? userDetails.getUser() : null);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Stock adjusted successfully. Reference: " + reference);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/inventory";
    }

    /**
     * Lists stock movements paginated, latest movements first.
     */
    @GetMapping("/movements")
    public String listMovements(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Page<StockMovement> movementsPage = inventoryService.getMovements(page, 10);

        if (userDetails != null) {
            model.addAttribute("currentUser", userDetails.getUser());
            model.addAttribute("userRole", userDetails.getRole().name());
        }
        model.addAttribute("movementsPage", movementsPage);

        return "inventory/movements";
    }
}
