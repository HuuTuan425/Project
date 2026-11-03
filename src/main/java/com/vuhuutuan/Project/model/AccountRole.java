package com.vuhuutuan.Project.model;

public enum AccountRole {
    ADMIN("Quản trị viên"),
    CASHIER("Nhân viên bán hàng"),
    WAREHOUSE("Thủ kho"),
    MANAGER("Quản lý cửa hàng"),
    KHAC("Tài khoản khác (không còn phân quyền)");

    private final String displayName;

    AccountRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
