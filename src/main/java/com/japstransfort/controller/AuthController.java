package com.japstransfort.controller;

import com.japstransfort.config.CustomUserDetails;
import com.japstransfort.dto.ResellerRegistrationDto;
import com.japstransfort.repository.CategoryRepository;
import com.japstransfort.repository.ProductRepository;
import com.japstransfort.repository.ResellerRepository;
import com.japstransfort.repository.ResellerTierRepository;
import com.japstransfort.repository.UserRepository;
import com.japstransfort.service.ResellerService;
import com.japstransfort.validator.PasswordValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller handling authentication views, public registration, and admin/staff dashboard.
 * Reseller portal routes are handled by PortalController.
 */
@Controller
public class AuthController {

    private final ProductRepository productRepository;
    private final ResellerRepository resellerRepository;
    private final ResellerTierRepository tierRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ResellerService resellerService;

    public AuthController(ProductRepository productRepository,
                          ResellerRepository resellerRepository,
                          ResellerTierRepository tierRepository,
                          CategoryRepository categoryRepository,
                          UserRepository userRepository,
                          ResellerService resellerService) {
        this.productRepository = productRepository;
        this.resellerRepository = resellerRepository;
        this.tierRepository = tierRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.resellerService = resellerService;
    }

    @GetMapping("/")
    public String root(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_RESELLER".equals(a.getAuthority()))) {
            return "redirect:/portal";
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registered", required = false) String registered,
                            HttpServletRequest request,
                            Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_RESELLER".equals(a.getAuthority()))) {
                return "redirect:/portal";
            }
            return "redirect:/dashboard";
        }

        if (error != null) {
            HttpSession session = request.getSession(false);
            Object lastException = session != null ? session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) : null;
            String disabledType = null;
            if (lastException instanceof DisabledException disabledEx) {
                disabledType = disabledEx.getMessage();
            }

            if ("pending".equalsIgnoreCase(error) || "PENDING".equalsIgnoreCase(disabledType)) {
                model.addAttribute("errorMessage", "Your account is awaiting administrator approval.");
            } else if ("rejected".equalsIgnoreCase(error) || "REJECTED".equalsIgnoreCase(disabledType)) {
                model.addAttribute("errorMessage", "Your application was not approved. Please contact the administrator.");
            } else if ("disabled".equalsIgnoreCase(error)
                    || lastException instanceof DisabledException
                    || (lastException instanceof Throwable && ((Throwable) lastException).getCause() instanceof DisabledException)) {
                model.addAttribute("errorMessage", "Your account is inactive. Please contact the administrator.");
            } else {
                model.addAttribute("errorMessage", "Invalid email or password.");
            }
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        if (registered != null && !model.containsAttribute("successMessage")) {
            model.addAttribute("successMessage", "Application received. You can log in once the administrator approves your account.");
        }

        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_RESELLER".equals(a.getAuthority()))) {
                return "redirect:/portal";
            }
            return "redirect:/dashboard";
        }
        if (!model.containsAttribute("registrationDto")) {
            model.addAttribute("registrationDto", new ResellerRegistrationDto());
        }
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registrationDto") ResellerRegistrationDto dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            List<String> passwordErrors = PasswordValidator.validate(dto.getPassword());
            for (String err : passwordErrors) {
                bindingResult.rejectValue("password", "invalid", err);
            }
        }

        if (dto.getConfirmPassword() != null && dto.getPassword() != null
                && !dto.getConfirmPassword().equals(dto.getPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match.");
        }

        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            if (userRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
                bindingResult.rejectValue("email", "duplicate", "This email address is already registered.");
            }
        }

        if (bindingResult.hasErrors()) {
            dto.setPassword(null);
            dto.setConfirmPassword(null);
            return "register";
        }

        try {
            resellerService.register(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Application received. You can log in once the administrator approves your account.");
            return "redirect:/login?registered=true";
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
            dto.setPassword(null);
            dto.setConfirmPassword(null);
            return "register";
        }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("currentUser", userDetails.getUser());
            model.addAttribute("userRole", userDetails.getRole().name());
        }

        model.addAttribute("totalProducts", productRepository.count());
        model.addAttribute("totalResellers", resellerRepository.count());
        model.addAttribute("totalTiers", tierRepository.count());
        model.addAttribute("totalCategories", categoryRepository.count());
        model.addAttribute("pendingApplications", resellerRepository.countByStatus("pending"));

        return "dashboard";
    }
}
