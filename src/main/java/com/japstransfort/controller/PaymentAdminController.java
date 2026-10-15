package com.japstransfort.controller;

import com.japstransfort.service.OrderService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for admin payment receipt printing (/admin/payments).
 */
@Controller
@RequestMapping("/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class PaymentAdminController {

    private final OrderService orderService;

    public PaymentAdminController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Displays printable payment receipt.
     */
    @GetMapping("/{id}/receipt")
    public String paymentReceipt(
            @PathVariable("id") Integer id,
            Model model,
            HttpServletResponse response) {

        return orderService.getReceiptForAdmin(id)
                .map(receipt -> {
                    model.addAttribute("receipt", receipt);
                    model.addAttribute("company", receipt.getCompany());
                    model.addAttribute("backUrl", "/admin/orders/" + receipt.getOrderId());
                    return "print/receipt";
                })
                .orElseGet(() -> {
                    response.setStatus(HttpStatus.NOT_FOUND.value());
                    return "portal/order-not-found";
                });
    }
}
