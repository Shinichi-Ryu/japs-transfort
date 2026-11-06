package com.japstransfort.util;

import com.japstransfort.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Shared formatter to normalize status, payment status, and payment method labels
 * to Title Case across all UI views and tables without mutating entity/database values.
 */
@Component("statusFormatter")
public class StatusFormatter {

    /**
     * Converts any status, payment status, or payment method string/enum into Title Case.
     * Examples:
     *   "pending" -> "Pending"
     *   "approved" -> "Approved"
     *   "packed" -> "Packed"
     *   "shipped" -> "Shipped"
     *   "delivered" -> "Delivered"
     *   "cancelled" -> "Cancelled"
     *   "unpaid" -> "Unpaid"
     *   "partial" -> "Partial"
     *   "paid" -> "Paid"
     *   "COD" -> "Cash On Delivery"
     *   "CREDIT" -> "On Account"
     *   "ONLINE" -> "Online"
     *   "bank_transfer" -> "Bank Transfer"
     *   "cash-on-delivery" -> "Cash On Delivery"
     */
    public static String format(Object input) {
        if (input == null) {
            return "";
        }
        if (input instanceof PaymentMethod pm) {
            return formatPaymentMethod(pm);
        }

        String raw = input.toString().trim();
        if (raw.isEmpty()) {
            return "";
        }

        if ("COD".equalsIgnoreCase(raw)) {
            return "Cash On Delivery";
        }
        if ("CREDIT".equalsIgnoreCase(raw)) {
            return "On Account";
        }
        if ("ONLINE".equalsIgnoreCase(raw)) {
            return "Online";
        }

        // Replace underscores and hyphens with single spaces
        String replaced = raw.replace('_', ' ').replace('-', ' ');
        String[] words = replaced.split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                sb.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }

        return sb.toString();
    }

    public static String formatPaymentMethod(PaymentMethod method) {
        if (method == null) {
            return "On Account";
        }
        return switch (method) {
            case COD -> "Cash On Delivery";
            case CREDIT -> "On Account";
            case ONLINE -> "Online";
        };
    }
}
