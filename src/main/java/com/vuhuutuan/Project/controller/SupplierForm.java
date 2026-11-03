package com.vuhuutuan.Project.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SupplierForm {
    private Long id;

    @NotBlank(message = "Tên nhà cung cấp không được để trống.")
    @Size(max = 150)
    private String name;

    @Size(max = 30)
    private String phone;

    @Size(max = 150)
    private String email;

    @Size(max = 255)
    private String address;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
