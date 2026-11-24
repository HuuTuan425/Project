package com.vuhuutuan.Project.controller;

import com.vuhuutuan.Project.model.KhachHang;
import com.vuhuutuan.Project.model.LoaiKhachHang;
import com.vuhuutuan.Project.service.KhachHangService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;

@Controller
@RequestMapping("/khach-hang")
public class KhachHangController {
    private static final int CUSTOMER_PAGE_SIZE = 10;

    @Autowired
    private KhachHangService khachHangService;

    // Hiển thị danh sách
    @GetMapping
    public String danhSach(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        String search = q == null ? "" : q.trim();
        String searchPattern = search.isEmpty()
                ? null : "%" + search.toLowerCase(java.util.Locale.ROOT) + "%";
        int requestedPage = Math.max(0, page);
        Page<KhachHang> customerPage = khachHangService.searchCustomers(
                searchPattern,
                PageRequest.of(requestedPage, CUSTOMER_PAGE_SIZE,
                        Sort.by(Sort.Direction.ASC, "maKhachHang"))
        );
        int totalPages = Math.max(1, customerPage.getTotalPages());
        int currentPage = Math.min(requestedPage, totalPages - 1);
        if (currentPage != requestedPage) {
            customerPage = khachHangService.searchCustomers(
                    searchPattern,
                    PageRequest.of(currentPage, CUSTOMER_PAGE_SIZE,
                            Sort.by(Sort.Direction.ASC, "maKhachHang"))
            );
        }
        long totalCustomers = customerPage.getTotalElements();
        int pageStart = totalCustomers == 0 ? 0 : currentPage * CUSTOMER_PAGE_SIZE + 1;
        int pageEnd = (int) Math.min((long) (currentPage + 1) * CUSTOMER_PAGE_SIZE, totalCustomers);
        model.addAttribute("dsKhachHang", customerPage.getContent());
        model.addAttribute("q", search);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("pageStart", pageStart);
        model.addAttribute("pageEnd", pageEnd);
        return "khachhang/danhsach";
    }

    //  Mở form Thêm mới
    @GetMapping("/them")
    public String formThem(Model model, @RequestParam(required = false) String from) {
        KhachHang khachHang = new KhachHang();
        khachHang.setDiemThuong(0);
        model.addAttribute("khachHang", khachHang);
        model.addAttribute("loaiKhachHangs", Arrays.asList(LoaiKhachHang.values()));
        model.addAttribute("fromSale", "sale".equals(from));
        return "khachhang/form";
    }

    //  lưu Thêm mới
    @PostMapping("/them")
    public String them(
            @Valid @ModelAttribute("khachHang") KhachHang khachHang,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("loaiKhachHangs", Arrays.asList(LoaiKhachHang.values()));
            model.addAttribute("fromSale", isCashier(authentication));
            return "khachhang/form";
        }
        khachHang.setMaKhachHang(null);
        khachHang.setDiemThuong(0);
        khachHangService.save(khachHang);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm khách hàng.");
        if (isCashier(authentication)) {
            return "redirect:/hoa-don/lap?khachHangId=" + khachHang.getMaKhachHang();
        }
        return "redirect:/khach-hang";
    }

    //  form Sửa
    @GetMapping("/sua/{id}")
    public String formSua(@PathVariable("id") Long id, Model model) {
        model.addAttribute("khachHang", khachHangService.findById(id));
        model.addAttribute("loaiKhachHangs", Arrays.asList(LoaiKhachHang.values()));
        model.addAttribute("fromSale", false);
        return "khachhang/form";
    }

    // Xử lý lưu Sửa
    @PostMapping("/sua")
    public String sua(@ModelAttribute("khachHang") KhachHang khachHang) {
        khachHangService.save(khachHang);
        return "redirect:/khach-hang";
    }

    //  Xử lý Xóa
    @PostMapping("/xoa/{id}")
    public String xoa(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            khachHangService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã xóa mềm khách hàng; lịch sử hóa đơn vẫn được giữ.");
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/khach-hang";
    }

    private boolean isCashier(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CASHIER"));
    }
}