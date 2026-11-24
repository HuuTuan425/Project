package com.vuhuutuan.Project.model;
public enum LoaiKhachHang {
    MUA_LE("Khách mua lẻ"),
    MUA_BUON("Khách mua buôn");

    private final String displayName;

    LoaiKhachHang(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}