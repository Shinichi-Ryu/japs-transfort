package com.japstransfort.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for the Receivables report — one row per reseller.
 */
public class ReceivablesRowDto {

    private final Integer resellerId;
    private final String businessName;
    private final BigDecimal balance;
    private final BigDecimal creditLimit;
    private final BigDecimal availableCredit;
    private final long openOrderCount;
    private final LocalDate oldestOpenOrderDate;
    // aging buckets of remaining amounts (total - paid) per order date
    private final BigDecimal bucket0to30;
    private final BigDecimal bucket31to60;
    private final BigDecimal bucket61to90;
    private final BigDecimal bucketOver90;
    private final BigDecimal totalRemaining;

    public ReceivablesRowDto(Integer resellerId, String businessName,
                             BigDecimal balance, BigDecimal creditLimit,
                             long openOrderCount, LocalDate oldestOpenOrderDate,
                             BigDecimal bucket0to30, BigDecimal bucket31to60,
                             BigDecimal bucket61to90, BigDecimal bucketOver90,
                             BigDecimal totalRemaining) {
        this.resellerId = resellerId;
        this.businessName = businessName;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.creditLimit = creditLimit != null ? creditLimit : BigDecimal.ZERO;
        this.availableCredit = this.creditLimit.subtract(this.balance);
        this.openOrderCount = openOrderCount;
        this.oldestOpenOrderDate = oldestOpenOrderDate;
        this.bucket0to30 = bucket0to30 != null ? bucket0to30 : BigDecimal.ZERO;
        this.bucket31to60 = bucket31to60 != null ? bucket31to60 : BigDecimal.ZERO;
        this.bucket61to90 = bucket61to90 != null ? bucket61to90 : BigDecimal.ZERO;
        this.bucketOver90 = bucketOver90 != null ? bucketOver90 : BigDecimal.ZERO;
        this.totalRemaining = totalRemaining != null ? totalRemaining : BigDecimal.ZERO;
    }

    public boolean isOverCreditLimit() {
        return balance.compareTo(creditLimit) > 0;
    }

    public Integer getResellerId() { return resellerId; }
    public String getBusinessName() { return businessName; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public BigDecimal getAvailableCredit() { return availableCredit; }
    public long getOpenOrderCount() { return openOrderCount; }
    public LocalDate getOldestOpenOrderDate() { return oldestOpenOrderDate; }
    public BigDecimal getBucket0to30() { return bucket0to30; }
    public BigDecimal getBucket31to60() { return bucket31to60; }
    public BigDecimal getBucket61to90() { return bucket61to90; }
    public BigDecimal getBucketOver90() { return bucketOver90; }
    public BigDecimal getTotalRemaining() { return totalRemaining; }
}
