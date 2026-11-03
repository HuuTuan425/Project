package com.vuhuutuan.Project.controller;

import com.vuhuutuan.Project.model.AccountRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

public class CreateAccountForm {
    @NotBlank(message = "Vui lòng nhập tên đăng nhập.")
    @Size(max = 50, message = "Tên đăng nhập không được quá 50 ký tự.")
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Tên đăng nhập chỉ gồm chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.")
    private String username;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @Size(min = 8, max = 64, message = "Mật khẩu phải có từ 8 đến 64 ký tự.")
    private String password;

    @NotNull(message = "Vui lòng chọn loại tài khoản.")
    private AccountRole role;

    @NotBlank(message = "Vui lòng nhập họ và tên nhân viên.")
    @Size(max = 120, message = "Họ tên không được quá 120 ký tự.")
    private String fullName;

    @Size(max = 30, message = "Số điện thoại không được quá 30 ký tự.")
    private String phone;

    @Email(message = "Địa chỉ email không hợp lệ.")
    @Size(max = 150, message = "Email không được quá 150 ký tự.")
    private String email;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AccountRole getRole() {
        return role;
    }

    public void setRole(AccountRole role) {
        this.role = role;
    }

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
}
