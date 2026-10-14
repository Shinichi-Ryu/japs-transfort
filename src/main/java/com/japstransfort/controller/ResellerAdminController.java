package com.japstransfort.controller;

import com.japstransfort.dto.ResellerFormDto;
import com.japstransfort.entity.Reseller;
import com.japstransfort.service.ResellerService;
import com.japstransfort.service.TierService;
import com.japstransfort.validator.PasswordValidator;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for Admin CRUD operations on Resellers (/admin/resellers).
 */
@Controller
@RequestMapping("/admin/resellers")
@PreAuthorize("hasRole('ADMIN')")
public class ResellerAdminController {

    private final ResellerService resellerService;
    private final TierService tierService;

    public ResellerAdminController(ResellerService resellerService, TierService tierService) {
        this.resellerService = resellerService;
        this.tierService = tierService;
    }

    @GetMapping
    public String listResellers(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "tierId", required = false) Integer tierId,
            @RequestParam(name = "status", required = false, defaultValue = "all") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", defaultValue = "businessName") String sort,
            @RequestParam(name = "dir", defaultValue = "asc") String dir,
            Model model) {

        Page<Reseller> resellersPage = resellerService.getResellers(search, tierId, status, page, 10, sort, dir);

        model.addAttribute("resellersPage", resellersPage);
        model.addAttribute("tiers", tierService.findAll());
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("tierId", tierId);
        model.addAttribute("status", status != null ? status : "all");
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("reverseDir", "asc".equalsIgnoreCase(dir) ? "desc" : "asc");

        return "resellers/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("resellerDto")) {
            model.addAttribute("resellerDto", new ResellerFormDto());
        }
        model.addAttribute("tiers", tierService.findAll());
        model.addAttribute("parentResellers", resellerService.findAllActive(null));
        return "resellers/form";
    }

    @PostMapping("/create")
    public String createReseller(@Valid @ModelAttribute("resellerDto") ResellerFormDto dto,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
            bindingResult.rejectValue("password", "required", "Password is required for the new login user.");
        } else {
            List<String> passwordErrors = PasswordValidator.validate(dto.getPassword());
            for (String error : passwordErrors) {
                bindingResult.rejectValue("password", "invalid", error);
            }
        }

        if (dto.getConfirmPassword() == null || !dto.getConfirmPassword().equals(dto.getPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("tiers", tierService.findAll());
            model.addAttribute("parentResellers", resellerService.findAllActive(null));
            return "resellers/form";
        }

        try {
            Reseller created = resellerService.create(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reseller '" + created.getBusinessName() + "' created successfully.");
            return "redirect:/admin/resellers";
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("email")) {
                bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            } else if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("match")) {
                bindingResult.rejectValue("confirmPassword", "mismatch", ex.getMessage());
            } else if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("password")) {
                bindingResult.rejectValue("password", "invalid", ex.getMessage());
            } else {
                bindingResult.reject("error", ex.getMessage());
            }
            model.addAttribute("tiers", tierService.findAll());
            model.addAttribute("parentResellers", resellerService.findAllActive(null));
            return "resellers/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        Reseller reseller = resellerService.findById(id);

        ResellerFormDto dto = new ResellerFormDto();
        dto.setResellerId(reseller.getResellerId());
        dto.setBusinessName(reseller.getBusinessName());
        dto.setContactNo(reseller.getContactNo());
        dto.setAddress(reseller.getAddress());
        dto.setTerritory(reseller.getTerritory());
        dto.setTierId(reseller.getTier() != null ? reseller.getTier().getTierId() : null);
        dto.setParentResellerId(reseller.getParentReseller() != null ? reseller.getParentReseller().getResellerId() : null);
        dto.setCreditLimit(reseller.getCreditLimit());
        dto.setStatus(reseller.getStatus());

        if (reseller.getUser() != null) {
            dto.setFullName(reseller.getUser().getFullName());
            dto.setEmail(reseller.getUser().getEmail());
        }

        model.addAttribute("resellerDto", dto);
        model.addAttribute("tiers", tierService.findAll());
        model.addAttribute("parentResellers", resellerService.findAllActive(id));
        return "resellers/form";
    }

    @PostMapping("/edit/{id}")
    public String updateReseller(@PathVariable("id") Integer id,
                                 @Valid @ModelAttribute("resellerDto") ResellerFormDto dto,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        boolean hasPassword = dto.getPassword() != null && !dto.getPassword().isEmpty();
        boolean hasConfirm = dto.getConfirmPassword() != null && !dto.getConfirmPassword().isEmpty();

        if (hasPassword || hasConfirm) {
            if (!hasPassword) {
                bindingResult.rejectValue("password", "required", "Password is required.");
            } else {
                List<String> passwordErrors = PasswordValidator.validate(dto.getPassword());
                for (String error : passwordErrors) {
                    bindingResult.rejectValue("password", "invalid", error);
                }
            }

            if (dto.getConfirmPassword() == null || !dto.getConfirmPassword().equals(dto.getPassword())) {
                bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("tiers", tierService.findAll());
            model.addAttribute("parentResellers", resellerService.findAllActive(id));
            return "resellers/form";
        }

        try {
            Reseller updated = resellerService.update(id, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reseller '" + updated.getBusinessName() + "' updated successfully.");
            return "redirect:/admin/resellers";
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("email")) {
                bindingResult.rejectValue("email", "duplicate", ex.getMessage());
            } else if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("match")) {
                bindingResult.rejectValue("confirmPassword", "mismatch", ex.getMessage());
            } else if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("password")) {
                bindingResult.rejectValue("password", "invalid", ex.getMessage());
            } else {
                bindingResult.reject("error", ex.getMessage());
            }
            model.addAttribute("tiers", tierService.findAll());
            model.addAttribute("parentResellers", resellerService.findAllActive(id));
            return "resellers/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String softDeleteReseller(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            resellerService.softDelete(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reseller deactivated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/resellers";
    }

    @PostMapping("/restore/{id}")
    public String restoreReseller(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            resellerService.restore(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reseller restored to active status.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/resellers";
    }

    @PostMapping("/approve/{id}")
    public String approveReseller(@PathVariable("id") Integer id,
                                  @RequestParam("tierId") Integer tierId,
                                  @RequestParam("creditLimit") BigDecimal creditLimit,
                                  RedirectAttributes redirectAttributes) {
        try {
            resellerService.approve(id, tierId, creditLimit);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reseller application approved successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/resellers";
    }

    @PostMapping("/reject/{id}")
    public String rejectReseller(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            resellerService.reject(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reseller application rejected.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/resellers";
    }
}
