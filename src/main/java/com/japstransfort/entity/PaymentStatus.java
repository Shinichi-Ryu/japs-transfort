package com.japstransfort.entity;

/**
 * Matches the PostgreSQL payment_status ENUM type.
 */
public enum PaymentStatus {
    unpaid,
    partial,
    paid,
    refunded
}
