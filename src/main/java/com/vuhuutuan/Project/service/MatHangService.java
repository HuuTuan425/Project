package com.vuhuutuan.Project.service;

import com.vuhuutuan.Project.model.MatHang;
import com.vuhuutuan.Project.repository.MatHangRepository;
import com.vuhuutuan.Project.repository.ProductBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class MatHangService {

    private final MatHangRepository matHangRepository;
    private final ProductBatchRepository productBatchRepository;

    public MatHangService(
            MatHangRepository matHangRepository,
            ProductBatchRepository productBatchRepository
    ) {
        this.matHangRepository = matHangRepository;
        this.productBatchRepository = productBatchRepository;
    }

    public List<MatHang> findAll() {
        return matHangRepository.findByDeletedAtIsNullOrderByTenMatHangAsc();
    }

    public List<MatHang> searchProducts(Long productId, String query) {
        return matHangRepository.searchProducts(productId, query);
    }

    public MatHang findById(Long id) {
        return matHangRepository.findByMaMatHangAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy mặt hàng."));
    }

    public List<MatHang> findAvailable() {
        return findAll().stream()
                .filter(matHang -> matHang.getQuantityInStock() != null && matHang.getQuantityInStock() > 0)
                .filter(matHang -> productBatchRepository.hasSellableStock(
                        matHang.getMaMatHang(), java.time.LocalDate.now()))
                .toList();
    }

    public List<MatHang> searchAvailable(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        return matHangRepository.findByDeletedAtIsNullAndTenMatHangContainingIgnoreCaseOrderByTenMatHangAsc(
                        normalizedQuery)
                .stream()
                .filter(matHang -> matHang.getQuantityInStock() != null && matHang.getQuantityInStock() > 0)
                .filter(matHang -> productBatchRepository.hasSellableStock(
                        matHang.getMaMatHang(), java.time.LocalDate.now()))
                .toList();
    }

    public void save(MatHang matHang) {
        matHangRepository.save(matHang);
    }

    @Transactional
    public void deleteById(Long id) {
        MatHang matHang = matHangRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy mặt hàng."));
        if (matHang.isDeleted()) {
            throw new IllegalStateException("Mặt hàng đã được xóa.");
        }
        matHang.setDeletedAt(LocalDateTime.now());
        matHangRepository.save(matHang);
    }
}