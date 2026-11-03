package com.vuhuutuan.Project.repository;

import com.vuhuutuan.Project.model.ChiTietHoaDon;
import com.vuhuutuan.Project.model.MatHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChiTietHoaDonRepository extends JpaRepository<ChiTietHoaDon, Long> {
    boolean existsByMatHang(MatHang matHang);
}