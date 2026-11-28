package com.vuhuutuan.Project.model;
public enum LoaiKhachHang {
    KHACH_HANG_THONG_THUONG("Khách hàng thông thường"),
    KHACH_HANG_THANH_VIEN("Khách hàng thành viên");

    private final String displayName;

    LoaiKhachHang(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}