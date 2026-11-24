package com.vuhuutuan.Project.service;

import com.vuhuutuan.Project.model.KhachHang;
import com.vuhuutuan.Project.repository.KhachHangRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KhachHangService {

    @Autowired
    private KhachHangRepository khachHangRepository;

    public List<KhachHang> findAll() {
        return khachHangRepository.findAll().stream()
                .filter(khachHang -> !khachHang.isDeleted())
                .toList();
    }

    public Page<KhachHang> searchCustomers(String search, Pageable pageable) {
        return khachHangRepository.searchCustomers(search, pageable);
    }

    public KhachHang findById(Long id) {
        return khachHangRepository.findById(id)
                .filter(khachHang -> !khachHang.isDeleted())
                .orElse(null);
    }

    public void save(KhachHang khachHang) {
        khachHangRepository.save(khachHang);
    }

    @Transactional
    public void deleteById(Long id) {
        KhachHang khachHang = khachHangRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy khách hàng."));
        if (khachHang.isDeleted()) {
            throw new IllegalStateException("Khách hàng đã được xóa.");
        }
        khachHang.setDeletedAt(LocalDateTime.now());
        khachHangRepository.save(khachHang);
    }
}
