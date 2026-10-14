package com.japstransfort.config;

import com.japstransfort.entity.User;
import com.japstransfort.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom UserDetails implementation wrapping the User entity.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final boolean enabled;
    private final String resellerStatus;

    public CustomUserDetails(User user) {
        this(user, Boolean.TRUE.equals(user.getIsActive()), null);
    }

    public CustomUserDetails(User user, boolean enabled) {
        this(user, enabled, null);
    }

    public CustomUserDetails(User user, boolean enabled, String resellerStatus) {
        this.user = user;
        this.enabled = enabled;
        this.resellerStatus = resellerStatus;
    }

    public String getResellerStatus() {
        return resellerStatus;
    }

    public User getUser() {
        return user;
    }

    public Integer getUserId() {
        return user.getUserId();
    }

    public String getFullName() {
        return user.getFullName();
    }

    public UserRole getRole() {
        return user.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // e.g. "ROLE_ADMIN", "ROLE_STAFF", "ROLE_RESELLER", etc.
        return Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getAuthority()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
