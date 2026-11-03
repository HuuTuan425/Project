package com.vuhuutuan.Project.service;

import com.vuhuutuan.Project.model.MatHang;
import com.vuhuutuan.Project.repository.ChiTietHoaDonRepository;
import com.vuhuutuan.Project.repository.MatHangRepository;
import com.vuhuutuan.Project.repository.ProductBatchRepository;
import com.vuhuutuan.Project.repository.PurchaseOrderLineRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class MatHangService {

    private final MatHangRepository matHangRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final ProductBatchRepository productBatchRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;

    public MatHangService(
            MatHangRepository matHangRepository,
            ChiTietHoaDonRepository chiTietHoaDonRepository,
            ProductBatchRepository productBatchRepository,
            PurchaseOrderLineRepository purchaseOrderLineRepository
    ) {
        this.matHangRepository = matHangRepository;
        this.chiTietHoaDonRepository = chiTietHoaDonRepository;
        this.productBatchRepository = productBatchRepository;
        this.purchaseOrderLineRepository = purchaseOrderLineRepository;
    }

    public List<MatHang> findAll() {
        return matHangRepository.findAll();
    }

    public List<MatHang> searchProducts(Long productId, String query) {
        return matHangRepository.searchProducts(productId, query);
    }

    public MatHang findById(Long id) {
        return matHangRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy mặt hàng."));
    }

    public List<MatHang> findAvailable() {
        return matHangRepository.findAll().stream()
                .filter(matHang -> matHang.getQuantityInStock() != null && matHang.getQuantityInStock() > 0)
                .filter(matHang -> productBatchRepository.hasSellableStock(
                        matHang.getMaMatHang(), java.time.LocalDate.now()))
                .toList();
    }

    public List<MatHang> searchAvailable(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        return matHangRepository.findByTenMatHangContainingIgnoreCaseOrderByTenMatHangAsc(normalizedQuery)
                .stream()
                .filter(matHang -> matHang.getQuantityInStock() != null && matHang.getQuantityInStock() > 0)
                .filter(matHang -> productBatchRepository.hasSellableStock(
                        matHang.getMaMatHang(), java.time.LocalDate.now()))
                .toList();
    }

    public void save(MatHang matHang) {
        matHangRepository.save(matHang);
    }

    public void deleteById(Long id) {
        MatHang matHang = findById(id);
        if (chiTietHoaDonRepository.existsByMatHang(matHang)
                || purchaseOrderLineRepository.existsByProduct(matHang)) {
            throw new IllegalStateException("Mặt hàng đã có giao dịch, không thể xóa.");
        }
        matHangRepository.deleteById(id);
    }
}