package com.japstransfort.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO for creating and editing a ResellerTier.
 */
public class TierFormDto {

    private Integer tierId;

    @NotBlank(message = "Tier name is required.")
    @Size(max = 30, message = "Tier name must be 30 characters or fewer.")
    private String tierName;

    @NotNull(message = "Discount percentage is required.")
    @DecimalMin(value = "0.00", message = "Discount must be at least 0%.")
    @DecimalMax(value = "100.00", message = "Discount cannot exceed 100%.")
    private BigDecimal discountPct;

    @NotNull(message = "Commission percentage is required.")
    @DecimalMin(value = "0.00", message = "Commission must be at least 0%.")
    @DecimalMax(value = "100.00", message = "Commission cannot exceed 100%.")
    private BigDecimal commissionPct;

    @NotNull(message = "Minimum monthly purchase is required.")
    @DecimalMin(value = "0.00", message = "Minimum monthly purchase cannot be negative.")
    private BigDecimal minMonthlyPurchase;

    public TierFormDto() {
    }

    public TierFormDto(Integer tierId, String tierName, BigDecimal discountPct,
                       BigDecimal commissionPct, BigDecimal minMonthlyPurchase) {
        this.tierId = tierId;
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
