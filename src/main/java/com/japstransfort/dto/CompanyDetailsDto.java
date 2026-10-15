package com.japstransfort.dto;

public class CompanyDetailsDto {

    private String name;
    private String address;
    private String phone;
    private String email;
    private String tin;
    private String footer;

    public CompanyDetailsDto() {
    }

    public CompanyDetailsDto(String name, String address, String phone, String email, String tin, String footer) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.tin = tin;
        this.footer = footer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTin() {
        return tin;
    }

    public void setTin(String tin) {
        this.tin = tin;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }
}
