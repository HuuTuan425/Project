package com.vuhuutuan.Project.repository;

import com.vuhuutuan.Project.model.MatHang;
import com.vuhuutuan.Project.model.PurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long> {
    boolean existsByProduct(MatHang product);
}
