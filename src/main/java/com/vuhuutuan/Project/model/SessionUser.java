package com.vuhuutuan.Project.model;

import java.io.Serializable;

public record SessionUser(Long id, String username, AccountRole role, String passwordHash) implements Serializable {
    public boolean isAdmin() {
        return role == AccountRole.ADMIN;
    }

    public boolean isCashier() {
        return role == AccountRole.CASHIER;
    }

    public boolean isWarehouse() {
        return role == AccountRole.WAREHOUSE;
    }

    public boolean isPurchasing() {
        return role == AccountRole.PURCHASING;
    }

    public boolean isManager() {
        return role == AccountRole.MANAGER;
    }
}
