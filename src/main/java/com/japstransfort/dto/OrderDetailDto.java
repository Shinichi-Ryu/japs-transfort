package com.japstransfort.dto;

import com.japstransfort.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only DTO carrying all data needed for the portal order detail page.
 * Assembled inside a @Transactional(readOnly=true) service method so no lazy
 * loading can escape into the view layer.
 */
public class OrderDetailDto {

    // ── Order header ──────────────────────────────────────────────────────────
    private Integer orderId;
    private String orderNo;
    private LocalDateTime createdAt;
    private OrderStatus status;
    private com.japstransfort.entity.PaymentStatus paymentStatus;
    private com.japstransfort.entity.PaymentMethod paymentMethod;

    // ── Financials ────────────────────────────────────────────────────────────
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private BigDecimal amountPaid;
    private BigDecimal amountRemaining;

    // ── Payments history (snapshot) ──────────────────────────────────────────
    private List<PaymentHistoryDto> payments = new java.util.ArrayList<>();

    // ── Cancellation ─────────────────────────────────────────────────────────
    private String cancelReason;
    private String cancelledBy;
    private LocalDateTime cancelledAt;

    // ── Items (snapshot) ─────────────────────────────────────────────────────
    private List<OrderItemDto> items;

    // ── Shipment (optional – not yet tracked in DB; included for future use) ─
    private String courier;
    private String trackingNumber;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    // ── Payment History DTO ───────────────────────────────────────────────────
    public static class PaymentHistoryDto {
        private Integer paymentId;
        private BigDecimal amount;
        private String method;
        private String referenceNo;
        private LocalDateTime paidAt;
        private String receivedByName;

        public PaymentHistoryDto(Integer paymentId, BigDecimal amount, String method,
                                 String referenceNo, LocalDateTime paidAt, String receivedByName) {
            this.paymentId = paymentId;
            this.amount = amount;
            this.method = method;
            this.referenceNo = referenceNo;
            this.paidAt = paidAt;
            this.receivedByName = receivedByName;
        }

        public Integer getPaymentId() { return paymentId; }
        public BigDecimal getAmount() { return amount; }
        public String getMethod() { return method; }
        public String getReferenceNo() { return referenceNo; }
        public LocalDateTime getPaidAt() { return paidAt; }
        public String getReceivedByName() { return receivedByName; }
    }

    // ── Inner item DTO ────────────────────────────────────────────────────────
    public static class OrderItemDto {
        private String sku;
        private String productName;
        private String unit;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;

        public OrderItemDto(String sku, String productName, String unit,
                            Integer quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
            this.sku = sku;
            this.productName = productName;
            this.unit = unit;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.lineTotal = lineTotal;
        }

        public String getSku() { return sku; }
        public String getProductName() { return productName; }
        public String getUnit() { return unit; }
        public Integer getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getLineTotal() { return lineTotal; }
    }

    // ── Constructors / builder ────────────────────────────────────────────────
    public OrderDetailDto() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public com.japstransfort.entity.PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(com.japstransfort.entity.PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public com.japstransfort.entity.PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(com.japstransfort.entity.PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public BigDecimal getAmountRemaining() { return amountRemaining; }
    public void setAmountRemaining(BigDecimal amountRemaining) { this.amountRemaining = amountRemaining; }

    public List<PaymentHistoryDto> getPayments() { return payments; }
    public void setPayments(List<PaymentHistoryDto> payments) { this.payments = payments; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }

    public String getCourier() { return courier; }
    public void setCourier(String courier) { this.courier = courier; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    /** List price total = total + discount (sum of qty × SRP at order time). */
    public java.math.BigDecimal getSrpTotal() {
        java.math.BigDecimal t = total != null ? total : java.math.BigDecimal.ZERO;
        java.math.BigDecimal d = discount != null ? discount : java.math.BigDecimal.ZERO;
        return t.add(d).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** Convenience: is this order cancellable by the reseller? */
    public boolean isCancellable() {
        return status == com.japstransfort.entity.OrderStatus.pending
                || status == com.japstransfort.entity.OrderStatus.approved;
    }

    /** True if the order has any shipment data to display. */
    public boolean hasShipment() {
        return courier != null || trackingNumber != null
                || shippedAt != null || deliveredAt != null;
    }
}
