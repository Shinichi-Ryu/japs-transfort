package com.japstransfort.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDto {

    private CompanyDetailsDto company;
    private Integer orderId;
    private String orderNo;
    private LocalDateTime orderDate;
    private String status;
    private boolean eligible;

    // Bill to
    private String businessName;
    private String contactPerson;
    private String address;
    private String cityLocation;
    private String contactNumber;

    // Payment method
    private String paymentMethod;

    // Items
    private List<InvoiceItemDto> items = new ArrayList<>();

    // Calculations
    private BigDecimal listPriceTotal;
    private BigDecimal discountAmount;
    private BigDecimal discountPct;
    private String discountLabel;
    private BigDecimal total;
    private BigDecimal amountPaid;
    private BigDecimal balanceDue;

    // Payment status badge: PAID, PARTIAL, UNPAID
    private String paymentStatusBadge;

    // Payments received table
    private List<InvoicePaymentDto> payments = new ArrayList<>();

    public InvoiceDto() {
    }

    public CompanyDetailsDto getCompany() {
        return company;
    }

    public void setCompany(CompanyDetailsDto company) {
        this.company = company;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCityLocation() {
        return cityLocation;
    }

    public void setCityLocation(String cityLocation) {
        this.cityLocation = cityLocation;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public List<InvoiceItemDto> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItemDto> items) {
        this.items = items;
    }

    public BigDecimal getListPriceTotal() {
        return listPriceTotal;
    }

    public void setListPriceTotal(BigDecimal listPriceTotal) {
        this.listPriceTotal = listPriceTotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getDiscountPct() {
        return discountPct;
    }

    public void setDiscountPct(BigDecimal discountPct) {
        this.discountPct = discountPct;
    }

    public String getDiscountLabel() {
        return discountLabel;
    }

    public void setDiscountLabel(String discountLabel) {
        this.discountLabel = discountLabel;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getBalanceDue() {
        return balanceDue;
    }

    public void setBalanceDue(BigDecimal balanceDue) {
        this.balanceDue = balanceDue;
    }

    public String getPaymentStatusBadge() {
        return paymentStatusBadge;
    }

    public void setPaymentStatusBadge(String paymentStatusBadge) {
        this.paymentStatusBadge = paymentStatusBadge;
    }

    public List<InvoicePaymentDto> getPayments() {
        return payments;
    }

    public void setPayments(List<InvoicePaymentDto> payments) {
        this.payments = payments;
    }
}
