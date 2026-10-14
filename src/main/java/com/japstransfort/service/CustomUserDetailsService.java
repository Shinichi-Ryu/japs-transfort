package com.japstransfort.service;

import com.japstransfort.config.CustomUserDetails;
import com.japstransfort.entity.Reseller;
import com.japstransfort.entity.User;
import com.japstransfort.entity.UserRole;
import com.japstransfort.repository.ResellerRepository;
import com.japstransfort.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service to load user records from PostgreSQL database for Spring Security authentication.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final ResellerRepository resellerRepository;

    public CustomUserDetailsService(UserRepository userRepository, ResellerRepository resellerRepository) {
        this.userRepository = userRepository;
        this.resellerRepository = resellerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        boolean enabled = Boolean.TRUE.equals(user.getIsActive());
        String resellerStatus = null;
        if (user.getRole() == UserRole.reseller) {
            Optional<Reseller> resellerOpt = resellerRepository.findByUser_UserId(user.getUserId());
            if (resellerOpt.isPresent()) {
                resellerStatus = resellerOpt.get().getStatus();
            }
            if (resellerOpt.isEmpty() || !"active".equalsIgnoreCase(resellerStatus)) {
                enabled = false;
            }
        }

        return new CustomUserDetails(user, enabled, resellerStatus);
    }
}
