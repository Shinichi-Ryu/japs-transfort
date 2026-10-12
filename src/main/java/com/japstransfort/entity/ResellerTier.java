package com.japstransfort.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entity representing reseller tiers (e.g. Bronze, Silver, Gold).
 * Determines discounts and commissions for resellers.
 */
@Entity
@Table(name = "reseller_tiers")
public class ResellerTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tier_id")
    private Integer tierId;

    @Column(name = "tier_name", nullable = false, unique = true, length = 30)
    private String tierName;

    @Column(name = "discount_pct", precision = 5, scale = 2)
    private BigDecimal discountPct = BigDecimal.ZERO;

    @Column(name = "commission_pct", precision = 5, scale = 2)
    private BigDecimal commissionPct = BigDecimal.ZERO;

    @Column(name = "min_monthly_purchase", precision = 12, scale = 2)
    private BigDecimal minMonthlyPurchase = BigDecimal.ZERO;

    public ResellerTier() {
    }

    public ResellerTier(String tierName, BigDecimal discountPct, BigDecimal commissionPct, BigDecimal minMonthlyPurchase) {
        this.tierName = tierName;
        this.discountPct = discountPct;
        this.commissionPct = commissionPct;
        this.minMonthlyPurchase = minMonthlyPurchase;
    }

    public Integer getTierId() {
        return tierId;
    }

    public void setTierId(Integer tierId) {
        this.tierId = tierId;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public BigDecimal getDiscountPct() {
        return discountPct;
    }

    public void setDiscountPct(BigDecimal discountPct) {
        this.discountPct = discountPct;
    }

    public BigDecimal getCommissionPct() {
        return commissionPct;
    }

    public void setCommissionPct(BigDecimal commissionPct) {
        this.commissionPct = commissionPct;
    }

    public BigDecimal getMinMonthlyPurchase() {
        return minMonthlyPurchase;
    }

    public void setMinMonthlyPurchase(BigDecimal minMonthlyPurchase) {
        this.minMonthlyPurchase = minMonthlyPurchase;
    }
}
