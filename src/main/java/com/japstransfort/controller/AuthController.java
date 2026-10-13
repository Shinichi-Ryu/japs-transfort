package com.japstransfort.controller;

import com.japstransfort.config.CustomUserDetails;
import com.japstransfort.entity.Reseller;
import com.japstransfort.repository.CategoryRepository;
import com.japstransfort.repository.ProductRepository;
import com.japstransfort.repository.ResellerRepository;
import com.japstransfort.repository.ResellerTierRepository;
import com.japstransfort.service.ProductService;
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
 * Controller handling authentication views, dashboard, and reseller portal home.
 * Catalog, cart, and orders are handled by PortalController.
 */
@Controller
public class AuthController {

    private final ProductRepository productRepository;
    private final ResellerRepository resellerRepository;
    private final ResellerTierRepository tierRepository;
    private final CategoryRepository categoryRepository;
    private final ProductService productService;

    public AuthController(ProductRepository productRepository,
                          ResellerRepository resellerRepository,
                          ResellerTierRepository tierRepository,
                          CategoryRepository categoryRepository,
                          ProductService productService) {
        this.productRepository = productRepository;
        this.resellerRepository = resellerRepository;
        this.tierRepository = tierRepository;
        this.categoryRepository = categoryRepository;
        this.productService = productService;
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
}

    @GetMapping("/portal/catalog")
    @PreAuthorize("hasRole('RESELLER')")
    @Transactional(readOnly = true)
    public String portalCatalog(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", defaultValue = "name") String sort,
            @RequestParam(name = "dir", defaultValue = "asc") String dir,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Reseller reseller = null;
        ResellerTier tier = null;
        BigDecimal discountPct = BigDecimal.ZERO;
        String tierName = "Unassigned";

        if (userDetails != null) {
            model.addAttribute("currentUser", userDetails.getUser());
            reseller = resellerRepository.findByUser_UserId(userDetails.getUserId()).orElse(null);
            if (reseller != null) {
                tier = reseller.getTier();
                if (tier != null) {
                    tierName = tier.getTierName();
                    if (tier.getDiscountPct() != null) {
                        discountPct = tier.getDiscountPct();
                    }
                }
            }
        }

        Page<Product> productsPage = productService.getProducts(search, categoryId, "active", page, 10, sort, dir);
        for (Product product : productsPage) {
            product.setYourPrice(product.calculateYourPrice(discountPct));
        }

        model.addAttribute("productsPage", productsPage);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("reseller", reseller);
        model.addAttribute("tierName", tierName);
        model.addAttribute("discountPct", discountPct);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("reverseDir", "asc".equalsIgnoreCase(dir) ? "desc" : "asc");

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

