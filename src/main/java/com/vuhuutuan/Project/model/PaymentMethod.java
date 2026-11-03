package com.vuhuutuan.Project.model;

public enum PaymentMethod {
    TIEN_MAT("Tiền mặt"),
    CHUYEN_KHOAN("Chuyển khoản"),
    VIET_QR("VietQR"),
    MOMO("MoMo"),
    ZALOPAY("ZaloPay");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
