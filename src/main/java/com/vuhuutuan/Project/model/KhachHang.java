package com.vuhuutuan.Project.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "khach_hang")
public class KhachHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_khach_hang")
    private Long maKhachHang;

    @Column(name = "so_cmnd")
    private String soCMND;

    @Column(name = "ho_ten")
    @NotBlank(message = "Họ tên không được để trống.")
    private String hoTen;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "dia_chi")
    private String diaChi;

    @Column(name = "email")
    private String email;

    @Column(name = "so_dien_thoai")
    private String soDienThoai;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_khach_hang")
    private LoaiKhachHang loaiKhachHang;

    @Column(name = "diem_thuong")
    private Integer diemThuong;

    @Column(name = "da_xoa_luc")
    private LocalDateTime deletedAt;

    public Long getMaKhachHang() {
        return maKhachHang;
    }

    public void setMaKhachHang(Long maKhachHang) {
        this.maKhachHang = maKhachHang;
    }

    public String getSoCMND() {
        return soCMND;
    }

    public void setSoCMND(String soCMND) {
        this.soCMND = soCMND;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(LocalDate ngaySinh) {
        this.ngaySinh = ngaySinh;
    }

    public String getDiaChi() {
        return diaChi;
    }

    public void setDiaChi(String diaChi) {
        this.diaChi = diaChi;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public LoaiKhachHang getLoaiKhachHang() {
        return loaiKhachHang;
    }

    public void setLoaiKhachHang(LoaiKhachHang loaiKhachHang) {
        this.loaiKhachHang = loaiKhachHang;
    }

    public Integer getDiemThuong() {
        return diemThuong;
    }

    public void setDiemThuong(Integer diemThuong) {
        this.diemThuong = diemThuong;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
    
}