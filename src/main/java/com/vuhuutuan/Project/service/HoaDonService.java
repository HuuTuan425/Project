package com.vuhuutuan.Project.service;

import com.vuhuutuan.Project.controller.InvoiceLineForm;
import com.vuhuutuan.Project.model.AppUser;
import com.vuhuutuan.Project.model.ChiTietHoaDon;
import com.vuhuutuan.Project.model.HoaDon;
import com.vuhuutuan.Project.model.KhachHang;
import com.vuhuutuan.Project.model.MatHang;
import com.vuhuutuan.Project.model.PaymentMethod;
import com.vuhuutuan.Project.model.ProductBatch;
import com.vuhuutuan.Project.repository.AppUserRepository;
import com.vuhuutuan.Project.repository.HoaDonRepository;
import com.vuhuutuan.Project.repository.KhachHangRepository;
import com.vuhuutuan.Project.repository.MatHangRepository;
import com.vuhuutuan.Project.repository.ProductBatchRepository;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

@Service
public class HoaDonService {
    private static final BigDecimal POINT_REDEMPTION_VALUE = BigDecimal.valueOf(1_000);
    private static final BigDecimal POINT_EARNING_THRESHOLD = BigDecimal.valueOf(10_000);

    private final HoaDonRepository hoaDonRepository;
    private final KhachHangRepository khachHangRepository;
    private final MatHangRepository matHangRepository;
    private final ProductBatchRepository productBatchRepository;
    private final AppUserRepository appUserRepository;

    public HoaDonService(
            HoaDonRepository hoaDonRepository,
            KhachHangRepository khachHangRepository,
            MatHangRepository matHangRepository,
            ProductBatchRepository productBatchRepository,
            AppUserRepository appUserRepository
    ) {
        this.hoaDonRepository = hoaDonRepository;
        this.khachHangRepository = khachHangRepository;
        this.matHangRepository = matHangRepository;
        this.productBatchRepository = productBatchRepository;
        this.appUserRepository = appUserRepository;
    }

    public List<HoaDon> findAll() {
        return hoaDonRepository.findAllByOrderByThoiGianThanhToanDesc();
    }

    @Transactional(readOnly = true)
    public Page<HoaDon> searchHistory(
            Long invoiceId,
            String customerName,
            LocalDateTime fromTime,
            LocalDateTime untilTime,
            Boolean cancelled,
            Pageable pageable
    ) {
        return hoaDonRepository.searchHistory(
                invoiceId, customerName, fromTime, untilTime, cancelled, pageable);
    }

    @Transactional(readOnly = true)
    public HoaDon findById(Long id) {
        return hoaDonRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy hóa đơn."));
    }

    @Transactional
    public HoaDon createInvoice(
            Long customerId,
            List<InvoiceLineForm> requestedLines,
            Integer requestedPoints,
            PaymentMethod paymentMethod,
            Double cashTendered,
            boolean nonCashPaymentConfirmed,
            String cashierUsername
    ) {
        if (customerId == null) {
            throw new IllegalArgumentException("Vui lòng chọn khách hàng.");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán.");
        }
        if (paymentMethod != PaymentMethod.TIEN_MAT && !nonCashPaymentConfirmed) {
            throw new IllegalArgumentException("Hãy xác nhận giao dịch ngoài hệ thống đã nhận thành công.");
        }
        if (requestedPoints == null || requestedPoints < 0) {
            throw new IllegalArgumentException("Số điểm dùng không hợp lệ.");
        }

        KhachHang customer = khachHangRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại."));
        AppUser cashier = appUserRepository.findByUsernameIgnoreCase(cashierUsername)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thu ngân đăng nhập."));
        if (requestedLines == null || requestedLines.isEmpty()) {
            throw new IllegalArgumentException("Hóa đơn cần ít nhất một mặt hàng.");
        }

        Map<Long, Integer> quantitiesByProduct = collectQuantities(requestedLines);
        Map<Long, MatHang> lockedProducts = new LinkedHashMap<>();
        quantitiesByProduct.keySet().stream().sorted(Comparator.naturalOrder())
                .forEach(id -> lockedProducts.put(id, matHangRepository.findByIdForUpdate(id)
                        .orElseThrow(() -> new IllegalArgumentException("Có mặt hàng không còn tồn tại."))));
        if (lockedProducts.values().stream().anyMatch(MatHang::isDeleted)) {
            throw new IllegalArgumentException("Không thể bán mặt hàng đã bị xóa.");
        }

        LocalDateTime paidAt = LocalDateTime.now();
        HoaDon invoice = new HoaDon();
        invoice.setKhachHang(customer);
        invoice.setThuNgan(cashier);
        invoice.setThoiGianThanhToan(paidAt);
        invoice.setPhuongThucThanhToan(paymentMethod);
        invoice.setDiemThuongDung(requestedPoints);

        List<ChiTietHoaDon> details = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantitiesByProduct.entrySet()) {
            MatHang product = lockedProducts.get(entry.getKey());
            int requestedQuantity = entry.getValue();
            if (product.getQuantityInStock() == null || product.getQuantityInStock() < requestedQuantity) {
                throw new IllegalArgumentException(
                        "Không đủ tồn kho cho mặt hàng \"" + product.getTenMatHang() + "\"."
                );
            }

            Double priceValue = product.getGiaApDung(paidAt);
            if (priceValue == null || !Double.isFinite(priceValue) || priceValue <= 0) {
                throw new IllegalArgumentException(
                        "Mặt hàng \"" + product.getTenMatHang() + "\" chưa có giá bán hợp lệ."
                );
            }
            BigDecimal unitPrice = BigDecimal.valueOf(priceValue).setScale(2, RoundingMode.HALF_UP);

            List<ProductBatch> batches = productBatchRepository.findSellableBatchesForUpdate(
                    product.getMaMatHang(), paidAt.toLocalDate());
            int sellableQuantity = batches.stream().mapToInt(ProductBatch::getRemainingQuantity).sum();
            if (sellableQuantity < requestedQuantity) {
                throw new IllegalArgumentException(
                        "Không đủ hàng chưa hết hạn cho mặt hàng \"" + product.getTenMatHang() + "\"."
                );
            }

            int remainingToAllocate = requestedQuantity;
            for (ProductBatch batch : batches) {
                if (remainingToAllocate == 0) {
                    break;
                }
                int allocated = Math.min(remainingToAllocate, batch.getRemainingQuantity());
                batch.setRemainingQuantity(batch.getRemainingQuantity() - allocated);
                productBatchRepository.save(batch);

                ChiTietHoaDon detail = new ChiTietHoaDon();
                detail.setHoaDon(invoice);
                detail.setMatHang(product);
                detail.setLoHang(batch);
                detail.setSoLuong(allocated);
                detail.setGiaTaiThoiDiem(unitPrice.doubleValue());
                detail.setGiaVonTaiThoiDiem(batch.getUnitCost());
                details.add(detail);
                remainingToAllocate -= allocated;
            }

            product.setQuantityInStock(product.getQuantityInStock() - requestedQuantity);
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(requestedQuantity)));
        }

        int availablePoints = customer.getDiemThuong() == null ? 0 : customer.getDiemThuong();
        if (requestedPoints > availablePoints) {
            throw new IllegalArgumentException("Khách hàng không đủ điểm thưởng.");
        }
        BigDecimal discount = POINT_REDEMPTION_VALUE.multiply(BigDecimal.valueOf(requestedPoints));
        if (discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("Số điểm dùng vượt quá tổng tiền hàng.");
        }

        BigDecimal amountDue = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tendered;
        BigDecimal change;
        if (paymentMethod == PaymentMethod.TIEN_MAT) {
            if (cashTendered == null || !Double.isFinite(cashTendered) || cashTendered < 0) {
                throw new IllegalArgumentException("Vui lòng nhập số tiền khách đưa.");
            }
            tendered = BigDecimal.valueOf(cashTendered).setScale(2, RoundingMode.HALF_UP);
            if (tendered.compareTo(amountDue) < 0) {
                throw new IllegalArgumentException("Tiền khách đưa chưa đủ thanh toán.");
            }
            change = tendered.subtract(amountDue);
        } else {
            tendered = amountDue;
            change = BigDecimal.ZERO;
        }

        int earnedPoints = amountDue.divide(POINT_EARNING_THRESHOLD, 0, RoundingMode.DOWN).intValueExact();
        customer.setDiemThuong(Math.addExact(availablePoints - requestedPoints, earnedPoints));
        invoice.setDiemThuongCong(earnedPoints);
        invoice.setTongTien(amountDue.doubleValue());
        invoice.setTienKhachTra(tendered.doubleValue());
        invoice.setTienThua(change.doubleValue());
        invoice.setChiTiet(details);
        return hoaDonRepository.save(invoice);
    }

    @Transactional
    public void cancelInvoice(Long invoiceId, String reason, String managerUsername) {
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isEmpty() || normalizedReason.length() > 500) {
            throw new IllegalArgumentException("Vui lòng nhập lý do hủy (tối đa 500 ký tự).");
        }

        HoaDon invoice = hoaDonRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy hóa đơn."));
        if (invoice.isCancelled()) {
            throw new IllegalStateException("Hóa đơn này đã được hủy trước đó.");
        }
        if (invoice.getChiTiet() == null || invoice.getChiTiet().isEmpty()) {
            throw new IllegalStateException("Không thể hủy hóa đơn không có chi tiết hàng hóa.");
        }
        if (invoice.getChiTiet().stream().anyMatch(detail -> detail.getLoHang() == null)) {
            throw new IllegalStateException(
                    "Không thể hoàn kho an toàn cho hóa đơn cũ không lưu thông tin lô hàng."
            );
        }

        KhachHang customer = invoice.getKhachHang() == null ? null
                : khachHangRepository.findByIdForUpdate(invoice.getKhachHang().getMaKhachHang())
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy khách hàng của hóa đơn."));
        AppUser manager = appUserRepository.findByUsernameIgnoreCase(managerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản lý."));

        Map<Long, MatHang> lockedProducts = new LinkedHashMap<>();
        invoice.getChiTiet().stream()
                .map(detail -> detail.getMatHang().getMaMatHang())
                .distinct()
                .sorted()
                .forEach(id -> lockedProducts.put(id, matHangRepository.findByIdForUpdate(id)
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy mặt hàng trong hóa đơn."))));

        Map<Long, Integer> returnedByBatch = new LinkedHashMap<>();
        Map<Long, Integer> returnedByProduct = new LinkedHashMap<>();
        for (ChiTietHoaDon detail : invoice.getChiTiet()) {
            int quantity = detail.getSoLuong() == null ? 0 : detail.getSoLuong();
            if (quantity <= 0) {
                throw new IllegalStateException("Chi tiết hóa đơn có số lượng không hợp lệ.");
            }
            Long batchId = detail.getLoHang().getId();
            Long productId = detail.getMatHang().getMaMatHang();
            if (!productId.equals(detail.getLoHang().getProduct().getMaMatHang())) {
                throw new IllegalStateException("Lô hàng không khớp mặt hàng trong hóa đơn.");
            }
            returnedByBatch.merge(batchId, quantity, Math::addExact);
            returnedByProduct.merge(productId, quantity, Math::addExact);
        }

        for (Map.Entry<Long, Integer> returned : returnedByBatch.entrySet()) {
            ProductBatch batch = productBatchRepository.findByIdForUpdate(returned.getKey())
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy lô hàng cần hoàn kho."));
            if (!returnedByProduct.containsKey(batch.getProduct().getMaMatHang())) {
                throw new IllegalStateException("Lô hàng không khớp mặt hàng trong hóa đơn.");
            }
            batch.setRemainingQuantity(Math.addExact(batch.getRemainingQuantity(), returned.getValue()));
        }
        for (Map.Entry<Long, Integer> returned : returnedByProduct.entrySet()) {
            MatHang product = lockedProducts.get(returned.getKey());
            int stock = product.getQuantityInStock() == null ? 0 : product.getQuantityInStock();
            product.setQuantityInStock(Math.addExact(stock, returned.getValue()));
        }

        if (customer != null) {
            int balance = customer.getDiemThuong() == null ? 0 : customer.getDiemThuong();
            int pointsEarned = invoice.getDiemThuongCong() == null ? 0 : invoice.getDiemThuongCong();
            int pointsRedeemed = invoice.getDiemThuongDung() == null ? 0 : invoice.getDiemThuongDung();
            long adjustedBalance = (long) balance - pointsEarned + pointsRedeemed;
            if (adjustedBalance < 0) {
                throw new IllegalStateException(
                        "Không thể hủy: khách hàng đã dùng số điểm được cộng từ hóa đơn này."
                );
            }
            customer.setDiemThuong(Math.toIntExact(adjustedBalance));
        }

        invoice.setCancelled(true);
        invoice.setCancelledAt(LocalDateTime.now());
        invoice.setCancelledBy(manager);
        invoice.setCancellationReason(normalizedReason);
        hoaDonRepository.save(invoice);
    }

    private Map<Long, Integer> collectQuantities(List<InvoiceLineForm> requestedLines) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        Set<Long> uniqueProducts = new HashSet<>();
        for (InvoiceLineForm line : requestedLines) {
            if (line == null || (line.getProductId() == null && line.getQuantity() == null)) {
                continue;
            }
            if (line.getProductId() == null || line.getQuantity() == null || line.getQuantity() <= 0) {
                throw new IllegalArgumentException("Mỗi mặt hàng được chọn phải có số lượng lớn hơn 0.");
            }
            if (!uniqueProducts.add(line.getProductId())) {
                throw new IllegalArgumentException("Không chọn trùng mặt hàng trong cùng hóa đơn.");
            }
            quantities.put(line.getProductId(), line.getQuantity());
        }
        if (quantities.isEmpty()) {
            throw new IllegalArgumentException("Hóa đơn cần ít nhất một mặt hàng.");
        }
        return quantities;
    }

    @Transactional(readOnly = true)
    public SalesSummary summarize(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        List<HoaDon> invoices = findAll().stream()
                .filter(invoice -> !invoice.isCancelled())
                .filter(invoice -> invoice.getThoiGianThanhToan() != null
                        && !invoice.getThoiGianThanhToan().isBefore(start)
                        && invoice.getThoiGianThanhToan().isBefore(endExclusive))
                .toList();

        BigDecimal revenue = invoices.stream()
                .map(HoaDon::getTongTien)
                .filter(total -> total != null)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal cost = invoices.stream()
                .filter(invoice -> invoice.getChiTiet() != null)
                .flatMap(invoice -> invoice.getChiTiet().stream())
                .filter(detail -> detail.getGiaVonTaiThoiDiem() != null)
                .map(detail -> BigDecimal.valueOf(detail.getGiaVonTaiThoiDiem())
                        .multiply(BigDecimal.valueOf(detail.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal costedRevenue = BigDecimal.ZERO;
        for (HoaDon invoice : invoices) {
            if (invoice.getChiTiet() == null || invoice.getTongTien() == null) {
                continue;
            }
            BigDecimal invoiceGross = invoice.getChiTiet().stream()
                    .filter(detail -> detail.getGiaTaiThoiDiem() != null)
                    .map(detail -> BigDecimal.valueOf(detail.getGiaTaiThoiDiem())
                            .multiply(BigDecimal.valueOf(detail.getSoLuong())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal costedGross = invoice.getChiTiet().stream()
                    .filter(detail -> detail.getGiaVonTaiThoiDiem() != null
                            && detail.getGiaTaiThoiDiem() != null)
                    .map(detail -> BigDecimal.valueOf(detail.getGiaTaiThoiDiem())
                            .multiply(BigDecimal.valueOf(detail.getSoLuong())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (invoiceGross.signum() > 0) {
                costedRevenue = costedRevenue.add(costedGross
                        .multiply(BigDecimal.valueOf(invoice.getTongTien()))
                        .divide(invoiceGross, 2, RoundingMode.HALF_UP));
            }
        }
        costedRevenue = costedRevenue.setScale(2, RoundingMode.HALF_UP);
        long detailsWithoutCost = invoices.stream()
                .filter(invoice -> invoice.getChiTiet() != null)
                .flatMap(invoice -> invoice.getChiTiet().stream())
                .filter(detail -> detail.getGiaVonTaiThoiDiem() == null)
                .count();
        Map<String, List<HoaDon>> invoicesByCashier = new TreeMap<>();
        for (HoaDon invoice : invoices) {
            String cashierName = invoice.getThuNgan() == null
                    ? "Không lưu (hóa đơn cũ)" : invoice.getThuNgan().getUsername();
            invoicesByCashier.computeIfAbsent(cashierName, ignored -> new ArrayList<>()).add(invoice);
        }
        List<CashierSales> cashierSales = invoicesByCashier.entrySet().stream()
                .map(entry -> {
                    AppUser cashier = entry.getValue().getFirst().getThuNgan();
                    String displayName = cashier == null ? entry.getKey() : cashier.getFullName();
                    if (displayName == null || displayName.isBlank()) {
                        displayName = entry.getKey();
                    }
                    return new CashierSales(
                            displayName,
                            entry.getValue().size(),
                            entry.getValue().stream()
                                    .map(HoaDon::getTongTien)
                                    .filter(total -> total != null)
                                    .map(BigDecimal::valueOf)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    );
                })
                .toList();
        return new SalesSummary(
                from, to, invoices.size(), revenue, cost, costedRevenue,
                costedRevenue.subtract(cost), detailsWithoutCost, cashierSales
        );
    }

    public record CashierSales(String cashier, long invoiceCount, BigDecimal revenue) {
    }

    public record SalesSummary(
            LocalDate from,
            LocalDate to,
            long invoiceCount,
            BigDecimal revenue,
            BigDecimal cost,
            BigDecimal costedRevenue,
            BigDecimal grossProfit,
            long detailsWithoutCost,
            List<CashierSales> cashierSales
    ) {
    }
}
