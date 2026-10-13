package com.japstransfort.controller;

import com.japstransfort.config.CustomUserDetails;
import com.japstransfort.repository.CategoryRepository;
import com.japstransfort.repository.ProductRepository;
import com.japstransfort.repository.ResellerRepository;
import com.japstransfort.repository.ResellerTierRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller handling authentication views and the primary dashboard.
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
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }

        return "login";
    }

    @GetMapping("/dashboard")
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
}
