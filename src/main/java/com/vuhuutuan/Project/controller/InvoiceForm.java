package com.vuhuutuan.Project.controller;

import java.util.ArrayList;
import java.util.List;

public class InvoiceForm {
    private Long customerId;
    private List<InvoiceLineForm> lines = new ArrayList<>();
    private Integer pointsToRedeem = 0;
    private com.vuhuutuan.Project.model.PaymentMethod paymentMethod;
    private Double cashTendered;
    private boolean nonCashPaymentConfirmed;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<InvoiceLineForm> getLines() {
        return lines;
    }

    public void setLines(List<InvoiceLineForm> lines) {
        this.lines = lines;
    }

    public Integer getPointsToRedeem() {
        return pointsToRedeem;
    }

    public void setPointsToRedeem(Integer pointsToRedeem) {
        this.pointsToRedeem = pointsToRedeem;
    }

    public com.vuhuutuan.Project.model.PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(com.vuhuutuan.Project.model.PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getCashTendered() {
        return cashTendered;
    }

    public void setCashTendered(Double cashTendered) {
        this.cashTendered = cashTendered;
    }

    public boolean isNonCashPaymentConfirmed() {
        return nonCashPaymentConfirmed;
    }

    public void setNonCashPaymentConfirmed(boolean nonCashPaymentConfirmed) {
        this.nonCashPaymentConfirmed = nonCashPaymentConfirmed;
    }
}
