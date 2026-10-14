package com.japstransfort.controller;

import com.japstransfort.config.CustomUserDetails;
import com.japstransfort.entity.Commission;
import com.japstransfort.service.CommissionService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/**
 * Controller for administrative commission management (/admin/commissions).
 * Restricted to administrators only.
 */
@Controller
@RequestMapping("/admin/commissions")
@PreAuthorize("hasRole('ADMIN')")
public class CommissionAdminController {

    private final CommissionService commissionService;

    public CommissionAdminController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    /**
     * Lists commissions paginated with search, status filtering, and summary cards.
     */
    @GetMapping
    public String listCommissions(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Page<Commission> commissionsPage = commissionService.getCommissions(search, status, page, size);
        BigDecimal totalPending = commissionService.getTotalPending();
        BigDecimal totalPaid = commissionService.getTotalPaid();

        if (userDetails != null) {
            model.addAttribute("currentUser", userDetails.getUser());
            model.addAttribute("userRole", userDetails.getRole().name());
        }

        model.addAttribute("commissionsPage", commissionsPage);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("selectedStatus", status != null ? status : "");

        return "admin/commissions/list";
    }

    /**
     * Marks a pending commission as paid.
     */
    @PostMapping("/{id}/pay")
    public String markAsPaid(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            commissionService.markAsPaid(id);
            redirectAttributes.addFlashAttribute("successMessage", "Commission marked as paid successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update commission status.");
        }

        return "redirect:/admin/commissions";
    }
}
