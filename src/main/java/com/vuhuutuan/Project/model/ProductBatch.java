package com.vuhuutuan.Project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "lo_hang")
public class ProductBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_lo_hang")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_mat_hang", nullable = false)
    private MatHang product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phieu_nhap", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "ngay_san_xuat")
    private LocalDate manufactureDate;

    @Column(name = "han_su_dung")
    private LocalDate expiryDate;

    @Column(name = "gia_nhap", nullable = false)
    private Double unitCost;

    @Column(name = "so_luong_ban_dau", nullable = false)
    private Integer initialQuantity;

    @Column(name = "so_luong_con_lai", nullable = false)
    private Integer remainingQuantity;

    public Long getId() {
        return id;
    }

    public MatHang getProduct() {
        return product;
    }

    public void setProduct(MatHang product) {
        this.product = product;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public void setManufactureDate(LocalDate manufactureDate) {
        this.manufactureDate = manufactureDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Double getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Double unitCost) {
        this.unitCost = unitCost;
    }

    public Integer getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(Integer initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public Integer getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(Integer remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }
}
