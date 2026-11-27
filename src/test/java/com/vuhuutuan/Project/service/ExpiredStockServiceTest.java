package com.vuhuutuan.Project.service;

import com.vuhuutuan.Project.model.MatHang;
import com.vuhuutuan.Project.model.ProductBatch;
import com.vuhuutuan.Project.repository.MatHangRepository;
import com.vuhuutuan.Project.repository.ProductBatchRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpiredStockServiceTest {
    private final MatHangRepository products = mock(MatHangRepository.class);
    private final ProductBatchRepository batches = mock(ProductBatchRepository.class);
    private final ExpiredStockService service = new ExpiredStockService(products, batches);

    @Test
    void removesOnlyQuantitiesReturnedAsExpiredAndDoesNotRemoveThemTwice() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        MatHang product = new MatHang();
        product.setQuantityInStock(10);
        ProductBatch expired = new ProductBatch();
        expired.setRemainingQuantity(4);
        ProductBatch valid = new ProductBatch();
        valid.setRemainingQuantity(6);

        when(batches.findProductIdsWithExpiredStock(today)).thenReturn(List.of(1L));
        when(products.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(batches.findExpiredBatchesForUpdate(1L, today)).thenReturn(List.of(expired));

        assertEquals(4, service.removeExpiredStock(today));
        assertEquals(6, product.getQuantityInStock());
        assertEquals(0, expired.getRemainingQuantity());
        assertEquals(6, valid.getRemainingQuantity());
        when(batches.findProductIdsWithExpiredStock(today)).thenReturn(List.of());
        assertEquals(0, service.removeExpiredStock(today));
        assertEquals(6, product.getQuantityInStock());
    }

    @Test
    void refusesToRemoveExpiredStockWhenTotalStockIsInconsistent() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        MatHang product = new MatHang();
        product.setQuantityInStock(2);
        ProductBatch expired = new ProductBatch();
        expired.setRemainingQuantity(3);

        when(batches.findProductIdsWithExpiredStock(today)).thenReturn(List.of(1L));
        when(products.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(batches.findExpiredBatchesForUpdate(1L, today)).thenReturn(List.of(expired));

        assertThrows(IllegalStateException.class, () -> service.removeExpiredStock(today));
        assertEquals(2, product.getQuantityInStock());
        assertEquals(3, expired.getRemainingQuantity());
    }
}
