package com.japstransfort.controller;

import com.japstransfort.config.CustomUserDetails;
import com.japstransfort.entity.Reseller;
import com.japstransfort.repository.CategoryRepository;
import com.japstransfort.repository.ProductRepository;
import com.japstransfort.repository.ResellerRepository;
import com.japstransfort.repository.ResellerTierRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller handling authentication views, dashboard, and reseller portal shell.
 */
@Controller
public class AuthController {

    private final ProductRepository productRepository;
    private final ResellerRepository resellerRepository;
    private final ResellerTierRepository tierRepository;
    private final CategoryRepository categoryRepository;

    public AuthController(ProductRepository productRepository,
                          ResellerRepository resellerRepository,
                          ResellerTierRepository tierRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.resellerRepository = resellerRepository;
        this.tierRepository = tierRepository;
        this.categoryRepository = categoryRepository;
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
            boolean isDisabled = "disabled".equalsIgnoreCase(error)
                    || lastException instanceof DisabledException
                    || (lastException instanceof Throwable && ((Throwable) lastException).getCause() instanceof DisabledException);
            if (isDisabled) {
                model.addAttribute("errorMessage", "Your account is inactive. Please contact the administrator.");
            } else {
                model.addAttribute("errorMessage", "Invalid email or password.");
            }
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }

        return "login";
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("currentUser", userDetails.getUser());
            model.addAttribute("userRole", userDetails.getRole().name());
        }

        // Placeholder summary stats for Phase 1
        model.addAttribute("totalProducts", productRepository.count());
        model.addAttribute("totalResellers", resellerRepository.count());
        model.addAttribute("totalTiers", tierRepository.count());
        model.addAttribute("totalCategories", categoryRepository.count());

        return "dashboard";
    }

    @GetMapping("/portal")
    @PreAuthorize("hasRole('RESELLER')")
    @Transactional(readOnly = true)
    public String portal(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            Reseller reseller = resellerRepository.findByUser_UserId(userDetails.getUserId())
                    .orElse(null);
            model.addAttribute("currentUser", userDetails.getUser());
            model.addAttribute("reseller", reseller);
        }
        return "portal/home";
    }

    @GetMapping("/portal/catalog")
    @PreAuthorize("hasRole('RESELLER')")
    public String portalCatalog(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("currentUser", userDetails.getUser());
        }
        return "portal/catalog";
    }

    @GetMapping("/portal/orders")
    @PreAuthorize("hasRole('RESELLER')")
    public String portalOrders(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("currentUser", userDetails.getUser());
        }
        return "portal/orders";
    }
}

