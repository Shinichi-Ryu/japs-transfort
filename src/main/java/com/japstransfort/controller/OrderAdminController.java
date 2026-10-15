package com.japstransfort.controller;

import com.japstransfort.config.CustomUserDetails;
import com.japstransfort.entity.Order;
import com.japstransfort.entity.OrderStatus;
import com.japstransfort.entity.Payment;
import com.japstransfort.entity.PaymentMethod;
import com.japstransfort.entity.Shipment;
import com.japstransfort.entity.User;
import com.japstransfort.service.CommissionService;
import com.japstransfort.service.OrderAdminService;
import com.japstransfort.service.OrderService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Controller for administrative order management (/admin/orders).
 * Restricted to administrators only.
 */
@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class OrderAdminController {

    private final OrderAdminService orderAdminService;
    private final OrderService orderService;
    private final CommissionService commissionService;

    public OrderAdminController(OrderAdminService orderAdminService,
                                OrderService orderService,
                                CommissionService commissionService) {
        this.orderAdminService = orderAdminService;
        this.orderService = orderService;
        this.commissionService = commissionService;
    }

    /**
     * Lists orders paginated with search, status, and payment status filtering.
     */
    @GetMapping
    public String listOrders(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "paymentStatus", required = false) String paymentStatus,
            @RequestParam(name = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Page<Order> ordersPage = orderAdminService.getOrders(search, status, paymentStatus, paymentMethod, page, size);

        if (userDetails != null) {
            model.addAttribute("currentUser", userDetails.getUser());
            model.addAttribute("userRole", userDetails.getRole().name());
        }
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("warehouses", orderAdminService.findAllWarehouses());
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("selectedStatus", status != null ? status : "");
        model.addAttribute("selectedPaymentStatus", paymentStatus != null ? paymentStatus : "");
        model.addAttribute("selectedPaymentMethod", paymentMethod != null ? paymentMethod : "");
        model.addAttribute("allStatuses", OrderStatus.values());
        model.addAttribute("allPaymentMethods", PaymentMethod.values());

        return "admin/orders/list";
    }

    /**
     * Displays detail view for a specific order, including any shipment data and payment history.
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
            // Attach shipment so the template can show courier/tracking/dates
            orderAdminService.getShipmentForOrder(id).ifPresent(s -> model.addAttribute("shipment", s));
            // Attach commission if one exists for this order
            commissionService.getCommissionForOrder(id).ifPresent(c -> model.addAttribute("commission", c));

            List<Payment> payments = orderService.getPaymentsForOrder(id);
            BigDecimal amountPaid = orderService.getAmountPaidForOrder(id);
            BigDecimal orderTotal = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;
            BigDecimal amountRemaining = orderTotal.subtract(amountPaid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            model.addAttribute("payments", payments);
            model.addAttribute("amountPaid", amountPaid);
            model.addAttribute("amountRemaining", amountRemaining);

            return "admin/orders/detail";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/orders";
        }
    }

    /**
     * Displays printable invoice for an order.
     */
    @GetMapping("/{id}/invoice")
    public String orderInvoice(
            @PathVariable("id") Integer id,
            Model model,
            HttpServletResponse response) {

        return orderService.getInvoiceForAdmin(id)
                .map(invoice -> {
                    model.addAttribute("invoice", invoice);
                    model.addAttribute("company", invoice.getCompany());
                    model.addAttribute("backUrl", "/admin/orders/" + id);
                    return "print/invoice";
                })
                .orElseGet(() -> {
                    response.setStatus(HttpStatus.NOT_FOUND.value());
                    return "portal/order-not-found";
                });
    }

    /**
     * Records a payment against an order.
     */
    @PostMapping("/{id}/payments")
    public String recordPayment(
            @PathVariable("id") Integer id,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("method") String method,
            @RequestParam(value = "referenceNo", required = false) String referenceNo,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            User admin = userDetails != null ? userDetails.getUser() : null;
            orderService.recordPayment(id, amount, method, referenceNo, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Payment recorded successfully.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
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
     * Marks an approved order as packed.
     */
    @PostMapping("/{id}/pack")
    public String packOrder(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            Order order = orderAdminService.markPacked(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order #" + order.getOrderNo() + " marked as packed.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    /**
     * Ships a packed order: requires courier name, optional tracking number.
     */
    @PostMapping("/{id}/ship")
    public String shipOrder(
            @PathVariable("id") Integer id,
            @RequestParam(value = "courier", required = false) String courier,
            @RequestParam(value = "trackingNo", required = false) String trackingNo,
            RedirectAttributes redirectAttributes) {

        try {
            Order order = orderAdminService.shipOrder(id, courier, trackingNo);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order #" + order.getOrderNo() + " shipped successfully.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    /**
     * Marks a shipped order as delivered.
     */
    @PostMapping("/{id}/deliver")
    public String deliverOrder(
            @PathVariable("id") Integer id,
            @RequestParam(value = "cashCollected", defaultValue = "false") boolean cashCollected,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            User admin = userDetails != null ? userDetails.getUser() : null;
            Order order = orderAdminService.markDelivered(id, cashCollected, admin);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order #" + order.getOrderNo() + " marked as delivered.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    /**
     * Cancels an order (pending or approved). Requires a reason from the standard list.
     */
    @PostMapping("/{id}/cancel")
    public String cancelOrder(
            @PathVariable("id") Integer id,
            @RequestParam(value = "cancelReason", required = false) String cancelReason,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            User admin = userDetails != null ? userDetails.getUser() : null;
            Order order = orderAdminService.cancelOrder(id, cancelReason, "ADMIN", admin);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order #" + order.getOrderNo() + " cancelled successfully.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }
}
