package com.japstransfort.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity matching the order_messages table exactly.
 * Messages between admin and reseller regarding an order.
 * Immutable: no edit, no delete.
 */
@Entity
@Table(name = "order_messages")
public class OrderMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Integer messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User senderUser;

    @Column(name = "sender_role", length = 20, nullable = false)
    private String senderRole;

    @Column(name = "body", length = 1000, nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_by_admin", nullable = false)
    private boolean readByAdmin;

    @Column(name = "read_by_reseller", nullable = false)
    private boolean readByReseller;

    public OrderMessage() {
    }

    public OrderMessage(Order order, User senderUser, String senderRole, String body,
                        LocalDateTime createdAt, boolean readByAdmin, boolean readByReseller) {
        this.order = order;
        this.senderUser = senderUser;
        this.senderRole = senderRole;
        this.body = body;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.readByAdmin = readByAdmin;
        this.readByReseller = readByReseller;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getMessageId() {
        return messageId;
    }

    public void setMessageId(Integer messageId) {
        this.messageId = messageId;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public User getSenderUser() {
        return senderUser;
    }

    public void setSenderUser(User senderUser) {
        this.senderUser = senderUser;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isReadByAdmin() {
        return readByAdmin;
    }

    public boolean getReadByAdmin() {
        return readByAdmin;
    }

    public void setReadByAdmin(boolean readByAdmin) {
        this.readByAdmin = readByAdmin;
    }

    public boolean isReadByReseller() {
        return readByReseller;
    }

    public boolean getReadByReseller() {
        return readByReseller;
    }

    public void setReadByReseller(boolean readByReseller) {
        this.readByReseller = readByReseller;
    }
}
