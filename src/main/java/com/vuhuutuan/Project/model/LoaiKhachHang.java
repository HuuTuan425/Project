package com.vuhuutuan.Project.model;
public enum LoaiKhachHang {
    MUA_LE("Khách hàng thông thường"),
    MUA_BUON("Khách hàng thành viên");

    private final String displayName;

    LoaiKhachHang(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}