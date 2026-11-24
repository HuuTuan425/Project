package com.vuhuutuan.Project.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mat_hang")
public class MatHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_mat_hang")
    private Long maMatHang;

    @Column(name = "ten_mat_hang")
    @NotBlank(message = "Tên mặt hàng không được để trống.")
    private String tenMatHang;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_mat_hang")
    @NotNull(message = "Vui lòng chọn loại mặt hàng.")
    private LoaiMatHang loaiMatHang;

    @Column(name = "gia_ban_goc")
    @NotNull(message = "Vui lòng nhập giá bán.")
    @Positive(message = "Giá bán phải lớn hơn 0.")
    private Double giaBanGoc;

    @Column(name = "hang_san_xuat")
    private String hangSanXuat;

    @Column(name = "ngay_san_xuat")
    private LocalDate ngaySanXuat;

    @Column(name = "han_su_dung")
    private LocalDate hanSuDung;

    @Column(name = "gia_khuyen_mai")
    private Double giaKhuyenMai;

    @Column(name = "thoi_gian_bat_dau_km")
    private LocalDateTime thoiGianBatDauKM;

    @Column(name = "thoi_gian_ket_thuc_km")
    private LocalDateTime thoiGianKetThucKM;

    @Column(name = "quantity_in_stock", nullable = false)
    private Integer quantityInStock = 0;

    @Column(name = "da_xoa_luc")
    private LocalDateTime deletedAt;

    public Double getGiaApDung(LocalDateTime thoiDiem) {
        if (giaKhuyenMai == null || giaKhuyenMai <= 0) {
            return giaBanGoc;
        }
        if (thoiGianBatDauKM != null && thoiDiem.isBefore(thoiGianBatDauKM)) {
            return giaBanGoc;
        }
        if (thoiGianKetThucKM != null && thoiDiem.isAfter(thoiGianKetThucKM)) {
            return giaBanGoc;
        }
        return giaKhuyenMai;
    }

    public String getTrangThaiHienThi() {
        return quantityInStock != null && quantityInStock > 0 ? "Còn hàng" : "Hết hàng";
    }

    public boolean isConHang() {
        return quantityInStock != null && quantityInStock > 0;
    }

    public Long getMaMatHang() {
        return maMatHang;
    }

    public void setMaMatHang(Long maMatHang) {
        this.maMatHang = maMatHang;
    }

    public String getTenMatHang() {
        return tenMatHang;
    }

    public void setTenMatHang(String tenMatHang) {
        this.tenMatHang = tenMatHang;
    }

    public LoaiMatHang getLoaiMatHang() {
        return loaiMatHang;
    }

    public void setLoaiMatHang(LoaiMatHang loaiMatHang) {
        this.loaiMatHang = loaiMatHang;
    }

    public Double getGiaBanGoc() {
        return giaBanGoc;
    }

    public void setGiaBanGoc(Double giaBanGoc) {
        this.giaBanGoc = giaBanGoc;
    }

    public String getHangSanXuat() {
        return hangSanXuat;
    }

    public void setHangSanXuat(String hangSanXuat) {
        this.hangSanXuat = hangSanXuat;
    }

    public LocalDate getNgaySanXuat() {
        return ngaySanXuat;
    }

    public void setNgaySanXuat(LocalDate ngaySanXuat) {
        this.ngaySanXuat = ngaySanXuat;
    }

    public LocalDate getHanSuDung() {
        return hanSuDung;
    }

    public void setHanSuDung(LocalDate hanSuDung) {
        this.hanSuDung = hanSuDung;
    }

    public Double getGiaKhuyenMai() {
        return giaKhuyenMai;
    }

    public void setGiaKhuyenMai(Double giaKhuyenMai) {
        this.giaKhuyenMai = giaKhuyenMai;
    }

    public LocalDateTime getThoiGianBatDauKM() {
        return thoiGianBatDauKM;
    }

    public void setThoiGianBatDauKM(LocalDateTime thoiGianBatDauKM) {
        this.thoiGianBatDauKM = thoiGianBatDauKM;
    }

    public LocalDateTime getThoiGianKetThucKM() {
        return thoiGianKetThucKM;
    }

    public void setThoiGianKetThucKM(LocalDateTime thoiGianKetThucKM) {
        this.thoiGianKetThucKM = thoiGianKetThucKM;
    }

    public Integer getQuantityInStock() {
        return quantityInStock;
    }

    public void setQuantityInStock(Integer quantityInStock) {
        this.quantityInStock = quantityInStock;
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