package com.vuhuutuan.Project.service;

import com.vuhuutuan.Project.model.MatHang;
import com.vuhuutuan.Project.repository.MatHangRepository;
import com.vuhuutuan.Project.repository.ProductBatchRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MatHangExpiryTest {
    private final MatHangRepository products = mock(MatHangRepository.class);
    private final ProductBatchRepository batches = mock(ProductBatchRepository.class);
    private final MatHangService service = new MatHangService(products, batches);

    @Test
    void showsDaysUntilAndAfterExpiry() {
        LocalDate today = LocalDate.of(2026, 11, 25);
        MatHang first = new MatHang();
        first.setMaMatHang(1L);
        MatHang second = new MatHang();
        second.setMaMatHang(2L);
        MatHang third = new MatHang();
        third.setMaMatHang(3L);
        MatHang noDate = new MatHang();
        noDate.setMaMatHang(4L);

        ProductBatchRepository.NearestExpiry future = expiry(1L, today.plusDays(5));
        ProductBatchRepository.NearestExpiry dueToday = expiry(2L, today);
        ProductBatchRepository.NearestExpiry past = expiry(3L, today.minusDays(2));
        when(batches.findNearestExpiryDates(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(List.of(future, dueToday, past));

        var result = service.nearestExpiryDates(List.of(first, second, third, noDate), today);

        assertEquals(5, result.get(1L).daysRemaining());
        assertEquals(today.plusDays(5), result.get(1L).date());
        assertEquals(0, result.get(2L).daysRemaining());
        assertEquals(-2, result.get(3L).daysRemaining());
        assertTrue(!result.containsKey(4L));
    }

    @Test
    void skipsQueryForEmptyProductList() {
        assertTrue(service.nearestExpiryDates(List.of(), LocalDate.now()).isEmpty());
        verifyNoInteractions(batches);
    }

    private ProductBatchRepository.NearestExpiry expiry(Long productId, LocalDate date) {
        ProductBatchRepository.NearestExpiry result = mock(ProductBatchRepository.NearestExpiry.class);
        when(result.getProductId()).thenReturn(productId);
        when(result.getExpiryDate()).thenReturn(date);
        return result;
    }
}
