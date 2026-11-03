package com.vuhuutuan.Project.service;

import com.vuhuutuan.Project.controller.PurchaseOrderLineForm;
import com.vuhuutuan.Project.model.AppUser;
import com.vuhuutuan.Project.model.MatHang;
import com.vuhuutuan.Project.model.ProductBatch;
import com.vuhuutuan.Project.model.PurchaseOrder;
import com.vuhuutuan.Project.model.PurchaseOrderLine;
import com.vuhuutuan.Project.model.PurchaseOrderStatus;
import com.vuhuutuan.Project.model.Supplier;
import com.vuhuutuan.Project.repository.AppUserRepository;
import com.vuhuutuan.Project.repository.MatHangRepository;
import com.vuhuutuan.Project.repository.ProductBatchRepository;
import com.vuhuutuan.Project.repository.PurchaseOrderRepository;
import com.vuhuutuan.Project.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class PurchasingService {
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MatHangRepository matHangRepository;
    private final ProductBatchRepository productBatchRepository;
    private final AppUserRepository appUserRepository;

    public PurchasingService(
            SupplierRepository supplierRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            MatHangRepository matHangRepository,
            ProductBatchRepository productBatchRepository,
            AppUserRepository appUserRepository
    ) {
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.matHangRepository = matHangRepository;
        this.productBatchRepository = productBatchRepository;
        this.appUserRepository = appUserRepository;
    }

    public List<Supplier> findSuppliers() {
        return supplierRepository.findAllByOrderByNameAsc();
    }

    public List<PurchaseOrder> findPurchaseOrders() {
        return purchaseOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrder> searchPurchaseOrders(
            Long purchaseOrderId,
            String supplierName,
            LocalDateTime fromTime,
            LocalDateTime untilTime,
            PurchaseOrderStatus status,
            Pageable pageable
    ) {
        return purchaseOrderRepository.searchHistory(
                purchaseOrderId, supplierName, fromTime, untilTime, status, pageable);
    }

    public Long findAccountId(String username) {
        return appUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy tài khoản đăng nhập."))
                .getId();
    }

    public PurchaseOrder findPurchaseOrder(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiếu nhập."));
    }

    public Supplier createSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    public Supplier findSupplier(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy nhà cung cấp."));
    }

    @Transactional
    public Supplier updateSupplier(Long supplierId, Supplier updates) {
        Supplier supplier = findSupplier(supplierId);
        supplier.setName(updates.getName());
        supplier.setPhone(updates.getPhone());
        supplier.setEmail(updates.getEmail());
        supplier.setAddress(updates.getAddress());
        return supplierRepository.save(supplier);
    }

    @Transactional
    public void deleteSupplier(Long supplierId) {
        Supplier supplier = findSupplier(supplierId);
        if (purchaseOrderRepository.existsBySupplier_Id(supplierId)) {
            throw new IllegalStateException(
                    "Không thể xóa nhà cung cấp đã có phiếu nhập. Hãy giữ lại để bảo toàn lịch sử."
            );
        }
        supplierRepository.delete(supplier);
    }

    @Transactional
    public PurchaseOrder createDraft(Long supplierId, Long creatorId, List<PurchaseOrderLineForm> requestedLines) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Nhà cung cấp không tồn tại."));
        AppUser creator = appUserRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản tạo phiếu không tồn tại."));
        if (requestedLines == null || requestedLines.isEmpty()) {
            throw new IllegalArgumentException("Phiếu nhập cần ít nhất một dòng hàng.");
        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setCreatedBy(creator);
        purchaseOrder.setCreatedAt(LocalDateTime.now());
        purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT);
        BigDecimal totalCost = BigDecimal.ZERO;
        List<PurchaseOrderLine> lines = new ArrayList<>();

        for (PurchaseOrderLineForm requested : requestedLines) {
            if (requested == null || (requested.getProductId() == null && requested.getQuantity() == null
                    && requested.getUnitCost() == null)) {
                continue;
            }
            if (requested.getProductId() == null || requested.getQuantity() == null
                    || requested.getQuantity() <= 0 || requested.getUnitCost() == null
                    || !Double.isFinite(requested.getUnitCost()) || requested.getUnitCost() <= 0) {
                throw new IllegalArgumentException("Mỗi dòng nhập phải có mặt hàng, số lượng và giá nhập hợp lệ.");
            }
            if (requested.getManufactureDate() != null && requested.getExpiryDate() != null
                    && requested.getExpiryDate().isBefore(requested.getManufactureDate())) {
                throw new IllegalArgumentException("Hạn sử dụng không được trước ngày sản xuất.");
            }
            if (requested.getExpiryDate() != null && requested.getExpiryDate().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Không thể nhập lô hàng đã hết hạn sử dụng.");
            }

            MatHang product = matHangRepository.findById(requested.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Có mặt hàng không còn tồn tại."));
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setPurchaseOrder(purchaseOrder);
            line.setProduct(product);
            line.setQuantity(requested.getQuantity());
            line.setUnitCost(BigDecimal.valueOf(requested.getUnitCost())
                    .setScale(2, RoundingMode.HALF_UP).doubleValue());
            line.setManufactureDate(requested.getManufactureDate());
            line.setExpiryDate(requested.getExpiryDate());
            lines.add(line);
            totalCost = totalCost.add(BigDecimal.valueOf(line.getUnitCost())
                    .multiply(BigDecimal.valueOf(line.getQuantity())));
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Phiếu nhập cần ít nhất một dòng hàng.");
        }

        purchaseOrder.setLines(lines);
        purchaseOrder.setTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP).doubleValue());
        return purchaseOrderRepository.save(purchaseOrder);
    }

    @Transactional
    public void receive(Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdForUpdate(purchaseOrderId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phiếu nhập."));
        if (purchaseOrder.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Phiếu nhập này đã được nhận trước đó.");
        }

        Map<Long, MatHang> lockedProducts = new LinkedHashMap<>();
        purchaseOrder.getLines().stream()
                .map(line -> line.getProduct().getMaMatHang())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .forEach(id -> lockedProducts.put(id, matHangRepository.findByIdForUpdate(id)
                        .orElseThrow(() -> new IllegalStateException("Mặt hàng trong phiếu nhập không còn tồn tại."))));

        for (PurchaseOrderLine line : purchaseOrder.getLines()) {
            MatHang product = lockedProducts.get(line.getProduct().getMaMatHang());
            product.setQuantityInStock(Math.addExact(product.getQuantityInStock(), line.getQuantity()));
            ProductBatch batch = new ProductBatch();
            batch.setProduct(product);
            batch.setPurchaseOrder(purchaseOrder);
            batch.setManufactureDate(line.getManufactureDate());
            batch.setExpiryDate(line.getExpiryDate());
            batch.setUnitCost(line.getUnitCost());
            batch.setInitialQuantity(line.getQuantity());
            batch.setRemainingQuantity(line.getQuantity());
            productBatchRepository.save(batch);
        }
        purchaseOrder.setReceivedAt(LocalDateTime.now());
        purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
        purchaseOrderRepository.save(purchaseOrder);
    }
}
