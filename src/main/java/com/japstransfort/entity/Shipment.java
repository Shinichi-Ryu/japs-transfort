package com.japstransfort.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity matching the shipments table exactly (status is a plain VARCHAR).
 */
@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipment_id")
    private Integer shipmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "courier", length = 60)
    private String courier;

    @Column(name = "tracking_no", length = 60)
    private String trackingNo;

    /** Plain VARCHAR – values: preparing, shipped, delivered */
    @Column(name = "status", length = 20)
    private String status = "preparing";

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    public Shipment() {}

    public Integer getShipmentId() { return shipmentId; }
    public void setShipmentId(Integer shipmentId) { this.shipmentId = shipmentId; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public String getCourier() { return courier; }
    public void setCourier(String courier) { this.courier = courier; }

    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
}
