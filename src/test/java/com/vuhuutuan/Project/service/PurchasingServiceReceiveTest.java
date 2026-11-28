package com.vuhuutuan.Project.service;

import com.vuhuutuan.Project.model.PurchaseOrder;
import com.vuhuutuan.Project.model.PurchaseOrderLine;
import com.vuhuutuan.Project.model.PurchaseOrderStatus;
import com.vuhuutuan.Project.repository.AppUserRepository;
import com.vuhuutuan.Project.repository.MatHangRepository;
import com.vuhuutuan.Project.repository.ProductBatchRepository;
import com.vuhuutuan.Project.repository.PurchaseOrderRepository;
import com.vuhuutuan.Project.repository.SupplierProposalRepository;
import com.vuhuutuan.Project.repository.SupplierRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PurchasingServiceReceiveTest {
    @Test
    void rejectsNegativeAndMoreThanRemainingWithSameMessage() {
        var orders = mock(PurchaseOrderRepository.class);
        var products = mock(MatHangRepository.class);
        var batches = mock(ProductBatchRepository.class);
        var service = new PurchasingService(
                mock(SupplierRepository.class), mock(SupplierProposalRepository.class),
                orders, products, batches, mock(AppUserRepository.class));
        var line = new PurchaseOrderLine();
        line.setQuantity(10);
        line.setReceivedQuantity(4);
        var order = new PurchaseOrder();
        order.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        order.setLines(List.of(line));
        when(orders.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        for (int quantity : List.of(-1, 7, 11)) {
            var error = assertThrows(IllegalArgumentException.class,
                    () -> service.receive(1L, List.of(quantity)));
            assertEquals("Số lượng không hợp lệ.", error.getMessage());
        }
        assertEquals(4, line.getReceivedQuantity());
        verifyNoInteractions(products, batches);
    }
}
