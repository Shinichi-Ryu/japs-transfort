package com.japstransfort.config;

import com.japstransfort.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Spring Security configuration for role-based access control and BCrypt authentication.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setPreAuthenticationChecks(userDetails -> {
            if (!userDetails.isAccountNonLocked()) {
                throw new LockedException("User account is locked");
            }
            if (!userDetails.isEnabled()) {
                if (userDetails instanceof CustomUserDetails custom) {
                    String status = custom.getResellerStatus();
                    if ("pending".equalsIgnoreCase(status)) {
                        throw new DisabledException("PENDING");
                    } else if ("rejected".equalsIgnoreCase(status)) {
                        throw new DisabledException("REJECTED");
                    }
                }
                throw new DisabledException("INACTIVE");
            }
            if (!userDetails.isAccountNonExpired()) {
                throw new AccountExpiredException("User account has expired");
            }
        });
        return provider;
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isReseller = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_RESELLER".equals(a.getAuthority()));
            if (isReseller) {
                response.sendRedirect(request.getContextPath() + "/portal");
            } else {
                response.sendRedirect(request.getContextPath() + "/dashboard");
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            request.getSession().setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
            if (exception instanceof DisabledException) {
                String msg = exception.getMessage();
                if ("PENDING".equalsIgnoreCase(msg)) {
                    response.sendRedirect(request.getContextPath() + "/login?error=pending");
                    return;
                } else if ("REJECTED".equalsIgnoreCase(msg)) {
                    response.sendRedirect(request.getContextPath() + "/login?error=rejected");
                    return;
                } else {
                    response.sendRedirect(request.getContextPath() + "/login?error=disabled");
                    return;
                }
            }
            response.sendRedirect(request.getContextPath() + "/login?error=true");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   DaoAuthenticationProvider authenticationProvider) throws Exception {
        http
            .authenticationProvider(authenticationProvider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .requestMatchers("/portal", "/portal/**").hasRole("RESELLER")
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                .requestMatchers("/dashboard", "/dashboard/**").hasAnyRole("ADMIN", "STAFF")
                .requestMatchers("/account", "/account/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(roleBasedAuthenticationSuccessHandler())
                .failureHandler(authenticationFailureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}
