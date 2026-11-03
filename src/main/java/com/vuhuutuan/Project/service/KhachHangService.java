package com.vuhuutuan.Project.service;

import com.vuhuutuan.Project.model.KhachHang;
import com.vuhuutuan.Project.repository.KhachHangRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KhachHangService {

    @Autowired
    private KhachHangRepository khachHangRepository;

    public List<KhachHang> findAll() {
        return khachHangRepository.findAll();
    }

    public Page<KhachHang> searchCustomers(String search, Pageable pageable) {
        return khachHangRepository.searchCustomers(search, pageable);
    }

    public KhachHang findById(Long id) {
        return khachHangRepository.findById(id).orElse(null);
    }

    public void save(KhachHang khachHang) {
        khachHangRepository.save(khachHang);
    }

    public void deleteById(Long id) {
        khachHangRepository.deleteById(id);
    }
}
