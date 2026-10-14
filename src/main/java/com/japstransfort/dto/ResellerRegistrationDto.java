package com.japstransfort.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for public Reseller self-registration.
 */
public class ResellerRegistrationDto {

    @NotBlank(message = "Business name is required.")
    @Size(max = 120, message = "Business name cannot exceed 120 characters.")
    private String businessName;

    @NotBlank(message = "Contact person is required.")
    @Size(max = 100, message = "Contact person cannot exceed 100 characters.")
    private String fullName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    @Size(max = 120, message = "Email cannot exceed 120 characters.")
    private String email;

    @NotBlank(message = "Contact number is required.")
    @Size(max = 20, message = "Contact number cannot exceed 20 characters.")
    private String contactNo;

    @NotBlank(message = "Address is required.")
    private String address;

    @NotBlank(message = "City/Location is required.")
    @Size(max = 80, message = "City/Location cannot exceed 80 characters.")
    private String territory;

    @NotBlank(message = "Password is required.")
    private String password;

    @NotBlank(message = "Confirm password is required.")
    private String confirmPassword;

    public ResellerRegistrationDto() {
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
