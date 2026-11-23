package com.vuhuutuan.Project.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordResetForm {
    @NotBlank(message = "Liên kết đặt lại mật khẩu không hợp lệ.")
    private String token;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
    @Size(min = 8, max = 64, message = "Mật khẩu phải có từ 8 đến 64 ký tự.")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu mới.")
    private String confirmPassword;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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
