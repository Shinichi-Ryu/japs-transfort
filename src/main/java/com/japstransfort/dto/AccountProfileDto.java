package com.japstransfort.dto;

import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for profile updates on the account settings page.
 * Strictly excludes any user ID or reseller ID to ensure security.
 */
public class AccountProfileDto {

    @Size(max = 100, message = "Full name cannot exceed 100 characters.")
    private String fullName;

    @Size(max = 120, message = "Business name cannot exceed 120 characters.")
    private String businessName;

    @Size(max = 20, message = "Contact number cannot exceed 20 characters.")
    private String contactNo;

    private String address;

    @Size(max = 80, message = "City/Location cannot exceed 80 characters.")
    private String territory;

    public AccountProfileDto() {
    }

    public AccountProfileDto(String fullName, String businessName, String contactNo, String address, String territory) {
        this.fullName = fullName;
        this.businessName = businessName;
        this.contactNo = contactNo;
        this.address = address;
        this.territory = territory;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
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
}
