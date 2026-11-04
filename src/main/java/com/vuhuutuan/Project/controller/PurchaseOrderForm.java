package com.vuhuutuan.Project.controller;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class PurchaseOrderForm {
    private Long supplierId;
    private LocalDate expectedDeliveryDate;
    private String paymentTerms;
    private List<PurchaseOrderLineForm> lines = new ArrayList<>();

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public List<PurchaseOrderLineForm> getLines() {
        return lines;
    }

    public void setLines(List<PurchaseOrderLineForm> lines) {
        this.lines = lines;
    }
}
