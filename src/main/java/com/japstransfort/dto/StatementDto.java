package com.japstransfort.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Full Statement of Account data passed to the Thymeleaf template.
 */
public class StatementDto {

    // ── Reseller info ──────────────────────────────────────────────────────────
    private Integer resellerId;
    private String businessName;
    private String contactPerson;
    private String address;
    private String cityLocation;

    // ── Period ─────────────────────────────────────────────────────────────────
    private LocalDate fromDate;
    private LocalDate toDate;

    // ── Balances ───────────────────────────────────────────────────────────────
    /** Total charges before fromDate minus total payments before fromDate */
    private BigDecimal openingBalance;

    /** openingBalance + periodCharges − periodPayments */
    private BigDecimal closingBalance;

    /** resellers.balance (live column value) */
    private BigDecimal accountBalance;

    private BigDecimal creditLimit;
    private BigDecimal availableCredit;

    // ── Period totals ──────────────────────────────────────────────────────────
    private BigDecimal periodCharges;
    private BigDecimal periodPayments;

    // ── Rows in the period ─────────────────────────────────────────────────────
    private List<StatementRowDto> rows;

    // ── Reconciliation ─────────────────────────────────────────────────────────
    /** true when toDate == today */
    private boolean todayReport;
    /** |closingBalance − accountBalance| */
    private BigDecimal reconciliationDiff;
    /** true when reconciliationDiff < 0.01 */
    private boolean reconciliationOk;

    // ── Company header (reuse existing pattern) ─────────────────────────────────
    private CompanyDetailsDto company;

    public StatementDto() {}

    // Getters and setters ──────────────────────────────────────────────────────

    public Integer getResellerId() { return resellerId; }
    public void setResellerId(Integer resellerId) { this.resellerId = resellerId; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCityLocation() { return cityLocation; }
    public void setCityLocation(String cityLocation) { this.cityLocation = cityLocation; }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }

    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }

    public BigDecimal getClosingBalance() { return closingBalance; }
    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = closingBalance; }

    public BigDecimal getAccountBalance() { return accountBalance; }
    public void setAccountBalance(BigDecimal accountBalance) { this.accountBalance = accountBalance; }

    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

    public BigDecimal getAvailableCredit() { return availableCredit; }
    public void setAvailableCredit(BigDecimal availableCredit) { this.availableCredit = availableCredit; }

    public BigDecimal getPeriodCharges() { return periodCharges; }
    public void setPeriodCharges(BigDecimal periodCharges) { this.periodCharges = periodCharges; }

    public BigDecimal getPeriodPayments() { return periodPayments; }
    public void setPeriodPayments(BigDecimal periodPayments) { this.periodPayments = periodPayments; }

    public List<StatementRowDto> getRows() { return rows; }
    public void setRows(List<StatementRowDto> rows) { this.rows = rows; }

    public boolean isTodayReport() { return todayReport; }
    public void setTodayReport(boolean todayReport) { this.todayReport = todayReport; }

    public BigDecimal getReconciliationDiff() { return reconciliationDiff; }
    public void setReconciliationDiff(BigDecimal reconciliationDiff) { this.reconciliationDiff = reconciliationDiff; }

    public boolean isReconciliationOk() { return reconciliationOk; }
    public void setReconciliationOk(boolean reconciliationOk) { this.reconciliationOk = reconciliationOk; }

    public CompanyDetailsDto getCompany() { return company; }
    public void setCompany(CompanyDetailsDto company) { this.company = company; }
}
