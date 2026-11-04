package com.vuhuutuan.Project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "de_xuat_nha_cung_cap")
public class SupplierProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(name = "ten_nha_cung_cap", nullable = false, length = 150)
    private String name;

    @Size(max = 30)
    @Column(name = "so_dien_thoai", length = 30)
    private String phone;

    @Size(max = 150)
    @Column(name = "email", length = 150)
    private String email;

    @Size(max = 255)
    @Column(name = "dia_chi", length = 255)
    private String address;

    @Column(name = "nguoi_de_xuat", nullable = false, length = 50)
    private String requestedBy;

    @Column(name = "thoi_gian_de_xuat", nullable = false)
    private LocalDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private SupplierProposalStatus status = SupplierProposalStatus.PENDING;

    @Column(name = "nguoi_xu_ly", length = 50)
    private String reviewedBy;

    @Column(name = "thoi_gian_xu_ly")
    private LocalDateTime reviewedAt;

    public SupplierProposal() {
    }

    public Long getId() {
        return id;
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

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public SupplierProposalStatus getStatus() {
        return status;
    }

    public void setStatus(SupplierProposalStatus status) {
        this.status = status;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
