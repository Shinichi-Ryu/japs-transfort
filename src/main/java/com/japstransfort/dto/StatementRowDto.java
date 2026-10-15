package com.japstransfort.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One ledger row on the Statement of Account.
 */
public class StatementRowDto {

    /** yyyy-MM-dd transaction date */
    private LocalDate date;

    /** "Invoice" or "Payment" */
    private String type;

    /** order_no (Invoice) or formatted receipt number (Payment) */
    private String reference;

    /** orderId for Invoice rows (used to build order detail link) */
    private Integer orderId;

    /** paymentId for Payment rows (used to build receipt link) */
    private Integer paymentId;

    /** debit amount (charges) – null for payments */
    private BigDecimal debit;

    /** credit amount (payments) – null for invoices */
    private BigDecimal credit;

    /** running balance after this row */
    private BigDecimal balance;

    /** sort tiebreaker: 0 = Invoice, 1 = Payment (invoices before payments on same date) */
    private int sortOrder;

    public StatementRowDto() {}

    public StatementRowDto(LocalDate date, String type, String reference,
                           Integer orderId, Integer paymentId,
                           BigDecimal debit, BigDecimal credit, int sortOrder) {
        this.date = date;
        this.type = type;
        this.reference = reference;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.debit = debit;
        this.credit = credit;
        this.sortOrder = sortOrder;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getPaymentId() { return paymentId; }
    public void setPaymentId(Integer paymentId) { this.paymentId = paymentId; }

    public BigDecimal getDebit() { return debit; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }

    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
