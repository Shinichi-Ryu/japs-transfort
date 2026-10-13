package com.japstransfort.controller;

import com.japstransfort.dto.TierFormDto;
import com.japstransfort.entity.ResellerTier;
import com.japstransfort.service.TierService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Admin CRUD operations on Reseller Tiers (/admin/tiers).
 */
@Controller
@RequestMapping("/admin/tiers")
public class TierAdminController {

    private final TierService tierService;

    public TierAdminController(TierService tierService) {
        this.tierService = tierService;
    }

    @GetMapping
    public String listTiers(Model model) {
        model.addAttribute("tiers", tierService.findAll());
        if (!model.containsAttribute("tierFormDto")) {
            model.addAttribute("tierFormDto", new TierFormDto());
        }
        return "tiers/list";
    }

    @PostMapping("/create")
    public String createTier(@Valid @ModelAttribute("tierFormDto") TierFormDto tierFormDto,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tiers", tierService.findAll());
            return "tiers/list";
        }

        try {
            ResellerTier created = tierService.create(tierFormDto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Tier '" + created.getTierName() + "' created successfully.");
            return "redirect:/admin/tiers";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("tierName", "duplicate", ex.getMessage());
            model.addAttribute("tiers", tierService.findAll());
            return "tiers/list";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        ResellerTier tier = tierService.findById(id);
        TierFormDto dto = new TierFormDto(
                tier.getTierId(),
                tier.getTierName(),
                tier.getDiscountPct(),
                tier.getCommissionPct(),
                tier.getMinMonthlyPurchase()
        );
        model.addAttribute("tierFormDto", dto);
        return "tiers/form";
    }

    @PostMapping("/edit/{id}")
    public String updateTier(@PathVariable("id") Integer id,
                             @Valid @ModelAttribute("tierFormDto") TierFormDto tierFormDto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "tiers/form";
        }

        try {
            tierService.update(id, tierFormDto);
            redirectAttributes.addFlashAttribute("successMessage", "Tier updated successfully.");
            return "redirect:/admin/tiers";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("tierName", "duplicate", ex.getMessage());
            return "tiers/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteTier(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            tierService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Tier deleted successfully.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/tiers";
    }
}
