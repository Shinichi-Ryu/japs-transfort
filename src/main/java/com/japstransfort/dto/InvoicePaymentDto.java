package com.japstransfort.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoicePaymentDto {

    private LocalDateTime paidAt;
    private String method;
    private String reference;
    private BigDecimal amount;

    public InvoicePaymentDto() {
    }

    public InvoicePaymentDto(LocalDateTime paidAt, String method, String reference, BigDecimal amount) {
        this.paidAt = paidAt;
        this.method = method;
        this.reference = reference;
        this.amount = amount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
