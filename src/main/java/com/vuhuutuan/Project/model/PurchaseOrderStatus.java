package com.vuhuutuan.Project.model;

public enum PurchaseOrderStatus {
    DRAFT("Chờ nhận hàng"),
    RECEIVED("Đã nhận hàng");

    private final String displayName;

    PurchaseOrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
