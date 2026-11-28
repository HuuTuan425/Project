package com.vuhuutuan.Project.controller;

import com.vuhuutuan.Project.model.AccountRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateAccountForm {
    @NotBlank(message = "Vui lòng nhập họ và tên nhân viên.")
    @Size(max = 120, message = "Họ tên không được quá 120 ký tự.")
    private String fullName;

    @Size(max = 30, message = "Số điện thoại không được quá 30 ký tự.")
    private String phone;

    @NotBlank(message = "Vui lòng nhập email để có thể đặt lại mật khẩu.")
    @Email(message = "Địa chỉ email không hợp lệ.")
    @Size(max = 150, message = "Email không được quá 150 ký tự.")
    private String email;

    @NotNull(message = "Vui lòng chọn loại tài khoản.")
    private AccountRole role;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public AccountRole getRole() {
        return role;
    }

    public void setRole(AccountRole role) {
        this.role = role;
    }
}
