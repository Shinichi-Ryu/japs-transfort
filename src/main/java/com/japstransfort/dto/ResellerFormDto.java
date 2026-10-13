package com.japstransfort.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Data Transfer Object for Reseller creation and editing forms.
 * Separates HTTP form binding from the database entity.
 */
public class ResellerFormDto {

    private Integer resellerId;

    @NotBlank(message = "Business name is required.")
    @Size(max = 120, message = "Business name cannot exceed 120 characters.")
    private String businessName;

    @NotBlank(message = "Contact person / full name is required.")
    @Size(max = 100, message = "Full name cannot exceed 100 characters.")
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    @Size(max = 120, message = "Email cannot exceed 120 characters.")
    private String email;

    @Size(max = 100, message = "Password cannot exceed 100 characters.")
    private String password;

    @Size(max = 20, message = "Contact number cannot exceed 20 characters.")
    private String contactNo;

    private String address;

    @Size(max = 80, message = "City/Location cannot exceed 80 characters.")
    private String territory;

    @NotNull(message = "Tier selection is required.")
    private Integer tierId;

    private Integer parentResellerId;

    @NotNull(message = "Credit limit is required.")
    @DecimalMin(value = "0.00", message = "Credit limit must be zero or positive.")
    private BigDecimal creditLimit = BigDecimal.ZERO;

    private String status = "active";

    public ResellerFormDto() {
    }

    public Integer getResellerId() {
        return resellerId;
    }

    public void setResellerId(Integer resellerId) {
        this.resellerId = resellerId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Integer getTierId() {
        return tierId;
    }

    public void setTierId(Integer tierId) {
        this.tierId = tierId;
    }

    public Integer getParentResellerId() {
        return parentResellerId;
    }

    public void setParentResellerId(Integer parentResellerId) {
        this.parentResellerId = parentResellerId;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
