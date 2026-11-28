package com.vuhuutuan.Project.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SupplierProposalForm {
    @NotBlank(message = "Tên nhà cung cấp không được để trống.")
    @Size(max = 150, message = "Tên nhà cung cấp không được vượt quá 150 ký tự.")
    private String name;

    @Pattern(regexp = "[0-9]*", message = "Thông tin không hợp lệ.")
    @Size(max = 30, message = "Số điện thoại không được vượt quá 30 chữ số.")
    private String phone;

    @Email(message = "Thông tin không hợp lệ.")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự.")
    private String email;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự.")
    private String address;

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
