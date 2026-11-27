package com.vuhuutuan.Project.controller;

import com.vuhuutuan.Project.model.KhachHang;
import com.vuhuutuan.Project.model.HoaDon;
import com.vuhuutuan.Project.model.MatHang;
import com.vuhuutuan.Project.model.PaymentMethod;
import com.vuhuutuan.Project.model.SessionUser;
import com.vuhuutuan.Project.config.SessionAuthInterceptor;
import com.vuhuutuan.Project.service.HoaDonService;
import com.vuhuutuan.Project.service.KhachHangService;
import com.vuhuutuan.Project.service.MatHangService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Arrays;
import java.util.Locale;

@Controller
@RequestMapping("/hoa-don")
public class HoaDonController {
    private static final int INVOICE_PAGE_SIZE = 10;

    private final HoaDonService hoaDonService;
    private final KhachHangService khachHangService;
    private final MatHangService matHangService;

    public HoaDonController(
            HoaDonService hoaDonService,
            KhachHangService khachHangService,
            MatHangService matHangService
    ) {
        this.hoaDonService = hoaDonService;
        this.khachHangService = khachHangService;
        this.matHangService = matHangService;
    }

    @GetMapping
    public String danhSach(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LocalDate tuNgay,
            @RequestParam(required = false) LocalDate denNgay,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        String search = q == null ? "" : q.trim();
        String status = trangThai == null ? "" : trangThai.trim();
        boolean invalidFilter = false;
        if (!status.isEmpty() && !status.equals("PAID") && !status.equals("CANCELLED")) {
            model.addAttribute("errorMessage", "Trạng thái lọc không hợp lệ.");
            invalidFilter = true;
        }
        if (tuNgay != null && denNgay != null && tuNgay.isAfter(denNgay)) {
            model.addAttribute("errorMessage", "Từ ngày phải trước hoặc bằng đến ngày.");
            invalidFilter = true;
        }

        Long invoiceId = null;
        if (!search.isEmpty()) {
            try {
                invoiceId = Long.valueOf(search);
            } catch (NumberFormatException ignored) {
                // Non-numeric searches are matched against customer names.
            }
        }
        String customerName = search.isEmpty() ? null : "%" + search.toLowerCase(Locale.ROOT) + "%";
        Boolean cancelled = status.isEmpty() ? null : status.equals("CANCELLED");
        int requestedPage = Math.max(0, page);
        Page<HoaDon> invoicePage = null;
        int currentPage = 0;
        int totalPages = 1;
        long totalInvoices = 0;
        List<HoaDon> pageInvoices = List.of();
        if (!invalidFilter) {
            invoicePage = hoaDonService.searchHistory(
                    invoiceId,
                    customerName,
                    tuNgay == null ? null : tuNgay.atStartOfDay(),
                    denNgay == null ? null : denNgay.plusDays(1).atStartOfDay(),
                    cancelled,
                    PageRequest.of(requestedPage, INVOICE_PAGE_SIZE,
                            Sort.by(Sort.Direction.DESC, "thoiGianThanhToan"))
            );
            totalPages = Math.max(1, invoicePage.getTotalPages());
            currentPage = Math.min(requestedPage, totalPages - 1);
            if (currentPage != requestedPage) {
                invoicePage = hoaDonService.searchHistory(
                        invoiceId,
                        customerName,
                        tuNgay == null ? null : tuNgay.atStartOfDay(),
                        denNgay == null ? null : denNgay.plusDays(1).atStartOfDay(),
                        cancelled,
                        PageRequest.of(currentPage, INVOICE_PAGE_SIZE,
                                Sort.by(Sort.Direction.DESC, "thoiGianThanhToan"))
                );
            }
            totalInvoices = invoicePage.getTotalElements();
            pageInvoices = invoicePage.getContent();
        }
        int pageStart = totalInvoices == 0 ? 0 : currentPage * INVOICE_PAGE_SIZE + 1;
        int pageEnd = (int) Math.min((long) (currentPage + 1) * INVOICE_PAGE_SIZE, totalInvoices);
        model.addAttribute("dsHoaDon", pageInvoices);
        model.addAttribute("q", search);
        model.addAttribute("tuNgay", tuNgay);
        model.addAttribute("denNgay", denNgay);
        model.addAttribute("trangThai", status);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalInvoices", totalInvoices);
        model.addAttribute("pageStart", pageStart);
        model.addAttribute("pageEnd", pageEnd);
        return "hoadon/danhsach";
    }

    @GetMapping("/lap")
    public String formLap(
            @RequestParam(required = false) Long khachHangId,
            Model model
    ) {
        model.addAttribute("invoiceForm", new InvoiceForm());
        ((InvoiceForm) model.getAttribute("invoiceForm")).setCustomerId(khachHangId);
        addInvoiceOptions(model);
        return "hoadon/form";
    }

    @PostMapping("/lap")
    public String lap(
            @ModelAttribute("invoiceForm") InvoiceForm invoiceForm,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            var invoice = hoaDonService.createInvoice(
                    invoiceForm.getCustomerId(),
                    invoiceForm.getLines(),
                    invoiceForm.getPointsToRedeem(),
                    invoiceForm.getPaymentMethod(),
                    invoiceForm.getCashTendered(),
                    invoiceForm.isNonCashPaymentConfirmed(),
                    currentUsername(session)
            );
            redirectAttributes.addFlashAttribute("successMessage", "Đã lập hóa đơn thành công.");
            return "redirect:/hoa-don/" + invoice.getMaHoaDon();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            addInvoiceOptions(model);
            return "hoadon/form";
        }
    }

    @GetMapping("/bao-cao")
    public String baoCao(
            @RequestParam(required = false) LocalDate tuNgay,
            @RequestParam(required = false) LocalDate denNgay,
            Model model
    ) {
        LocalDate today = LocalDate.now();
        LocalDate from = tuNgay == null ? today.withDayOfMonth(1) : tuNgay;
        LocalDate to = denNgay == null ? today : denNgay;
        model.addAttribute("tuNgay", from);
        model.addAttribute("denNgay", to);
        if (from.isAfter(to)) {
            model.addAttribute("errorMessage", "Từ ngày phải trước hoặc bằng đến ngày.");
            return "hoadon/baocao";
        }
        model.addAttribute("summary", hoaDonService.summarize(from, to));
        return "hoadon/baocao";
    }

    @GetMapping("/{id}")
    public String chiTiet(
            @PathVariable Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            var invoice = hoaDonService.findById(id);
            if (!isAdmin(session) && !isManager(session)
                    && (invoice.getThuNgan() == null
                    || !invoice.getThuNgan().getUsername().equalsIgnoreCase(currentUsername(session)))) {
                return "redirect:/khong-co-quyen";
            }
            model.addAttribute("hoaDon", invoice);
            return "hoadon/chitiet";
        } catch (NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/hoa-don";
        }
    }

    @PostMapping("/{id}/huy")
    public String huy(
            @PathVariable Long id,
            @RequestParam String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            hoaDonService.cancelInvoice(id, reason, currentUsername(session));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã hủy hóa đơn, hoàn lại tồn kho và điều chỉnh điểm thưởng.");
        } catch (IllegalArgumentException | IllegalStateException | NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/hoa-don/" + id;
    }

    private void addInvoiceOptions(Model model) {
        List<InvoiceLineForm> lines = new ArrayList<>();
        InvoiceLineForm initialLine = new InvoiceLineForm();
        initialLine.setQuantity(1);
        lines.add(initialLine);
        InvoiceForm form = (InvoiceForm) model.getAttribute("invoiceForm");
        if (form == null) {
            form = new InvoiceForm();
        }
        if (form.getLines() == null || form.getLines().isEmpty()) {
            form.setLines(lines);
        }
        List<KhachHang> customers = khachHangService.findAll();
        List<MatHang> products = matHangService.findAvailable();
        LocalDateTime priceTime = LocalDateTime.now();
        Map<Long, Double> productPrices = new HashMap<>();
        for (MatHang product : products) {
            Double price = product.getGiaApDung(priceTime);
            productPrices.put(product.getMaMatHang(),
                    price != null && Double.isFinite(price) && price > 0 ? price : null);
        }
        model.addAttribute("khachHangs", customers);
        model.addAttribute("matHangs", products);
        model.addAttribute("giaMatHangs", productPrices);
        model.addAttribute("paymentMethods", Arrays.asList(PaymentMethod.values()));
        int redeemablePoints = 0;
        for (KhachHang customer : customers) {
            if (customer.getMaKhachHang().equals(form.getCustomerId())) {
                redeemablePoints = customer.getDiemThuong() == null ? 0 : customer.getDiemThuong();
                break;
            }
        }
        model.addAttribute("redeemablePoints", redeemablePoints);
    }

    private boolean isAdmin(HttpSession session) {
        SessionUser user = (SessionUser) session.getAttribute(SessionAuthInterceptor.SESSION_USER_ATTRIBUTE);
        return user != null && user.isAdmin();
    }

    private boolean isManager(HttpSession session) {
        SessionUser user = (SessionUser) session.getAttribute(SessionAuthInterceptor.SESSION_USER_ATTRIBUTE);
        return user != null && user.isManager();
    }

    private String currentUsername(HttpSession session) {
        SessionUser user = (SessionUser) session.getAttribute(SessionAuthInterceptor.SESSION_USER_ATTRIBUTE);
        return user == null ? "" : user.username();
    }
}
