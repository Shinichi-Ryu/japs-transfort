package com.japstransfort.controller;

import com.japstransfort.config.CustomUserDetails;
import com.japstransfort.entity.Order;
import com.japstransfort.entity.OrderStatus;
import com.japstransfort.entity.User;
import com.japstransfort.service.OrderAdminService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for administrative order management (/admin/orders).
 * Restricted to administrators only.
 */
@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class OrderAdminController {

    private final OrderAdminService orderAdminService;

    public OrderAdminController(OrderAdminService orderAdminService) {
        this.orderAdminService = orderAdminService;
    }

    /**
     * Lists orders paginated with search and status filtering.
     */
    @GetMapping
    public String listOrders(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Page<Order> ordersPage = orderAdminService.getOrders(search, status, page, size);

        if (userDetails != null) {
            model.addAttribute("currentUser", userDetails.getUser());
            model.addAttribute("userRole", userDetails.getRole().name());
        }
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("warehouses", orderAdminService.findAllWarehouses());
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("selectedStatus", status != null ? status : "");
        model.addAttribute("allStatuses", OrderStatus.values());

        return "admin/orders/list";
    }

    /**
     * Displays detail view for a specific order.
     */
    @GetMapping("/{id}")
    public String orderDetail(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Order order = orderAdminService.getOrderDetail(id);
            if (userDetails != null) {
                model.addAttribute("currentUser", userDetails.getUser());
                model.addAttribute("userRole", userDetails.getRole().name());
            }
            model.addAttribute("order", order);
            model.addAttribute("warehouses", orderAdminService.findAllWarehouses());
            return "admin/orders/detail";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/orders";
        }
    }

    /**
     * Approves a pending order, deducting stock from the selected warehouse.
     */
    @PostMapping("/{id}/approve")
    public String approveOrder(
            @PathVariable("id") Integer id,
            @RequestParam("warehouseId") Integer warehouseId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            User admin = userDetails != null ? userDetails.getUser() : null;
            Order order = orderAdminService.approveOrder(id, warehouseId, admin);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order #" + order.getOrderNo() + " approved successfully.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    /**
     * Cancels an order (pending or approved).
     */
    @PostMapping("/{id}/cancel")
    public String cancelOrder(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            User admin = userDetails != null ? userDetails.getUser() : null;
            Order order = orderAdminService.cancelOrder(id, admin);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order #" + order.getOrderNo() + " cancelled successfully.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
