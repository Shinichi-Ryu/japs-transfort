package com.japstransfort.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a reseller business profile.
 * Linked 1-to-1 with a User record (role = 'reseller').
 */
@Entity
@Table(name = "resellers")
public class Reseller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reseller_id")
    private Integer resellerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "business_name", nullable = false, length = 120)
    private String businessName;

    @Column(name = "contact_no", length = 20)
    private String contactNo;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "territory", length = 80)
    private String territory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id")
    private ResellerTier tier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_reseller_id")
    private Reseller parentReseller;

    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "balance", precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "status", length = 15)
    private String status = "active";

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Reseller() {
    }

    public Reseller(User user, String businessName, String contactNo, String address, String territory, ResellerTier tier) {
        this.user = user;
        this.businessName = businessName;
        this.contactNo = contactNo;
        this.address = address;
        this.territory = territory;
        this.tier = tier;
        this.status = "active";
        this.creditLimit = BigDecimal.ZERO;
        this.balance = BigDecimal.ZERO;
    }

    public Integer getResellerId() {
        return resellerId;
    }

    public void setResellerId(Integer resellerId) {
        this.resellerId = resellerId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTerritory() {
        return territory;
    }

    public void setTerritory(String territory) {
        this.territory = territory;
    }

    public ResellerTier getTier() {
        return tier;
    }

    public void setTier(ResellerTier tier) {
        this.tier = tier;
    }

    public Reseller getParentReseller() {
        return parentReseller;
    }

    public void setParentReseller(Reseller parentReseller) {
        this.parentReseller = parentReseller;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
