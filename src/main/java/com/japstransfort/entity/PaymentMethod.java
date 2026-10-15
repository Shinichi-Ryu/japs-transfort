package com.japstransfort.entity;

/**
 * Payment method for orders:
 * COD: Cash on delivery
 * ONLINE: Online payment (GCash / bank transfer)
 * CREDIT: On account (credit)
 */
public enum PaymentMethod {
    COD("Cash on delivery"),
    ONLINE("Online payment"),
    CREDIT("On account");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
