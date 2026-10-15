package com.japstransfort.controller;

import com.japstransfort.config.CustomUserDetails;
import com.japstransfort.dto.CartItem;
import com.japstransfort.dto.CartRow;
import com.japstransfort.dto.OrderDetailDto;
import com.japstransfort.entity.Commission;
import com.japstransfort.entity.Order;
import com.japstransfort.entity.Reseller;
import com.japstransfort.entity.ResellerTier;
import com.japstransfort.repository.CategoryRepository;
import com.japstransfort.repository.ProductRepository;
import com.japstransfort.repository.ResellerRepository;
import com.japstransfort.service.CommissionService;
import com.japstransfort.service.OrderService;
import com.japstransfort.service.ProductService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.japstransfort.entity.Product;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for the reseller portal: home, catalog, cart, orders.
 */
@Controller
@RequestMapping("/portal")
@PreAuthorize("hasRole('RESELLER')")
public class PortalController {

    private static final String CART_SESSION_KEY = "RESELLER_CART";

    private final ResellerRepository resellerRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final OrderService orderService;
    private final CommissionService commissionService;

    public PortalController(ResellerRepository resellerRepository,
                            CategoryRepository categoryRepository,
                            ProductRepository productRepository,
                            ProductService productService,
                            OrderService orderService,
                            CommissionService commissionService) {
        this.resellerRepository = resellerRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.orderService = orderService;
        this.commissionService = commissionService;
    }

    // ─── Cart helper ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> getCart(HttpSession session) {
        Map<Integer, Integer> cart = (Map<Integer, Integer>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new LinkedHashMap<>();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    private void saveCart(HttpSession session, Map<Integer, Integer> cart) {
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    private Reseller loadReseller(CustomUserDetails userDetails) {
        if (userDetails == null) return null;
        return resellerRepository.findByUser_UserId(userDetails.getUserId()).orElse(null);
    }

    private BigDecimal getDiscountPct(Reseller reseller) {
        if (reseller == null) return BigDecimal.ZERO;
        ResellerTier tier = reseller.getTier();
        if (tier == null || tier.getDiscountPct() == null) return BigDecimal.ZERO;
        return tier.getDiscountPct();
    }

    // ─── Portal Home ──────────────────────────────────────────────────────────

    @GetMapping({"", "/"})
    @Transactional(readOnly = true)
    public String portalHome(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("currentUser", userDetails.getUser());
        }
        Reseller reseller = loadReseller(userDetails);
        model.addAttribute("reseller", reseller);
        return "portal/home";
    }

    // ─── Catalog ──────────────────────────────────────────────────────────────

    @GetMapping("/catalog")
    @Transactional(readOnly = true)
    public String catalog(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", defaultValue = "name") String sort,
            @RequestParam(name = "dir", defaultValue = "asc") String dir,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session,
            Model model) {

        Reseller reseller = loadReseller(userDetails);
        BigDecimal discountPct = getDiscountPct(reseller);
        String tierName = (reseller != null && reseller.getTier() != null)
                ? reseller.getTier().getTierName() : "Unassigned";

        Page<Product> productsPage = productService.getProducts(search, categoryId, "active", page, 10, sort, dir);
        for (Product product : productsPage) {
            product.setYourPrice(product.calculateYourPrice(discountPct));
        }

        int cartSize = getCart(session).values().stream().mapToInt(Integer::intValue).sum();

        if (userDetails != null) model.addAttribute("currentUser", userDetails.getUser());
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
        model.addAttribute("cartSize", cartSize);

        return "portal/catalog";
    }

    // ─── Add to cart ──────────────────────────────────────────────────────────

    @PostMapping("/cart/add")
    public String addToCart(
            @RequestParam("productId") Integer productId,
            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (quantity < 1) quantity = 1;

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || !Boolean.TRUE.equals(product.getIsActive())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Product is not available.");
            return "redirect:/portal/catalog";
        }

        Map<Integer, Integer> cart = getCart(session);
        cart.merge(productId, quantity, Integer::sum);
        saveCart(session, cart);

        redirectAttributes.addFlashAttribute("successMessage",
                "\"" + product.getName() + "\" added to cart (qty " + quantity + ").");
        return "redirect:/portal/catalog";
    }

    // ─── Add selected to cart ─────────────────────────────────────────────────

    @PostMapping("/cart/add-selected")
    public String addSelectedToCart(
            @RequestParam(value = "selectedIds", required = false) List<String> selectedIds,
            @RequestParam(value = "productIds", required = false) List<String> productIds,
            @RequestParam(value = "quantities", required = false) List<String> quantities,
            @RequestParam Map<String, String> allParams,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", defaultValue = "name") String sort,
            @RequestParam(name = "dir", defaultValue = "asc") String dir,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        List<String> ids = (selectedIds != null && !selectedIds.isEmpty()) ? selectedIds : productIds;
        if (ids == null || ids.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "No items selected.");
            return redirectCatalog(search, categoryId, sort, dir, page);
        }

        int addedCount = 0;
        int skippedCount = 0;
        Map<Integer, Integer> cart = getCart(session);

        for (int i = 0; i < ids.size(); i++) {
            Integer productId = null;
            try {
                productId = Integer.parseInt(ids.get(i).trim());
            } catch (Exception e) {
                skippedCount++;
                continue;
            }

            Integer quantity = null;
            if (quantities != null && i < quantities.size()) {
                try {
                    quantity = Integer.parseInt(quantities.get(i).trim());
                } catch (Exception e) {
                    quantity = null;
                }
            } else if (allParams.containsKey("quantity_" + productId)) {
                try {
                    quantity = Integer.parseInt(allParams.get("quantity_" + productId).trim());
                } catch (Exception e) {
                    quantity = null;
                }
            }

            if (quantity == null || quantity < 1 || quantity > 9999) {
                skippedCount++;
                continue;
            }

            Product product = productRepository.findById(productId).orElse(null);
            if (product == null || !Boolean.TRUE.equals(product.getIsActive())) {
                skippedCount++;
                continue;
            }

            cart.merge(productId, quantity, Integer::sum);
            addedCount++;
        }

        saveCart(session, cart);

        if (addedCount > 0) {
            String itemPart = addedCount == 1 ? "1 item added to cart" : (addedCount + " items added to cart");
            String skipPart = skippedCount > 0 ? " (" + skippedCount + " skipped)" : "";
            redirectAttributes.addFlashAttribute("successMessage", itemPart + skipPart + ".");
        } else if (skippedCount > 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "No items added to cart (" + skippedCount + " skipped).");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "No items selected.");
        }

        return redirectCatalog(search, categoryId, sort, dir, page);
    }

    private String redirectCatalog(String search, Integer categoryId, String sort, String dir, int page) {
        org.springframework.web.util.UriComponentsBuilder builder =
                org.springframework.web.util.UriComponentsBuilder.fromPath("/portal/catalog");
        if (search != null && !search.trim().isEmpty()) {
            builder.queryParam("search", search);
        }
        if (categoryId != null) {
            builder.queryParam("categoryId", categoryId);
        }
        if (sort != null && !sort.trim().isEmpty()) {
            builder.queryParam("sort", sort);
        }
        if (dir != null && !dir.trim().isEmpty()) {
            builder.queryParam("dir", dir);
        }
        if (page > 0) {
            builder.queryParam("page", page);
        }
        return "redirect:" + builder.toUriString();
    }

    // ─── Cart view ────────────────────────────────────────────────────────────

    @GetMapping("/cart")
    @Transactional(readOnly = true)
    public String viewCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session,
            Model model) {

        Reseller reseller = loadReseller(userDetails);
        BigDecimal discountPct = getDiscountPct(reseller);

        Map<Integer, Integer> cart = getCart(session);
        List<CartRow> rows = orderService.buildCartRows(cart, discountPct);

        BigDecimal subtotal = rows.stream()
                .map(CartRow::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (userDetails != null) model.addAttribute("currentUser", userDetails.getUser());
        model.addAttribute("cartRows", rows);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("total", subtotal);
        model.addAttribute("reseller", reseller);
        model.addAttribute("cartSize", cart.values().stream().mapToInt(Integer::intValue).sum());

        return "portal/cart";
    }

    // ─── Update quantity ──────────────────────────────────────────────────────

    @PostMapping("/cart/update")
    public String updateCart(
            @RequestParam("productId") Integer productId,
            @RequestParam("quantity") int quantity,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Map<Integer, Integer> cart = getCart(session);
        if (quantity < 1) {
            cart.remove(productId);
            redirectAttributes.addFlashAttribute("successMessage", "Item removed from cart.");
        } else {
            cart.put(productId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Cart updated.");
        }
        saveCart(session, cart);
        return "redirect:/portal/cart";
    }

    // ─── Remove item ──────────────────────────────────────────────────────────

    @PostMapping("/cart/remove")
    public String removeFromCart(
            @RequestParam("productId") Integer productId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Map<Integer, Integer> cart = getCart(session);
        cart.remove(productId);
        saveCart(session, cart);
        redirectAttributes.addFlashAttribute("successMessage", "Item removed from cart.");
        return "redirect:/portal/cart";
    }

    // ─── Place order ──────────────────────────────────────────────────────────

    @PostMapping("/cart/place-order")
    public String placeOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "selectedIds", required = false) List<Integer> selectedIds,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Reseller reseller = loadReseller(userDetails);
        if (reseller == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Reseller profile not found.");
            return "redirect:/portal/cart";
        }

        if (selectedIds == null || selectedIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select at least one item to order.");
            return "redirect:/portal/cart";
        }

        BigDecimal discountPct = getDiscountPct(reseller);
        Map<Integer, Integer> cart = getCart(session);

        // Build sub-cart from session cart for selected IDs only (ignore any id not in cart)
        Set<Integer> selectedSet = new java.util.HashSet<>(selectedIds);
        Map<Integer, Integer> subCart = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            if (selectedSet.contains(entry.getKey())) {
                subCart.put(entry.getKey(), entry.getValue());
            }
        }

        try {
            Order order = orderService.placeOrder(reseller, subCart, discountPct, paymentMethod);
            // Remove only the ordered items from the session cart; leave unchecked items
            for (Integer pid : subCart.keySet()) {
                cart.remove(pid);
            }
            saveCart(session, cart);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order placed successfully! Order number: " + order.getOrderNo());
            return "redirect:/portal/orders";
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/portal/cart";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    e.getMessage() != null ? e.getMessage() : "Failed to place order.");
            return "redirect:/portal/cart";
        }
    }

    // ─── My Orders ───────────────────────────────────────────────────────────

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public String myOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Reseller reseller = loadReseller(userDetails);
        List<Order> orders = reseller != null
                ? orderService.getMyOrders(reseller.getResellerId())
                : List.of();

        if (userDetails != null) model.addAttribute("currentUser", userDetails.getUser());
        model.addAttribute("orders", orders);
        model.addAttribute("reseller", reseller);

        return "portal/orders";
    }

    // ─── Order Detail ─────────────────────────────────────────────────────────

    @GetMapping("/orders/{id}")
    public String orderDetail(
            @PathVariable("id") Integer orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            HttpServletResponse response) {

        Reseller reseller = loadReseller(userDetails);
        if (reseller == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "portal/order-not-found";
        }

        java.util.Optional<OrderDetailDto> opt =
                orderService.getOrderDetailForReseller(orderId, reseller.getResellerId());

        if (opt.isEmpty()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "portal/order-not-found";
        }

        if (userDetails != null) model.addAttribute("currentUser", userDetails.getUser());
        model.addAttribute("order", opt.get());
        model.addAttribute("reseller", reseller);
        return "portal/order-detail";
    }

    // ─── Cancel Order ─────────────────────────────────────────────────────────

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(
            @PathVariable("id") Integer orderId,
            @RequestParam(value = "cancelReason", required = false) String cancelReason,
            @RequestParam(value = "fromDetail", required = false, defaultValue = "false") boolean fromDetail,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Reseller reseller = loadReseller(userDetails);
        if (reseller == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Reseller profile not found.");
            return "redirect:/portal/orders";
        }

        try {
            Order order = orderService.cancelOrderByReseller(orderId, reseller.getResellerId(), cancelReason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order #" + order.getOrderNo() + " has been cancelled.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        if (fromDetail) {
            return "redirect:/portal/orders/" + orderId;
        }
        return "redirect:/portal/orders";
    }

    // ─── My Commissions ──────────────────────────────────────────────────────

    @GetMapping("/commissions")
    public String myCommissions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Reseller reseller = loadReseller(userDetails);
        if (reseller == null) {
            model.addAttribute("commissions", List.of());
            model.addAttribute("totalPending", BigDecimal.ZERO);
            model.addAttribute("totalPaid", BigDecimal.ZERO);
            return "portal/commissions";
        }

        List<Commission> commissions = commissionService.getCommissionsForReseller(reseller.getResellerId());
        BigDecimal totalPending = commissionService.getPendingTotalForReseller(reseller.getResellerId());
        BigDecimal totalPaid = commissionService.getPaidTotalForReseller(reseller.getResellerId());

        if (userDetails != null) {
            model.addAttribute("currentUser", userDetails.getUser());
        }
        model.addAttribute("commissions", commissions);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("reseller", reseller);

        return "portal/commissions";
    }
}

