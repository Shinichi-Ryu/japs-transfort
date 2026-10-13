package com.japstransfort.entity;

/**
 * Matches the PostgreSQL order_status ENUM type.
 */
public enum OrderStatus {
    pending,
    approved,
    packed,
    shipped,
    delivered,
    cancelled,
    returned
}
