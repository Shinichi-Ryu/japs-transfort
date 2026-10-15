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

    /** URL to the order detail or receipt page */
    private String referenceUrl;

    /** debit amount (charges) – null for payments */
    private BigDecimal debit;

    /** credit amount (payments) – null for invoices */
    private BigDecimal credit;

    /** running balance after this row */
    private BigDecimal balance;

    /** sort tiebreaker: 0 = Invoice, 1 = Payment (invoices before payments on same date) */
    private int sortOrder;

    public StatementRowDto() {}

    public StatementRowDto(LocalDate date, String type, String reference, String referenceUrl,
                           BigDecimal debit, BigDecimal credit, int sortOrder) {
        this.date = date;
        this.type = type;
        this.reference = reference;
        this.referenceUrl = referenceUrl;
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

    public String getReferenceUrl() { return referenceUrl; }
    public void setReferenceUrl(String referenceUrl) { this.referenceUrl = referenceUrl; }

    public BigDecimal getDebit() { return debit; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }

    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
