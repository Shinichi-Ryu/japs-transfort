package com.japstransfort.dto;

import java.math.BigDecimal;

/**
 * DTO for commission report — one row per reseller showing totals.
 */
public class CommissionReportRowDto {

    private final Integer resellerId;
    private final String businessName;
    private final long commissionCount;
    private final BigDecimal pendingAmount;
    private final BigDecimal paidAmount;
    private final BigDecimal totalAmount;

    public CommissionReportRowDto(Integer resellerId, String businessName,
                                  long commissionCount,
                                  BigDecimal pendingAmount,
                                  BigDecimal paidAmount,
                                  BigDecimal totalAmount) {
        this.resellerId = resellerId;
        this.businessName = businessName;
        this.commissionCount = commissionCount;
        this.pendingAmount = pendingAmount != null ? pendingAmount : BigDecimal.ZERO;
        this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public Integer getResellerId() { return resellerId; }
    public String getBusinessName() { return businessName; }
    public long getCommissionCount() { return commissionCount; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}
