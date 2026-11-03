package com.vuhuutuan.Project.model;

public enum LoaiMatHang {
    THUC_PHAM("Thực phẩm"),
    HANG_THOI_TRANG("Hàng thời trang"),
    DIEN_MAY("Điện máy"),
    MY_PHAM("Mỹ phẩm");

    private final String displayName;

    LoaiMatHang(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}