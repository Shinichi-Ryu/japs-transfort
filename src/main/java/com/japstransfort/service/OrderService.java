package com.japstransfort.service;

import com.japstransfort.dto.CartItem;
import com.japstransfort.dto.CartRow;
import com.japstransfort.entity.*;
import com.japstransfort.repository.OrderRepository;
import com.japstransfort.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for reseller cart and order placement.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    /**
     * Builds CartRow list from session cart items.
     * Skips inactive products. Prices computed server-side.
     */
    @Transactional(readOnly = true)
    public List<CartRow> buildCartRows(Map<Integer, Integer> cart, BigDecimal discountPct) {
        List<CartRow> rows = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            productRepository.findById(entry.getKey()).ifPresent(product -> {
                if (Boolean.TRUE.equals(product.getIsActive())) {
                    BigDecimal unitPrice = product.calculateYourPrice(discountPct);
                    rows.add(new CartRow(product, entry.getValue(), unitPrice));
                }
            });
        }
        return rows;
    }

    /**
     * Places an order for the given reseller from the current cart.
     * Validates credit limit, creates Order and OrderItems in one transaction.
     * Returns the placed Order.
     * Throws IllegalStateException with a user-friendly message on validation failure.
     */
    @Transactional
    public Order placeOrder(Reseller reseller, Map<Integer, Integer> cart, BigDecimal discountPct) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalStateException("Your cart is empty. Please add items before placing an order.");
        }

        // Build rows to compute total (server-side prices, skip inactive)
        List<CartRow> rows = buildCartRows(cart, discountPct);
        if (rows.isEmpty()) {
            throw new IllegalStateException("Your cart contains no active products.");
        }

        BigDecimal subtotal = rows.stream()
                .map(CartRow::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountAmount = rows.stream()
                .map(r -> r.getProduct().getSrp()
                        .multiply(BigDecimal.valueOf(r.getQuantity()))
                        .subtract(r.getLineTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = subtotal;

        // Credit limit check
        BigDecimal creditLimit = reseller.getCreditLimit() != null ? reseller.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal balance = reseller.getBalance() != null ? reseller.getBalance() : BigDecimal.ZERO;
        BigDecimal pendingTotal = orderRepository.sumPendingTotalByReseller(reseller.getResellerId());
        if (pendingTotal == null) pendingTotal = BigDecimal.ZERO;

        BigDecimal availableCredit = creditLimit.subtract(balance).subtract(pendingTotal);

        if (total.compareTo(availableCredit) > 0) {
            throw new IllegalStateException(String.format(
                    "Insufficient credit. Order total: ₱%s | Available credit: ₱%s (Limit: ₱%s − Outstanding balance: ₱%s − Pending orders: ₱%s).",
                    format(total), format(availableCredit), format(creditLimit), format(balance), format(pendingTotal)));
        }

        // Generate unique order number
        String orderNo = generateOrderNo();

        // Create Order
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setReseller(reseller);
        order.setWarehouse(null);
        order.setStatus(OrderStatus.pending);
        order.setPaymentStatus(PaymentStatus.unpaid);
        order.setSubtotal(subtotal);
        order.setDiscount(discountAmount);
        order.setTotal(total);

        // Create OrderItems (price snapshot)
        for (CartRow row : rows) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(row.getProduct());
            item.setQuantity(row.getQuantity());
            item.setUnitPrice(row.getUnitPrice());
            item.setLineTotal(row.getLineTotal());
            order.getItems().add(item);
        }

        return orderRepository.save(order);
    }

    /**
     * Returns the reseller's own orders, newest first.
     */
    @Transactional(readOnly = true)
    public List<Order> getMyOrders(Integer resellerId) {
        return orderRepository.findByReseller_ResellerIdOrderByCreatedAtDesc(resellerId);
    }

    private String generateOrderNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ORD-" + datePart + "-";
        long count = orderRepository.countByOrderNoPrefix(prefix);
        String seq = String.format("%04d", count + 1);
        String candidate = prefix + seq;
        // Ensure uniqueness in case of race (increment until free)
        while (orderRepository.countByOrderNo(candidate) > 0) {
            count++;
            seq = String.format("%04d", count + 1);
            candidate = prefix + seq;
        }
        return candidate;
    }

    private String format(BigDecimal v) {
        return String.format("%,.2f", v);
    }
}
