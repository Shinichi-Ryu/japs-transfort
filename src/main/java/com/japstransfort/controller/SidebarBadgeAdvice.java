package com.japstransfort.controller;

import com.japstransfort.config.CustomUserDetails;
import com.japstransfort.entity.Reseller;
import com.japstransfort.repository.ResellerRepository;
import com.japstransfort.repository.UserRepository;
import com.japstransfort.service.OrderMessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller advice that exposes unread order message counts to the shell layout
 * for authenticated ADMIN and RESELLER users.
 */
@ControllerAdvice
public class SidebarBadgeAdvice {

    private final OrderMessageService orderMessageService;
    private final ResellerRepository resellerRepository;
    private final UserRepository userRepository;

    public SidebarBadgeAdvice(OrderMessageService orderMessageService,
                              ResellerRepository resellerRepository,
                              UserRepository userRepository) {
        this.orderMessageService = orderMessageService;
        this.resellerRepository = resellerRepository;
        this.userRepository = userRepository;
    }

    @ModelAttribute("unreadMessageCount")
    public Long unreadMessageCount(Authentication authentication, HttpServletRequest request) {
        if (isExcludedPath(request)) {
            return null;
        }

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        if (authentication.getAuthorities() == null) {
            return null;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));
        boolean isReseller = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_RESELLER".equals(a) || "RESELLER".equals(a));

        if (!isAdmin && !isReseller) {
            return null;
        }

        try {
            if (isAdmin) {
                return orderMessageService.countUnreadForAdmin();
            } else {
                Integer resellerId = resolveResellerId(authentication);
                if (resellerId == null) {
                    return 0L;
                }
                return orderMessageService.countUnreadForReseller(resellerId);
            }
        } catch (Exception e) {
            // Failure results in 0 and never breaks a page
            return 0L;
        }
    }

    private boolean isExcludedPath(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        return isExcluded(request.getServletPath()) || isExcluded(request.getRequestURI());
    }

    private boolean isExcluded(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return path.startsWith("/login")
                || path.startsWith("/register")
                || path.startsWith("/error")
                || path.startsWith("/css")
                || path.startsWith("/js")
                || path.startsWith("/img")
                || path.startsWith("/favicon");
    }

    private Integer resolveResellerId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            Integer userId = cud.getUserId();
            if (userId != null) {
                return resellerRepository.findByUser_UserId(userId)
                        .map(Reseller::getResellerId)
                        .orElse(null);
            }
        }
        String username = authentication.getName();
        if (username != null && userRepository != null) {
            return userRepository.findByEmail(username)
                    .flatMap(u -> resellerRepository.findByUser_UserId(u.getUserId()))
                    .map(Reseller::getResellerId)
                    .orElse(null);
        }
        return null;
    }
}
