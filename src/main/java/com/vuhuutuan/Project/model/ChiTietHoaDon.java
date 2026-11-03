package com.vuhuutuan.Project.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chi_tiet_hoa_don")
public class ChiTietHoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ma_hoa_don")
    private HoaDon hoaDon;

    @ManyToOne
    @JoinColumn(name = "ma_mat_hang", nullable = false)
    private MatHang matHang;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "gia_tai_thoi_diem")
    private Double giaTaiThoiDiem;

    @Column(name = "gia_von_tai_thoi_diem")
    private Double giaVonTaiThoiDiem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_lo_hang")
    private ProductBatch loHang;

    // Hàm tính thành tiền (dùng để hiển thị ở giao diện)
    public Double getThanhTien() {
        Double donGia = giaTaiThoiDiem != null
                ? giaTaiThoiDiem
                : matHang.getGiaApDung(hoaDon.getThoiGianThanhToan() != null
                        ? hoaDon.getThoiGianThanhToan()
                        : LocalDateTime.now());
        return donGia * soLuong;
    }

    public Double getGiaVonTaiThoiDiem() {
        return giaVonTaiThoiDiem;
    }

    public void setGiaVonTaiThoiDiem(Double giaVonTaiThoiDiem) {
        this.giaVonTaiThoiDiem = giaVonTaiThoiDiem;
    }

    public ProductBatch getLoHang() {
        return loHang;
    }

    public void setLoHang(ProductBatch loHang) {
        this.loHang = loHang;
    }

    public Double getGiaTaiThoiDiem() {
        return giaTaiThoiDiem;
    }

    public void setGiaTaiThoiDiem(Double giaTaiThoiDiem) {
        this.giaTaiThoiDiem = giaTaiThoiDiem;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public HoaDon getHoaDon() {
        return hoaDon;
    }

    public void setHoaDon(HoaDon hoaDon) {
        this.hoaDon = hoaDon;
    }

    public MatHang getMatHang() {
        return matHang;
    }

    public void setMatHang(MatHang matHang) {
        this.matHang = matHang;
    }

    public Integer getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(Integer soLuong) {
        this.soLuong = soLuong;
    }
}
