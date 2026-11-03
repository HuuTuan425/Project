package com.vuhuutuan.Project.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "hoa_don")
public class HoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_hoa_don")
    private Long maHoaDon;

    @ManyToOne
    @JoinColumn(name = "ma_khach_hang")
    private KhachHang khachHang;

    @Column(name = "thoi_gian_thanh_toan")
    private LocalDateTime thoiGianThanhToan;

    @Column(name = "tong_tien")
    private Double tongTien;

    @Column(name = "diem_thuong_cong")
    private Integer diemThuongCong;

    @Column(name = "diem_thuong_dung", nullable = false)
    private Integer diemThuongDung = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "phuong_thuc_thanh_toan", nullable = false, length = 20)
    private PaymentMethod phuongThucThanhToan;

    @Column(name = "tien_khach_tra")
    private Double tienKhachTra;

    @Column(name = "tien_thua")
    private Double tienThua;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_thu_ngan")
    private AppUser thuNgan;

    @Column(name = "da_huy", nullable = false)
    private boolean cancelled;

    @Column(name = "ngay_huy")
    private LocalDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_huy")
    private AppUser cancelledBy;

    @Column(name = "ly_do_huy", length = 500)
    private String cancellationReason;

    // Liên kết 1-N với ChiTietHoaDon
    @OneToMany(mappedBy = "hoaDon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChiTietHoaDon> chiTiet;

    public Long getMaHoaDon() {
        return maHoaDon;
    }

    public void setMaHoaDon(Long maHoaDon) {
        this.maHoaDon = maHoaDon;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }

    public LocalDateTime getThoiGianThanhToan() {
        return thoiGianThanhToan;
    }

    public void setThoiGianThanhToan(LocalDateTime thoiGianThanhToan) {
        this.thoiGianThanhToan = thoiGianThanhToan;
    }

    public Double getTongTien() {
        return tongTien;
    }

    public void setTongTien(Double tongTien) {
        this.tongTien = tongTien;
    }

    public Integer getDiemThuongCong() {
        return diemThuongCong;
    }

    public void setDiemThuongCong(Integer diemThuongCong) {
        this.diemThuongCong = diemThuongCong;
    }

    public Integer getDiemThuongDung() {
        return diemThuongDung;
    }

    public void setDiemThuongDung(Integer diemThuongDung) {
        this.diemThuongDung = diemThuongDung;
    }

    public PaymentMethod getPhuongThucThanhToan() {
        return phuongThucThanhToan;
    }

    public void setPhuongThucThanhToan(PaymentMethod phuongThucThanhToan) {
        this.phuongThucThanhToan = phuongThucThanhToan;
    }

    public Double getTienKhachTra() {
        return tienKhachTra;
    }

    public void setTienKhachTra(Double tienKhachTra) {
        this.tienKhachTra = tienKhachTra;
    }

    public Double getTienThua() {
        return tienThua;
    }

    public void setTienThua(Double tienThua) {
        this.tienThua = tienThua;
    }

    public AppUser getThuNgan() {
        return thuNgan;
    }

    public void setThuNgan(AppUser thuNgan) {
        this.thuNgan = thuNgan;
    }

    public List<ChiTietHoaDon> getChiTiet() {
        return chiTiet;
    }

    public void setChiTiet(List<ChiTietHoaDon> chiTiet) {
        this.chiTiet = chiTiet;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public AppUser getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(AppUser cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}