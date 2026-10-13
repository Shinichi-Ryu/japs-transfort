package com.japstransfort.service;

import com.japstransfort.config.CustomUserDetails;
import com.japstransfort.entity.User;
import com.japstransfort.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to load user records from PostgreSQL database for Spring Security authentication.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UsernameNotFoundException("Account is inactive for email: " + email);
        }

        return new CustomUserDetails(user);
    }
}
