package com.vuhuutuan.Project.controller;

import com.vuhuutuan.Project.model.MatHang;
import com.vuhuutuan.Project.model.PurchaseOrder;
import com.vuhuutuan.Project.model.PurchaseOrderStatus;
import com.vuhuutuan.Project.model.Supplier;
import com.vuhuutuan.Project.repository.MatHangRepository;
import com.vuhuutuan.Project.service.PurchasingService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.LocalDate;

@Controller
@RequestMapping("/nhap-hang")
public class PurchasingController {
    private static final int PURCHASE_ORDER_PAGE_SIZE = 10;

    private final PurchasingService purchasingService;
    private final MatHangRepository matHangRepository;

    public PurchasingController(PurchasingService purchasingService, MatHangRepository matHangRepository) {
        this.purchasingService = purchasingService;
        this.matHangRepository = matHangRepository;
    }

    @GetMapping("/nha-cung-cap")
    public String suppliers(Model model) {
        model.addAttribute("suppliers", purchasingService.findSuppliers());
        model.addAttribute("supplierForm", new SupplierForm());
        return "nhaphang/nhacungcap";
    }

    @GetMapping("/nha-cung-cap/{id}/sua")
    public String editSupplier(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Supplier supplier = purchasingService.findSupplier(id);
            SupplierForm form = new SupplierForm();
            form.setId(supplier.getId());
            form.setName(supplier.getName());
            form.setPhone(supplier.getPhone());
            form.setEmail(supplier.getEmail());
            form.setAddress(supplier.getAddress());
            model.addAttribute("suppliers", purchasingService.findSuppliers());
            model.addAttribute("supplierForm", form);
            return "nhaphang/nhacungcap";
        } catch (java.util.NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/nhap-hang/nha-cung-cap";
        }
    }

    @PostMapping("/nha-cung-cap")
    public String createSupplier(
            @Valid @ModelAttribute("supplierForm") SupplierForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("suppliers", purchasingService.findSuppliers());
            return "nhaphang/nhacungcap";
        }
        Supplier supplier = new Supplier();
        supplier.setName(form.getName().trim());
        supplier.setPhone(form.getPhone());
        supplier.setEmail(form.getEmail());
        supplier.setAddress(form.getAddress());
        if (form.getId() == null) {
            purchasingService.createSupplier(supplier);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm nhà cung cấp.");
        } else {
            try {
                purchasingService.updateSupplier(form.getId(), supplier);
                redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật nhà cung cấp.");
            } catch (java.util.NoSuchElementException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            }
        }
        return "redirect:/nhap-hang/nha-cung-cap";
    }

    @PostMapping("/nha-cung-cap/{id}/xoa")
    public String deleteSupplier(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            purchasingService.deleteSupplier(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa nhà cung cấp.");
        } catch (IllegalStateException | java.util.NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/nhap-hang/nha-cung-cap";
    }

    @GetMapping
    public String purchaseOrders(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LocalDate tuNgay,
            @RequestParam(required = false) LocalDate denNgay,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        String search = q == null ? "" : q.trim();
        String statusFilter = trangThai == null ? "" : trangThai.trim();
        boolean invalidFilter = false;
        if (!statusFilter.isEmpty()
                && !statusFilter.equals(PurchaseOrderStatus.DRAFT.name())
                && !statusFilter.equals(PurchaseOrderStatus.RECEIVED.name())) {
            model.addAttribute("errorMessage", "Trạng thái lọc không hợp lệ.");
            invalidFilter = true;
        }
        if (tuNgay != null && denNgay != null && tuNgay.isAfter(denNgay)) {
            model.addAttribute("errorMessage", "Từ ngày phải trước hoặc bằng đến ngày.");
            invalidFilter = true;
        }

        Long purchaseOrderId = null;
        if (!search.isEmpty()) {
            try {
                purchaseOrderId = Long.valueOf(search);
            } catch (NumberFormatException ignored) {
                // Non-numeric searches are matched against supplier names.
            }
        }
        String supplierName = search.isEmpty() ? null : "%" + search.toLowerCase(Locale.ROOT) + "%";
        PurchaseOrderStatus status = statusFilter.isEmpty() || invalidFilter
                ? null : PurchaseOrderStatus.valueOf(statusFilter);
        int requestedPage = Math.max(0, page);
        int currentPage = 0;
        int totalPages = 1;
        long totalOrders = 0;
        List<PurchaseOrder> pageOrders = List.of();
        if (!invalidFilter) {
            Page<PurchaseOrder> orderPage = purchasingService.searchPurchaseOrders(
                    purchaseOrderId,
                    supplierName,
                    tuNgay == null ? null : tuNgay.atStartOfDay(),
                    denNgay == null ? null : denNgay.plusDays(1).atStartOfDay(),
                    status,
                    PageRequest.of(requestedPage, PURCHASE_ORDER_PAGE_SIZE,
                            Sort.by(Sort.Direction.DESC, "createdAt"))
            );
            totalPages = Math.max(1, orderPage.getTotalPages());
            currentPage = Math.min(requestedPage, totalPages - 1);
            if (currentPage != requestedPage) {
                orderPage = purchasingService.searchPurchaseOrders(
                        purchaseOrderId,
                        supplierName,
                        tuNgay == null ? null : tuNgay.atStartOfDay(),
                        denNgay == null ? null : denNgay.plusDays(1).atStartOfDay(),
                        status,
                        PageRequest.of(currentPage, PURCHASE_ORDER_PAGE_SIZE,
                                Sort.by(Sort.Direction.DESC, "createdAt"))
                );
            }
            totalOrders = orderPage.getTotalElements();
            pageOrders = orderPage.getContent();
        }
        int pageStart = totalOrders == 0 ? 0 : currentPage * PURCHASE_ORDER_PAGE_SIZE + 1;
        int pageEnd = (int) Math.min((long) (currentPage + 1) * PURCHASE_ORDER_PAGE_SIZE, totalOrders);
        model.addAttribute("purchaseOrders", pageOrders);
        model.addAttribute("q", search);
        model.addAttribute("tuNgay", tuNgay);
        model.addAttribute("denNgay", denNgay);
        model.addAttribute("trangThai", statusFilter);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pageStart", pageStart);
        model.addAttribute("pageEnd", pageEnd);
        return "nhaphang/danhsach";
    }

    @GetMapping("/tao")
    public String createForm(Model model) {
        model.addAttribute("purchaseOrderForm", new PurchaseOrderForm());
        addPurchaseFormOptions(model);
        return "nhaphang/form";
    }

    @PostMapping
    public String create(
            @ModelAttribute("purchaseOrderForm") PurchaseOrderForm form,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Long creatorId = purchasingService.findAccountId(authentication.getName());
            var purchaseOrder = purchasingService.createDraft(form.getSupplierId(), creatorId, form.getLines());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã tạo phiếu nhập ở trạng thái chờ nhận hàng.");
            return "redirect:/nhap-hang/" + purchaseOrder.getId();
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            addPurchaseFormOptions(model);
            return "nhaphang/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("purchaseOrder", purchasingService.findPurchaseOrder(id));
            return "nhaphang/chitiet";
        } catch (java.util.NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/nhap-hang";
        }
    }

    @PostMapping("/{id}/nhan")
    public String receive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            purchasingService.receive(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã nhận hàng và cập nhật tồn kho theo lô.");
        } catch (IllegalStateException | java.util.NoSuchElementException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/nhap-hang/" + id;
    }

    private void addPurchaseFormOptions(Model model) {
        PurchaseOrderForm form = (PurchaseOrderForm) model.getAttribute("purchaseOrderForm");
        if (form == null) {
            form = new PurchaseOrderForm();
            form.setLines(new ArrayList<>(List.of(new PurchaseOrderLineForm())));
            model.addAttribute("purchaseOrderForm", form);
        } else if (form.getLines() == null || form.getLines().isEmpty()) {
            form.setLines(new ArrayList<>(List.of(new PurchaseOrderLineForm())));
        }
        List<MatHang> products = matHangRepository.findAll();
        model.addAttribute("suppliers", purchasingService.findSuppliers());
        model.addAttribute("matHangs", products);
    }
}
