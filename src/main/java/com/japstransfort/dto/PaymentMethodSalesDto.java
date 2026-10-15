package com.japstransfort.dto;

import com.japstransfort.entity.PaymentMethod;
import java.math.BigDecimal;

public record PaymentMethodSalesDto(
        PaymentMethod paymentMethod,
        long orderCount,
        BigDecimal total
) {
    public String getDisplayName() {
        if (paymentMethod == null) return "Unknown";
        return switch (paymentMethod) {
            case COD -> "Cash on delivery";
            case ONLINE -> "Online";
            case CREDIT -> "On account";
        };
    }
}
