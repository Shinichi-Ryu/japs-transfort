package com.japstransfort.service;

import com.japstransfort.dto.CartItem;
import com.japstransfort.dto.CartRow;
import com.japstransfort.dto.OrderDetailDto;
import com.japstransfort.entity.*;
import com.japstransfort.repository.InventoryRepository;
import com.japstransfort.repository.OrderRepository;
import com.japstransfort.repository.ProductRepository;
import com.japstransfort.repository.ResellerRepository;
import com.japstransfort.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ResellerRepository resellerRepository;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        InventoryRepository inventoryRepository,
                        StockMovementRepository stockMovementRepository,
                        ResellerRepository resellerRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.resellerRepository = resellerRepository;
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
        BigDecimal pendingTotal = BigDecimal.ZERO;
        try {
            BigDecimal pt = orderRepository.sumPendingTotalByReseller(reseller.getResellerId());
            if (pt != null) {
                pendingTotal = pt;
            }
        } catch (Exception e) {
            try {
                pendingTotal = getMyOrders(reseller.getResellerId()).stream()
                        .filter(o -> o.getStatus() == OrderStatus.pending && o.getTotal() != null)
                        .map(Order::getTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } catch (Exception ignored) {
            }
        }

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

    /**
     * Cancels a reseller's own order.
     * Validates ownership, status (pending or approved only), and reason server-side.
     * Reuses the same stock-reversal logic as admin cancel.
     * Throws IllegalStateException / IllegalArgumentException with user-friendly messages.
     */
    @Transactional
    public Order cancelOrderByReseller(Integer orderId, Integer resellerId, String cancelReason) {
        if (orderId == null) throw new IllegalArgumentException("Order ID must not be null.");

        String reason = cancelReason != null ? cancelReason.trim() : "";
        if (reason.isEmpty() || reason.length() > 255) {
            throw new IllegalArgumentException("A valid cancellation reason (max 255 characters) is required.");
        }

        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));

        if (order.getReseller() == null || !order.getReseller().getResellerId().equals(resellerId)) {
            throw new IllegalStateException("You can only cancel your own orders.");
        }

        if (order.getStatus() != OrderStatus.pending && order.getStatus() != OrderStatus.approved) {
            throw new IllegalStateException("Only orders that are pending or approved can be cancelled. This order is: " + order.getStatus() + ".");
        }

        // Already cancelled guard (stale page / double click)
        if (order.getCancelledAt() != null) {
            throw new IllegalStateException("This order has already been cancelled.");
        }

        order.setCancelReason(reason);
        order.setCancelledBy("RESELLER");
        order.setCancelledAt(LocalDateTime.now());

        if (order.getStatus() == OrderStatus.pending) {
            order.setStatus(OrderStatus.cancelled);
            orderRepository.save(order);
            orderRepository.touchUpdatedAt(orderId);
            return order;
        }

        // Approved: reverse stock and reduce reseller balance
        Warehouse warehouse = order.getWarehouse();
        if (warehouse == null) {
            throw new IllegalStateException("Approved order does not have an assigned warehouse. Please contact admin.");
        }

        Reseller reseller = resellerRepository.findByIdWithLock(resellerId)
                .orElseThrow(() -> new IllegalStateException("Reseller profile not found."));

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product == null) continue;
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;

            java.util.Optional<Inventory> invOpt = inventoryRepository.findByIdWithLock(warehouse.getWarehouseId(), product.getProductId());
            Inventory inventory = invOpt.orElseGet(() -> new Inventory(warehouse, product, 0));
            int currentQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
            inventory.setQuantity(currentQty + quantity);
            inventoryRepository.save(inventory);

            StockMovement movement = new StockMovement(warehouse, product, MoveType.return_, quantity, order.getOrderNo(), null);
            stockMovementRepository.save(movement);
        }

        BigDecimal currentBalance = reseller.getBalance() != null ? reseller.getBalance() : BigDecimal.ZERO;
        BigDecimal orderTotal = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;
        reseller.setBalance(currentBalance.subtract(orderTotal));
        resellerRepository.save(reseller);

        order.setStatus(OrderStatus.cancelled);
        orderRepository.save(order);
        orderRepository.touchUpdatedAt(orderId);
        return order;
    }

    /**
     * Loads an order for the portal detail page.
     * Returns an empty Optional if the order does not exist OR belongs to a
     * different reseller, so the controller can safely return 404 without
     * leaking the existence of the order.
     */
    @Transactional(readOnly = true)
    public java.util.Optional<OrderDetailDto> getOrderDetailForReseller(Integer orderId, Integer resellerId) {
        if (orderId == null || resellerId == null) return java.util.Optional.empty();

        return orderRepository.findWithItemsById(orderId).flatMap(order -> {
            // Ownership check
            if (order.getReseller() == null
                    || !order.getReseller().getResellerId().equals(resellerId)) {
                return java.util.Optional.empty();
            }

            OrderDetailDto dto = new OrderDetailDto();
            dto.setOrderId(order.getOrderId());
            dto.setOrderNo(order.getOrderNo());
            dto.setCreatedAt(order.getCreatedAt());
            dto.setStatus(order.getStatus());
            dto.setSubtotal(order.getSubtotal() != null ? order.getSubtotal() : java.math.BigDecimal.ZERO);
            dto.setDiscount(order.getDiscount() != null ? order.getDiscount() : java.math.BigDecimal.ZERO);
            dto.setTotal(order.getTotal() != null ? order.getTotal() : java.math.BigDecimal.ZERO);
            dto.setCancelReason(order.getCancelReason());
            dto.setCancelledBy(order.getCancelledBy());
            dto.setCancelledAt(order.getCancelledAt());

            List<OrderDetailDto.OrderItemDto> itemDtos = new ArrayList<>();
            for (OrderItem item : order.getItems()) {
                Product p = item.getProduct();
                String sku  = p != null ? p.getSku()  : "N/A";
                String name = p != null ? p.getName() : "Unknown Product";
                String unit = (p != null && p.getUnit() != null) ? p.getUnit() : "pc";
                itemDtos.add(new OrderDetailDto.OrderItemDto(
                        sku, name, unit,
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ));
            }
            dto.setItems(itemDtos);

            // Shipment: no Shipment entity exists yet; fields stay null → hasShipment() == false
            return java.util.Optional.of(dto);
        });
    }

    private String generateOrderNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ORD-" + datePart + "-";
        long count = 0;
        try {
            count = orderRepository.countByOrderNoPrefix(prefix);
        } catch (Exception ignored) {
        }
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
