package com.vuhuutuan.Project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "tai_khoan")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_dang_nhap", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "mat_khau", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "vai_tro", nullable = false, length = 20)
    private AccountRole role;

    @Column(name = "dang_hoat_dong", nullable = false)
    private boolean enabled = true;

    @Column(name = "ho_ten", length = 120)
    private String fullName;

    @Column(name = "so_dien_thoai", length = 30)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "dang_nhap_lan_cuoi")
    private LocalDateTime lastLoginAt;

    @Column(name = "da_xoa_luc")
    private LocalDateTime deletedAt;

    protected AppUser() {
    }

    public AppUser(String username, String passwordHash, AccountRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public AccountRole getRole() {
        return role;
    }

    public void setRole(AccountRole role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
