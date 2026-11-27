package com.vuhuutuan.Project.service;

import com.vuhuutuan.Project.model.MatHang;
import com.vuhuutuan.Project.model.ProductBatch;
import com.vuhuutuan.Project.repository.MatHangRepository;
import com.vuhuutuan.Project.repository.ProductBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ExpiredStockService {
    private final MatHangRepository matHangRepository;
    private final ProductBatchRepository productBatchRepository;

    public ExpiredStockService(
            MatHangRepository matHangRepository,
            ProductBatchRepository productBatchRepository
    ) {
        this.matHangRepository = matHangRepository;
        this.productBatchRepository = productBatchRepository;
    }

    @Transactional
    public int removeExpiredStock(LocalDate today) {
        int totalExpired = 0;
        for (Long productId : productBatchRepository.findProductIdsWithExpiredStock(today)) {
            MatHang product = matHangRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy mặt hàng có lô hết hạn: " + productId));
            List<ProductBatch> batches = productBatchRepository.findExpiredBatchesForUpdate(productId, today);
            int expiredQuantity = 0;
            for (ProductBatch batch : batches) {
                expiredQuantity = Math.addExact(expiredQuantity, batch.getRemainingQuantity());
            }
            Integer stock = product.getQuantityInStock();
            if (stock == null || stock < expiredQuantity) {
                throw new IllegalStateException("Tồn kho không đủ để trừ hàng hết hạn của mặt hàng: " + productId);
            }
            for (ProductBatch batch : batches) {
                batch.setRemainingQuantity(0);
            }
            product.setQuantityInStock(stock - expiredQuantity);
            totalExpired = Math.addExact(totalExpired, expiredQuantity);
        }
        return totalExpired;
    }
}
