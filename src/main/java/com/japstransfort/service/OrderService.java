package com.japstransfort.service;

import com.japstransfort.dto.CartItem;
import com.japstransfort.dto.CartRow;
import com.japstransfort.dto.CompanyDetailsDto;
import com.japstransfort.dto.InvoiceDto;
import com.japstransfort.dto.InvoiceItemDto;
import com.japstransfort.dto.InvoicePaymentDto;
import com.japstransfort.dto.OrderDetailDto;
import com.japstransfort.dto.PaymentReceiptDto;
import com.japstransfort.entity.*;
import com.japstransfort.repository.InventoryRepository;
import com.japstransfort.repository.OrderRepository;
import com.japstransfort.repository.PaymentRepository;
import com.japstransfort.repository.ProductRepository;
import com.japstransfort.repository.ResellerRepository;
import com.japstransfort.repository.ShipmentRepository;
import com.japstransfort.repository.StockMovementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for reseller cart and order placement.
 */
@Service
public class OrderService {

    @Value("${app.company.name:Japs Transfort}")
    private String companyName;

    @Value("${app.company.address:}")
    private String companyAddress;

    @Value("${app.company.phone:}")
    private String companyPhone;

    @Value("${app.company.email:}")
    private String companyEmail;

    @Value("${app.company.tin:}")
    private String companyTin;

    @Value("${app.invoice.footer:}")
    private String invoiceFooter;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ResellerRepository resellerRepository;
    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;
    private final CommissionService commissionService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        InventoryRepository inventoryRepository,
                        StockMovementRepository stockMovementRepository,
                        ResellerRepository resellerRepository,
                        ShipmentRepository shipmentRepository,
                        PaymentRepository paymentRepository,
                        CommissionService commissionService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.resellerRepository = resellerRepository;
        this.shipmentRepository = shipmentRepository;
        this.paymentRepository = paymentRepository;
        this.commissionService = commissionService;
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
    public Order placeOrder(Reseller reseller, Map<Integer, Integer> cart, BigDecimal discountPct, String paymentMethod) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalStateException("Your cart is empty. Please add items before placing an order.");
        }

        // Validate payment method
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Please select a payment method.");
        }
        String cleanPm = paymentMethod.trim().toUpperCase();
        if ("ONLINE".equals(cleanPm)) {
            throw new IllegalArgumentException("Online payment is currently unavailable. Please choose another payment method.");
        }
        if (!"COD".equals(cleanPm) && !"CREDIT".equals(cleanPm)) {
            throw new IllegalArgumentException("Invalid payment method selected.");
        }

        PaymentMethod pm = PaymentMethod.valueOf(cleanPm);

        BigDecimal creditLimit = reseller.getCreditLimit() != null ? reseller.getCreditLimit() : BigDecimal.ZERO;
        if (pm == PaymentMethod.CREDIT && creditLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("On account (credit) is only available for accounts with a credit limit.");
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

        // Credit limit check applies ONLY to On account (CREDIT) orders. COD orders skip it.
        if (pm == PaymentMethod.CREDIT) {
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
        order.setPaymentMethod(pm);
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

    @Transactional
    public Order placeOrder(Reseller reseller, Map<Integer, Integer> cart, BigDecimal discountPct) {
        return placeOrder(reseller, cart, discountPct, PaymentMethod.CREDIT.name());
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

        if (paymentRepository.existsByOrder_OrderId(orderId)) {
            throw new IllegalStateException("This order has payments recorded. Cancelling is not available yet.");
        }

        if (order.getReseller() == null || !order.getReseller().getResellerId().equals(resellerId)) {
            throw new IllegalStateException("You can only cancel your own orders.");
        }

        if (order.getStatus() != OrderStatus.pending && order.getStatus() != OrderStatus.approved) {
            String s = order.getStatus().name();
            if ("packed".equals(s) || "shipped".equals(s) || "delivered".equals(s)) {
                throw new IllegalStateException(
                        "This order cannot be cancelled because it has already been " + s +
                        ". Only pending or approved orders can be cancelled.");
            }
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

            // Financials and payments
            dto.setPaymentStatus(order.getPaymentStatus());
            dto.setPaymentMethod(order.getPaymentMethod());
            BigDecimal amountPaid = paymentRepository.sumAmountByOrderId(orderId);
            if (amountPaid == null) amountPaid = BigDecimal.ZERO;
            amountPaid = amountPaid.setScale(2, RoundingMode.HALF_UP);
            dto.setAmountPaid(amountPaid);

            BigDecimal orderTotal = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;
            BigDecimal remaining = orderTotal.subtract(amountPaid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            dto.setAmountRemaining(remaining);

            List<Payment> payments = paymentRepository.findByOrderIdWithReceivedBy(orderId);
            List<OrderDetailDto.PaymentHistoryDto> paymentDtos = new ArrayList<>();
            for (Payment p : payments) {
                String receivedBy = p.getReceivedBy() != null ? p.getReceivedBy().getFullName() : "Admin";
                paymentDtos.add(new OrderDetailDto.PaymentHistoryDto(
                        p.getPaymentId(),
                        p.getAmount(),
                        p.getMethod(),
                        p.getReferenceNo(),
                        p.getPaidAt(),
                        receivedBy
                ));
            }
            dto.setPayments(paymentDtos);

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

            // Shipment: load from DB if present
            shipmentRepository.findByOrder_OrderId(orderId).ifPresent(s -> {
                dto.setCourier(s.getCourier());
                dto.setTrackingNumber(s.getTrackingNo());
                dto.setShippedAt(s.getShippedAt());
                dto.setDeliveredAt(s.getDeliveredAt());
            });

            return java.util.Optional.of(dto);
        });
    }

    /**
     * Retrieves all payments for an order with receivedBy eagerly loaded.
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsForOrder(Integer orderId) {
        if (orderId == null) return List.of();
        return paymentRepository.findByOrderIdWithReceivedBy(orderId);
    }

    /**
     * Computes the total amount paid so far for an order.
     */
    @Transactional(readOnly = true)
    public BigDecimal getAmountPaidForOrder(Integer orderId) {
        if (orderId == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sum = paymentRepository.sumAmountByOrderId(orderId);
        return sum != null ? sum.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Records a payment against an order.
     * ONE @Transactional method locking both the order and reseller rows.
     * Re-checks status (approved, packed, shipped, delivered) and remaining amount inside lock.
     * Enforces amount > 0 with 2 decimals and <= remaining.
     * Inserts payment, reduces reseller balance, sets paymentStatus to partial or paid, updates updated_at.
     */
    @Transactional
    public Payment recordPayment(Integer orderId, BigDecimal amount, String method, String referenceNo, User admin) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID must not be null.");
        }

        // Lock order row (pessimistic write)
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found (ID " + orderId + ")."));

        if (order.getReseller() == null) {
            throw new IllegalStateException("Order #" + order.getOrderNo() + " has no associated reseller.");
        }

        // Lock reseller row (pessimistic write)
        Reseller reseller = resellerRepository.findByIdWithLock(order.getReseller().getResellerId())
                .orElseThrow(() -> new IllegalStateException("Reseller profile not found."));

        // Re-check order status inside lock: approved, packed, shipped, delivered only
        OrderStatus status = order.getStatus();
        if (status != OrderStatus.approved && status != OrderStatus.packed
                && status != OrderStatus.shipped && status != OrderStatus.delivered) {
            throw new IllegalStateException(
                    "Payments can only be recorded for approved, packed, shipped, or delivered orders. Current status: " + status + ".");
        }

        // Re-check remaining amount inside lock
        BigDecimal orderTotal = order.getTotal() != null
                ? order.getTotal().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal currentPaid = paymentRepository.sumAmountByOrderId(orderId);
        if (currentPaid == null) {
            currentPaid = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            currentPaid = currentPaid.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal remaining = orderTotal.subtract(currentPaid).setScale(2, RoundingMode.HALF_UP);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("This order is already fully paid.");
        }

        // Validate amount: must be > 0 with at most 2 decimals and <= remaining
        if (amount == null) {
            throw new IllegalArgumentException("Payment amount is required.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        if (amount.scale() > 2 || amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Payment amount cannot have more than 2 decimal places.");
        }
        BigDecimal paymentAmount = amount.setScale(2, RoundingMode.HALF_UP);

        if (paymentAmount.compareTo(remaining) > 0) {
            throw new IllegalStateException(String.format(
                    "Payment amount (₱%s) exceeds the remaining balance of ₱%s.",
                    format(paymentAmount), format(remaining)));
        }

        // Validate method and reference number
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required.");
        }
        String cleanMethod = method.trim();
        String cleanRef = referenceNo != null ? referenceNo.trim() : "";
        if (!"Cash".equalsIgnoreCase(cleanMethod)) {
            if (cleanRef.isEmpty()) {
                throw new IllegalArgumentException("Reference number is required for " + cleanMethod + " payments.");
            }
        }
        String refToSave = cleanRef.isEmpty() ? null : cleanRef;

        // Insert payment: paid_at is now, received_by is logged-in admin
        Payment payment = new Payment(order, paymentAmount, cleanMethod, refToSave, LocalDateTime.now(), admin);
        Payment savedPayment = paymentRepository.save(payment);

        // Lower reseller's balance by the amount
        BigDecimal currentBalance = reseller.getBalance() != null
                ? reseller.getBalance().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        reseller.setBalance(currentBalance.subtract(paymentAmount).setScale(2, RoundingMode.HALF_UP));
        resellerRepository.save(reseller);

        // Set order payment_status: partial (paid > 0 and remaining > 0) or paid (remaining = 0)
        BigDecimal newPaid = currentPaid.add(paymentAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newRemaining = orderTotal.subtract(newPaid).setScale(2, RoundingMode.HALF_UP);

        if (newRemaining.compareTo(BigDecimal.ZERO) == 0) {
            order.setPaymentStatus(PaymentStatus.paid);
        } else if (newPaid.compareTo(BigDecimal.ZERO) > 0 && newRemaining.compareTo(BigDecimal.ZERO) > 0) {
            order.setPaymentStatus(PaymentStatus.partial);
        } else {
            order.setPaymentStatus(PaymentStatus.unpaid);
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        orderRepository.touchUpdatedAt(orderId);

        commissionService.createCommissionIfEligible(order);

        return savedPayment;
    }

    public CompanyDetailsDto getCompanyDetails() {
        return new CompanyDetailsDto(
                companyName != null && !companyName.trim().isEmpty() ? companyName : "Japs Transfort",
                companyAddress,
                companyPhone,
                companyEmail,
                companyTin,
                invoiceFooter
        );
    }

    /**
     * Loads invoice data for an order (admin view).
     */
    @Transactional(readOnly = true)
    public Optional<InvoiceDto> getInvoiceForAdmin(Integer orderId) {
        return buildInvoiceDto(orderId, null);
    }

    /**
     * Loads invoice data for an order (reseller view, enforcing ownership).
     */
    @Transactional(readOnly = true)
    public Optional<InvoiceDto> getInvoiceForReseller(Integer orderId, Integer resellerId) {
        if (resellerId == null) return Optional.empty();
        return buildInvoiceDto(orderId, resellerId);
    }

    private Optional<InvoiceDto> buildInvoiceDto(Integer orderId, Integer resellerId) {
        if (orderId == null) return Optional.empty();

        Optional<Order> orderOpt = orderRepository.findWithItemsById(orderId);
        if (orderOpt.isEmpty()) return Optional.empty();

        Order order = orderOpt.get();

        if (resellerId != null) {
            if (order.getReseller() == null || !order.getReseller().getResellerId().equals(resellerId)) {
                return Optional.empty();
            }
        }

        InvoiceDto dto = new InvoiceDto();
        dto.setCompany(getCompanyDetails());
        dto.setOrderId(order.getOrderId());
        dto.setOrderNo(order.getOrderNo());
        dto.setOrderDate(order.getCreatedAt());
        dto.setStatus(order.getStatus() != null ? order.getStatus().name() : "");

        boolean eligible = order.getStatus() == OrderStatus.approved
                || order.getStatus() == OrderStatus.packed
                || order.getStatus() == OrderStatus.shipped
                || order.getStatus() == OrderStatus.delivered;
        dto.setEligible(eligible);

        Reseller reseller = order.getReseller();
        if (reseller != null) {
            dto.setBusinessName(reseller.getBusinessName());
            if (reseller.getUser() != null) {
                dto.setContactPerson(reseller.getUser().getFullName());
            }
            dto.setAddress(reseller.getAddress());
            dto.setCityLocation(reseller.getTerritory());
            dto.setContactNumber(reseller.getContactNo());
        }

        if (order.getPaymentMethod() != null) {
            switch (order.getPaymentMethod()) {
                case COD -> dto.setPaymentMethod("Cash on delivery (COD)");
                case CREDIT -> dto.setPaymentMethod("On account (Credit)");
                case ONLINE -> dto.setPaymentMethod("Online");
                default -> dto.setPaymentMethod(order.getPaymentMethod().name());
            }
        } else {
            dto.setPaymentMethod("On account");
        }

        List<InvoiceItemDto> itemDtos = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                String sku = item.getProduct() != null ? item.getProduct().getSku() : "N/A";
                String description = item.getProduct() != null ? item.getProduct().getName() : "Unknown Product";
                String unit = (item.getProduct() != null && item.getProduct().getUnit() != null) ? item.getProduct().getUnit() : "pc";
                Integer qty = item.getQuantity() != null ? item.getQuantity() : 0;
                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineTotal = item.getLineTotal() != null ? item.getLineTotal().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

                itemDtos.add(new InvoiceItemDto(sku, description, unit, qty, unitPrice, lineTotal));
            }
        }
        dto.setItems(itemDtos);

        BigDecimal total = order.getTotal() != null ? order.getTotal().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = order.getDiscount() != null ? order.getDiscount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal listPriceTotal = total.add(discount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountPct = BigDecimal.ZERO;
        if (reseller != null && reseller.getTier() != null && reseller.getTier().getDiscountPct() != null) {
            discountPct = reseller.getTier().getDiscountPct();
        }
        String pctStr = discountPct.stripTrailingZeros().toPlainString() + "%";
        dto.setDiscountLabel("Tier discount (" + pctStr + ")");
        dto.setDiscountPct(discountPct);

        dto.setListPriceTotal(listPriceTotal);
        dto.setDiscountAmount(discount);
        dto.setTotal(total);

        BigDecimal amountPaid = paymentRepository.sumAmountByOrderId(order.getOrderId());
        if (amountPaid == null) {
            amountPaid = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            amountPaid = amountPaid.setScale(2, RoundingMode.HALF_UP);
        }
        dto.setAmountPaid(amountPaid);

        BigDecimal balanceDue = total.subtract(amountPaid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        dto.setBalanceDue(balanceDue);

        if (balanceDue.compareTo(BigDecimal.ZERO) == 0 && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            dto.setPaymentStatusBadge("PAID");
        } else if (amountPaid.compareTo(BigDecimal.ZERO) > 0 && balanceDue.compareTo(BigDecimal.ZERO) > 0) {
            dto.setPaymentStatusBadge("PARTIAL");
        } else {
            dto.setPaymentStatusBadge("UNPAID");
        }

        List<Payment> payments = paymentRepository.findByOrderIdWithReceivedBy(order.getOrderId());
        List<InvoicePaymentDto> paymentDtos = new ArrayList<>();
        if (payments != null) {
            List<Payment> sortedPayments = new ArrayList<>(payments);
            sortedPayments.sort(Comparator.comparing(Payment::getPaymentId));
            for (Payment p : sortedPayments) {
                paymentDtos.add(new InvoicePaymentDto(
                        p.getPaidAt(),
                        p.getMethod() != null ? p.getMethod() : "Cash",
                        p.getReferenceNo(),
                        p.getAmount() != null ? p.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                ));
            }
        }
        dto.setPayments(paymentDtos);

        return Optional.of(dto);
    }

    /**
     * Loads payment receipt data (admin view).
     */
    @Transactional(readOnly = true)
    public Optional<PaymentReceiptDto> getReceiptForAdmin(Integer paymentId) {
        return buildReceiptDto(paymentId, null);
    }

    /**
     * Loads payment receipt data (reseller view, enforcing ownership).
     */
    @Transactional(readOnly = true)
    public Optional<PaymentReceiptDto> getReceiptForReseller(Integer paymentId, Integer resellerId) {
        if (resellerId == null) return Optional.empty();
        return buildReceiptDto(paymentId, resellerId);
    }

    private Optional<PaymentReceiptDto> buildReceiptDto(Integer paymentId, Integer resellerId) {
        if (paymentId == null) return Optional.empty();

        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) return Optional.empty();

        Payment payment = paymentOpt.get();
        Order order = payment.getOrder();
        if (order == null) return Optional.empty();

        Reseller reseller = order.getReseller();
        if (resellerId != null) {
            if (reseller == null || !reseller.getResellerId().equals(resellerId)) {
                return Optional.empty();
            }
        }

        String receiptNo = String.format("RCT-%06d", payment.getPaymentId());

        List<Payment> allOrderPayments = paymentRepository.findByOrderIdWithReceivedBy(order.getOrderId());
        List<Payment> sortedPayments = new ArrayList<>(allOrderPayments);
        sortedPayments.sort(Comparator.comparing(Payment::getPaymentId));

        BigDecimal cumulativePaid = BigDecimal.ZERO;
        for (Payment p : sortedPayments) {
            if (p.getAmount() != null) {
                cumulativePaid = cumulativePaid.add(p.getAmount());
            }
            if (p.getPaymentId().equals(payment.getPaymentId())) {
                break;
            }
        }

        BigDecimal orderTotal = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;
        BigDecimal remainingAfterThisPayment = orderTotal.subtract(cumulativePaid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        PaymentReceiptDto dto = new PaymentReceiptDto();
        dto.setCompany(getCompanyDetails());
        dto.setPaymentId(payment.getPaymentId());
        dto.setReceiptNo(receiptNo);
        dto.setPaidAt(payment.getPaidAt());
        dto.setBusinessName(reseller != null ? reseller.getBusinessName() : "N/A");
        dto.setAmount(payment.getAmount() != null ? payment.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        dto.setMethod(payment.getMethod() != null ? payment.getMethod() : "Cash");
        dto.setReferenceNo(payment.getReferenceNo());
        dto.setOrderNo(order.getOrderNo());
        dto.setRemainingBalance(remainingAfterThisPayment);
        dto.setOrderId(order.getOrderId());

        return Optional.of(dto);
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
