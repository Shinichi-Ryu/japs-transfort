package com.japstransfort.entity;

/**
 * Enumeration matching the PostgreSQL user_role ENUM type:
 * ('admin', 'staff', 'warehouse', 'reseller').
 */
public enum UserRole {
    admin,
    staff,
    warehouse,
    reseller;

    /**
     * Returns the Spring Security role name format with ROLE_ prefix.
     */
    public String getAuthority() {
        return "ROLE_" + name().toUpperCase();
    }
}
