package com.vuhuutuan.Project.controller;

import java.util.ArrayList;
import java.util.List;

public class PurchaseOrderForm {
    private Long supplierId;
    private List<PurchaseOrderLineForm> lines = new ArrayList<>();

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public List<PurchaseOrderLineForm> getLines() {
        return lines;
    }

    public void setLines(List<PurchaseOrderLineForm> lines) {
        this.lines = lines;
    }
}
