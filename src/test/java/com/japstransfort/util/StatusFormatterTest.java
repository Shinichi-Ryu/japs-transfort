package com.japstransfort.util;

import com.japstransfort.entity.OrderStatus;
import com.japstransfort.entity.PaymentMethod;
import com.japstransfort.entity.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatusFormatterTest {

    @Test
    @DisplayName("Null and empty inputs return empty string safely")
    void testNullAndEmptyInputs() {
        assertEquals("", StatusFormatter.format(null));
        assertEquals("", StatusFormatter.format(""));
        assertEquals("", StatusFormatter.format("   "));
        assertEquals("On Account", StatusFormatter.formatPaymentMethod(null));
    }

    @ParameterizedTest
    @CsvSource({
        "pending, Pending",
        "approved, Approved",
        "packed, Packed",
        "shipped, Shipped",
        "delivered, Delivered",
        "cancelled, Cancelled",
        "returned, Returned"
    })
    @DisplayName("Formats OrderStatus strings to Title Case")
    void testOrderStatusStrings(String input, String expected) {
        assertEquals(expected, StatusFormatter.format(input));
    }

    @Test
    @DisplayName("Formats OrderStatus enum instances directly")
    void testOrderStatusEnums() {
        assertEquals("Pending", StatusFormatter.format(OrderStatus.pending));
        assertEquals("Approved", StatusFormatter.format(OrderStatus.approved));
        assertEquals("Packed", StatusFormatter.format(OrderStatus.packed));
        assertEquals("Shipped", StatusFormatter.format(OrderStatus.shipped));
        assertEquals("Delivered", StatusFormatter.format(OrderStatus.delivered));
        assertEquals("Cancelled", StatusFormatter.format(OrderStatus.cancelled));
        assertEquals("Returned", StatusFormatter.format(OrderStatus.returned));
    }

    @ParameterizedTest
    @CsvSource({
        "unpaid, Unpaid",
        "partial, Partial",
        "paid, Paid",
        "refunded, Refunded"
    })
    @DisplayName("Formats PaymentStatus strings to Title Case")
    void testPaymentStatusStrings(String input, String expected) {
        assertEquals(expected, StatusFormatter.format(input));
    }

    @Test
    @DisplayName("Formats PaymentStatus enum instances directly")
    void testPaymentStatusEnums() {
        assertEquals("Unpaid", StatusFormatter.format(PaymentStatus.unpaid));
        assertEquals("Partial", StatusFormatter.format(PaymentStatus.partial));
        assertEquals("Paid", StatusFormatter.format(PaymentStatus.paid));
        assertEquals("Refunded", StatusFormatter.format(PaymentStatus.refunded));
    }

    @Test
    @DisplayName("Formats PaymentMethod enum instances and codes to Title Case")
    void testPaymentMethods() {
        assertEquals("Cash On Delivery", StatusFormatter.format(PaymentMethod.COD));
        assertEquals("Online", StatusFormatter.format(PaymentMethod.ONLINE));
        assertEquals("On Account", StatusFormatter.format(PaymentMethod.CREDIT));

        assertEquals("Cash On Delivery", StatusFormatter.format("COD"));
        assertEquals("Cash On Delivery", StatusFormatter.format("cod"));
        assertEquals("Online", StatusFormatter.format("ONLINE"));
        assertEquals("Online", StatusFormatter.format("online"));
        assertEquals("On Account", StatusFormatter.format("CREDIT"));
        assertEquals("On Account", StatusFormatter.format("credit"));
    }

    @ParameterizedTest
    @CsvSource({
        "bank_transfer, Bank Transfer",
        "cash_on_delivery, Cash On Delivery",
        "on_account, On Account",
        "already_shipped, Already Shipped",
        "over-the-counter, Over The Counter",
        "direct-debit, Direct Debit"
    })
    @DisplayName("Handles underscores and hyphens in compound words")
    void testUnderscoresAndHyphens(String input, String expected) {
        assertEquals(expected, StatusFormatter.format(input));
    }
}
