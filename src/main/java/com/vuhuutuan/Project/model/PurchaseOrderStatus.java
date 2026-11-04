package com.vuhuutuan.Project.model;

public enum PurchaseOrderStatus {
    DRAFT("Chờ nhận hàng"),
    PARTIALLY_RECEIVED("Nhận một phần"),
    RECEIVED("Đã nhận hàng");

    private final String displayName;

    PurchaseOrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
